/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.core;

import java.util.Date;

import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.SelectivePullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.handlers.CatchAxisFault.NonPersistedErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.AlreadyChangedException;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPayloadEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;

/**
 * Is a facade to the {@link IUpdateManager} of the <i>Persistency Provider</i> to facilitate updating the meta-data of 
 * message units. 
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class StorageManager {

    /**
     * The update manager provided by the Persistency Provider which does the "real" work of storing the data
     */
    protected IUpdateManager  parent;

    /**
     * Creates a new facade to the given update manager of the persistency provider so other Core classes can update the
     * meta-data of a message unit.
     *
     * @param parent    The update manager from the persistency provider
     */
    public StorageManager(final IUpdateManager parent) {
        this.parent = parent;
    }

    /**
     * Creates a new persistency object to store the meta-data of the given message unit that is received by Holodeck
     * B2B. Will set the direction as IN and assign the RECEIVED processing state if no state has been assigned yet. 
     *
     * @param <T>           Limits the <code>messageUnit</code> to message units types
     * @param <V>           Only entity objects will be returned, V and T will be of the same message type
     * @param messageUnit   The meta-data on message unit that should be stored in the new persistent object
     * @return              The created persistency object.
     * @throws PersistenceException        If an error occurs when saving the new message unit to the database.
     */
    @SuppressWarnings("unchecked")
	public <T extends IMessageUnit, V extends IMessageUnitEntity> V storeIncomingMessageUnit(T messageUnit)
                                                                                           throws PersistenceException {
        MessageUnit tempObject = createMutableObject(messageUnit);
        // Set correct direction
        tempObject.setDirection(Direction.IN);
        if (tempObject.getCurrentProcessingState() == null) 
        	tempObject.setProcessingState(ProcessingState.RECEIVED);
        try {
			return (V) parent.storeMessageUnit(tempObject);
		} catch (DuplicateMessageIdException e) {
			// This exception should never be thrown for received messages
			throw new IllegalStateException("Illegal duplicate check exception!");
		}
    }

    /**
     * Creates a new persistency object to store the meta-data of the given message unit that is sent by Holodeck
     * B2B.
     *
     * @param <T>           Limits the <code>messageUnit</code> to message units types
     * @param <V>           Only entity objects will be returned, V and T will be of the same message type
     * @param messageUnit   The meta-data on message unit that should be stored in the new persistent object
     * @return              The created persistency object.
     * @throws DuplicateMessageIdException When the MessageId of the message unit already exists.  
     * @throws PersistenceException        If an error occurs when saving the new message unit to the database.
     */
    @SuppressWarnings("unchecked")
	public <T extends IMessageUnit, V extends IMessageUnitEntity> V storeOutGoingMessageUnit(T messageUnit)
                                                            throws DuplicateMessageIdException, PersistenceException {
        MessageUnit mutableObject = createMutableObject(messageUnit);
        // Set correct direction
        mutableObject.setDirection(Direction.OUT);
        if (messageUnit instanceof IUserMessage || messageUnit instanceof IPullRequest)
        	mutableObject.setProcessingState(ProcessingState.SUBMITTED);        	
        else
            mutableObject.setProcessingState(ProcessingState.CREATED);

        if (Utils.isNullOrEmpty(mutableObject.getMessageId()))
            mutableObject.setMessageId(MessageIdUtils.createMessageId());
        if (mutableObject.getTimestamp() == null)
            mutableObject.setTimestamp(new Date());

        return (V) parent.storeMessageUnit(mutableObject);
    }

    /**
     * Sets the ID of the P-Mode that defines how the message unit should be processed.
     *
     * @param msgUnit   The entity object representing the message unit
     * @param pmodeId   The ID of the P-Mode that defines how the message unit must be processed
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								   entity object is updated to the latest meta-data available.
     * @throws PersistenceException    If some other error occured when saving the updated message unit to the database
     */
    public void setPModeId(final IMessageUnitEntity msgUnit, final String pmodeId) throws PersistenceException {
		msgUnit.setPModeId(pmodeId);
		saveMessageUnit(msgUnit);
    }

    /**
     * Sets the ID of the P-Mode and [label of] the Leg that define how the Error Message unit should be processed.
     *
     * @param msgUnit   The entity object representing the Error Message unit
     * @param pl   		Pair consisting of the P-Mode and label of the Leg that govern the processing of the Error 
     * 					Message 
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								   entity object is updated to the latest meta-data available.
     * @throws PersistenceException    If some other error occured when saving the updated message unit to the database
     * @since 6.0.0
     */
    public void setPModeAndLeg(final IErrorMessageEntity msgUnit, final Pair<IPMode, ILeg.Label> pl) 
    																					throws PersistenceException {
    	msgUnit.setPModeId(pl.value1().getId());
    	msgUnit.setLeg(pl.value2());
    	saveMessageUnit(msgUnit);
    }
    
    /**
     * Updates the processing state of the given message unit to the specified state, setting the start time of the new 
     * state to the current time. Returns a boolean indicating whether the message unit's processing state was updated
     * successfully. In case the state could not be updated, the entity object will contain the latest version of the
     * meta-data.
     * 
     * @param msgUnit           The entity object representing the message unit
     * @param newProcState      The new processing state
     * @return                  <code>true</code> if the processing state has been updated,<br>
     *                          <code>false</code> if the processing state was not updated because the current 
     *                          processing state has already changed by another thread
     * @throws PersistenceException When a problem occurs updating the processing state of the message unit
     * @since 7.0.0 returns result of the processing state update
     */
    public boolean setProcessingState(final IMessageUnitEntity msgUnit, final ProcessingState newProcState)
                                                                                        throws PersistenceException {
        return this.setProcessingState(msgUnit, newProcState, null);
    }
    
    /**
     * Updates the processing state of the given message unit to the specified state and description, setting the start 
     * time of the new state to the current time. Returns a boolean indicating whether the message unit's processing 
     * state was updated successfully. In case the state could not be updated, the entity object will contain the latest 
     * version of the meta-data.
     *
     * @param msgUnit           The entity object representing the message unit
     * @param newProcState      The new processing state
     * @param description		The additional description for the new processing state
     * @return                  <code>true</code> if the processing state has been updated,<br>
     *                          <code>false</code> if the processing state was not updated because the current 
     *                          processing state has already changed by another thread
     * @throws PersistenceException When a problem occurs updating the processing state of the message unit
     * @since 7.0.0 removed parameter for current state
     */
    public boolean setProcessingState(final IMessageUnitEntity msgUnit, final ProcessingState newProcState,
    							   	  final String description) throws PersistenceException {
    	final ProcessingState cState = msgUnit.getCurrentProcessingState().getState(); 
		try {			
			msgUnit.setProcessingState(newProcState, description);
			saveMessageUnit(msgUnit);
			return true;
		} catch (AlreadyChangedException changed) {
			// This probably indicates that the processing state has already been changed
			if (cState != msgUnit.getCurrentProcessingState().getState())
				return false;
			else
				throw changed;
		}    
    }
    
    /**
     * Sets the multi-hop indicator of the message unit.
     *
     * @param msgUnit       The entity object representing the message unit which multi-hop indicator should be set
     * @param isMultihop    The indicator whether this message unit uses multi-hop
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								   entity object is updated to the latest meta-data available.
     * @throws PersistenceException    If some other error occured when saving the updated message unit to the database
     */
    public void setMultiHop(final IMessageUnitEntity msgUnit, final boolean isMultihop) throws PersistenceException {
        msgUnit.setMultiHop(isMultihop);
        saveMessageUnit(msgUnit);
    }

    /**
     * Sets the indicator on an error message whether a SOAP Fault should be added to the message when sending the
     * error message.
     * <p>Note that the decision whether the SOAP Fault will be added to the message is taken when the actual message
     * is constructed and depends also on the other message units that are included in the same message.
     *
     * @param errorMessage      The entity object representing the Error Message
     * @param addSOAPFault      The indicator whether to add a SOAP fault.
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								   entity object is updated to the latest meta-data available.
     * @throws PersistenceException    If some other error occured when saving the updated message unit to the database
     */
    public void setAddSOAPFault(final IErrorMessageEntity errorMessage, final boolean addSOAPFault)
                                                                                        throws PersistenceException {
        errorMessage.setAddSOAPFault(addSOAPFault);
        saveMessageUnit(errorMessage);
    }

    /**
     * Helper method to save the message unit data to the database. In case there is a problem in the persistency layer,
     * the Holodeck B2B Core will use an in-memory Error Message entity object to be able to still respond to the 
     * sending MSH. Updates to this non persisted Error Message should however not be passed to the persistency 
     * provider.  
     * 
     * @param m the entity object to be saved to the database
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								   entity object is updated to the latest meta-data available.
     * @throws PersistenceException    If some other error occured when saving the updated message unit to the database
     */
    private void saveMessageUnit(IMessageUnitEntity m) throws AlreadyChangedException, PersistenceException {
    	if (!(m instanceof NonPersistedErrorMessage))
    		parent.updateMessageUnit(m);    	
    }

    /**
     * Updates the information on the payload.
     *
     * @param payloadInfo   The updated meta-data on the payload which must be persisted
     * @throws PersistenceException     If an error occurs when saving the payload meta-data to the database
     */
    public void setPayloadInformation(final IPayloadEntity payloadInfo) throws PersistenceException {
        parent.updatePayload(payloadInfo);        
    }
    
    /**
     * Deletes the meta-data of the given message unit from the database.
     *
     * @param messageUnit       The {@link IMessageUnitEntity} object to be deleted
     * @throws PersistenceException     When a problem occurs while removing the message unit from the database.
     */
    public void deleteMessageUnit(IMessageUnitEntity messageUnit) throws PersistenceException {
        parent.deleteMessageUnit(messageUnit);
    }

    /**
     * Helper method to create a temporary message unit object to enable setting of the correct processing state and
     * generation of a messageId.
     *
     * @param messageUnit   The message unit that needs to be stored
     * @return              A {@link MessageUnit} representing the given message unit
     */
    private MessageUnit createMutableObject(IMessageUnit messageUnit) {
        if (messageUnit instanceof MessageUnit)
            return (MessageUnit) messageUnit;

        if (messageUnit instanceof IUserMessage)
            return new UserMessage((IUserMessage) messageUnit);
        else if (messageUnit instanceof ISelectivePullRequest)
            return new SelectivePullRequest((ISelectivePullRequest) messageUnit);
        else if (messageUnit instanceof IPullRequest)
            return new PullRequest((IPullRequest) messageUnit);
        else if (messageUnit instanceof IReceipt)
            return new Receipt((IReceipt) messageUnit);
        else
            return new ErrorMessage((IErrorMessage) messageUnit);
    }
}

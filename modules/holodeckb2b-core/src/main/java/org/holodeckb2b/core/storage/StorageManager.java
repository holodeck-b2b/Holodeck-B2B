/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.events.impl.MessagePurgeFailure;
import org.holodeckb2b.common.events.impl.MessageUnitPurged;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.SelectivePullRequest;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.AlreadyChangedException;
import org.holodeckb2b.interfaces.storage.providers.IMetadataStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.IPayloadStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;

/**
 * Is a facade to the {@link IMetadataStorageProvider} and {@link IPayloadStorageProvider} that provides a unified
 * interface to Core components to manage the data of message units.
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class StorageManager {
	private static final Logger	log = LogManager.getLogger();
	
	/**
	 * The Metadata Storage Provider in use for storing the message meta-data
	 */
	private final IMetadataStorageProvider	mdsProvider;

	/**
	 * The Payload Storage Provider in use for storing the message meta-data
	 */
	private final IPayloadStorageProvider	psProvider;
	
    /**
     * Creates a new facade to the given Metadata and Payload Storage Providers so other Core classes can update the
     * data of message units.
     *
     * @param mdsp    The Metadata Storage Provider in use
     * @param psp     The Payload Storage Provider in use
     */
    public StorageManager(final IMetadataStorageProvider mdsp, final IPayloadStorageProvider psp) {
        this.mdsProvider = mdsp;
        this.psProvider = psp;
    }

    /**
     * Stores the meta-data of a received message unit. The processing state of the new entity object will be set to 
     * {@linkplain ProcessingState#CREATED}.
     *
     * @param <T>           Limits the <code>messageUnit</code> to message units types
     * @param <V>           Only entity objects will be returned, V and T will be of the same message type
     * @param messageUnit   The meta-data on message unit that should be stored
     * @return              The created entity object.
     * @throws StorageException If an error occurs when saving the new message unit to the database.
     */
	@SuppressWarnings("unchecked")
	public <T extends IMessageUnit, V extends IMessageUnitEntity> V storeReceivedMessageUnit(T messageUnit)
                                                                                           	throws StorageException {
		log.trace("Store received {} with MessageId={}", MessageUnitUtils.getMessageUnitName(messageUnit),
					messageUnit.getMessageId());
		// We must be able to set at least the direction, and possibly the state, so create a mutable version
		MessageUnit tempObject = createMutableObject(messageUnit);
		tempObject.setDirection(Direction.IN);
		tempObject.setProcessingState(ProcessingState.RECEIVED);
		try {
			V entity = mdsProvider.storeMessageUnit(tempObject);
			log.debug("Stored received {} with MessageId={}", MessageUnitUtils.getMessageUnitName(messageUnit),
						messageUnit.getMessageId());
			if (entity instanceof IUserMessageEntity)
				entity = (V) new UserMessageEntityProxy((IUserMessageEntity) entity);
			return entity;
		} catch (DuplicateMessageIdException invalidDupCheck) {
			log.error("The Metadata Storage Provder ({}) executed duplicate check on incoming message!");
			throw new IllegalStateException("Illegal duplicate check exception!");
		} catch (StorageException saveFailed) {
			log.error("Error saving meta-data of received message unit (msgId={}) : {}", messageUnit.getMessageId(),
						Utils.getExceptionTrace(saveFailed));
			throw saveFailed;
		}
    }
     
    /**
     * Stores the meta-data of a message unit to be sent. If the message unit has not yet been assigned a MessageId or
     * time stamp, they will be assigned. Also if no processing state is set, it is set to {@linkplain 
     * ProcessingState#SUBMITTED} for User Messages and Pull Requests or {@linkplain ProcessingState#CREATED} for 
     * Receipt Messages.<br/>
     * If the message to store is a User Message, the manager will also store the payload content if not already stored.
     * <p>
     * NOTE: To store a new Error Message, the {@link #createErrorMsgFor(Collection, IMessageUnitEntity)} method should
     * be used.
     *
     * @param <T>           Limits the <code>messageUnit</code> to message units types
     * @param <V>           Only entity objects will be returned, V and T will be of the same message type
     * @param messageUnit   The message unit that should be stored 
     * @return              The created entity object containing the meta-data of the message unit
     * @throws DuplicateMessageIdException When the MessageId of the message unit already exists.  
     * @throws StorageException  If an error occurs when storing the meta-data or the payload data (for a User Message) 
     * 							 of the message unit 
     */
	public <T extends IMessageUnit, V extends IMessageUnitEntity> V storeOutGoingMessageUnit(T messageUnit)
                                                            	throws DuplicateMessageIdException, StorageException {
		log.trace("Store outgoing {} with MessageId={}", MessageUnitUtils.getMessageUnitName(messageUnit),
					messageUnit.getMessageId());
		// We must be able to set at least the direction, and possibly the state, so create a mutable version
		MessageUnit mutableObject = createMutableObject(messageUnit);
		// Set correct direction
		mutableObject.setDirection(Direction.OUT);
		if (Utils.isNullOrEmpty(messageUnit.getProcessingStates())) {
			if (messageUnit instanceof IUserMessage || messageUnit instanceof IPullRequest)
				mutableObject.setProcessingState(ProcessingState.SUBMITTED);
			else
				mutableObject.setProcessingState(ProcessingState.CREATED);
		}
		if (Utils.isNullOrEmpty(mutableObject.getMessageId()))
			mutableObject.setMessageId(createMessageId());
		if (mutableObject.getTimestamp() == null)
			mutableObject.setTimestamp(new Date());
		
		V entity;
		try {
			entity = mdsProvider.storeMessageUnit(mutableObject);
		} catch (StorageException saveFailed) {
			log.error("Error saving meta-data of received message unit (msgId={}) : {}", messageUnit.getMessageId(),
						Utils.getExceptionTrace(saveFailed));
			throw saveFailed;
		}		
		
		if (entity instanceof IUserMessage) {
			UserMessageEntityProxy proxy = new UserMessageEntityProxy((IUserMessageEntity) entity);			
			log.trace("Check if payload data is already saved");
			final Collection<? extends IPayload> srcPayloads = ((IUserMessage) messageUnit).getPayloads();
			IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(messageUnit.getPModeId());
			for(PayloadEntityProxy p : proxy.getPayloads()) {
				IPayloadContent content = psProvider.getPayloadContent(p.getPayloadId());
				if (content == null) {
					log.trace("Store content of payload (URI={})", p.getPayloadURI());
					content = psProvider.createNewPayloadStorage(p.getPayloadId(), pmode, Direction.OUT);
					try(OutputStream contentStream = content.openStorage()) {
						Utils.copyStream(srcPayloads.stream()
											.filter(pl -> Utils.nullSafeEqual(pl.getPayloadURI(), p.getPayloadURI()))
											.findFirst().get().getContent(), 
										contentStream);						
						log.debug("Saved content of payload (URI={})", p.getPayloadURI());
					} catch (IOException plFailure) {
						log.error("Could not save content of payload (URI={}) of User Message (msgId={}) : {}",
									p.getPayloadURI(), messageUnit.getMessageId(), Utils.getExceptionTrace(plFailure));
						entity.setProcessingState(ProcessingState.FAILURE, "Payload storage error: URI=" 
																+ p.getPayloadURI() + " - " + plFailure.getMessage());
						mdsProvider.updateMessageUnit(entity);
						throw new StorageException("Could not save payload content", plFailure);
					}
				} else 
					log.debug("Payload content already stored");
				p.setContent(content);
			}
		}
		log.debug("Stored new {} with MessageId={}", MessageUnitUtils.getMessageUnitName(messageUnit), 
					entity.getMessageId());
		return entity;
    }
    
    /**
     * Creates a new Error Signal Message with the specified errors and stores the meta-data in the Holodeck B2B 
     * database. The processing state of the new message unit will be set to {@linkplain ProcessingState#CREATED}.  
     * <p>When the message in error is specified all errors must either reference this message or contain no reference. 
     * The new error message unit will automatically be set as related to the message in error.
     * 
     * @param errors		errors to include in the Error Message
     * @param msgInError	message unit in error, may be <code>null</code> if there is no specific message in error
     * @return	the create entity object 
     * @throws StorageException when an error occurs creating or saving the new entity object. Is also thrown when the
     * 							given collection of errors do not consistently reference the message in error  
     */
    public IErrorMessageEntity createErrorMsgFor(final Collection<IEbmsError> errors, 
    											 final IMessageUnitEntity msgInError) throws StorageException {
    	log.trace("Creating new Error Message");
    	ErrorMessage errMsg = new ErrorMessage(errors);
    	errMsg.setMessageId(createMessageId());
    	errMsg.setProcessingState(ProcessingState.CREATED);
    	
    	if (msgInError != null) {
    		log.trace("Check errors reference message in error");
    		final String refToMessageId = msgInError.getMessageId();
		    final Iterator<IEbmsError> it = errors.iterator();
	        boolean consistent = true;
	        while (it.hasNext() && consistent) {
	            final String errorRefTo = it.next().getRefToMessageInError();
	            consistent = Utils.isNullOrEmpty(errorRefTo) || Utils.nullSafeEqual(refToMessageId, errorRefTo);
	        }
	        if (!consistent) {
	        	log.error("Cannot create Error Message as errors do not (all) reference message in error!");
	        	throw new StorageException("Errors do not reference message in error");
    	    }
	        errMsg.setDirection(msgInError.getDirection() == Direction.IN ? Direction.OUT : Direction.IN);
	        errMsg.setRefToMessageId(refToMessageId);
	        errMsg.setPModeId(msgInError.getPModeId());
    	} else 
    		// When there is no message in error, the error is always a reaction to a request 
    		errMsg.setDirection(Direction.OUT);
    	
    	IErrorMessageEntity entity; 
    	try {
    		log.trace("Store error message in database");
    		entity = mdsProvider.storeMessageUnit(errMsg);
    	} catch (DuplicateMessageIdException e) {
    		log.error("The Metadata Storage Provder ({}) executed duplicate check on incoming message!");
			throw new IllegalStateException("Illegal duplicate check exception!");
		} catch (StorageException saveFailed) {
			log.error("Error saving meta-data of Error message : {}", Utils.getExceptionTrace(saveFailed));
			throw saveFailed;
		}
		if (msgInError != null) { 
			log.trace("Relate Error Message and message in error");
			ILeg leg = PModeUtils.getLeg(msgInError);
			entity.setLeg(leg != null ? leg.getLabel() : null);  
			try {
				mdsProvider.updateMessageUnit(entity);				
			} catch (StorageException updFailure) {
				log.warn("Could not add relationship between Error Message (coreId={}) and message in error (coreId={})",
						 entity.getCoreId(), msgInError.getCoreId());
				throw updFailure;
			}
		}
		log.debug("Created new {} Error Message with MessageId={} and RefToMessageId={}", 
					entity.getDirection() == Direction.IN ? "incoming" : "outgoing", entity.getMessageId(), 
					entity.getRefToMessageId());	
		return entity;
    }
    
    /**
     * Saves the meta-data and content of the submitted payload to storage.
     * 
     * @param payload 	the submitted payload
     * @param pmode		the P-Mode that governs the sending of the User Message this payload will be contained in
     * @return	the {@link IPayloadEntity} object representing the saved payload 
     * @throws StorageException when an error occurs storing the submitted payload
     */
    public IPayloadEntity storeSubmittedPayload(final IPayload payload, final IPMode pmode) throws StorageException {
    	PayloadEntityProxy entity = null;
    	try {
	    	log.trace("Store meta-data of submitted payload");
	    	entity = new PayloadEntityProxy(mdsProvider.storePayloadMetadata(payload));
	    	log.trace("Store content of submitted payload");
	    	IPayloadContent content = psProvider.createNewPayloadStorage(entity.getPayloadId(), pmode, Direction.OUT);
			try(OutputStream contentStream = content.openStorage()) {
				Utils.copyStream(payload.getContent(), contentStream);						
			}
			log.debug("Saved content of payload");
			entity.setContent(content);
			return entity;
		} catch (IOException contentFailure) {
			log.error("Could not save content of submitted payload : {}", Utils.getExceptionTrace(contentFailure));
			try {
				mdsProvider.deletePayloadMetadata(entity.getSource());
			} catch (StorageException mdRemoveFailed) {
				log.error("Could not remove the meta-data of failed payload submission! Created payloadId={}", 
							entity.getPayloadId());
			}
			throw new StorageException("Could not save payload content", contentFailure);
		} catch (StorageException saveFailed) {
			log.error("Could not save meta-data of submitted payload : {}", Utils.getExceptionTrace(saveFailed));
			throw saveFailed;
		}
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
     * @throws StorageException When a problem occurs updating the processing state of the message unit
     * @since 7.0.0 returns result of the processing state update
     */
    public boolean setProcessingState(final IMessageUnitEntity msgUnit, final ProcessingState newProcState)
                                                                                        throws StorageException {
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
     * @throws StorageException When a problem occurs updating the processing state of the message unit
     * @since 7.0.0 removed parameter for current state
     */
    public boolean setProcessingState(final IMessageUnitEntity msgUnit, final ProcessingState newProcState,
    							   	  final String description) throws StorageException {
    	final ProcessingState cState = msgUnit.getCurrentProcessingState().getState(); 
		try {			
			updateEntity(msgUnit, m -> m.setProcessingState(newProcState, description));
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
     * Sets the ID of the P-Mode that defines how the message unit should be processed.
     *
     * @param msgUnit   The entity object representing the message unit
     * @param pmodeId   The ID of the P-Mode that defines how the message unit must be processed
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								   entity object is updated to the latest meta-data available.
     * @throws StorageException  	   If some other error occured when saving the updated message unit to the database
     */
    public void setPModeId(final IMessageUnitEntity msgUnit, final String pmodeId) throws StorageException {
		updateEntity(msgUnit, m -> m.setPModeId(pmodeId));
    }

    /**
     * Sets the ID of the P-Mode and [label of] the Leg that define how the Error Message unit should be processed.
     *
     * @param msgUnit   The entity object representing the Error Message unit
     * @param pl   		Pair consisting of the P-Mode and label of the Leg that govern the processing of the Error 
     * 					Message 
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								   entity object is updated to the latest meta-data available.
     * @throws StorageException    	   If some other error occured when saving the updated message unit to the database
     * @since 6.0.0
     */
    public void setPModeAndLeg(final IErrorMessageEntity msgUnit, final Pair<IPMode, ILeg.Label> pl) 
    																					throws StorageException {
    	updateEntity(msgUnit, m -> { m.setPModeId(pl.value1().getId()); m.setLeg(pl.value2()); });
    }
        
    /**
     * Sets the multi-hop indicator of the message unit.
     *
     * @param msgUnit       The entity object representing the message unit which multi-hop indicator should be set
     * @param isMultihop    The indicator whether this message unit uses multi-hop
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								   entity object is updated to the latest meta-data available.
     * @throws StorageException    	   If some other error occured when saving the updated message unit to the database
     */
    public void setMultiHop(final IMessageUnitEntity msgUnit, final boolean isMultihop) throws StorageException {
        updateEntity(msgUnit, m -> m.setMultiHop(isMultihop));
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
     * @throws StorageException    	   If some other error occured when saving the updated message unit to the database
     */
    public void setAddSOAPFault(final IErrorMessageEntity errorMessage, final boolean addSOAPFault)
                                                                                        	throws StorageException {
        updateEntity(errorMessage, e -> e.setAddSOAPFault(addSOAPFault));        
    }

    
    /**
     * Helper method to update and save the meta-data of a message unit to the database. 
     * <p>
     * In case there is a problem in the persistency layer, the Holodeck B2B Core will use an in-memory Error Message 
     * entity object to be able to still respond to the sending MSH. Updates to this non persisted Error Message should 
     * however not be passed to the persistency provider.  
     * 
     * @param m the entity object to be saved to the database
     * @throws AlreadyChangedException When the database contains more up to date data. The meta-data contained in the
     * 								   entity object is updated to the latest meta-data available.
     * @throws StorageException    	   If some other error occured when saving the updated message unit to the database
     */    
    private <E extends IMessageUnitEntity> void updateEntity(E entity, Consumer<E> update) throws StorageException {
    	update.accept(entity);
    	if (!(entity instanceof NonPersistedErrorMessage))
    		try {
    			if (entity instanceof UserMessageEntityProxy)
    				mdsProvider.updateMessageUnit(((UserMessageEntityProxy) entity).getSource());
    			else
    				mdsProvider.updateMessageUnit(entity);    			
    		} catch (AlreadyChangedException alreadyChanged) {
    			log.warn("The meta-data of message unit (msgId={}) was already updated!", entity.getMessageId());
    			throw alreadyChanged;
    		} catch (StorageException updFailure) {
    	    	log.error("Error in update ({}) of message unit (msgId={}) : {}",  
    	    				updFailure.fillInStackTrace().getStackTrace()[1].getMethodName(), entity.getMessageId(),
    	    				Utils.getExceptionTrace(updFailure));
    	    	throw updFailure;    			
    		}
    }
    
    /**
     * Deletes the meta-data and for User Messages the paylaod contents of the given message unit.
     *
     * @param messageUnit       The {@link IMessageUnitEntity} object to be deleted
     * @throws StorageException When a problem occurs while removing the message unit from the database.
     */
    public void deleteMessageUnit(IMessageUnitEntity messageUnit) throws StorageException {
        log.trace("Deleting {} with MessageId={}", MessageUnitUtils.getMessageUnitName(messageUnit), 
        			messageUnit.getMessageId());
        final Collection<? extends IPayloadEntity> payloads = messageUnit instanceof IUserMessage ? 
        														((IUserMessageEntity) messageUnit).getPayloads() : null;        
        
        Collection<StorageException> errs = new ArrayList<>();  
        if (payloads != null) {
        	log.trace("Remove payload content of User Message");
        	for (IPayloadEntity p : payloads) {
        		try {
        			psProvider.removePayloadContent(p.getPayloadId());
        			log.trace("Removed payload content (URI={})", p.getPayloadURI()); 
        		} catch (StorageException deleteFailed) {
        			log.warn("Error removing payload content (URI={}) of User Messsage (msgId={}): {}", 
							 p.getPayloadURI(), messageUnit.getMessageId(), Utils.getExceptionTrace(deleteFailed));
        			errs.add(new StorageException("Error removing payload content (URI=" + p.getPayloadURI() + ")",
        											deleteFailed));
				}
        	}
        }    	

        // If not all payload content was removed, keep the meta-data of the message, so we can try again later
        if (errs.isEmpty())
	        try {
	        	mdsProvider.deleteMessageUnit(messageUnit);
	        	log.info("{} (MessageId={}) removed from storage", MessageUnitUtils.getMessageUnitName(messageUnit),
	        				messageUnit.getMessageId());
	        } catch (StorageException deleteFailed) {
				log.error("Error deleting meta-data of message unit (msgId={}) : {}", messageUnit.getMessageId(), 
							Utils.getExceptionTrace(deleteFailed));
				errs.add(deleteFailed);        	
	        }
        
    	log.debug("Raise event to indicate that message unit (msgId={}) {} removed", messageUnit.getMessageId(),
    				errs.isEmpty() ? "is" : "could not be");
    	HolodeckB2BCoreInterface.getEventProcessor().raiseEvent(errs.isEmpty() ? new MessageUnitPurged(messageUnit) 
																	  : new MessagePurgeFailure(messageUnit, errs));    	
    }
    
    /**
     * Updates the payload meta-data.
     *
     * @param payloadInfo   The updated meta-data on the payload which must be persisted
     * @throws StorageException     If an error occurs when saving the payload meta-data to the database
     */
    public void setPayloadInformation(final IPayloadEntity payloadInfo) throws StorageException {
		try {
			mdsProvider.updatePayloadMetadata(payloadInfo);
		} catch (AlreadyChangedException alreadyChanged) {
			log.warn("The meta-data of payload (URI={}) contained in User Message (coreId={}) was already updated!", 
						payloadInfo.getPayloadURI(), payloadInfo.getParentCoreId());
			throw alreadyChanged;
		} catch (StorageException updFailure) {
	    	log.error("Error in update of payload meta-data (URI={}) contained in User Message (coreId={}) : {}",  
	    			  payloadInfo.getPayloadURI(), payloadInfo.getParentCoreId(), Utils.getExceptionTrace(updFailure));
	    	throw updFailure;    			
		}
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
    
    /**
     * Helper method to create a MessageId that uses the configured host name as right part.
     * 
     * @return a unique MessageId 
     */
    private String createMessageId() {
    	return MessageIdUtils.createContentId(HolodeckB2BCoreInterface.getConfiguration().getHostName());
    }
}

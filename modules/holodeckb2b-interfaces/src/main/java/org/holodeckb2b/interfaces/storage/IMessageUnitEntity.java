/*
 * Copyright (C) 2016 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.storage;

import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Defines the interface of the stored object that is used by the Holodeck B2B to store the general message
 * unit meta-data.
 * <p>It is based on the {@link IMessageUnit} interface from the message model. It however does not include
 * <i>setter</i> methods as changes need to be done through the DAO objects. This to ensure that changes in the
 * meta-data of the message unit are saved correctly.
 * <p>As persistency implementations may optimize the loading of collections it is not required to load collection
 * valued fields immediately but perform <i>lazy loading</i>. Therefore this interface adds the
 * {@link #isLoadedCompletely()} method to the getter methods already defined in the {@link IMessageUnit} interface
 * which indicates whether all information is loaded from storage. If a getter method is used for a field that has not
 * been loaded a {@link StorageException} will be thrown.<br>
 * Lazy loading SHOULD ONLY be used by persistency implementations when multiple entity objects are retrieved from
 * storage and it is not clear if all meta-data will be used in further processing.
 * <p>In case of the general meta-data that applies to all message units only the list of processing states may be
 * lazily loaded, i.e. {@link #getProcessingStates()} should only be called when the data is completely loaded. <b>
 * NOTE</b> that implementations MUST return the current processing state even if the object is not completely loaded.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public interface IMessageUnitEntity extends IMessageUnit {
	
	/**
	 * Gets the <i>CoreId</i> which uniquely identifies the message unit object in the Holodeck B2B instance. Although
	 * the MessageId of a message unit should be globally unique this is not enforced for received message. Assigning 
	 * each message unit instance a unique internal identifier therefore helps to make unambiguous references.
	 * 
	 * @return	internal unique identifier assigned to this message unit
	 * @since 7.0.0
	 */
	String getCoreId();
		
    /**
     * Indicates whether all meta-data of the object have been loaded. See the class documentation which fields may be
     * loaded lazily.
     *
     * @return  <code>true</code> if all data has been retrieved from storage,<br>
     *          <code>false</code> otherwise
     */
    boolean isLoadedCompletely();

    /**
     * Gets the indication whether this message unit is send using a multi-hop exchange
     *
     * @return  <code>true</code> if multi-hop is used for exchange of this message unit,<br>
     *          <code>false</code> otherwise
     */
    boolean usesMultiHop();
    
    /**
     * Sets the indication whether this message unit is send using a multi-hop exchange
     *
     * @param usingMultiHop		<code>true</code> if multi-hop is used for exchange of this message unit,<br>
     *          				<code>false</code> otherwise
     * @since 7.0.0
     */
    void setMultiHop(boolean usingMultiHop);

    /**
     * Sets the identifier of the P-Mode that governs the message exchange of the message unit.
     * 
     * @param pmodeId	the P-Mode.id
     * @since 7.0.0
     */
    void setPModeId(String pmodeId);
    
    /**
     * Sets the new processing state of the message unit and additional description on it. The start time of the new 
     * state should be set to the current time. 
     * 
     * @param newState		the new processing state
     * @param description	additional description on the new state
     * @since 7.0.0
     */
    void setProcessingState(ProcessingState newState, String description);

    /**
     * Gets the current processing state the message unit is in.
     * <p>Although the current state is the last item in the list that is returned by the {@link #getProcessingStates()}
     * method this method is simpler to use and it also allows implements to optimise the handling of the current
     * processing state.
     *
     * @return  The {@link IMessageUnitProcessingState} the message unit is currently in
     * @since 7.0.0
     */
    IMessageUnitProcessingState getCurrentProcessingState();    
}

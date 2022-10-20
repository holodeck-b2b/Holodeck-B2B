/**
 * Copyright (C) 2022 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.interfaces.delivery;

import java.util.Collection;

import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;

/**
 * Defines the interface of the Holodeck B2B Core component that is responsible for managing the delivery of message 
 * units to the back-end application. 
 * <p>
 * Holodeck B2B itself does not deliver the message unit to the back-end, but uses <i>Delivery Method</i>s for the 
 * actual delivery. This creates flexibility as new <i>Delivery Method</i> can be deployed when needed for a a specific
 * type of back-end integration, for example using a specific protocol, without making any change to the Holodeck B2B 
 * Core. 
 * <p>
 * How an actual delivery must be executed is configured by a <i>Delivery Specification</i> which tells the <i>Delivery
 * Manager</i> how to setup the <i>Delivery Method</i>. Because the initialisation of the <i>Delivery Method</i> may be
 * a costly operation these can be cached. The key for caching will be the <i>Delivery Specification</i>'s identifier. 
 * Each actual integration therefore should have its own <i>Delivery Specification</i> with a unique identifier.<br/>
 * The <i>Delivery Specification</i> to use for the delivery of a message unit must be provided by its P-Mode. To allow
 * re-use of <i>Delivery Specification</i>s between P-Modes they can also be pre-registered with the <i>Delivery Manager
 * </i>, in which case the P-Mode only needs to provide the <i>Delivery Specification's</i> identifier. How the 
 * pre-registration is done is implementation specific. Because the caching of <i>Delivery Methods</i> is based on the
 * <i>Delivery Specification's</i> identifier, one can only be registered if the identifier does not exist in the set
 * of already registered specifications or in the cache of <i>Delivery Methods</i>.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 6.0.0
 * @see IDeliveryMethod
 * @see IDeliverySpecification
 */
public interface IDeliveryManager {
    
    /**
     * Checks if the message unit needs to/can be delivered to the back-end application and if so starts the delivery 
     * process. <i>User Messages</i> should always be delivered, but for <i>Signal Messages</i> it depends on the 
     * configuration specified in the P-Mode. For these the delivery (also called notification) must be explicitly 
     * defined by the P-Mode parameters {@link IReceiptConfiguration#shouldNotifyReceiptToBusinessApplication()} 
     * and {@link IErrorHandling#shouldNotifyErrorToBusinessApplication()}.  
     * <p>
     * How the message unit needs to be delivered is also specified in the P-Mode with the option to use specific 
     * delivery configurations for <i>Receipts</i> and <i>Errors</i>.
     * <p>
     * Only received message units that are in or have been in <i>READY_FOR_DELIVERY</i> processing state can be 
     * delivered to ensure that only messaging protocol valid messages are provided to the back-end.
     * <p>
     * Before the delivery process is started there is a check that the current processing state registered in the 
     * database still matches to the current processing state provided in the given message unit before the delivery 
     * process is started to reduce the risk of concurrent processing of the message unit. 
     * 
     * @param messageUnit for which the delivery process should be restarted
     * @throws IllegalStateException    when the given message unit is an outgoing message or when it has never been in
     *  								<i>READY_FOR_DELIVERY</i> state.
     * @throws MessageDeliveryException when the message unit cannot be delivered because the delivery process cannot
     * 									be started (for async deliveries) or completed (for sync). The processing state
     * 									of the message unit will be set to <i>DELIVERY_FAILED}</i> or <i>FAILURE</i>
     * 									depending on whether the error is permanent or not
     */
    void deliver(IMessageUnitEntity messageUnit) throws IllegalStateException, MessageDeliveryException;
	
	/**
	 * Registers a <i>Delivery Specification</i> for later referencing by P-Modes.
	 * <p>
	 * When there already is a <i>Delivery Specification</i> registered with same id it will be replaced by the provided
	 * specification. Also the <i>Delivery Method</i> specified by the same id will be removed from the cache so the new
	 * settings will be used on a next message unit delivery.   
	 * 
	 * @param spec	the specification to register
	 */
    void registerDeliverySpec(IDeliverySpecification spec);
    
    /**
     * Checks if a <i>Delivery Specification</i> with the given identifier is already in use, including both the 
     * pre-registered ones and the ones that are related to the cached <i>Delivery Methods</i>.
     * 
     * @param id	the identifier to check for existence	
     * @return		<code>true</code> if a <i>Delivery Specification</i> with the given is registered or used by a 
     * 				cached <i>Delivery Method</i>,<br/><code>false</code> otherwise
     */
    boolean isSpecIdUsed(final String id);        
    
    /**
     * Gets the registered <i>Delivery Specification</i> with the specified identifier.
     * <p>
     * Note that this method can only be used to retrieve a <i>Delivery Specification</i> that has been registered using
     * the {@link #registerDeliverySpec(IDeliverySpecification)} method and cannot retrieve the specification of a
     * cached <i>Delivery Method</i>. The reason for this is that the specification of the cached <i>Delivery Method</i>
     * only exists as long as the delivery method is in the cache.  
     * 
     * @param id	the identifier of the pre-registered <i>Delivery Specification</i> to retrieve.
     * @return	the {@link IDeliverySpecification} with the specified id, or <code>null</code> when there is no 
     * 			pre-registered one with the given id. 
     * 			
     */
    IDeliverySpecification getDeliverySpecification(final String id);
    
    /**
     * Gets all registered <i>Delivery Specifications</i>. Note that this excludes the ones related to cached <i>
     * Delivery Methods</i>.
     * 
     * @return collection with all registered <i>Delivery Specifications</i>
     */
    Collection<IDeliverySpecification> getAllRegisteredSpecs();
    
    /**
     * Removes a registered <i>Delivery Specification</i> and, if it exists, removes the related <i>Delivery Method</i>
     * from the cache.
     * 
     * @param id 	the identifier of the <i>Delivery Specification</i> to remove.
     */
    void removeDeliverySpec(final String id);
}

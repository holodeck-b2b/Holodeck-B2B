/**
 * Copyright (C) 2022 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.delivery;

import java.util.Map;

import org.holodeckb2b.interfaces.events.IMessageDelivered;
import org.holodeckb2b.interfaces.events.IMessageDeliveryFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Defines the interface of a <i>Delivery Method</i> that is used by the Holodeck B2B Core to deliver a received
 * message unit to the business application. Decoupling the internal [Delivery Method] and external interface 
 * [of the business application] creates flexibility as only a new <i>Delivery Method</i> needs to be deployed when 
 * needed for the back-end integration without making any change to the Holodeck B2B Core.
 * <p>
 * To setup the Delivery Method for a specific back-end integration and allow re-use of resources it is initialised 
 * before the first delivery operation is requested. The parameters to use for an actual back-end integration are 
 * specified by an {@link IDeliverySpecification} which are generally provided by the P-Mode of the message unit to be 
 * delivered.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 6.0.0
 * @see IDeliverySpecification
 * @see IDeliveryManager
 */
public interface IDeliveryMethod {

	/**
     * Initialises the delivery method with the specific settings to use for the delivery of the message units. This 
     * method is only called once by Holodeck B2B before requesting the first message delivery.
     *
     * @param settings    The settings to use for initialisation of the delivery method
     * @throws MessageDeliveryException When the delivery method is unable to successfully initialise itself and 
     * 									therefore can not deliver messages.
     */    
	void init(Map<String, ?> settings) throws MessageDeliveryException;
	
	/**
	 * Indicates whether this delivery method supports the asynchronous delivery of message units.  
	 * 
	 * @return	<code>true</code> when asynchronous delivery is supported by this delivery method,<br/> 
	 * 			<code>false</code> if not
	 */
	boolean supportsAsyncDelivery();
	
    /**
     * Directly, i.e. synchronously, delivers the message unit to the business application. 
     * <p>When this method finishes successfully, i.e. without throwing an exception, Holodeck B2B assumes the message
     * is delivered to the business application and it can send the <i>Receipt</i> to the sending MSH. It also implies 
     * that all payload files can safely be deleted. The processing state of the message unit will be set to {@link 
     * ProcessingState#DELIVERED} for <i>User Messages</i> or {@link ProcessingState#DONE} for <i>Signal Messages</i>.
     * Additionally a {@link IMessageDelivered} event will be raised.
     * <p>
     * When an exception is thrown that indicates a permanent failure, Holodeck B2B will set the processing state of the
     * message unit for {@link ProcessingState#DELIVERY_FAILED} and for a <i>User Message</i> also generate an 
     * <i>Error</i> as response to the Sender so it knows the message cannot be delivered.<br/>
     * When an exception is thrown that does not indicate a permanent failure, Holodeck B2B will not send a response to
     * the Sender so the message can be retried.<br/>
     * In both cases a {@link IMessageDeliveryFailure} event will be raised.
     * <p><b>NOTE:</b> Whether a <i>Receipt</i> will be send depends on both the messaging protocol and message exchange
     * configuration used for the delivered message unit.
     *
     * @param rcvdMsgUnit                   The {@link IMessageUnit} to be delivered to the business application
     * @throws MessageDeliveryException     When delivery of the message unit to the business application fails
     */
    void deliver(IMessageUnit rcvdMsgUnit) throws MessageDeliveryException;
    
    /**
     * Starts the [asynchronous] delivery process to the business application for the given message unit.   
     * <p>When this method finishes successfully, i.e. without throwing an exception, Holodeck B2B assumes the message
     * can be delivered to the business application by this delivery method and it can send the <i>Receipt</i> to the 
     * sending MSH. Holodeck B2B will however not assume the message unit has actually been delivered and will wait
     * with updating the processing state and possible removal of payloads until the delivery method has reported the
     * result of the delivery process using the call back.<br/>
     * If the delivery fails there should be recovery process to ensure that the message or information conatained in it
     * can be provided to the back-end system or end user. For example by triggering redelivery or downloading the 
     * payload(s) of the message.  
     * <p>
     * When an exception is thrown that indicates a permanent failure, Holodeck B2B will set the processing state of the
     * message unit for {@link ProcessingState#DELIVERY_FAILED} and for a <i>User Message</i> also generate an 
     * <i>Error</i> as response to the Sender so it knows the message cannot be delivered.<br/>
     * When an exception is thrown that does not indicate a permanent failure, Holodeck B2B will not send a response to
     * the Sender so the message can be retried.<br/>
     * In both cases a {@link IMessageDeliveryFailure} event will be raised.
     * <p><b>NOTE 1:</b> The delivery method implementation is responsible for managing the delivery process, including
     * thread management, etc.
     * <p><b>NOTE 2:</b> Whether a response (<i>Receipt</i> or <i>Error</i>) will be send depends on both the messaging 
     * protocol and message exchange configuration used for the delivered message unit.
     *
     * @param rcvdMsgUnit	The {@link IMessageUnit} to be delivered to the business application
     * @param callback		The {@link IDeliveryCallback} handler to use for reporting the delivery status 
     * @throws MessageDeliveryException     When delivery of the message unit to the business application fails
     */
    default void deliver(IMessageUnit rcvdMsgUnit, IDeliveryCallback callback) throws MessageDeliveryException {}

    /**
     * Is called by the Holodeck B2B Core when the delivery method is no longer needed. This method should be used by
     * implementations to release any resources it holds.
     */
    default void shutdown() {};
}

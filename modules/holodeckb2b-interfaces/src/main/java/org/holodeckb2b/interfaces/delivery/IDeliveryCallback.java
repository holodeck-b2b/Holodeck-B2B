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

import org.holodeckb2b.interfaces.events.IMessageDelivered;
import org.holodeckb2b.interfaces.events.IMessageDeliveryFailure;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Defines the interface of the callback handler that processes the result of an asynchronous message delivery process.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 6.0.0
 */
public interface IDeliveryCallback {

	/**
	 * This method must be called by the {@link IDeliveryMethod} when it successfully delivered the message unit to the
	 * back-end application.
	 * <p>The Holodeck B2B Core will set the processing state of the message unit to {@link ProcessingState#DELIVERED}
	 * for <i>User Messages</i> or {@link ProcessingState#DONE} for <i>Signal Messages</i> and trigger a {@link
	 * IMessageDelivered} event.
	 */
	void success();

	/**
	 * This method must be called by the {@link IDeliveryMethod} when it could not deliver the message unit to the
	 * back-end application.
     * <p>
     * The exception should indicate if the error condition is permanent, i.e. if retrying the delivery will not solve 
     * the problem. Holodeck B2B will always set the processing state of the message unit to {@linkplain 
     * ProcessingState#DELIVERY_FAILED}, but if the error condition is indicated to be permanent the state will then be 
     * set to {@linkplain ProcessingState#FAILURE} which means that the message will be marked as a  duplicate when it 
     * is received again from the sender.<br/>
     * In both cases a {@link IMessageDeliveryFailure} event will be raised.
	 *
	 * @param failure	the reason why the delivery failed
	 */
	void failed(MessageDeliveryException failure);
}

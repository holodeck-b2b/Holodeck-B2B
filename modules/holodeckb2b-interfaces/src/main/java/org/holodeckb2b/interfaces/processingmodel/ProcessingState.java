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
package org.holodeckb2b.interfaces.processingmodel;

/**
 * Enumerates all the <i>processing states</i> a message unit can be in during its processing by Holodeck B2B.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public enum ProcessingState {
	
    /**
     * Is the first processing state of message units that are submitted to the Holodeck B2B Core for sending. Therefore
     * only applies to <i>User Message</i> and <i>Pull Request</i> message units as only these can be submitted to the
     * Core. <i>Error</i> and <i>Receipt</i> Signal messages [to be sent] are created during the processing of other
     * message units and therefor start in {@link #CREATED} state.
     */
    SUBMITTED(false),

    /**
     * Is the first processing state of message unit that are created by the Holodeck B2B Core. Only applies to
     * <i>Error</i> and <i>Receipt</i> Signal messages that are created in response to received message units.
     */
    CREATED(false),

    /**
     * Is the first processing state of message units received by Holodeck B2B.
     */
    RECEIVED(false),

    /**
     * The message unit is waiting to be pulled by another MSH.
     */
    AWAITING_PULL(false),

    /**
     * The message unit is ready to be pushed to another MSH.
     */
    READY_TO_PUSH(false),

    /**
     * The message unit is currently being processed by Holodeck B2B.
     */
    PROCESSING(false),

    /**
     * The message unit is currently being transferred to the other MSH. Note that this state also applies to messages
     * that are pulled by the other MSH.
     */
    SENDING(false),

    /**
     * A problem occurred while the message unit was being in transfer to the other MSH.
     */
    TRANSPORT_FAILURE(false),

    /**
     * The User Message is waiting for a Receipt.
     */
    AWAITING_RECEIPT(false),

    /**
     * The message unit is ready to be delivered (User Message) / notified (Signal Message) to the business application.
     */
    READY_FOR_DELIVERY(false),

    /**
     * The message unit is being delivered (User Message) / notified (Signal Message) to the business application.
     */
    OUT_FOR_DELIVERY(false),

    /**
     * The attempt to deliver the message unit to the business application has failed.
     */
    DELIVERY_FAILED(false),

    /**
     * Indicates that an ebMS Error with severity <i>warning</i> was reported for the message unit. Because processing
     * of the message unit can continue this state is not defined as final.
     */
    WARNING(false),

    /**
     * This state is used to indicate that an [unexpected] error occurred during the processing of an outgoing User 
     * Message that currently prevents further processing, but which could potentially be resolved by an external action
     * after which processing could be resumed.     
     * 
     * @since 5.3.0
     */
    SUSPENDED(false),
    
    /**
     * This final state indicates that a <i>User Message</i> message unit is successfully delivered either to the other
     * MSH or to the business application.
     */
    DELIVERED(true),

    /**
     * This final state indicates that processing of a <i>Signal Message</i> message unit has completed successfully,
     * i.e. the message unit is delivered to the other MSH or a received signal has been processed completely (which
     * may include delivery to the business application).
     */
    DONE(true),

    /**
     * This final state indicates that the processing of the message unit failed and no further processing will occur.
     */
    FAILURE(true),

    /**
     * This final state indicates that the received <i>User Message</i> message unit is a duplicate of an already
     * received and delivered one and therefor not processed any further. Note that if a response signal is generated
     * for this User Message it is still send.
     */
    DUPLICATE(true);

    /**
     * Indicates whether this processing state is a <i>final</i> state, i.e. no further updates will occur.
     */
    private boolean isFinal;

    /**
     * Initializes a processing state
     *
     * @param isFinal           Indicator whether this state is final
     */
    ProcessingState(final boolean isFinal) {
        this.isFinal = isFinal;
    }

    /**
     * @return indication whether this processing state is a <i>final</i> state, i.e. no further updates will occur.
     */
    public boolean isFinal() {
		return isFinal;
	}
}

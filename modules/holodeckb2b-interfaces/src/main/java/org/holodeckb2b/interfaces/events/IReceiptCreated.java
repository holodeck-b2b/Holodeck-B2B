/*
 * Copyright (C) 2018 The Holodeck B2B Team.
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
package org.holodeckb2b.interfaces.events;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;

/**
 * Is the <i>message processing event</i> that indicates that a <i>Receipt</i> has been created for a <b>received</b>
 * <i>User Message</i>. This event is to inform the business application that Holodeck B2B will acknowledge to the
 * Sender of the User Message that is it correctly received the message. Not to be confused with the <i>Notify</i>
 * operation that indicates that a Receipt is received by Holodeck B2B for an earlier sent User Message!
 * <p>NOTE: There is no guarantee on the order in which this event and the related User Message will be delivered to the
 * business application. It depends on the mechanisms used to deliver the User Message and the event in which order they
 * will arrive.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public interface IReceiptCreated extends IMessageProcessingEvent {

    /**
     * Gets the <i>Receipt</i> that was created to acknowledge that the <i>User Message</i> that is the subject of this
     * event was received by Holodeck B2B.
     *
     * @return The {@link IReceipt} that was created
     */ 
	public IReceipt getReceipt();

    /**
     * Indicates whether the received <i>User Message</i> for which this <i>Receipt</i> was created is a <i>duplicate
     * </i>and because of <i>duplicate elimination</i> not delivered to the business application. Duplicate elimination
     * is part of the <i>AS4 Reception Awareness feature</i> and can be configured in the P-Mode.
     *
     * @return  <code>true</code> if the Receipt was created for a duplicate,<br>
     *          <code>false</code> otherwise
     */
	public boolean isForEliminatedDuplicate();
}

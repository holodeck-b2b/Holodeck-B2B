/*
 * Copyright (C) 2017 The Holodeck B2B Team.
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
package org.holodeckb2b.interfaces.events.receptionawareness;

import org.holodeckb2b.interfaces.as4.pmode.IReceptionAwareness;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is the <i>message processing event</i> that indicates that a <i>User Message</i> is resent because the <i>AS4
 * Reception Awareness Feature</i> is enabled in the P-Mode of the message unit and no <i>Receipt</i> was received
 * before the configured deadline (and there are still retries left).
 * <p>NOTE: Message resending means that the message unit is put in the "send queue" by changing its processing state
 * to either {@link ProcessingState#READY_TO_PUSH} or {@link ProcessingState#AWAITING_PULL}. The actual transmission
 * depends on the availability of a sender or the pulling by the receiving MSH.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 * @see IReceptionAwareness
 */
public interface IMessageResentEvent extends IMessageProcessingEvent {

}

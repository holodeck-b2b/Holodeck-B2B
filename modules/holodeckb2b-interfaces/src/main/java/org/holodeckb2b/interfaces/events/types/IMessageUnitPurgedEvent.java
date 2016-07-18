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

package org.holodeckb2b.interfaces.events.types;

import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.workerpool.IWorkerTask;

/**
 * Is the <i>message processing event</i> that indicates that a message unit is deleted from the Holodeck B2B Core 
 * message database because the period for maintaining it's meta-data has expired.
 * <p>Note that the <i>"purge"</i> {@link IWorkerTask} implementation is responsible for triggering this event and it 
 * therefore depends on this implementation when this event is exactly triggered. An implementation can for example only 
 * trigger the event for <i>User Message</i> message units and also decide not to include the payload data.
 * <p>This event must be considered to be the last opportunity for extension to process a message unit. After the event
 * is handled all meta-data, and for User Messages payload data is removed from the system. But note that these data can
 * still be included in back-ups of database and/or file system. It is up to the operator of the system to removed these
 * if needed.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */
public interface IMessageUnitPurgedEvent extends IMessageProcessingEvent {

}

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
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.storage.StorageException;

/**
 * Is the <i>message processing event</i> that indicates that a message unit is deleted from the Holodeck B2B Core
 * message database because the period for maintaining it's meta-data has expired.
 * <p>This event is triggered by the Holodeck B2B Core's <i>Storage Manager</i> when the meta-data and, 
 * for User Messages, the payload content has been removed from storage. This event is therefore the last opportunity 
 * for extensions to process the meta-data of a message unit.
 * <p>NOTE: As the payload data has already been removed a call to {@link IPayload#getContent()} for a payload contained
 * in the purged User Message will not return the data and may throw a {@link StorageException}.  
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public interface IMessageUnitPurged extends IMessageProcessingEvent {

}

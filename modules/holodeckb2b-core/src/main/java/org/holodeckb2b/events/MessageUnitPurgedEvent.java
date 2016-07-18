/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.events;

import org.holodeckb2b.common.events.AbstractMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.types.IMessageUnitPurgedEvent;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is the implementation class of {@link IMessageUnitPurgedEvent} to indicate that 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  2.1.0
 */
public class MessageUnitPurgedEvent extends AbstractMessageProcessingEvent implements IMessageUnitPurgedEvent {
    
    
    public MessageUnitPurgedEvent(IMessageUnit subject) {
        super(subject);
    }
    
}

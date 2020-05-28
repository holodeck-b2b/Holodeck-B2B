/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.events.impl;

import org.holodeckb2b.interfaces.events.IMessageTransfered;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is the implementation class of {@link IMessageTransfered} to indicate that indicates that a message unit was 
 * transfered to the other MSH, either by sending it as a request or including it as a response failed.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class MessageTransfered extends AbstractMessageProcessingEvent implements IMessageTransfered {

    /**
     * Creates a new <code>MessageTransfered</code> for the successful transfer of the given message unit.
     *
     * @param subject   The transferred message unit
     */
    public MessageTransfered(final IMessageUnit subject) {
        super(subject);
    }

}

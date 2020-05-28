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

import org.holodeckb2b.interfaces.events.IMessageTransferFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is the implementation class of {@link IMessageTransferFailure} to indicate that an attempt to transfer a message unit 
 * to the other MSH, either by sending it as a request or including it as a response failed.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class MessageTransferFailure extends AbstractMessageProcessingEvent implements IMessageTransferFailure {
    /**
     * The exception that caused the transfer to fail
     */
    final private Exception failureReason;

    /**
     * Creates a new <code>MessageTransfer</code> for the unsuccessful transfer of the given message unit that was
     * caused by the specified exception.
     *
     * @param subject   The transferred message unit
     * @param failure   The reason why the transfer failed
     */
    public MessageTransferFailure(final IMessageUnit subject, final Exception failure) {
        super(subject);
        this.failureReason = failure;
    }

    @Override
    public Exception getFailureReason() {
        return failureReason;
    }
}

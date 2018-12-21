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
package org.holodeckb2b.events;

import org.holodeckb2b.common.events.AbstractMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageTransfer;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is the implementation class of {@link IMessageTransfer} to indicate that indicates that there was an attempt to
 * transfer a message unit to the other MSH, either by sending it as a request or including it as a response.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @@since 4.0.0
 */
public class MessageTransfer extends AbstractMessageProcessingEvent implements IMessageTransfer {
    /**
     * Indicator whether the transfer was successful or not
     */
    final private boolean   successful;
    /**
     * The exception that caused the transfer to fail
     */
    final private Exception failureReason;

    /**
     * Creates a new <code>MessageTransfer</code> indicating successful transfer of the given message unit.
     *
     * @param subject   The transferred message unit
     */
    public MessageTransfer(final IMessageUnit subject) {
        super(subject);
        this.successful = true;
        this.failureReason = null;
    }

    /**
     * Creates a new <code>MessageTransfer</code> for the unsuccessful transfer of the given message unit that was
     * caused by the specified exception.
     *
     * @param subject   The transferred message unit
     * @param failure   The reason why the transfer failed
     */
    public MessageTransfer(final IMessageUnit subject, final Exception failure) {
        super(subject);
        this.successful = false;
        this.failureReason = failure;
    }

    @Override
    public boolean isTransferSuccessful() {
        return successful;
    }

    @Override
    public Exception getFailureReason() {
        return failureReason;
    }
}

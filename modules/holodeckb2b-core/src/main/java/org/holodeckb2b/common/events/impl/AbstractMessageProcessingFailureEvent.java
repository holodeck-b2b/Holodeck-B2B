/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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

import org.holodeckb2b.interfaces.events.IMessageProcessingFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is an abstract implementation of a <i>message processing event</i> that indicates that there was a problem in the
 * processing of a message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
abstract class AbstractMessageProcessingFailureEvent<E extends Throwable> extends AbstractMessageProcessingEvent
																		  implements IMessageProcessingFailure {

    private E     failureReason;

    /**
     * Creates a new <code>AbstractMessageProcessingFailureEvent</code> for the given message unit and failure reason.
     *
     * @param subject   The message unit
     * @param reason    The reason why the processing failed
     */
    AbstractMessageProcessingFailureEvent(final IMessageUnit subject, final E reason) {
    	this(subject, null, reason);
    }

    /**
     * Creates a new <code>AbstractMessageProcessingFailureEvent</code> for the given message unit and failure reason.
     *
     * @param subject   	The message unit
     * @param description 	A description of the failure
     * @param reason    	The exception that caused the processing to fail
     */
    AbstractMessageProcessingFailureEvent(final IMessageUnit subject, final String description, final E reason) {
        super(subject, description);
        this.failureReason = reason;
    }

    @Override
	public E getFailureReason() {
        return failureReason;
    }

}

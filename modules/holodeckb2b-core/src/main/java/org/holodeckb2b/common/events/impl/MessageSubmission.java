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

import org.holodeckb2b.interfaces.events.IMessageSubmission;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.submit.MessageSubmitException;

/**
 * Is the implementation class of {@link IMessageSubmission} to inform external components that a message unit was 
 * submitted for sending to the Holodeck B2B Core.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public class MessageSubmission extends AbstractMessageProcessingEvent implements IMessageSubmission {
    /**
     * Indicates whether the message unit was successfully submitted
     */
    private final boolean successFulSubmission;
    /**
     * The exception that caused the submission to fail
     */
    private final MessageSubmitException failureReason;

    /**
     * Creates an instance of the event for the successful submission of the message unit.
     *  
     * @param subject	The submitted message unit
     */
    public MessageSubmission(final IMessageUnit subject) {
        super(subject);
        this.successFulSubmission = true;
        this.failureReason = null;
    }
    
    /**
     * Creates an instance of the event for the case there was an error during the submission of the message unit.
     *  
     * @param subject			The submitted message unit
     * @param failureReason		Exception that caused the submission to fail
     */
    public MessageSubmission(final IMessageUnit subject, final MessageSubmitException failureReason) {
    	super(subject);
    	this.successFulSubmission = false;
    	if (failureReason == null)
    		throw new IllegalArgumentException("Failure reason must be specified in case of a failed submission");
    	this.failureReason = failureReason;
    }

	@Override
	public boolean isSubmissionSuccessful() {
		return successFulSubmission;
	}

	@Override
	public MessageSubmitException getFailureReason() {
		return failureReason;
	}

}

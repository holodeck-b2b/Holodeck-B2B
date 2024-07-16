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

import java.util.Collection;

import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;

/**
 * Is the <i>message processing event</i> that indicates that the validation of the header of a received message unit
 * has failed. Using this event the back-end or extensions can be made aware that a received message was rejected and
 * take additional action, for example informing an operator.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public interface IHeaderValidationFailure extends IReceivedMessageProcessingFailure {

	/**
     * Gets the exception that caused the header validation to fail. This means that the validation did not complete,
     * i.e. there is no information available on the validity of headers.
     *
     * @return  The {@link MessageValidationException} that caused the failure. <code>null</code> in case the
     * 			message headers did not meet the validation criteria.
     * @since 7.0.0
     */
	@Override
	MessageValidationException getFailureReason();

    /**
     * Gets the information on the errors that were found during the validation of the message header
     *
     * @return  The detected validation errors
     */
	Collection<MessageValidationError> getValidationErrors();
}

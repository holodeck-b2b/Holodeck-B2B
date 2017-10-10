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
package org.holodeckb2b.interfaces.events;

import java.util.Collection;
import java.util.Map;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;

/**
 * Is the <i>message processing event</i> that indicates that the custom validation of a m<i>User Message</i> message
 * unit has failed. This event is to inform the back-end business application (or Connector) about possible issues with
 * the message unit.
 * <p>NOTE: This event is always raised in case a validation error occurs, regardless whether message processing
 * continues or not.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 * @see IMessageValidationSpecification
 */
public interface ICustomValidationFailedEvent extends IMessageProcessingEvent {

    /**
     * Gets the information on the errors that were found during the custom validation of the message unit, grouped by
     * the validator that detected them. The validator is identified by the id assigned to it in the corresponding
     * {@link IMessageValidatorConfiguration}.
     * <p>NOTE: Because the validation can be stopped before all validators have been executed (see also {@link
     * #executedAllValidators()} it can not be guaranteed that all possible validation errors are reported.
     *
     * @return  The detected validation errors, grouped per validator
     */
    Map<String, Collection<MessageValidationError>> getValidationErrors();

    /**
     * Indicates whether all configured validators were executed or whether the validation was stopped after errors
     * were found.
     * <p>NOTE: It depends on the validation configuration when the validation stops. As a result the event can still
     * contain errors from multiple validators.
     *
     * @return <code>true</code> when not all validators were executed, <br><code>false</code> otherwise
     */
    boolean executedAllValidators();

    /**
     * Indicates whether the message unit was rejected and no further message processing takes place due to the detected
     * validation errors.
     *
     * @return  <code>true</code> when the message processing was stopped,<br><code>false</code> otherwise
     */
    boolean isMessageRejected();
}

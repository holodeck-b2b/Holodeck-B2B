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
package org.holodeckb2b.interfaces.customvalidation;

import java.util.List;
import org.holodeckb2b.interfaces.events.types.ICustomValidationFailedEvent;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;

/**
 * Defines the interface for the configuration of the custom validation(s) that must be performed on User Message
 * message units.
 * <p>This configuration is part of the <i>User Message flow</i> ({@link IUserMessageFlow}) of the P-Mode
 * and lists all <i>validators</i> that must be executed for each User Message message unit which processing is governed
 * by this P-Mode. Beside the validators that should be executed the configuration also includes indicators that
 * define whether the validations must be executed in order, whether they should be completed and whether the message
 * processing should fail when a validation error of a certain severity is detected.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface IMessageValidationSpecification {


    /**
     * Gets the validators that should be executed for all User Message message units.
     * <p>NOTE: Whether the validations are executed in the order as specified in the returned list depends on the
     * {@link #mustExecuteInOrder()} indicator. Only when this indicator is <code>Boolean.TRUE</code> the validations
     * are guaranteed to be executed in order, otherwise the order of execution may be different and validation may be
     * executed in parallel.
     *
     * @return  A list of {@link IMessageValidatorConfiguration}s the define the validations to be executed.
     */
    List<IMessageValidatorConfiguration> getValidators();

    /**
     * Indicates whether the validations must be executed in the order specified.
     *
     * @return  <code>Boolean.TRUE</code> if the validation must be executed in the specified order,<br>
     *          <code>Boolean.FALSE</code> or <code>null</code> otherwise.
     */
    Boolean mustExecuteInOrder();

    /**
     * Returns the validation error severity level at which the execution of the validations should stop, i.e. if a
     * validator finds a {@link MessageValidationError} with the returned severity no further validations are performed.
     *
     * @return  The severity level when to stop further validations,<br>
     *          or <code>null</code> when to perform all validations
     */
    MessageValidationError.Severity getStopSeverity();

    /**
     * Returns the validation error severity level at which the processing of the message unit should stop, i.e. if a
     * validator finds a {@link MessageValidationError} with the returned severity the processing state of the message
     * unit is set to <i>FAILURE</i> and an ebMS Error Signal is returned.
     * <p>NOTE: Even if the message unit is not rejected (<code>null</code> is returned) there is still a {@link
     * ICustomValidationFailedEvent} raised.
     *
     * @return  The severity level when to stop further validations,<br>
     *          or <code>null</code> when the message unit should not be rejected at all.
     */
    MessageValidationError.Severity getRejectionSeverity();
}

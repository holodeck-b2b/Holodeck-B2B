/*
 * Copyright (C) 2016 The Holodeck B2B Team.
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
package org.holodeckb2b.interfaces.pmode.validation;

import java.util.Collection;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * Defines the interface of the component responsible for the validation of P-Modes before they can be added to the set
 * of deployed P-Modes.
 * <p>Using a separate component for the validation of a P-Mode decouples the implementation of P-Mode storage and
 * validation, e.g. an implementation of {@link IPMode} is now only responsible for supplying the P-Mode parameter
 * values as they are configured by the user. It also allows to use different validations depending on domain specific
 * requirements.
 * <p>The <code>PModeManager</code> from the Core will ensure that the validation is executed for each P-Mode that is
 * deployed. Note however that the PModeManager may always execute some basic validations regardless of the used
 * validator implementation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public interface IPModeValidator {

    /**
     * Checks whether the given P-Mode conforms to the set of rules as set by this validator.
     * <p>NOTE: Implementations must ensure that this method is thread safe!
     *
     * @param pmode     The {@link IPMode} to check
     * @return          <code>null</code> or empty collection if the given P-Mode is valid,<br>
     *                  or a <code>Collection</code> of {@link PModeValidationError}s the describe the validation errors
     *                  found. It is RECOMMENDED that implementations check all rules and return all validation errors.
     */
    public Collection<PModeValidationError> isPModeValid(final IPMode pmode);
}

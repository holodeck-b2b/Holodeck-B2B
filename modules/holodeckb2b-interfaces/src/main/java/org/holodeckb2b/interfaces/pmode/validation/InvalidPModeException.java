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
import java.util.Collections;
import org.holodeckb2b.interfaces.pmode.PModeSetException;

/**
 * Is an extension of {@link PModeSetException} to indicate that a P-Mode can not be deployed because it violates one
 * or more P-Mode validation rules.
 * <p>NOTE: The list of errors as included in the exception is provided by the {@link IPModeValidator} and it therefore
 * depends on the implementation used whether all validation problems are listed in the exception or not, for example
 * because the validator stops after finding the first problem.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  HB2B_NEXT_VERSION
 */
public class InvalidPModeException extends PModeSetException {

    private final Collection<PModeValidationError>  errors;

    /**
     * Creates a new <code>InvalidPModeException</code> and includes the provided list of validation errors.
     *
     * @param errors    The validation errors that were found by the {@link IPModeValidator} and/or <code>
     *                  PModeManager</code>.
     */
    public InvalidPModeException(final Collection<PModeValidationError> errors) {
        this.errors = Collections.unmodifiableCollection(errors);
    }

    /**
     * Gets the list of validation errors that were found in the P-Mode that was being deployed.
     *
     * @return  The list of validation error that prevented successful deployment of the P-Mode
     */
    public Collection<PModeValidationError> getValidationErrors() {
        return errors;
    }

}

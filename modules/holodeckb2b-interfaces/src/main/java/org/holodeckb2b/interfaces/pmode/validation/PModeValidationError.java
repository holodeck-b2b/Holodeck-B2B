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

/**
 * Represents an error that was detected during the validation of a {@link IPMode} when it was attempted to be deployed.
 * <p>These errors will be included in the {@link InvalidPModeException} when the P-Mode that is being deployed is
 * rejected because it does not conform to the current validation rules. This error is therefore normally created by the
 * {@link IPModeValidator} but for some basic parameters it can also be created by the Core's <code>PModeManager</code>
 * directly.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  HB2B_NEXT_VERSION
 */
public class PModeValidationError {

    private final String  parameterName;
    private final String  errorDescription;

    /**
     * Creates a new P-Mode validation error for the given P-Mode parameter and error text.
     *
     * @param parameterInError      The name of the P-Mode parameter that is in error
     * @param errorDescription      Description of the validation rule violation
     */
    public PModeValidationError(final String parameterInError, final String errorDescription) {
        this.parameterName = parameterInError;
        this.errorDescription = errorDescription;
    }

    /**
     * Gets the name of the P-Mode parameter that violated a validation rule.
     *
     * @return  The P-Mode parameter name as a String. NOTE that there is no specified list of P-Mode parameter names
     *          that must be used by a {@link IPModeValidator}, so the processing of the error should not be made
     *          dependent on this result.
     */
    public String getParameterInError() {
        return parameterName;
    }

    /**
     * Gets the description of the validation issue that was encountered as given by the component that executed the
     * validation.
     *
     * @return  The description of the validation problem
     */
    public String getErrorDescription() {
        return errorDescription;
    }
}

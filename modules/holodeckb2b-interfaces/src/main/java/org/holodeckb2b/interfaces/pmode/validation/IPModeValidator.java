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
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 * @since  5.0.0  Added methods for initialisation and determining whether a P-Mode can be validated. Also 
 * 				  implementations MUST now be provided as a Java SPI Service Provider. 
 */
public interface IPModeValidator {

    /**
     * Gets the name of this validator to identify it in logging. This name is only used for logging purposes and it is 
     * recommended to include a version number of the implementation. If no name is specified by the implementation the 
     * class name will be used. 
     *
     * @return  The name of the validator to use in logging
     * @since 5.0.0
     */
    default String getName() { return this.getClass().getName(); }
	
    /**
     * Indicates whether this validator can be used to validate P-Mode of the given type. The <b>PMode.MEPbinding</b> 
     * parameter is used to indicate the type of the P-Mode.
     * 
     * @param pmodeType		type of P-Mode to be validated 
     * @return				<code>true</code> if this validator can validate P-Modes of this type
     * 						<code>false</code> if not. 
     * @since 5.0.0
     */
    boolean doesValidate(final String pmodeType);    
    
    /**
     * Checks whether the given P-Mode conforms to the set of rules as set by this validator.
     * <p>NOTE: Implementations must ensure that this method is thread safe!
     *
     * @param pmode     The {@link IPMode} to check
     * @return          <code>null</code> or empty collection if the given P-Mode is valid,<br>
     *                  or a <code>Collection</code> of {@link PModeValidationError}s the describe the validation errors
     *                  found. It is RECOMMENDED that implementations check all rules and return all validation errors.
     */
    Collection<PModeValidationError> validatePMode(final IPMode pmode);
}

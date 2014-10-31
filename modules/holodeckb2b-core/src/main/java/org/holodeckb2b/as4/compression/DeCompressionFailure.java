/*
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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

package org.holodeckb2b.as4.compression;

import org.holodeckb2b.ebms3.persistent.message.EbmsError;

/**
 * Represent the <i>DeCompressionFailure</i> error as defined in section 3.1 of the AS4 profile and signals that a
 * payload compressed using the AS4 Compression Feature could not be decompressed successfully.
 * <p>As this class is a child class of {@see EbmsError} it can be saved directly to the database. When retrieved from 
 * the database again it is however a "normal" <code>EbmsError</code> as this class is not defined as an JPA class.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class DeCompressionFailure extends EbmsError {
    
    /**
     * The error code as defined in the core specification
     */
    private static final String ERROR_CODE = "EBMS:0303";
    
    /**
     * The default severity of the error as defined in the core specification.
     */
    private static final Severity ERROR_SEVERITY = Severity.FAILURE;
    
    /**
     * The origin of this error is normally the AS4 Compression feature module
     */
    private static final String ERROR_ORIGIN = "as4compression";
    
    /**
     * The default category as specified in the core specification
     */
    private static final String ERROR_CATEGORY = "Communication";
    
    /**
     * The default error message
     */
    private static final String ERROR_MESSAGE = "DeCompressionFailure";
    
    /**
     * Constructs a new <i>DeCompressionFailure</i> error with the default values.
     */
    public DeCompressionFailure() {
        super();
        
        setErrorCode(ERROR_CODE);
        setSeverity(ERROR_SEVERITY);
        setOrigin(ERROR_ORIGIN);
        setCategory(ERROR_CATEGORY);        
        setMessage(ERROR_MESSAGE);
    }
}

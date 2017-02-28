/**
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
package org.holodeckb2b.ebms3.errors;

/**
 * Represent the standard <i>MimeInconsistency</i> error as defined in section 6.7 of the Core specification.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MimeInconsistency extends org.holodeckb2b.common.messagemodel.EbmsError {

    /**
     * The error code as defined in the core specification
     */
    private static final String ERROR_CODE = "EBMS:0007";

    /**
     * The default severity of the error as defined in the core specification.
     */
    private static final Severity ERROR_SEVERITY = Severity.FAILURE;

    /**
     * The origin of this error is normally the ebms module
     */
    private static final String ERROR_ORIGIN = "ebms";

    /**
     * The default category as specified in the core specification
     */
    private static final String ERROR_CATEGORY = "Unpackaging";

    /**
     * The default error message
     */
    private static final String ERROR_SHORT_DESCRIPTION = "MimeInconsistency";

    /**
     * Constructs a new <i>MimeInconsistency</i> error with the default values.
     */
    public MimeInconsistency() {
        super();

        setErrorCode(ERROR_CODE);
        setSeverity(ERROR_SEVERITY);
        setOrigin(ERROR_ORIGIN);
        setCategory(ERROR_CATEGORY);
        setMessage(ERROR_SHORT_DESCRIPTION);
    }

    /**
     * Constructs a new <i>MimeInconsistency</i> error with specified detail message
     *
     * @param errorDetail       A more detailed description of the error
     */
    public MimeInconsistency(final String errorDetail) {
        this();
        setErrorDetail(errorDetail);
    }

   /**
     * Constructs a new <i>MimeInconsistency</i> error with specified detail message and that refers to the given
     * message id
     *
     * @param errorDetail       A more detailed description of the error
     * @param refToMessageId    The message id of the message unit for which this error is created
     */
    public MimeInconsistency(final String errorDetail, final String refToMessageId) {
        this();
        setErrorDetail(errorDetail);
        setRefToMessageInError(refToMessageId);
    }
}

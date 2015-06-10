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

package org.holodeckb2b.as4.receptionawareness;

import org.holodeckb2b.as4.handlers.inflow.CreateReceipt;
import org.holodeckb2b.common.as4.pmode.IReceptionAwareness;
import org.holodeckb2b.ebms3.persistent.message.EbmsError;

/**
 * Represent the <i>MissingReceipt</i> error that is part of the AS4 <i>reception awareness feature</i> and is used to 
 * indicate that no receipt signal was received for a message. For more information on this error and the reception
 * awareness feature see section 3.2 of the AS4 profile.
 * <p>As this class is a child class of {@see EbmsError} it can be saved directly to the database. When retrieved from 
 * the database again it is however a "normal" <code>EbmsError</code> as this class is not defined as an JPA class.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IReceptionAwareness
 * @see RetransmissionWorker
 * @see CreateReceipt
 */
public class MissingReceipt extends EbmsError {
    
    /**
     * The error code as defined in the AS4 specification
     */
    private static final String ERROR_CODE = "EBMS:0301";
    
    /**
     * The default severity of the error as defined in the AS4 specification.
     */
    private static final Severity ERROR_SEVERITY = Severity.FAILURE;
    
    /**
     * The origin of this error is normally the ebms module
     */
    private static final String ERROR_ORIGIN = "ebms";
    
    /**
     * The default category as specified in the AS4 specification
     */
    private static final String ERROR_CATEGORY = "Communication";
    
    /**
     * The default error message
     */
    private static final String ERROR_MESSAGE = "MissingReceipt";
    
    /**
     * Constructs a new <i>MissingReceipt</i> error with the default values.
     */
    public MissingReceipt() {
        super();
        
        setErrorCode(ERROR_CODE);
        setSeverity(ERROR_SEVERITY);
        setOrigin(ERROR_ORIGIN);
        setCategory(ERROR_CATEGORY);        
        setMessage(ERROR_MESSAGE);
    }
    
    
    
}

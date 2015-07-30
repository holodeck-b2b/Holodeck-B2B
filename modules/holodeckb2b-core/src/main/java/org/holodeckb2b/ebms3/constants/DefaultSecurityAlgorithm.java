/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.constants;

import org.holodeckb2b.common.security.X509ReferenceType;

/**
 * Defines constants containing the identifiers for the security algorithms that Holodeck B2B uses as a default when
 * not specified in the P-Mode.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public final class DefaultSecurityAlgorithm {
   
    public static final String SIGNATURE = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    
    public static final String ENCRYPTION = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
    
    /**
     * Note that the default message digest algorithm is both used for signature and encryption (key transport)
     */
    public static final String MESSAGE_DIGEST = "http://www.w3.org/2001/04/xmlenc#sha256";
    
    public static final X509ReferenceType   KEY_REFERENCE = X509ReferenceType.IssuerAndSerial;
    
    public static final String KEY_TRANSPORT = "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";        
}

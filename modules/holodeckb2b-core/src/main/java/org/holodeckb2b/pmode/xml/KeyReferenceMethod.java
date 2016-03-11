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
package org.holodeckb2b.pmode.xml;

import org.holodeckb2b.interfaces.pmode.security.X509ReferenceType;
import org.simpleframework.xml.Text;

/**
 * Represents an element in the P-Mode XML document with type <code>KeyReferenceMethods</code>. Converts the strings 
 * used in the XML document to equivalent value of {@link X509ReferenceType} enumeration.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
class KeyReferenceMethod {

    private static final String ISSUER_SERIAL = "IssuerSerial";
    private static final String BST_REFERENCE = "BSTReference";
    private static final String SKI = "KeyIdentifier";
    
    @Text
    String  referenceMethod = null;
    
    X509ReferenceType   getRefMethod() {
        if (BST_REFERENCE.equals(referenceMethod))
            return X509ReferenceType.BSTReference;
        else if (SKI.equals(referenceMethod))
            return X509ReferenceType.KeyIdentifier;
        else
            return X509ReferenceType.IssuerAndSerial;
    }                        
    
}

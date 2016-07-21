/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.pmode.security;

/**
 * Enumerates the methods to reference an X.509 certificate in the WS-Security header in a SOAP message. The allowed
 * methods are defined in the WS-Security X.509 Certificate Token Profile Version 1.1.1 specification, section 3.2.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public enum X509ReferenceType {

    /**
     * Reference to an X.509 Subject Key Identifier
     */
    KeyIdentifier,

    /**
     * Reference to a Security Token
     */
    BSTReference,

    /**
     * Reference to an Issuer and Serial Number
     */
    IssuerAndSerial
}

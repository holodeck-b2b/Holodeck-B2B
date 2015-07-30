/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.pmode.impl;

import org.holodeckb2b.common.security.IEncryptionConfiguration;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Represents the <code>EncryptionConfiguration</code> element in the P-Mode 
 * XML document that contains the P-Mode parameters for message encryption.
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
@Root
public class EncryptionConfiguration implements IEncryptionConfiguration {
    
    @Element(name = "KeystoreAlias")
    private KeystoreAlias keyStoreRef;
    
    // encryption algorithm
    @Element(name = "Algorithm", required = false)
    private String algorithm = null;
    
    @Element(name = "KeyTransport", required = false)
    private KeyTransport keyTransport = null;
    
    
    @Override
    public String getKeystoreAlias() {
        return keyStoreRef.name;
    }

    @Override
    public String getCertificatePassword() {
        return keyStoreRef.password;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public KeyTransport getKeyTransport() {
        return keyTransport;
    }
    
}

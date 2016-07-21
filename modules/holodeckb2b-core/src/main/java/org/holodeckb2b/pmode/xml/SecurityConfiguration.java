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

import java.util.ArrayList;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.pmode.security.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.security.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.security.IUsernameTokenConfiguration;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;

/**
 * Contains the P-Mode parameters related to the security processing of messages. Security processing is described in
 * section 7 of the ebMS V3 Core Specification and its related P-Mode parameters in appendix D.3.6.
 * <p>NOTE: Security settings are part of the trading partner configuration in Holodeck B2B as it assumed that all
 * messages from one trading partner within the exchange will use the same security settings.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @aurhor Bram Bakx <bram at holodeck-b2b.org>
 */
@Root
public class SecurityConfiguration implements ISecurityConfiguration {

    @ElementList (entry = "UsernameToken", inline = true, required = false)
    protected ArrayList<UsernameToken>   usernameTokens;

    @Element (name = "Signing", required = false)
    protected SignatureConfiguration     signingConfiguration;

    @Element (name = "Encryption", required = false)
    protected EncryptionConfiguration     encryptionConfiguration;


    /**
     * Validates the read XML data to ensure that there are at most two UsernameToken child elements and that each
     * has its own target.
     *
     * @throws PersistenceException     When the read XML document contains more than 2 UsernameToken elements or the
     *                                  specified UsernameToken elements have the same target
     */
    @Validate
    public void validate() throws PersistenceException {
        if (usernameTokens == null)
            return;
        if (usernameTokens.size() > 2)
            throw new PersistenceException("There may not be more than 2 UsernameToken elements", null);
        else if (usernameTokens.size() == 2) {
            // Compare the target attribute value of both elements
            final int c = Utils.compareStrings(usernameTokens.get(0).target, usernameTokens.get(1).target);
            if (c == -1 || c == 0)
                // The targets are equal
                throw new PersistenceException("You must specify separate targets for the UsernameToken elements!",
                                                null);
        }
    }

    @Override
    public IUsernameTokenConfiguration getUsernameTokenConfiguration(final WSSHeaderTarget target) {
        if (usernameTokens == null)
            return null;

        for (final UsernameToken ut : usernameTokens) {
            if (target.name().equalsIgnoreCase(ut.target)
            || (target == WSSHeaderTarget.DEFAULT && (ut.target == null || ut.target.isEmpty())))
                return ut;
        }
        // The correct UsernameToken must be found before the for is exited, so the requested UT is not specified
        return null;
    }

    @Override
    public ISigningConfiguration getSignatureConfiguration() {
        return signingConfiguration;

    }

    @Override
    public IEncryptionConfiguration getEncryptionConfiguration() {
        return encryptionConfiguration;
    }

}

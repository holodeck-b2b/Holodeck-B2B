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

import org.apache.wss4j.dom.WSConstants;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.pmode.IKeyTransport;
import org.holodeckb2b.interfaces.pmode.X509ReferenceType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;

/**
 * Represents the <code>KeyTransport</code> element in the P-Mode XML document
 * that contains the P-Mode parameters for key transport when encrypting message.
 *
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
public class KeyTransport implements IKeyTransport {

    @Element (name = "Algorithm", required = false)
    private String algorithm;

    @Element (name = "MGFAlgorithm", required = false)
    private String MGFAlgorithm;

    @Element (name = "DigestAlgorithm", required = false)
    private String digestAlgorithm;

    @Element(name = "KeyReferenceMethod", required = false)
    private KeyReferenceMethod keyReferenceMethod;

    /**
     * Validates the <code>KeyTransport</code> element included in this P-Mode.
     * <p>When included the element must have at least one child. Also when the <i>RSA-OAEP</i> algorithm is specified
     * an MGF algorithm must be specified.
     *
     * @throws PersistenceException If there is not at least one child element or when no MGF algorithm is specified in
     *                              case <i>RSA-OAEP</i> is specified
     */
    @Validate
    public void validate() throws PersistenceException {

        // Check at least one child element is included
        if (Utils.getValue(algorithm, null) == null
        && Utils.getValue(MGFAlgorithm, null) == null
        && Utils.getValue(digestAlgorithm, null) == null
        && Utils.getValue(keyReferenceMethod.referenceMethod, null) == null)
            throw new PersistenceException("KeyTransport MUST have at least one child element", null);

        // Check if MGF is specified in case RSA-OAEP is kt algorithm
        if (WSConstants.KEYTRANSPORT_RSAOEP_XENC11.equalsIgnoreCase(algorithm)
            && Utils.getValue(MGFAlgorithm, null) == null)
            throw new PersistenceException("You MUST specify a MGF algorithm when specifying RSA-OAEP as "
                                            + "key transport algorithm", null);

    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String getMGFAlgorithm() {
        return MGFAlgorithm;
    }

    @Override
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    @Override
    public X509ReferenceType getKeyReferenceMethod() {
        return (keyReferenceMethod != null ? keyReferenceMethod.getRefMethod() : null);
    }
}

/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.packaging;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Created at 13:09 15.10.16
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class SOAPEnvTest {

    @Test
    public void testCreateEnvelope() throws Exception {
        SOAPEnvelope env =
                SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        Iterator<OMNamespace> it = env.getNamespacesInScope();

        HashSet<String> prefixSet = new HashSet<>();

        while (it.hasNext()) {
            OMNamespace omNamespace = it.next();
            String prefix = omNamespace.getPrefix();
            prefixSet.add(prefix);
            String uri = omNamespace.getNamespaceURI();
            if (prefix.equals("soapenv")) {
                assertTrue(uri.equals("http://www.w3.org/2003/05/soap-envelope"));
            } else if (prefix.equals(EbMSConstants.EBMS3_NS_PREFIX)) {
                assertTrue(uri.equals(EbMSConstants.EBMS3_NS_URI));
            } else if (prefix.equals("xsd")) {
                assertTrue(uri.equals("http://www.w3.org/1999/XMLSchema"));
            } else if (prefix.equals("xsi")) {
                assertTrue(uri.equals("http://www.w3.org/1999/XMLSchema-instance/"));
            }
        }

        assertTrue(prefixSet.contains("soapenv"));
        assertTrue(prefixSet.contains(EbMSConstants.EBMS3_NS_PREFIX));
        assertTrue(prefixSet.contains("xsd"));
        assertTrue(prefixSet.contains("xsi"));
    }
}
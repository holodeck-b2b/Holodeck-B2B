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
package org.holodeckb2b.security.handlers;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.packaging.*;
import org.holodeckb2b.ebms3.persistency.entities.AgreementReference;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.holodeckb2b.pmode.helpers.*;
import org.holodeckb2b.testhelpers.HolodeckCore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created at 20:31 16.10.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CreateWSSHeadersTest {

    static final QName SECURITY_ELEMENT_NAME =
            new QName(SecurityConstants.WSS_NAMESPACE_URI, "Security");
    static final QName SIGNATURE_ELEMENT_NAME =
            new QName(SecurityConstants.DSIG_NAMESPACE_URI, "Signature");
    static final QName CANONICALIZATION_METHOD_ELEMENT_NAME =
            new QName(SecurityConstants.DSIG_NAMESPACE_URI, "CanonicalizationMethod");
    static final QName SIGNATURE_METHOD_ELEMENT_NAME =
            new QName(SecurityConstants.DSIG_NAMESPACE_URI, "SignatureMethod");
    static final QName SIGNED_INFO_ELEMENT_NAME =
            new QName(SecurityConstants.DSIG_NAMESPACE_URI, "SignedInfo");
    static final QName SIGNATURE_VALUE_ELEMENT_NAME =
            new QName(SecurityConstants.DSIG_NAMESPACE_URI, "SignatureValue");
    static final QName KEY_INFO_ELEMENT_NAME =
            new QName(SecurityConstants.DSIG_NAMESPACE_URI, "KeyInfo");
    static final QName USERNAME_TOKEN_ELEMENT_NAME =
            new QName(SecurityConstants.WSS_NAMESPACE_URI, "UsernameToken");
    static final QName USERNAME_ELEMENT_NAME =
            new QName(SecurityConstants.WSS_NAMESPACE_URI, "Username");
    static final QName PASSWORD_ELEMENT_NAME =
            new QName(SecurityConstants.WSS_NAMESPACE_URI, "Password");

    private static String baseDir;

    private static HolodeckCore core;

    private CreateWSSHeaders handler;

    @BeforeClass
    public static void setUpClass() {
        baseDir = CreateWSSHeadersTest.class
                .getClassLoader().getResource("security").getPath();
        core = new HolodeckCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new CreateWSSHeaders();
    }

    @Test
    public void testDoProcessing () throws Exception {
        System.out.println("[testDoProcessing]>");
        final String mmdPath =
                this.getClass().getClassLoader()
                        .getResource("security/handlers/full_mmd.xml").getPath();
        final File f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        // Creating SOAP envelope
        SOAPEnvelope env =
                SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        UserMessage.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);
        mc.setProperty(SecurityConstants.ADD_SECURITY_HEADERS, Boolean.TRUE);

        // Setting signature configuration
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        sigConfig.setCertificatePassword("ExampleCA");

        // Setting encription configuration
        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("exampleca");
        encConfig.setCertificatePassword("ExampleCA");

        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        mc.setProperty(SecurityConstants.SIGNATURE, sigConfig);
        // First of all we need to test RaiseSignatureCreatedEvent
        // So, we need only signature headers by now
        // todo Setting encription causes org.apache.xml.security.exceptions.XMLSecurityException
        // todo for unknown reason. I'll check this later (T.S.)
//        mc.setProperty(SecurityConstants.ENCRYPTION, encConfig);
        mc.setProperty(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);
        mc.setProperty(SecurityConstants.DEFAULT_USERNAMETOKEN, tokenConfig);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        try {
            System.out.println("[before handler.invoke()]>");
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            System.out.println("<[after handler.invoke()]");
            assertEquals("InvocationResponse.CONTINUE", invokeResp.toString());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        env = mc.getEnvelope();
//        System.out.println(env);
        SOAPHeader header = env.getHeader();
        ArrayList securityHeaders =
                header.getHeaderBlocksWithNSURI(SecurityConstants.WSS_NAMESPACE_URI);
        assertNotNull(securityHeaders);
        // Should contain two security headers, one for the hb2b-sec:ebms;UsernameToken
        // another for hb2b-sec:wsse:UsernameToken
        assertTrue(securityHeaders.size() == 2);
        boolean containsSignatureElement = false;
        Iterator it;
        for(int i = 0; i < 2; i++) {
            OMElement s = (OMElement) securityHeaders.get(i);
            assertEquals(SECURITY_ELEMENT_NAME, s.getQName());
            it = s.getChildrenWithName(SIGNATURE_ELEMENT_NAME);
            if (it.hasNext()) {
                containsSignatureElement = true;
                OMElement sigElem = (OMElement)it.next();
                it = sigElem.getChildrenWithName(SIGNED_INFO_ELEMENT_NAME);
                assertTrue(it.hasNext());
                if (it.hasNext()) {
                    OMElement signedInfo = (OMElement)it.next();
                    it = signedInfo.getChildrenWithName(CANONICALIZATION_METHOD_ELEMENT_NAME);
                    assertTrue(it.hasNext());
                    it = signedInfo.getChildrenWithName(SIGNATURE_METHOD_ELEMENT_NAME);
                    assertTrue(it.hasNext());
                    // todo check presence of the References
                }
                it = sigElem.getChildrenWithName(SIGNATURE_VALUE_ELEMENT_NAME);
                assertTrue(it.hasNext());
                if (it.hasNext()) {
                    OMElement sigValue = (OMElement)it.next();
                    // todo check the contents of sigValue
                }
                it = sigElem.getChildrenWithName(KEY_INFO_ELEMENT_NAME);
                assertTrue(it.hasNext());
                if (it.hasNext()) {
                    OMElement keyInfo = (OMElement)it.next();
                    // todo check the contents of keyInfo
                }
            }
            it = s.getChildrenWithName(USERNAME_TOKEN_ELEMENT_NAME);
            assertTrue(it.hasNext());
            if (it.hasNext()) {
                OMElement usernameToken = (OMElement)it.next();
                it = usernameToken.getChildrenWithName(USERNAME_ELEMENT_NAME);
                assertTrue(it.hasNext());
                if(it.hasNext()) {
                    OMElement username = (OMElement)it.next();
                    assertEquals("username", username.getText());
                }
                it = usernameToken.getChildrenWithName(PASSWORD_ELEMENT_NAME);
                assertTrue(it.hasNext());
                if(it.hasNext()) {
                    OMElement password = (OMElement)it.next();
                    assertEquals("secret", password.getText());
                }
            }
        }
        // One of the two security elements should contain Signature element
        assertTrue(containsSignatureElement);
        System.out.println("<[testDoProcessing]");
    }
}

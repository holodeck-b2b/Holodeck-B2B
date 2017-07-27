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
package org.holodeckb2b.security.util;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.X509ReferenceType;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import org.holodeckb2b.pmode.helpers.UsernameTokenConfig;
import org.holodeckb2b.security.tokens.UsernameToken;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 22:02 06.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class SecurityUtilsTest {

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = SecurityUtilsTest.class.getClassLoader()
                .getResource("security").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        // Adding appender to the FindPModes logger
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    @Test
    public void testVerifyUsernameToken() throws Exception {
        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");
        tokenConfig.setPasswordType(UTPasswordType.TEXT);

        UsernameTokenPrincipal principalMock = mock(UsernameTokenPrincipal.class);
        when(principalMock.getName()).thenReturn("username");
        when(principalMock.getPassword()).thenReturn("secret");

        UsernameToken actualToken = new UsernameToken(principalMock);

        assertTrue(SecurityUtils.verifyUsernameToken(tokenConfig, actualToken));
    }

    @Test
    public void testVerifySignature() throws Exception {
        ISigningConfiguration expected = mock(ISigningConfiguration.class);

        String expAlias = "partya";
        final IConfiguration config =
                HolodeckB2BCoreInterface.getConfiguration();
        // Get the password for accessing the keystore
        char[] keystorePwd = config.getPublicKeyStorePassword().toCharArray();
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        FileInputStream fis =
                new java.io.FileInputStream(config.getPublicKeyStorePath());
        keyStore.load(fis, keystorePwd);
        X509Certificate cert =
                (X509Certificate) keyStore.getCertificate(expAlias);

        org.holodeckb2b.security.tokens.X509Certificate actual =
                new org.holodeckb2b.security.tokens.X509Certificate(cert);

        SecurityUtils.verifySignature(expected, actual);
    }

    @Test
    public void testCreateCryptoConfig() throws Exception {
        IConfiguration config = HolodeckB2BCoreInterface.getConfiguration();
        String     keyStoreFile = null;
        String     keyStorePwd = null;
        String     keyStoreType = "keystore";

        SecurityUtils.CertType certType = SecurityUtils.CertType.pub;
        Properties props = SecurityUtils.createCryptoConfig(certType);

        keyStoreFile = config.getPublicKeyStorePath();
        keyStorePwd = config.getPublicKeyStorePassword();

        assertEquals(props.getProperty("org.apache.wss4j.crypto.provider"),
                "org.apache.wss4j.common.crypto.Merlin");
        assertEquals(props.getProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".file"),
                keyStoreFile);
        assertEquals(props.getProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".type"),
                "jks");
        assertEquals(props.getProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".password"),
                keyStorePwd);

        certType = SecurityUtils.CertType.priv;
        props = SecurityUtils.createCryptoConfig(certType);

        keyStoreFile = config.getPrivateKeyStorePath();
        keyStorePwd = config.getPrivateKeyStorePassword();

        assertEquals(props.getProperty("org.apache.wss4j.crypto.provider"),
                "org.apache.wss4j.common.crypto.Merlin");
        assertEquals(props.getProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".file"),
                keyStoreFile);
        assertEquals(props.getProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".type"),
                "jks");
        assertEquals(props.getProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".password"),
                keyStorePwd);

        certType = SecurityUtils.CertType.trust;
        props = SecurityUtils.createCryptoConfig(certType);

        keyStoreFile = config.getTrustKeyStorePath();
        keyStorePwd = config.getTrustKeyStorePassword();
        keyStoreType = "truststore";

        assertEquals(props.getProperty("org.apache.wss4j.crypto.provider"),
                "org.apache.wss4j.common.crypto.Merlin");
        assertEquals(props.getProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".file"),
                keyStoreFile);
        assertEquals(props.getProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".type"),
                "jks");
        assertEquals(props.getProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".password"),
                keyStorePwd);
    }

    @Test
    public void testGetWSS4JX509KeyId() throws Exception {
        X509ReferenceType refType = X509ReferenceType.BSTReference;
        String keyId = SecurityUtils.getWSS4JX509KeyId(refType);
        assertEquals("DirectReference", keyId);
        refType = X509ReferenceType.KeyIdentifier;
        keyId = SecurityUtils.getWSS4JX509KeyId(refType);
        assertEquals("SKIKeyIdentifier", keyId);
        refType = X509ReferenceType.IssuerAndSerial;
        keyId = SecurityUtils.getWSS4JX509KeyId(refType);
        assertEquals("IssuerSerial", keyId);
    }

    @Test
    public void testGetKeystoreAlias() throws Exception {
        String expAlias = "partya";
        final IConfiguration config =
                HolodeckB2BCoreInterface.getConfiguration();
        // Get the password for accessing the keystore
        char[] keystorePwd = config.getPublicKeyStorePassword().toCharArray();
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        FileInputStream fis =
                new java.io.FileInputStream(config.getPublicKeyStorePath());
        keyStore.load(fis, keystorePwd);
        X509Certificate cert =
                (X509Certificate) keyStore.getCertificate(expAlias);
        String alias = SecurityUtils.getKeystoreAlias(cert);
        assertEquals(expAlias, alias);
    }

    @Test
    public void testGetSignatureReferences() throws Exception {
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        SOAPHeader header = mc.getEnvelope().getHeader();

        // Initializing spies for real objects
        MessageContext mcSpy = spy(mc);
        SOAPEnvelope envSpy = spy(env);
        SOAPHeader headerSpy = spy(header);
        SOAPHeaderBlock headerBlockSpy = spy(headerBlock);

        ArrayList<SOAPHeaderBlock> headerBlocks = new ArrayList<>();
        headerBlocks.add(headerBlockSpy);

        doReturn(envSpy).when(mcSpy).getEnvelope();
        doReturn(headerSpy).when(envSpy).getHeader();
        doReturn(headerBlocks).when(headerSpy)
                .getHeaderBlocksWithNSURI(SecurityConstants.WSS_NAMESPACE_URI);

        Iterator<OMElement> sigElemsItMock = mock(Iterator.class);
        when(sigElemsItMock.hasNext()).thenReturn(true);

        OMElement sigElemMock = mock(OMElement.class);
        when(sigElemsItMock.next()).thenReturn(sigElemMock);

        doReturn(sigElemsItMock).when(headerBlockSpy).getChildrenWithName(
                new QName(SecurityConstants.DSIG_NAMESPACE_URI, "Signature"));

        OMElement sigInfoElemMock = mock(OMElement.class);
        when(sigElemMock.getFirstElement()).thenReturn(sigInfoElemMock);

        Iterator<OMElement> sigInfoElemsItMock = mock(Iterator.class);
        // Making sure sigInfoElemsItMock can return only one element
        when(sigInfoElemsItMock.hasNext()).thenReturn(true).thenReturn(false);

        when(sigInfoElemMock.getChildrenWithName(
                new QName(SecurityConstants.DSIG_NAMESPACE_URI, "Reference")))
                .thenReturn(sigInfoElemsItMock);

        OMElement refMock = mock(OMElement.class);
        when(sigInfoElemsItMock.next()).thenReturn(refMock);

        Collection<OMElement> refs = SecurityUtils.getSignatureReferences(mcSpy);

        assertNotNull(refs);
        assertTrue(refs.size() == 1);
    }

    @Test
    public void testIsPrivateKeyAvailable() throws Exception {
        String alias = "exampleca";
        String keyPassword = "ExampleCA";
        boolean result = SecurityUtils.isPrivateKeyAvailable(alias, keyPassword);
        assertTrue(result);
    }

    @Test
    public void testIsCertificateAvailable() throws Exception {
        String alias = "exampleca";
        boolean keyTrust = true;
        boolean result = SecurityUtils.isCertificateAvailable(alias, keyTrust);
        assertTrue(result);
    }
}
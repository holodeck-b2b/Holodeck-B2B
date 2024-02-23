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
package org.holodeckb2b.ebms3.pmode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.holodeckb2b.common.pmode.BusinessInfo;
import org.holodeckb2b.common.pmode.EncryptionConfig;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.PartnerConfig;
import org.holodeckb2b.common.pmode.PullRequestFlow;
import org.holodeckb2b.common.pmode.SecurityConfig;
import org.holodeckb2b.common.pmode.SigningConfig;
import org.holodeckb2b.common.pmode.UserMessageFlow;
import org.holodeckb2b.common.pmode.UsernameTokenConfig;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link BasicPModeValidator}
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class BasicPModeValidatorTest {

    private static BasicPModeValidator validator = new BasicPModeValidator();


    @BeforeClass
    public static void setUpClass() throws Exception {
        HolodeckB2BTestCore core = new HolodeckB2BTestCore();
        core.setCertificateManager(new CertManagerMock(TestUtils.getTestClassBasePath()));
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    /**
     * Tests a valid simple P-Mode which does not contain security settings
     */
    @Test
    public void testMinimalValidPMode() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        validPMode.addLeg(new Leg());

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(validPMode)));
    }

    /**
     * Tests a valid P-Mode which contains security settings
     */
    @Test
    public void testValidPModeWithSecurity() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.TWO_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.TWO_WAY_PUSH_PULL);

        PartnerConfig initiator = new PartnerConfig();
        SecurityConfig secConfig = new SecurityConfig();
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        secConfig.setSignatureConfiguration(sigConfig);
        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("partya");
        secConfig.setEncryptionConfiguration(encConfig);
        initiator.setSecurityConfiguration(secConfig);
        validPMode.setInitiator(initiator);

        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        PartnerConfig responder = new PartnerConfig();
        secConfig = new SecurityConfig();
        sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("partyb");
        sigConfig.setCertificatePassword("ExampleB");
        secConfig.setSignatureConfiguration(sigConfig);
        encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("partyb");
        encConfig.setCertificatePassword("ExampleB");
        secConfig.setEncryptionConfiguration(encConfig);
        responder.setSecurityConfiguration(secConfig);
        validPMode.setResponder(responder);

        validPMode.addLeg(new Leg());

        Leg leg = new Leg();
        PullRequestFlow prFlow = new PullRequestFlow();
        prFlow.setMPC("sub-MPC");
        secConfig = new SecurityConfig();
        sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("partyc");
        secConfig.setSignatureConfiguration(sigConfig);
        prFlow.setSecurityConfiguration(secConfig);
        leg.addPullRequestFlow(prFlow);
        validPMode.addLeg(leg);

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(validPMode)));
    }

    /**
     * Tests valid P-Modes which contain multiple PullRequestFlows
     */
    @Test
    public void testValidMultiplePRFlows() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PULL);

        Leg leg = new Leg();
        PullRequestFlow prFlow = new PullRequestFlow();
        prFlow.setMPC(EbMSConstants.DEFAULT_MPC + "/sub-MPC-1");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC(EbMSConstants.DEFAULT_MPC + "/sub-MPC-2");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC(EbMSConstants.DEFAULT_MPC + "/sub-MPC-3");
        leg.addPullRequestFlow(prFlow);
        validPMode.addLeg(leg);

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(validPMode)));

        validPMode = new PMode();
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PULL);

        leg = new Leg();
        UserMessageFlow umFlow = new UserMessageFlow();
        BusinessInfo busInfo = new BusinessInfo();
        busInfo.setMpc("MPC");
        umFlow.setBusinnessInfo(busInfo);
        leg.setUserMessageFlow(umFlow);

        prFlow = new PullRequestFlow();
        prFlow.setMPC("MPC-1");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC("MPC-2");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC("MPC-3");
        leg.addPullRequestFlow(prFlow);
        validPMode.addLeg(leg);

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(validPMode)));
    }

    @Test
    public void testInvalidGeneralParameters() {
    	PMode invalidPMode = new PMode();

        invalidPMode.setMep(EbMSConstants.ONE_WAY_MEP+"/smth"); // wrong mep
        invalidPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        invalidPMode.addLeg(new Leg());

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
        invalidPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        invalidPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH + "/smth"); // wrong mep binding

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
        invalidPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        invalidPMode.addLeg(new Leg()); // adding second leg

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        invalidPMode.setLegs(null);
        invalidPMode.setMep(EbMSConstants.TWO_WAY_MEP);
        invalidPMode.addLeg(new Leg()); // adding only one leg into two way mep

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
    }

    @Test
    public void testInvalidUsernameTokenParameters() {
    	PMode invalidPMode = new PMode();

    	invalidPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        invalidPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        invalidPMode.addLeg(new Leg());

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        PartnerConfig initiator = new PartnerConfig();
        SecurityConfig secConfig = new SecurityConfig();
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        secConfig.setSignatureConfiguration(sigConfig);
        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("partya");
        secConfig.setEncryptionConfiguration(encConfig);
        initiator.setSecurityConfiguration(secConfig);
        invalidPMode.setInitiator(initiator);

        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();

        assertTrue(Utils.isNullOrEmpty(tokenConfig.getUsername()));

        // non null empty token with default header
        secConfig.setUsernameTokenConfiguration(
                SecurityHeaderTarget.DEFAULT, tokenConfig);

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        tokenConfig = new UsernameTokenConfig();

        // non null empty token with EBMS header
        secConfig.setUsernameTokenConfiguration(
                SecurityHeaderTarget.EBMS, tokenConfig);

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
    }

    @Test
    public void testInvalidX509Parameters() {
    	PMode invalidPMode =  HB2BTestUtils.create1WaySendPushPMode();

        PartnerConfig initiator = new PartnerConfig();
        SecurityConfig secConfig = new SecurityConfig();

        SigningConfig sigConfig = new SigningConfig();
        secConfig.setSignatureConfiguration(sigConfig);

        EncryptionConfig encConfig = new EncryptionConfig();
        secConfig.setEncryptionConfiguration(encConfig);

        initiator.setSecurityConfiguration(secConfig);
        invalidPMode.setInitiator(initiator);

        // Missing aliases
        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        encConfig.setKeystoreAlias("partya");
        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        encConfig.setCertificatePassword("ExampleA");

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        sigConfig.setKeystoreAlias("partyb");

        assertNotNull(sigConfig.getKeystoreAlias());

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        sigConfig.setCertificatePassword("ExampleB");
        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
    }

    static class CertManagerMock implements ICertificateManager {

    	private KeyStore	keystore;

    	CertManagerMock(Path ksPath) {
            try (FileInputStream fis = new java.io.FileInputStream(ksPath.resolve("keystore.jks").toFile())) {
            	keystore = KeyStore.getInstance("JKS");
                keystore.load(fis, "test123456".toCharArray());
            } catch (NullPointerException | IOException | KeyStoreException | NoSuchAlgorithmException
                    | CertificateException ex) {
                throw new IllegalArgumentException("Can not load the keystore!", ex);
            }
    	}

		@Override
		public PrivateKeyEntry getKeyPair(String alias, String password) throws SecurityProcessingException {
			try {
				return (KeyStore.PrivateKeyEntry) keystore.getEntry(alias,
															new KeyStore.PasswordProtection(password.toCharArray()));
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public X509Certificate getPartnerCertificate(String alias) throws SecurityProcessingException {
			try {
				return (X509Certificate) keystore.getCertificate(alias);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public String findCertificate(X509Certificate cert)
				throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IValidationResult validateTrust(List<X509Certificate> certs) throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public X509Certificate findCertificate(X500Principal issuer, BigInteger serial)
				throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public X509Certificate findCertificate(byte[] skiBytes) throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String findKeyPair(X509Certificate cert) throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String findKeyPair(PublicKey key) throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String findKeyPair(X500Principal issuer, BigInteger serial) throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String findKeyPair(byte[] skiBytes) throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String findKeyPair(byte[] hash, MessageDigest digester) throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<X509Certificate> getKeyPairCertificates(String alias) throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public X509Certificate findCertificate(byte[] hash, MessageDigest digester) throws SecurityProcessingException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void init(IConfiguration config) throws SecurityProcessingException {
			// TODO Auto-generated method stub

		}

		@Override
		public void shutdown() {
			// TODO Auto-generated method stub

		}
    }
}

/**
 * Copyright (C) 2924 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.cert.X509Certificate;
import java.util.Collection;

import org.apache.axis2.AxisFault;
import org.holodeckb2b.common.pmode.EncryptionConfig;
import org.holodeckb2b.common.pmode.KeyAgreementConfig;
import org.holodeckb2b.common.pmode.KeyDerivationConfig;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.PartnerConfig;
import org.holodeckb2b.common.pmode.SecurityConfig;
import org.holodeckb2b.common.pmode.SigningConfig;
import org.holodeckb2b.common.pmode.UsernameTokenConfig;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestCertificateManager;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.ebms3.security.PModeValidator;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PModeValidatorTest {

	@BeforeAll
	static void setup() throws AxisFault {
		HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
	}

	@AfterEach
	void cleanup() throws SecurityProcessingException {
		certManager().clear();
	}

	private TestCertificateManager certManager() throws SecurityProcessingException {
		return ((TestCertificateManager) HolodeckB2BCoreInterface.getCertificateManager());
	}

	@Test
	void testNoSecConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		pmode.setInitiator(new PartnerConfig());
		pmode.setResponder(new PartnerConfig());

		PModeValidator validator = new PModeValidator();

		assertTrue(validator.doesValidate(pmode.getMepBinding()));
		assertTrue(validator.validatePMode(pmode).isEmpty());
	}

	@Test
	void testValidUTConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);

		UsernameTokenConfig utConfig = new UsernameTokenConfig();
		utConfig.setUsername("uname");
		utConfig.setPassword("password");
		secConfig.setUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT, utConfig);
		UsernameTokenConfig utConfig2 = new UsernameTokenConfig();
		utConfig2.setUsername("uname");
		utConfig2.setPassword("password");
		secConfig.setUsernameTokenConfiguration(SecurityHeaderTarget.EBMS, utConfig2);

		PModeValidator validator = new PModeValidator();

		assertTrue(validator.validatePMode(pmode).isEmpty());
	}

	@Test
	void testInvalidDefaultUTConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);

		UsernameTokenConfig utConfig = new UsernameTokenConfig();
		secConfig.setUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT, utConfig);

		PModeValidator validator = new PModeValidator();

		Collection<PModeValidationError> errors = validator.validatePMode(pmode);
		assertEquals(2, errors.size());
		assertTrue(errors.stream().allMatch(e -> e.getParameterInError().contains("default")));

		utConfig.setUsername("uname");
		assertEquals(1, validator.validatePMode(pmode).size());

		utConfig.setUsername(null);
		utConfig.setPassword("password");
		assertEquals(1, validator.validatePMode(pmode).size());
	}

	@Test
	void testInvalidEbMSUTConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);

		UsernameTokenConfig utConfig = new UsernameTokenConfig();
		secConfig.setUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT, utConfig);

		PModeValidator validator = new PModeValidator();

		Collection<PModeValidationError> errors = validator.validatePMode(pmode);
		assertEquals(2, errors.size());
		assertTrue(errors.stream().allMatch(e -> e.getParameterInError().contains("default")));
	}

	@Test
	void testIgoreSigConfigReceiver() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setResponder(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);
		SigningConfig sigConfig = new SigningConfig();
		secConfig.setSignatureConfiguration(sigConfig);

		assertTrue(new PModeValidator().validatePMode(pmode).isEmpty());
	}

	@Test
	void testMissingSignKPConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);
		SigningConfig sigConfig = new SigningConfig();
		secConfig.setSignatureConfiguration(sigConfig);

		assertEquals(2, new PModeValidator().validatePMode(pmode).size());
	}

	@Test
	void testMissingSignKP() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);
		SigningConfig sigConfig = new SigningConfig();
		sigConfig.setKeystoreAlias("alias");
		sigConfig.setCertificatePassword("password");
		secConfig.setSignatureConfiguration(sigConfig);

		assertEquals(1, new PModeValidator().validatePMode(pmode).size());
	}

	@Test
	void testIncompatSigningKP() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);
		SigningConfig sigConfig = new SigningConfig();
		sigConfig.setKeystoreAlias("alias");
		sigConfig.setCertificatePassword("password");
		sigConfig.setSignatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
		secConfig.setSignatureConfiguration(sigConfig);

		assertDoesNotThrow(() ->
					certManager().registerKeyPair(KeystoreUtils.readKeyPairFromPKCS12(
																TestUtils.getTestResource("keypairs/ec.p12"), "test"),
										sigConfig.getKeystoreAlias(), sigConfig.getCertificatePassword()));

		Collection<PModeValidationError> errors = new PModeValidator().validatePMode(pmode);

		assertEquals(1, errors.size());
		assertTrue(errors.stream().allMatch(e -> e.getErrorDescription().contains("compatible")));

		sigConfig.setSignatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256");
		sigConfig.setKeystoreAlias("alias2");
		sigConfig.setCertificatePassword("password2");

		assertDoesNotThrow(() ->
					certManager().registerKeyPair(KeystoreUtils.readKeyPairFromPKCS12(
													TestUtils.getTestResource("keypairs/rsa.p12"), "test"),
							sigConfig.getKeystoreAlias(), sigConfig.getCertificatePassword()));

		errors = new PModeValidator().validatePMode(pmode);

		assertEquals(1, errors.size());
		assertTrue(errors.stream().allMatch(e -> e.getErrorDescription().contains("compatible")));
	}

	@Test
	void testValidSigConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);
		SigningConfig sigConfig = new SigningConfig();
		sigConfig.setKeystoreAlias("alias");
		sigConfig.setCertificatePassword("password");
		sigConfig.setSignatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
		secConfig.setSignatureConfiguration(sigConfig);

		assertDoesNotThrow(() ->
					certManager().registerKeyPair(KeystoreUtils.readKeyPairFromPKCS12(
																TestUtils.getTestResource("keypairs/rsa.p12"), "test"),
										sigConfig.getKeystoreAlias(), sigConfig.getCertificatePassword()));

		assertTrue(new PModeValidator().validatePMode(pmode).isEmpty());
	}

	@Test
	void testValidEncKTConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setResponder(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);

		EncryptionConfig encConfig = new EncryptionConfig();
		encConfig.setKeystoreAlias("alias");

		assertDoesNotThrow(() ->
					certManager().registerPartnerCertificate((X509Certificate) KeystoreUtils.readKeyPairFromPKCS12(
												TestUtils.getTestResource("keypairs/rsa.p12"), "test").getCertificate(),
							encConfig.getKeystoreAlias()));

		assertTrue(new PModeValidator().validatePMode(pmode).isEmpty());
	}

	@Test
	void testValidEncKAConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setResponder(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);

		EncryptionConfig encConfig = new EncryptionConfig();
		encConfig.setKeystoreAlias("alias");

		KeyAgreementConfig kaConfig = new KeyAgreementConfig();
		kaConfig.setAgreementMethod("http://www.w3.org/2009/xmlenc11#ECDH-ES");
		kaConfig.setKeyEncryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#kw-aes128");
		kaConfig.setCertReferenceMethod(X509ReferenceType.IssuerAndSerial);
		KeyDerivationConfig kdfConfig = new KeyDerivationConfig();
		kdfConfig.setAlgorithm("http://www.w3.org/2009/xmlenc11#ConcatKDF");
		kdfConfig.setDigestAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
		kaConfig.setKeyDerivatonMethod(kdfConfig);
		encConfig.setKeyAgreement(kaConfig);

		assertDoesNotThrow(() ->
		certManager().registerPartnerCertificate((X509Certificate) KeystoreUtils.readKeyPairFromPKCS12(
												TestUtils.getTestResource("keypairs/ec.p12"), "test").getCertificate(),
												encConfig.getKeystoreAlias()));

		assertTrue(new PModeValidator().validatePMode(pmode).isEmpty());
	}

	@Test
	void testInvalidEncKAConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setResponder(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);

		EncryptionConfig encConfig = new EncryptionConfig();
		encConfig.setKeystoreAlias("alias");
		secConfig.setEncryptionConfiguration(encConfig);

		KeyAgreementConfig kaConfig = new KeyAgreementConfig();
		kaConfig.setAgreementMethod("http://www.w3.org/2009/xmlenc11#DH-ES");
		kaConfig.setKeyEncryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#kw-aes128");
		kaConfig.setCertReferenceMethod(X509ReferenceType.IssuerAndSerial);
		KeyDerivationConfig kdfConfig = new KeyDerivationConfig();
		kdfConfig.setAlgorithm("http://www.w3.org/2009/xmlenc11#pbkdf2");
		kdfConfig.setDigestAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
		kaConfig.setKeyDerivatonMethod(kdfConfig);
		encConfig.setKeyAgreement(kaConfig);

		assertDoesNotThrow(() ->
		certManager().registerPartnerCertificate((X509Certificate) KeystoreUtils.readKeyPairFromPKCS12(
				TestUtils.getTestResource("keypairs/rsa.p12"), "test").getCertificate(),
				encConfig.getKeystoreAlias()));

		assertEquals(3, new PModeValidator().validatePMode(pmode).size());
	}

	@Test
	void testValidDecrConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);

		EncryptionConfig encConfig = new EncryptionConfig();
		encConfig.setKeystoreAlias("alias");
		encConfig.setCertificatePassword("password");
		secConfig.setEncryptionConfiguration(encConfig);

		assertDoesNotThrow(() ->
		certManager().registerKeyPair(KeystoreUtils.readKeyPairFromPKCS12(
												TestUtils.getTestResource("keypairs/rsa.p12"), "test"),
												encConfig.getKeystoreAlias(), encConfig.getCertificatePassword()));

		assertTrue(new PModeValidator().validatePMode(pmode).isEmpty());
	}

	@Test
	void testMissingDecrKPConfig() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);

		EncryptionConfig encConfig = new EncryptionConfig();
		secConfig.setEncryptionConfiguration(encConfig);

		PModeValidator validator = new PModeValidator();

		assertEquals(2, validator.validatePMode(pmode).size());
	}

	@Test
	void testMissingDecrKP() {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		PartnerConfig partnerConfig = new PartnerConfig();
		pmode.setInitiator(partnerConfig);
		SecurityConfig secConfig = new SecurityConfig();
		partnerConfig.setSecurityConfiguration(secConfig);

		EncryptionConfig encConfig = new EncryptionConfig();
		encConfig.setKeystoreAlias("alias");
		encConfig.setCertificatePassword("password");
		secConfig.setEncryptionConfiguration(encConfig);

		PModeValidator validator = new PModeValidator();

		assertEquals(1, validator.validatePMode(pmode).size());
	}


}

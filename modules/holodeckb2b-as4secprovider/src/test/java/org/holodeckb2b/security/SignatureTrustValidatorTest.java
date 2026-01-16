/*
 * Copyright (C) 2024 The Holodeck B2B Team
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

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.apache.axis2.AxisFault;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.validate.Credential;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestCertificateManager;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.ebms3.security.SignatureTrustValidator;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.security.trust.IValidationResult.Trust;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Functional tests for the signature trust validator.
 * <p>
 * The {@link SignatureTrustValidator} bridges WSS4J's signature verification with Holodeck B2B's
 * Certificate Manager for trust validation. It receives the certificate chain from a signature
 * and delegates trust validation to the configured Certificate Manager.
 * <p>
 * These tests verify:
 * <ul>
 *   <li>Certificates signed by trusted CA are accepted</li>
 *   <li>Self-signed certificates without trust anchor are rejected</li>
 *   <li>Missing or empty certificate chains are rejected</li>
 *   <li>Validation results are properly stored for retrieval</li>
 * </ul>
 */
class SignatureTrustValidatorTest {

    private static KeyStore.PrivateKeyEntry rsaKeyPair;
    private static KeyStore.PrivateKeyEntry ecKeyPair;

    private SignatureTrustValidator validator;

    @BeforeAll
    static void setupCore() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());

        rsaKeyPair = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("keypairs/rsa.p12"), "test");
        ecKeyPair = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("keypairs/ec.p12"), "test");
    }

    private TestCertificateManager certManager() throws SecurityProcessingException {
        return (TestCertificateManager) HolodeckB2BCoreInterface.getCertificateManager();
    }

    @BeforeEach
    void setUp() {
        validator = new SignatureTrustValidator();
    }

    @AfterEach
    void cleanup() throws SecurityProcessingException {
        certManager().clear();
    }

    // ==================================================================================
    // Tests for trusted certificate validation
    // ==================================================================================

    @Nested
    @DisplayName("When validating trusted certificates")
    class TrustedCertificates {

        @Test
        @DisplayName("it should accept a certificate that is registered as trusted")
        void acceptsTrustedCertificate() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();
            certManager().registerTrustedCertificate(cert, "trustedcert");

            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { cert });

            Credential result = validator.validate(credential, null);

            assertNotNull(result);
            assertEquals(credential, result);
        }

        @Test
        @DisplayName("it should provide the validation result after successful validation")
        void providesValidationResultForTrusted() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();
            certManager().registerTrustedCertificate(cert, "trustedcert");

            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { cert });

            validator.validate(credential, null);

            IValidationResult validationResult = validator.getValidationResult();

            assertNotNull(validationResult);
            assertEquals(Trust.OK, validationResult.getTrust());
        }

        @Test
        @DisplayName("it should accept EC certificates when trusted")
        void acceptsTrustedEcCertificate() throws Exception {
            X509Certificate cert = (X509Certificate) ecKeyPair.getCertificate();
            certManager().registerTrustedCertificate(cert, "trustedeccert");

            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { cert });

            Credential result = validator.validate(credential, null);

            assertNotNull(result);
        }
    }

    // ==================================================================================
    // Tests for untrusted certificate rejection
    // ==================================================================================

    @Nested
    @DisplayName("When validating untrusted certificates")
    class UntrustedCertificates {

        @Test
        @DisplayName("it should reject certificates that are not registered as trusted")
        void rejectsUntrustedCertificate() {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();
            // Certificate is NOT registered as trusted

            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { cert });

            WSSecurityException exception = assertThrows(WSSecurityException.class,
                () -> validator.validate(credential, null));

            assertEquals(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, exception.getErrorCode());
        }

        @Test
        @DisplayName("it should provide the validation result after failed validation")
        void providesValidationResultForUntrusted() {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();
            // Certificate is NOT registered as trusted

            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { cert });

            try {
                validator.validate(credential, null);
            } catch (WSSecurityException e) {
                // Expected
            }

            IValidationResult validationResult = validator.getValidationResult();

            assertNotNull(validationResult);
            assertEquals(Trust.NOK, validationResult.getTrust());
        }
    }

    // ==================================================================================
    // Tests for invalid input handling
    // ==================================================================================

    @Nested
    @DisplayName("When receiving invalid credentials")
    class InvalidCredentials {

        @Test
        @DisplayName("it should reject credentials with null certificate array")
        void rejectsNullCertificates() {
            Credential credential = new Credential();
            credential.setCertificates(null);

            WSSecurityException exception = assertThrows(WSSecurityException.class,
                () -> validator.validate(credential, null));

            assertEquals(WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN, exception.getErrorCode());
        }

        @Test
        @DisplayName("it should reject credentials with empty certificate array")
        void rejectsEmptyCertificates() {
            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[0]);

            WSSecurityException exception = assertThrows(WSSecurityException.class,
                () -> validator.validate(credential, null));

            assertEquals(WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN, exception.getErrorCode());
        }
    }

    // ==================================================================================
    // Tests for validation result availability
    // ==================================================================================

    @Nested
    @DisplayName("When querying validation results")
    class ValidationResults {

        @Test
        @DisplayName("it should return null when no validation has been performed")
        void returnsNullBeforeValidation() {
            IValidationResult result = validator.getValidationResult();

            assertNull(result);
        }

        @Test
        @DisplayName("validation result should contain the validated certificate path")
        void resultContainsCertificatePath() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();
            certManager().registerTrustedCertificate(cert, "trustedcert");

            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { cert });

            validator.validate(credential, null);

            IValidationResult result = validator.getValidationResult();

            assertNotNull(result.getValidatedCertPath());
            assertFalse(result.getValidatedCertPath().isEmpty());
            assertEquals(cert, result.getValidatedCertPath().get(0));
        }
    }
}

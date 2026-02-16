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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.validate.Credential;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.ebms3.security.SignatureTrustValidator;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.security.trust.IValidationResult.Trust;
import org.holodeckb2b.interfaces.security.trust.SecurityLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link SignatureTrustValidator} verifying that it correctly bridges between WSS4J's signature
 * verification and Holodeck B2B's Certificate Manager for trust validation.
 * <p>
 * The Certificate Manager is mocked to verify that the validator:
 * <ul>
 *   <li>Passes the correct certificate(s) to {@link ICertificateManager#validateCertificate}</li>
 *   <li>Stores the returned {@link IValidationResult} for later retrieval</li>
 *   <li>Throws the appropriate {@link WSSecurityException} based on the validation outcome</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SignatureTrustValidatorTest {

    private static HolodeckB2BTestCore testCore;

    @Mock
    private ICertificateManager mockCertManager;

    @Mock
    private IValidationResult mockResult;

    @Mock
    private X509Certificate testCert;

    private SignatureTrustValidator validator;

    @BeforeAll
    static void setupCore() throws Exception {
        testCore = new HolodeckB2BTestCore();
        HolodeckB2BCoreInterface.setImplementation(testCore);
    }

    @BeforeEach
    void setUp() {
        testCore.setCertificateManager(mockCertManager);
        validator = new SignatureTrustValidator();
    }

    // ==================================================================================
    // Tests for successful trust validation
    // ==================================================================================

    @Nested
    @DisplayName("When the Certificate Manager reports the certificate as trusted")
    class TrustedCertificate {

        @BeforeEach
        void setupTrustedResult() throws Exception {
            when(mockResult.getTrust()).thenReturn(Trust.OK);
            when(mockCertManager.validateCertificate(anyList(), any(SecurityLevel.class)))
                    .thenReturn(mockResult);
        }

        @Test
        @DisplayName("it should pass the certificate chain to the Certificate Manager")
        void passesCertificatesToCertManager() throws Exception {
            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { testCert });

            validator.validate(credential, null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<X509Certificate>> certsCaptor = ArgumentCaptor.forClass(List.class);
            verify(mockCertManager).validateCertificate(certsCaptor.capture(), eq(SecurityLevel.MLS));

            List<X509Certificate> passedCerts = certsCaptor.getValue();
            assertEquals(1, passedCerts.size());
            assertSame(testCert, passedCerts.get(0));
        }

        @Test
        @DisplayName("it should pass a full 3-level certificate path to the Certificate Manager")
        void passesFullCertificatePathToCertManager(@Mock X509Certificate intermediateCert,
                                                    @Mock X509Certificate rootCert) throws Exception {
            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { testCert, intermediateCert, rootCert });

            validator.validate(credential, null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<X509Certificate>> certsCaptor = ArgumentCaptor.forClass(List.class);
            verify(mockCertManager).validateCertificate(certsCaptor.capture(), eq(SecurityLevel.MLS));

            List<X509Certificate> passedCerts = certsCaptor.getValue();
            assertEquals(3, passedCerts.size());
            assertSame(testCert, passedCerts.get(0), "Leaf certificate should be first");
            assertSame(intermediateCert, passedCerts.get(1), "Intermediate CA should be second");
            assertSame(rootCert, passedCerts.get(2), "Root CA should be last");
        }

        @Test
        @DisplayName("it should return the credential on successful validation")
        void returnsCredential() throws Exception {
            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { testCert });

            Credential result = validator.validate(credential, null);

            assertSame(credential, result);
        }

        @Test
        @DisplayName("it should store the validation result for later retrieval")
        void storesValidationResult() throws Exception {
            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { testCert });

            validator.validate(credential, null);

            assertSame(mockResult, validator.getValidationResult());
        }
    }

    // ==================================================================================
    // Tests for failed trust validation
    // ==================================================================================

    @Nested
    @DisplayName("When the Certificate Manager reports the certificate as untrusted")
    class UntrustedCertificate {

        @BeforeEach
        void setupUntrustedResult() throws Exception {
            when(mockResult.getTrust()).thenReturn(Trust.NOK);
            when(mockCertManager.validateCertificate(anyList(), any(SecurityLevel.class)))
                    .thenReturn(mockResult);
        }

        @Test
        @DisplayName("it should throw WSSecurityException with FAILED_AUTHENTICATION")
        void throwsFailedAuthentication() {
            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { testCert });

            WSSecurityException exception = assertThrows(WSSecurityException.class,
                    () -> validator.validate(credential, null));

            assertEquals(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, exception.getErrorCode());
        }

        @Test
        @DisplayName("it should still store the validation result for later retrieval")
        void storesValidationResult() {
            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { testCert });

            assertThrows(WSSecurityException.class, () -> validator.validate(credential, null));

            assertSame(mockResult, validator.getValidationResult());
        }
    }

    // ==================================================================================
    // Tests for Certificate Manager errors
    // ==================================================================================

    @Nested
    @DisplayName("When the Certificate Manager throws an exception")
    class CertManagerError {

        @BeforeEach
        void setupError() throws Exception {
            when(mockCertManager.validateCertificate(anyList(), any(SecurityLevel.class)))
                    .thenThrow(new SecurityProcessingException("Certificate manager error"));
        }

        @Test
        @DisplayName("it should throw WSSecurityException with FAILED_CHECK")
        void throwsFailedCheck() {
            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[] { testCert });

            WSSecurityException exception = assertThrows(WSSecurityException.class,
                    () -> validator.validate(credential, null));

            assertEquals(WSSecurityException.ErrorCode.FAILED_CHECK, exception.getErrorCode());
        }
    }

    // ==================================================================================
    // Tests for invalid input handling
    // ==================================================================================

    @Nested
    @DisplayName("When receiving invalid credentials")
    class InvalidCredentials {

        @Test
        @DisplayName("it should reject credentials with null certificate array without calling Certificate Manager")
        void rejectsNullCertificates() throws Exception {
            Credential credential = new Credential();
            credential.setCertificates(null);

            WSSecurityException exception = assertThrows(WSSecurityException.class,
                    () -> validator.validate(credential, null));

            assertEquals(WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN, exception.getErrorCode());
            verifyNoInteractions(mockCertManager);
        }

        @Test
        @DisplayName("it should reject credentials with empty certificate array without calling Certificate Manager")
        void rejectsEmptyCertificates() throws Exception {
            Credential credential = new Credential();
            credential.setCertificates(new X509Certificate[0]);

            WSSecurityException exception = assertThrows(WSSecurityException.class,
                    () -> validator.validate(credential, null));

            assertEquals(WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN, exception.getErrorCode());
            verifyNoInteractions(mockCertManager);
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
            assertNull(validator.getValidationResult());
        }
    }
}

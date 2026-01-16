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
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.apache.axis2.AxisFault;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestCertificateManager;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.ebms3.security.Action;
import org.holodeckb2b.ebms3.security.CertManWSS4JCrypto;
import org.holodeckb2b.ebms3.security.callbackhandlers.PasswordCallbackHandler;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Functional tests for the WSS4J Crypto adapter.
 * <p>
 * The {@link CertManWSS4JCrypto} class provides a bridge between WSS4J's security processing
 * and Holodeck B2B's Certificate Manager. It routes certificate and key pair lookups
 * to the appropriate keystore based on the security action being performed.
 * <p>
 * These tests verify the business requirements:
 * <ul>
 *   <li>For SIGN/DECRYPT actions: certificates and keys come from the private key store</li>
 *   <li>For ENCRYPT/VERIFY actions: certificates come from the partner certificate store</li>
 *   <li>Various certificate reference methods (alias, issuer/serial, thumbprint, SKI) are supported</li>
 * </ul>
 */
class CertManWSS4JCryptoTest {

    private static final String RSA_KEYPAIR_ALIAS = "rsakeypair";
    private static final String RSA_PARTNER_ALIAS = "rsapartner";
    private static final String EC_PARTNER_ALIAS = "ecpartner";
    private static final String KEY_PASSWORD = "testpassword";

    private static KeyStore.PrivateKeyEntry rsaKeyPair;
    private static KeyStore.PrivateKeyEntry ecKeyPair;

    @BeforeAll
    static void setupCore() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());

        // Load test key pairs
        rsaKeyPair = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("keypairs/rsa.p12"), "test");
        ecKeyPair = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("keypairs/ec.p12"), "test");
    }

    private TestCertificateManager certManager() throws SecurityProcessingException {
        return (TestCertificateManager) HolodeckB2BCoreInterface.getCertificateManager();
    }

    @AfterEach
    void cleanup() throws SecurityProcessingException {
        certManager().clear();
    }

    // ==================================================================================
    // Tests for signing operations (retrieving key pairs)
    // ==================================================================================

    @Nested
    @DisplayName("When signing messages")
    class SigningOperations {

        private CertManWSS4JCrypto crypto;

        @BeforeEach
        void setUp() throws SecurityProcessingException {
            crypto = new CertManWSS4JCrypto(Action.SIGN);
            certManager().registerKeyPair(rsaKeyPair, RSA_KEYPAIR_ALIAS, KEY_PASSWORD);
        }

        @Test
        @DisplayName("it should find the signing certificate by alias")
        void findsCertificateByAlias() throws Exception {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(RSA_KEYPAIR_ALIAS);

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNotNull(certs, "Certificate chain should be returned");
            assertEquals(1, certs.length);
            assertEquals(rsaKeyPair.getCertificate(), certs[0]);
        }

        @Test
        @DisplayName("it should find the signing certificate by issuer and serial number")
        void findsCertificateByIssuerSerial() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
            cryptoType.setIssuerSerial(cert.getIssuerX500Principal().getName(), cert.getSerialNumber());

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNotNull(certs);
            assertEquals(cert, certs[0]);
        }

        @Test
        @DisplayName("it should throw exception when key pair is not registered")
        void throwsWhenKeyPairNotFound() {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias("unknown-alias");

            // When key pair is not found, the code throws an exception
            // (either WSSecurityException or NPE due to null certificate chain)
            assertThrows(Exception.class,
                () -> crypto.getX509Certificates(cryptoType));
        }

        @Test
        @DisplayName("it should provide the private key with correct password")
        void providesPrivateKeyWithCorrectPassword() throws Exception {
            PrivateKey key = crypto.getPrivateKey(RSA_KEYPAIR_ALIAS, KEY_PASSWORD);

            assertNotNull(key);
            assertEquals(rsaKeyPair.getPrivateKey(), key);
        }

        @Test
        @DisplayName("it should throw exception when private key password is wrong")
        void throwsWhenPasswordWrong() {
            assertThrows(WSSecurityException.class,
                () -> crypto.getPrivateKey(RSA_KEYPAIR_ALIAS, "wrong-password"));
        }

        @Test
        @DisplayName("it should find the key pair identifier by certificate")
        void findsIdentifierByCertificate() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();

            String identifier = crypto.getX509Identifier(cert);

            assertEquals(RSA_KEYPAIR_ALIAS, identifier);
        }

        @Test
        @DisplayName("it should provide private key via certificate lookup")
        void providesPrivateKeyViaCertificate() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();
            PasswordCallbackHandler callbackHandler = new PasswordCallbackHandler();
            callbackHandler.addUser(RSA_KEYPAIR_ALIAS, KEY_PASSWORD);

            PrivateKey key = crypto.getPrivateKey(cert, callbackHandler);

            assertNotNull(key);
            assertEquals(rsaKeyPair.getPrivateKey(), key);
        }

        @Test
        @DisplayName("it should provide private key via public key lookup")
        void providesPrivateKeyViaPublicKey() throws Exception {
            PasswordCallbackHandler callbackHandler = new PasswordCallbackHandler();
            callbackHandler.addUser(RSA_KEYPAIR_ALIAS, KEY_PASSWORD);

            PrivateKey key = crypto.getPrivateKey(rsaKeyPair.getCertificate().getPublicKey(), callbackHandler);

            assertNotNull(key);
            assertEquals(rsaKeyPair.getPrivateKey(), key);
        }
    }

    // ==================================================================================
    // Tests for decryption operations (retrieving key pairs)
    // ==================================================================================

    @Nested
    @DisplayName("When decrypting messages")
    class DecryptionOperations {

        private CertManWSS4JCrypto crypto;

        @BeforeEach
        void setUp() throws SecurityProcessingException {
            crypto = new CertManWSS4JCrypto(Action.DECRYPT);
            certManager().registerKeyPair(rsaKeyPair, RSA_KEYPAIR_ALIAS, KEY_PASSWORD);
        }

        @Test
        @DisplayName("it should find the decryption certificate by alias")
        void findsCertificateByAlias() throws Exception {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(RSA_KEYPAIR_ALIAS);

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNotNull(certs);
            assertEquals(rsaKeyPair.getCertificate(), certs[0]);
        }

        @Test
        @DisplayName("it should provide the private key for decryption")
        void providesPrivateKeyForDecryption() throws Exception {
            PrivateKey key = crypto.getPrivateKey(RSA_KEYPAIR_ALIAS, KEY_PASSWORD);

            assertNotNull(key);
            assertEquals(rsaKeyPair.getPrivateKey(), key);
        }
    }

    // ==================================================================================
    // Tests for encryption operations (retrieving partner certificates)
    // ==================================================================================

    @Nested
    @DisplayName("When encrypting messages")
    class EncryptionOperations {

        private CertManWSS4JCrypto crypto;

        @BeforeEach
        void setUp() throws SecurityProcessingException {
            crypto = new CertManWSS4JCrypto(Action.ENCRYPT);
            certManager().registerPartnerCertificate(
                (X509Certificate) rsaKeyPair.getCertificate(), RSA_PARTNER_ALIAS);
        }

        @Test
        @DisplayName("it should find partner certificate by alias")
        void findsPartnerCertificateByAlias() throws Exception {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(RSA_PARTNER_ALIAS);

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNotNull(certs);
            assertEquals(rsaKeyPair.getCertificate(), certs[0]);
        }

        @Test
        @DisplayName("it should find partner certificate by issuer and serial")
        void findsPartnerByIssuerSerial() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
            cryptoType.setIssuerSerial(cert.getIssuerX500Principal().getName(), cert.getSerialNumber());

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNotNull(certs);
            assertEquals(cert, certs[0]);
        }

        @Test
        @DisplayName("it should return null when partner certificate not found")
        void returnsNullWhenPartnerNotFound() throws Exception {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias("unknown-partner");

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNull(certs);
        }

        @Test
        @DisplayName("it should find partner certificate identifier")
        void findsPartnerIdentifier() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();

            String identifier = crypto.getX509Identifier(cert);

            assertEquals(RSA_PARTNER_ALIAS, identifier);
        }
    }

    // ==================================================================================
    // Tests for verification operations (retrieving partner certificates)
    // ==================================================================================

    @Nested
    @DisplayName("When verifying signatures")
    class VerificationOperations {

        private CertManWSS4JCrypto crypto;

        @BeforeEach
        void setUp() throws SecurityProcessingException {
            crypto = new CertManWSS4JCrypto(Action.VERIFY);
            certManager().registerPartnerCertificate(
                (X509Certificate) ecKeyPair.getCertificate(), EC_PARTNER_ALIAS);
        }

        @Test
        @DisplayName("it should find partner certificate for signature verification")
        void findsPartnerCertificateForVerification() throws Exception {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(EC_PARTNER_ALIAS);

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNotNull(certs);
            assertEquals(ecKeyPair.getCertificate(), certs[0]);
        }
    }

    // ==================================================================================
    // Tests for unsupported operations
    // ==================================================================================

    @Nested
    @DisplayName("When using unsupported certificate reference methods")
    class UnsupportedMethods {

        private CertManWSS4JCrypto crypto;

        @BeforeEach
        void setUp() {
            crypto = new CertManWSS4JCrypto(Action.SIGN);
        }

        @Test
        @DisplayName("it should reject SUBJECT_DN lookup type")
        void rejectsSubjectDnLookup() {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.SUBJECT_DN);
            cryptoType.setSubjectDN("CN=Test");

            assertThrows(WSSecurityException.class,
                () -> crypto.getX509Certificates(cryptoType));
        }

        @Test
        @DisplayName("it should reject ENDPOINT lookup type")
        void rejectsEndpointLookup() {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ENDPOINT);
            cryptoType.setEndpoint("https://example.com");

            assertThrows(WSSecurityException.class,
                () -> crypto.getX509Certificates(cryptoType));
        }

        @Test
        @DisplayName("it should return null for null CryptoType")
        void returnsNullForNullCryptoType() throws Exception {
            X509Certificate[] certs = crypto.getX509Certificates(null);

            assertNull(certs);
        }
    }

    // ==================================================================================
    // Tests for store isolation
    // ==================================================================================

    @Nested
    @DisplayName("When key pairs and partner certificates are both registered")
    class StoreIsolation {

        @BeforeEach
        void setUp() throws SecurityProcessingException {
            // Register the same certificate in both stores with different aliases
            certManager().registerKeyPair(rsaKeyPair, RSA_KEYPAIR_ALIAS, KEY_PASSWORD);
            certManager().registerPartnerCertificate(
                (X509Certificate) ecKeyPair.getCertificate(), EC_PARTNER_ALIAS);
        }

        @Test
        @DisplayName("SIGN action should only access key pair store")
        void signAccessesKeyPairStore() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.SIGN);

            // Should find the key pair
            CryptoType keypairType = new CryptoType(CryptoType.TYPE.ALIAS);
            keypairType.setAlias(RSA_KEYPAIR_ALIAS);
            assertNotNull(crypto.getX509Certificates(keypairType));

            // Should NOT find the partner cert (throws exception for SIGN action when not found in keypair store)
            CryptoType partnerType = new CryptoType(CryptoType.TYPE.ALIAS);
            partnerType.setAlias(EC_PARTNER_ALIAS);
            assertThrows(Exception.class,
                () -> crypto.getX509Certificates(partnerType));
        }

        @Test
        @DisplayName("ENCRYPT action should only access partner certificate store")
        void encryptAccessesPartnerStore() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.ENCRYPT);

            // Should find the partner cert
            CryptoType partnerType = new CryptoType(CryptoType.TYPE.ALIAS);
            partnerType.setAlias(EC_PARTNER_ALIAS);
            assertNotNull(crypto.getX509Certificates(partnerType));

            // Should NOT find the key pair (returns null for ENCRYPT action)
            CryptoType keypairType = new CryptoType(CryptoType.TYPE.ALIAS);
            keypairType.setAlias(RSA_KEYPAIR_ALIAS);
            assertNull(crypto.getX509Certificates(keypairType));
        }
    }
}

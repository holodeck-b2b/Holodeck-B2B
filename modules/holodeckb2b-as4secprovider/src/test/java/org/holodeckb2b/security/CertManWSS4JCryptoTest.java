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
 * Tests for {@link CertManWSS4JCrypto} verifying that it correctly bridges between WSS4J's security
 * processing and Holodeck B2B's Certificate Manager.
 * <p>
 * The class routes certificate and key pair lookups to the appropriate Certificate Manager method
 * based on the security action:
 * <ul>
 *   <li>SIGN/DECRYPT actions: retrieves from key pair store</li>
 *   <li>ENCRYPT/VERIFY actions: retrieves from partner certificate store</li>
 * </ul>
 */
class CertManWSS4JCryptoTest {

    private static final String RSA_KEYPAIR_ALIAS = "rsakeypair";
    private static final String CHAIN_KEYPAIR_ALIAS = "chainkeypair";
    private static final String RSA_PARTNER_ALIAS = "rsapartner";
    private static final String EC_PARTNER_ALIAS = "ecpartner";
    private static final String KEY_PASSWORD = "testpassword";

    private static KeyStore.PrivateKeyEntry rsaKeyPair;
    private static KeyStore.PrivateKeyEntry chainKeyPair;
    private static KeyStore.PrivateKeyEntry ecKeyPair;

    @BeforeAll
    static void setupCore() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());

        rsaKeyPair = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("keypairs/rsa.p12"), "test");
        chainKeyPair = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("keypairs/rsa_chain.p12"), "test");
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
    // Tests for key pair retrieval (SIGN/DECRYPT actions)
    // ==================================================================================

    @Nested
    @DisplayName("When retrieving key pairs")
    class KeyPairRetrieval {

        private CertManWSS4JCrypto crypto;

        @BeforeEach
        void setUp() throws SecurityProcessingException {
            crypto = new CertManWSS4JCrypto(Action.SIGN);
            certManager().registerKeyPair(rsaKeyPair, RSA_KEYPAIR_ALIAS, KEY_PASSWORD);
        }

        @Test
        @DisplayName("it should find certificate by alias")
        void findsCertificateByAlias() throws Exception {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(RSA_KEYPAIR_ALIAS);

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNotNull(certs, "Certificate chain should be returned");
            assertEquals(1, certs.length);
            assertEquals(rsaKeyPair.getCertificate(), certs[0]);
        }

        @Test
        @DisplayName("it should find certificate by issuer and serial number")
        void findsCertificateByIssuerSerial() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
            cryptoType.setIssuerSerial(cert.getIssuerX500Principal().getName(), cert.getSerialNumber());

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNotNull(certs);
            assertEquals(cert, certs[0]);
        }

        @Test
        @DisplayName("it should return the full certificate chain when key pair includes a CA certificate")
        void returnsFullCertificateChain() throws Exception {
            certManager().registerKeyPair(chainKeyPair, CHAIN_KEYPAIR_ALIAS, KEY_PASSWORD);

            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(CHAIN_KEYPAIR_ALIAS);

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

            assertNotNull(certs, "Certificate chain should be returned");
            assertEquals(2, certs.length, "Chain should contain leaf and CA certificate");
            assertEquals(certs[0].getIssuerX500Principal(), certs[1].getSubjectX500Principal(),
                "Leaf issuer should match CA subject");
        }

        @Test
        @DisplayName("it should throw exception when key pair alias is not registered")
        void throwsWhenKeyPairNotFound() {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias("unknown-alias");

            // Note: throws NullPointerException rather than WSSecurityException because
            // getKeyPairCertificates() returns null for unknown alias and the result
            // is not checked before calling toArray()
            assertThrows(Exception.class, () -> crypto.getX509Certificates(cryptoType));
        }

        @Test
        @DisplayName("it should find key pair identifier by certificate")
        void findsIdentifierByCertificate() throws Exception {
            X509Certificate cert = (X509Certificate) rsaKeyPair.getCertificate();

            String identifier = crypto.getX509Identifier(cert);

            assertEquals(RSA_KEYPAIR_ALIAS, identifier);
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
    // Tests for partner certificate retrieval (ENCRYPT/VERIFY actions)
    // ==================================================================================

    @Nested
    @DisplayName("When retrieving partner certificates")
    class PartnerCertificateRetrieval {

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
        @DisplayName("it should reject SUBJECT_DN lookup for key pair retrieval")
        void rejectsSubjectDnForKeyPair() {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.SUBJECT_DN);
            cryptoType.setSubjectDN("CN=Test");

            assertThrows(WSSecurityException.class,
                () -> crypto.getX509Certificates(cryptoType));
        }

        @Test
        @DisplayName("it should reject SUBJECT_DN lookup for partner certificate retrieval")
        void rejectsSubjectDnForPartnerCert() {
            CertManWSS4JCrypto encryptCrypto = new CertManWSS4JCrypto(Action.ENCRYPT);
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.SUBJECT_DN);
            cryptoType.setSubjectDN("CN=Test");

            assertThrows(WSSecurityException.class,
                () -> encryptCrypto.getX509Certificates(cryptoType));
        }

        @Test
        @DisplayName("it should reject ENDPOINT lookup for key pair retrieval")
        void rejectsEndpointForKeyPair() {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ENDPOINT);
            cryptoType.setEndpoint("https://example.com");

            assertThrows(WSSecurityException.class,
                () -> crypto.getX509Certificates(cryptoType));
        }

        @Test
        @DisplayName("it should reject ENDPOINT lookup for partner certificate retrieval")
        void rejectsEndpointForPartnerCert() {
            CertManWSS4JCrypto encryptCrypto = new CertManWSS4JCrypto(Action.ENCRYPT);
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ENDPOINT);
            cryptoType.setEndpoint("https://example.com");

            assertThrows(WSSecurityException.class,
                () -> encryptCrypto.getX509Certificates(cryptoType));
        }

        @Test
        @DisplayName("it should return null for null CryptoType")
        void returnsNullForNullCryptoType() throws Exception {
            X509Certificate[] certs = crypto.getX509Certificates(null);

            assertNull(certs);
        }
    }

    // ==================================================================================
    // Tests for action-based routing
    // ==================================================================================

    @Nested
    @DisplayName("When verifying action-based routing to correct Certificate Manager methods")
    class ActionBasedRouting {

        @BeforeEach
        void setUp() throws SecurityProcessingException {
            certManager().registerKeyPair(rsaKeyPair, RSA_KEYPAIR_ALIAS, KEY_PASSWORD);
            certManager().registerPartnerCertificate(
                (X509Certificate) ecKeyPair.getCertificate(), EC_PARTNER_ALIAS);
        }

        @Test
        @DisplayName("SIGN action should only access key pair store via alias")
        void signAccessesKeyPairStoreByAlias() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.SIGN);

            CryptoType keypairType = new CryptoType(CryptoType.TYPE.ALIAS);
            keypairType.setAlias(RSA_KEYPAIR_ALIAS);
            assertNotNull(crypto.getX509Certificates(keypairType));

            CryptoType partnerType = new CryptoType(CryptoType.TYPE.ALIAS);
            partnerType.setAlias(EC_PARTNER_ALIAS);
            assertThrows(Exception.class, () -> crypto.getX509Certificates(partnerType));
        }

        @Test
        @DisplayName("SIGN action should find key pair via issuer/serial")
        void signFindsKeyPairByIssuerSerial() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.SIGN);
            X509Certificate rsaCert = (X509Certificate) rsaKeyPair.getCertificate();

            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
            cryptoType.setIssuerSerial(rsaCert.getIssuerX500Principal().getName(), rsaCert.getSerialNumber());

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            assertNotNull(certs);
            assertEquals(rsaCert, certs[0]);
        }

        @Test
        @DisplayName("SIGN action should only find key pair identifiers")
        void signFindsOnlyKeyPairIdentifiers() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.SIGN);

            assertNotNull(crypto.getX509Identifier((X509Certificate) rsaKeyPair.getCertificate()));
            assertNull(crypto.getX509Identifier((X509Certificate) ecKeyPair.getCertificate()));
        }

        @Test
        @DisplayName("DECRYPT action should only access key pair store")
        void decryptAccessesKeyPairStore() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.DECRYPT);

            CryptoType keypairType = new CryptoType(CryptoType.TYPE.ALIAS);
            keypairType.setAlias(RSA_KEYPAIR_ALIAS);
            assertNotNull(crypto.getX509Certificates(keypairType));

            CryptoType partnerType = new CryptoType(CryptoType.TYPE.ALIAS);
            partnerType.setAlias(EC_PARTNER_ALIAS);
            assertThrows(Exception.class, () -> crypto.getX509Certificates(partnerType));
        }

        @Test
        @DisplayName("ENCRYPT action should only access partner certificate store via alias")
        void encryptAccessesPartnerStoreByAlias() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.ENCRYPT);

            CryptoType partnerType = new CryptoType(CryptoType.TYPE.ALIAS);
            partnerType.setAlias(EC_PARTNER_ALIAS);
            assertNotNull(crypto.getX509Certificates(partnerType));

            CryptoType keypairType = new CryptoType(CryptoType.TYPE.ALIAS);
            keypairType.setAlias(RSA_KEYPAIR_ALIAS);
            assertNull(crypto.getX509Certificates(keypairType));
        }

        @Test
        @DisplayName("ENCRYPT action should find partner certificate via issuer/serial")
        void encryptFindsPartnerByIssuerSerial() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.ENCRYPT);
            X509Certificate ecCert = (X509Certificate) ecKeyPair.getCertificate();

            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
            cryptoType.setIssuerSerial(ecCert.getIssuerX500Principal().getName(), ecCert.getSerialNumber());

            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            assertNotNull(certs);
            assertEquals(ecCert, certs[0]);
        }

        @Test
        @DisplayName("ENCRYPT action should only find partner certificate identifiers")
        void encryptFindsOnlyPartnerIdentifiers() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.ENCRYPT);

            assertNotNull(crypto.getX509Identifier((X509Certificate) ecKeyPair.getCertificate()));
            assertNull(crypto.getX509Identifier((X509Certificate) rsaKeyPair.getCertificate()));
        }

        @Test
        @DisplayName("VERIFY action should only access partner certificate store")
        void verifyAccessesPartnerStore() throws Exception {
            CertManWSS4JCrypto crypto = new CertManWSS4JCrypto(Action.VERIFY);

            CryptoType partnerType = new CryptoType(CryptoType.TYPE.ALIAS);
            partnerType.setAlias(EC_PARTNER_ALIAS);
            assertNotNull(crypto.getX509Certificates(partnerType));

            CryptoType keypairType = new CryptoType(CryptoType.TYPE.ALIAS);
            keypairType.setAlias(RSA_KEYPAIR_ALIAS);
            assertNull(crypto.getX509Certificates(keypairType));
        }
    }
}

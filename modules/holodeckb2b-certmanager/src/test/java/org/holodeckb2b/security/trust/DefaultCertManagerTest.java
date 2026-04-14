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
package org.holodeckb2b.security.trust;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.security.trust.IValidationResult.Trust;
import org.holodeckb2b.interfaces.security.trust.SecurityLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Functional tests for the Certificate Manager component.
 * <p>
 * The Certificate Manager is responsible for:
 * <ul>
 *   <li>Managing key pairs used for signing and decrypting messages</li>
 *   <li>Managing trading partner certificates used for encryption</li>
 *   <li>Validating trust in certificates used in message security</li>
 * </ul>
 * <p>
 * These tests verify the business requirements without depending on internal implementation details.
 */
class DefaultCertManagerTest {

    private static final String VALID_CONFIG = "valid-config.xml";
    private static final String REVO_OPT_CONFIG = "opt-revocation-config.xml";
    private static final String REVO_MAN_CONFIG = "man-revocation-config.xml";

    private static final String KEYSTORES_DIR = "keystores/";
    private static final String PRIVATE_KEYSTORE = KEYSTORES_DIR + "privatekeys.jks";
    private static final String PARTNER_KEYSTORE = KEYSTORES_DIR + "partnercerts.jks";
    private static final String TRUST_KEYSTORE = KEYSTORES_DIR + "trustanchors.jks";
    private static final String KEYSTORE_PWD = "test123";

    private static final String SELFSIGNED_ALIAS = "partye";
    private static final String CHAIN_ALIAS = "partyz";

    private static final String KEYPAIR_PASSWORD = "test123";

    private static final String PARTNER_ALIAS = "partyf";

    private static final String ROOT_CA_ALIAS = "root-ca";

    private ICertificateManager certManager;

    @BeforeEach
    void setUp() {
        certManager = new DefaultCertManager();
    }

    /**
     * Sets up a test environment with the necessary keystores and configuration.
     */
    private IConfiguration setupEnvironment(String configFile) throws IOException {
        Path confDir = TestUtils.getTestResource("conf");
        Files.createDirectories(confDir);

        // Copy config file
        Files.copy(TestUtils.getTestResource("config/" + configFile),
                   confDir.resolve("certmanager_config.xml"), StandardCopyOption.REPLACE_EXISTING);

        IConfiguration config = mock(IConfiguration.class);
        when(config.getHolodeckB2BHome()).thenReturn(TestUtils.getTestClassBasePath());
        return config;
    }

    private X509Certificate loadCertFromKeystore(String keystore, String alias) throws Exception {
    	return (X509Certificate) KeystoreUtils.load(TestUtils.getTestResource(keystore), KEYSTORE_PWD).getCertificate(alias);
    }

    private List<X509Certificate> loadCertPathFromKeystore() throws Exception {
    	KeyStore keystore = KeystoreUtils.load(TestUtils.getTestResource(PRIVATE_KEYSTORE), KEYSTORE_PWD);
		return Arrays.stream(keystore.getCertificateChain(CHAIN_ALIAS)).map(c -> (X509Certificate) c).collect(Collectors.toList());
    }

    private int countCertsInKeystore(Path keystorePath, String password) throws Exception {
    	return KeystoreUtils.load(keystorePath, password).size();
    }

    // ==================================================================================
    // Startup and Configuration Tests
    // ==================================================================================

    @Nested
    @DisplayName("When starting the Certificate Manager")
    class Startup {

        @Test
        @DisplayName("it should start successfully with valid configuration")
        void startsWithCompleteConfiguration() throws Exception {
            IConfiguration config = setupEnvironment(VALID_CONFIG);

            assertDoesNotThrow(() -> certManager.init(config));

            int defaults = countCertsInKeystore(Path.of(System.getProperty("java.home"), "lib", "security", "cacerts"),
            									"changeit");
            int partners = countCertsInKeystore(TestUtils.getTestResource(PARTNER_KEYSTORE), KEYSTORE_PWD);
            int trusted = countCertsInKeystore(TestUtils.getTestResource(TRUST_KEYSTORE), KEYSTORE_PWD);

            assertEquals(partners + trusted, certManager.getAllTrustedCertificates(SecurityLevel.TLS).size());
            assertEquals(defaults + partners + trusted, certManager.getAllTrustedCertificates(SecurityLevel.MLS).size());
        }

        @Test
        @DisplayName("it should use default settings if no configuration is provided")
        void startsWithDefaults() throws Exception {
            IConfiguration config = setupEnvironment("minimal-config.xml");

            assertDoesNotThrow(() -> certManager.init(config));

            int defaults = countCertsInKeystore(Path.of(System.getProperty("java.home"), "lib", "security", "cacerts"),
            									"changeit");
            int trusted = countCertsInKeystore(TestUtils.getTestResource(TRUST_KEYSTORE), KEYSTORE_PWD);

            assertEquals(defaults + trusted, certManager.getAllTrustedCertificates(SecurityLevel.TLS).size());
            assertEquals(trusted, certManager.getAllTrustedCertificates(SecurityLevel.MLS).size());
        }

        @Test
        @DisplayName("it should fail to start when configuration file is missing")
        void failsWhenConfigMissing() throws Exception {
            IConfiguration config = mock(IConfiguration.class);
            when(config.getHolodeckB2BHome()).thenReturn(TestUtils.getTestClassBasePath().resolve("not-there"));

            assertThrows(SecurityProcessingException.class, () -> certManager.init(config));
        }

        @Test
        @DisplayName("it should fail to start when keystore file does not exist")
        void failsWhenKeystoreMissing() throws Exception {
            IConfiguration config = setupEnvironment("invalid-keystore-path.xml");

            assertThrows(SecurityProcessingException.class, () -> certManager.init(config));
        }

        @Test
        @DisplayName("it should fail to start when keystore password is incorrect")
        void failsWhenPasswordWrong() throws Exception {
            IConfiguration config = setupEnvironment("wrong-password.xml");

            assertThrows(SecurityProcessingException.class, () -> certManager.init(config));
        }

        @Test
        @DisplayName("it should fail to start when not all key stores are configured")
        void failsWhenMissingKeyStoreConig() throws Exception {
        	IConfiguration config = setupEnvironment("keypairs-only.xml");
        	assertThrows(SecurityProcessingException.class, () -> certManager.init(config));
        }

        @Test
        @DisplayName("it should provide its name for identification in logs")
        void providesNameForLogging() {
            String name = certManager.getName();

            assertNotNull(name);
            assertFalse(name.isEmpty());
        }
    }

    // ==================================================================================
    // Key Pair Management Tests - for signing and decryption
    // ==================================================================================

    @Nested
    @DisplayName("When managing key pairs for signing and decryption")
    class KeyPairManagement {

        @BeforeEach
        void initCertManager() throws Exception {
            certManager.init(setupEnvironment(VALID_CONFIG));
        }

        @Test
        @DisplayName("it should provide the private key when correct password is given")
        void providesPrivateKeyWithCorrectPassword() throws Exception {
            KeyStore.PrivateKeyEntry entry = certManager.getKeyPair(SELFSIGNED_ALIAS, KEYPAIR_PASSWORD);

            assertNotNull(entry, "Key pair should be returned");
            assertNotNull(entry.getPrivateKey(), "Private key should be accessible");
        }

        @Test
        @DisplayName("it should reject access to private key with wrong password")
        void rejectsPrivateKeyWithWrongPassword() {
            assertThrows(SecurityProcessingException.class,
                () -> certManager.getKeyPair(SELFSIGNED_ALIAS, "wrongpassword"));
        }

        @Test
        @DisplayName("it should return null for non-existent key pair alias")
        void returnsNullForUnknownAlias() throws Exception {
            assertNull(certManager.getKeyPairCertificates("unknown-alias"));
            assertNull(certManager.getKeyPair("unknown-alias", "anypassword"));
        }

        @Test
        @DisplayName("it should provide the full certificate chain for a CA-signed key pair")
        void providesFullCertificateChain() throws Exception {
            List<X509Certificate> certs = certManager.getKeyPairCertificates(CHAIN_ALIAS);

            assertEquals(2, certs.size(), "Chain should contain leaf and CA certificates");
            X509Certificate leaf = certs.get(0);
            X509Certificate ca = certs.get(1);
            assertEquals(ca.getSubjectX500Principal(), leaf.getIssuerX500Principal(),
                "Leaf certificate should be issued by the CA certificate");
        }

        @Test
        @DisplayName("it should find key pair by its certificate")
        void findsKeyPairByCertificate() throws Exception {
            List<X509Certificate> certs = certManager.getKeyPairCertificates(SELFSIGNED_ALIAS);
            X509Certificate cert = certs.get(0);

            String alias = certManager.findKeyPair(cert);

            assertEquals(SELFSIGNED_ALIAS, alias);
        }

        @Test
        @DisplayName("it should find key pair by its public key")
        void findsKeyPairByPublicKey() throws Exception {
            List<X509Certificate> certs = certManager.getKeyPairCertificates(SELFSIGNED_ALIAS);

            String alias = certManager.findKeyPair(certs.get(0).getPublicKey());

            assertEquals(SELFSIGNED_ALIAS, alias);
        }

        @Test
        @DisplayName("it should find key pair by certificate issuer and serial number")
        void findsKeyPairByIssuerAndSerial() throws Exception {
            List<X509Certificate> certs = certManager.getKeyPairCertificates(SELFSIGNED_ALIAS);
            X509Certificate cert = certs.get(0);

            String alias = certManager.findKeyPair(cert.getIssuerX500Principal(), cert.getSerialNumber());

            assertEquals(SELFSIGNED_ALIAS, alias);
        }
    }

    // ==================================================================================
    // Trading Partner Certificate Tests - for encryption and identification
    // ==================================================================================

    @Nested
    @DisplayName("When managing trading partner certificates")
    class PartnerCertificateManagement {

        @BeforeEach
        void initCertManager() throws Exception {
            certManager.init(setupEnvironment(VALID_CONFIG));
        }

        @Test
        @DisplayName("it should provide partner certificate by alias")
        void providesPartnerCertificateByAlias() throws Exception {
            X509Certificate cert = certManager.getPartnerCertificate(PARTNER_ALIAS);

            assertNotNull(cert, "Partner certificate should be returned");
        }

        @Test
        @DisplayName("it should return null for unknown partner alias")
        void returnsNullForUnknownPartner() throws Exception {
            assertNull(certManager.getPartnerCertificate("unknown-partner"));
        }

        @Test
        @DisplayName("it should find partner certificate by certificate object")
        void findsPartnerByCertificate() throws Exception {
            X509Certificate cert = certManager.getPartnerCertificate(PARTNER_ALIAS);

            String alias = certManager.findCertificate(cert);

            assertEquals(PARTNER_ALIAS, alias);
        }

        @Test
        @DisplayName("it should find partner certificate by issuer and serial number")
        void findsPartnerByIssuerAndSerial() throws Exception {
            X509Certificate expected = certManager.getPartnerCertificate(PARTNER_ALIAS);

            X509Certificate found = certManager.findCertificate(
                expected.getIssuerX500Principal(),
                expected.getSerialNumber()
            );

            assertEquals(expected, found);
        }

        @Test
        @DisplayName("it should not find key pair certificate in partner store")
        void doesNotMixKeyPairsWithPartners() throws Exception {
            List<X509Certificate> keypairCerts = certManager.getKeyPairCertificates(SELFSIGNED_ALIAS);

            String alias = certManager.findCertificate(keypairCerts.get(0));

            assertNull(alias, "Key pair certificates should not be in partner store");
        }
    }

    // ==================================================================================
    // Trust Validation Tests - for verifying certificate authenticity
    // ==================================================================================

    @Nested
    @DisplayName("When validating trust in certificates")
    class TrustValidation {

        @BeforeEach
        void initCertManager() throws Exception {
            certManager.init(setupEnvironment(VALID_CONFIG));
        }

        @Test
        @DisplayName("it should reject validation when certificate list is null")
        void rejectsNullCertificateList() {
            assertThrows(SecurityProcessingException.class,
                () -> certManager.validateCertificate(null, SecurityLevel.MLS));
        }

        @Test
        @DisplayName("it should reject validation when certificate list is empty")
        void rejectsEmptyCertificateList() {
            assertThrows(SecurityProcessingException.class,
                () -> certManager.validateCertificate(List.of(), SecurityLevel.MLS));
        }

        @Test
        @DisplayName("it should trust a certificate that is a registered trust anchor")
        void trustsRegisteredTrustAnchor() throws Exception {
            X509Certificate trustAnchor = loadCertFromKeystore(TRUST_KEYSTORE, ROOT_CA_ALIAS);

            IValidationResult result = certManager.validateCertificate(List.of(trustAnchor), SecurityLevel.MLS);

            assertEquals(Trust.OK, result.getTrust(), "Trust anchor should be trusted");
        }

        @Test
        @DisplayName("it should trust a certificate path that links to a registered trust anchor")
        void trustsCertificatePathToTrustAnchor() throws Exception {
            List<X509Certificate> certPath = loadCertPathFromKeystore();

            IValidationResult result = certManager.validateCertificate(certPath, SecurityLevel.MLS);

            assertEquals(Trust.OK, result.getTrust(), "Certificate signed by trust anchor should be trusted");
            assertEquals(certPath.get(certPath.size() - 1).getIssuerDN(),
            			 result.getValidatedCertPath().get(result.getValidatedCertPath().size() - 1).getSubjectDN());
        }

        @Test
        @DisplayName("it should trust an out-of-order certificate path that links to a registered trust anchor")
        void trustsOutOfOrderPathToTrustAnchor() throws Exception {
			List<X509Certificate> certPath = loadCertPathFromKeystore();
			certPath.add(1, loadCertFromKeystore(TRUST_KEYSTORE, ROOT_CA_ALIAS));

            IValidationResult result = certManager.validateCertificate(certPath, SecurityLevel.MLS);

            assertEquals(Trust.OK, result.getTrust(), "Certificate signed by trust anchor should be trusted");
        }

        @Test
        @DisplayName("it should reject a certificate that does not link to any trust anchor")
        void rejectsUntrustedCertificate() throws Exception {
            X509Certificate untrustedCert = loadCertFromKeystore(PRIVATE_KEYSTORE, SELFSIGNED_ALIAS);

            IValidationResult result = certManager.validateCertificate(
                List.of(untrustedCert), SecurityLevel.MLS);

            assertEquals(Trust.NOK, result.getTrust(),
                "Self-signed certificate not in trust store should be rejected");
        }

        @Test
        @DisplayName("it should provide all trusted CA certificates for a security level")
        void providesAllTrustedCertificates() throws Exception {
            Collection<X509Certificate> trustedCerts = certManager.getAllTrustedCertificates(SecurityLevel.MLS);

            assertNotNull(trustedCerts);
            assertFalse(trustedCerts.isEmpty(), "Should have at least the configured trust anchors");
        }

        @Test
        @DisplayName("it should indicate that config-based validation is not supported")
        void indicatesNoConfigBasedValidation() {
            // The default implementation does not support additional validation parameters
            assertFalse(certManager.supportsConfigBasedValidation());
        }
    }

    // ==================================================================================
    // Revocation check tests
    // ==================================================================================
    @Nested
    @DisplayName("The certificate path should be marked as")
    class Revocation {
    	private RevocationCheckTestProvider provider;
    	private List<X509Certificate> certpath;

    	@BeforeEach
    	void setupTestProvider() throws Exception {
    		certManager.init(setupEnvironment(REVO_OPT_CONFIG));
    		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    		Security.addProvider(provider = new RevocationCheckTestProvider(
    															loadCertFromKeystore(TRUST_KEYSTORE, ROOT_CA_ALIAS)));
    		certpath = loadCertPathFromKeystore();
    	}

    	@AfterEach
    	void resetProvider() {
    		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    		Security.addProvider(new BouncyCastleProvider());
    	}

    	@Test
    	@DisplayName("valid when no exceptions are thrown")
    	void markValid() throws Exception {
    		provider.validateNextRequest();

    		IValidationResult result = assertDoesNotThrow(() ->
    										certManager.validateCertificate(certpath, SecurityLevel.MLS));

    		assertEquals(Trust.OK, result.getTrust());
    	}

    	@Test
    	@DisplayName("as invalid when revoked")
    	void rejectOnRevoked() throws Exception {
    		provider.invalidateNextRequest();

    		IValidationResult result = assertDoesNotThrow(() ->
											certManager.validateCertificate(certpath, SecurityLevel.MLS));

    		assertEquals(Trust.NOK, result.getTrust());
    	}

    	@Test
    	@DisplayName("as valid with warning on a failed check and check is set optional")
    	void warningOnFailedOptionalCheck() throws Exception {
    		provider.failNextRequest();

    		IValidationResult result = assertDoesNotThrow(() ->
											certManager.validateCertificate(certpath, SecurityLevel.MLS));

    		assertEquals(Trust.WITH_WARNINGS, result.getTrust());
    	}

    	@Test
    	@DisplayName("as invalid on a failed check and check is set mandatory")
    	void rejectOnFailedMandatoryCheck() throws Exception {
    		certManager.init(setupEnvironment(REVO_MAN_CONFIG));

    		provider.failNextRequest();

    		IValidationResult result = assertDoesNotThrow(() ->
											certManager.validateCertificate(certpath, SecurityLevel.MLS));

    		assertEquals(Trust.NOK, result.getTrust());
    	}
    }

    // ==================================================================================
    // Lifecycle Tests
    // ==================================================================================

    @Nested
    @DisplayName("When managing Certificate Manager lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("it should shutdown gracefully after initialization")
        void shutsDownGracefully() throws Exception {
            certManager.init(setupEnvironment(VALID_CONFIG));

            certManager.shutdown();
        }
    }
}

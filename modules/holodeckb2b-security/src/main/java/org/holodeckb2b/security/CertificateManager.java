/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.security.ICertificateManager;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.security.util.KeystoreUtils;
import org.holodeckb2b.security.util.SecurityUtils;

/**
 * Is the default security provider's implementation of the {@link ICertificateManager} which manages the storage of
 * keys needed for the processing of the WS-Security headers. The manager uses three JKS keystores which must be
 * configured when creating an instance:<ol>
 * <li>Keystore holding the key pairs used for signing and decryption of messages.</li>
 * <li>Keystore holding the certificates with public keys used for encryption of messages.</li>
 * <li>Keystore holding the certificates with public keys used for the verification of a message's signature.</li></ol>
 * <p>NOTE: The <i>compatibility mode</i> of the provider does not directly affect the public operations of the
 * certificate manager, i.e. certificates are only stored in the keystore that correspond to the provided usage.
 * However when <i>compatibility mode</i> is enabled also keys registered only for encryption use will also be used for
 * signature verification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
class CertificateManager implements ICertificateManager {

    private final Logger log = LogManager.getLogger(CertificateManager.class);

    /**
     * Path to the keystore holding the key pairs used for signing and decryption
     */
    private final String  privateKeystorePath;
    /**
     * Password to access the keystore holding the key pairs used for signing and decryption
     */
    private final String privateKeystorePwd;
    /**
     * Path to the keystore holding the certificates with the public keys used encryption
     */
    private final String  encryptionKeystorePath;
    /**
     * Password to access the keystore holding the certificates with the public keys used encryption
     */
    private final String encryptionKeystorePwd;
    /**
     * Path to the keystore holding the certificates with the public keys used for signature verification
     */
    private final String  trustKeystorePath;
    /**
     * Password to access the keystore holding the certificates with the public keys used for signature verification
     */
    private final String trustKeystorePwd;
    /**
     * Indicator whether the security provider is running in "compatability mode" and encryption certificates should
     * also be used for verification
     */
    private boolean inCompatibilityMode;

    /**
     * Creates a new <code>CertificateManager</code> manager instance using the specified path for accessing the
     * keystores.
     *
     * @param privateKeystorePath       Path to keystore holding the key pairs used for signing and decryption
     * @param privateKeystorePwd        Password of the private keystore
     * @param encryptionKeystorePath    Path to keystore holding the certificates used encryption
     * @param encryptionKeystorePwd     Password of the encryption keystore
     * @param trustKeystorePath         Path to keystore holding the certificates used for signature verification
     * @param trustKeystorePwd          Password of the trust keystore
     * @throws SecurityProcessingException When the keystores can not be accessed given the provided configuration.
     */
    CertificateManager(final String privateKeystorePath, final String privateKeystorePwd,
                       final String encryptionKeystorePath, final String encryptionKeystorePwd,
                       final String trustKeystorePath, final String trustKeystorePwd,
                       final boolean inCompatibilityMode) throws SecurityProcessingException {
        this.privateKeystorePath = privateKeystorePath;
        this.privateKeystorePwd = privateKeystorePwd;
        KeystoreUtils.check(privateKeystorePath, privateKeystorePwd);
        this.encryptionKeystorePath = encryptionKeystorePath;
        this.encryptionKeystorePwd = encryptionKeystorePwd;
        KeystoreUtils.check(encryptionKeystorePath, encryptionKeystorePwd);
        this.trustKeystorePath = trustKeystorePath;
        this.trustKeystorePwd = trustKeystorePwd;
        KeystoreUtils.check(trustKeystorePath, trustKeystorePwd);
        this.inCompatibilityMode = inCompatibilityMode;
    }


    @Override
    public synchronized String registerKeyPair(final KeyStore.PrivateKeyEntry keypair, final String alias,
                                               final String password) throws SecurityProcessingException {
        // Generate a pasword if needed
        final char[] entryPwd = Utils.isNullOrEmpty(password) ? SecurityUtils.generatePassword()
                                                              : password.toCharArray();
        // Load the keystore and add the entry
        KeyStore ks = KeystoreUtils.load(privateKeystorePath, privateKeystorePwd);
        X509Certificate cert = null;
        try {
            cert = (X509Certificate) keypair.getCertificate();
            if (ks.containsAlias(alias))
                log.warn("There already is a key pair registered with the alias [{}]. It will be replaced!", alias);
            ks.setEntry(alias, keypair, new KeyStore.PasswordProtection(entryPwd));
            KeystoreUtils.save(ks, privateKeystorePath, privateKeystorePwd);
            log.info("Registered key pair [Issuer/SerialNo of certificate={}/{}] under alias [{}]",
                     cert.getIssuerX500Principal().getName(), cert.getSerialNumber().toString(), alias);
            return new String(entryPwd);
        } catch (ClassCastException notX509) {
            log.error("Only key pairs containing a X509 Certificate can be registered!");
            throw new SecurityProcessingException("Not a X509 Certificate");
        } catch (KeyStoreException ex) {
            log.error("A problem occurred registering key pair [Issuer/SerialNo of certificate={}/{}] under alias [{}]!"
                    + "\n\tError details: {}", cert.getIssuerX500Principal().getName(),
                    cert.getSerialNumber().toString(), alias, ex.getMessage());
            throw new SecurityProcessingException("Can not registered key pair", ex);
        }
    }

    @Override
    public synchronized void registerCertificate(final X509Certificate cert, final CertificateUsage[] use,
                                                 final String alias) throws SecurityProcessingException {
        Map<CertificateUsage, KeyStore> modifiedKeystores = new HashMap<>(2);
        CertificateUsage usage = null;
        try {
            for (int i = 0; i < use.length; i++) {
                usage = use[i];
                // Load the keystore and add the entry
                KeyStore ks;
                if (usage == CertificateUsage.Encryption)
                    ks = KeystoreUtils.load(encryptionKeystorePath, encryptionKeystorePwd);
                else
                    ks = KeystoreUtils.load(trustKeystorePath, trustKeystorePwd);

                if (ks.containsAlias(alias))
                    log.warn("There already is a certificate registered for {} usage with the alias [{}]."
                            + " It will be replaced!", usage.toString(), alias);
                ks.setCertificateEntry(alias, cert);
                log.debug("Registering certificate [Issuer/SerialNo={}/{}] for {} usage under alias [{}]",
                         cert.getIssuerX500Principal().getName(), cert.getSerialNumber().toString(),
                         usage.toString(), alias);
                modifiedKeystores.put(usage, ks);
            }
            // Now save all modified keystores
            for (Map.Entry<CertificateUsage, KeyStore> entry : modifiedKeystores.entrySet()) {
                CertificateUsage u = entry.getKey();
                KeyStore ks = entry.getValue();
                KeystoreUtils.save(ks, u == CertificateUsage.Encryption ? encryptionKeystorePath : trustKeystorePath,
                                       u == CertificateUsage.Encryption ? encryptionKeystorePwd : trustKeystorePwd);
            }
            log.info("Registered certificate [Issuer/SerialNo={}/{}] for {} usage under alias [{}]",
                         cert.getIssuerX500Principal().getName(), cert.getSerialNumber().toString(),
                         use[0] + (use.length == 2 ? " and " + use[1] : ""), alias);
        } catch (KeyStoreException ex) {
            log.error("Problem registering certificate [Issuer/SerialNo={}/{}] for {} usage under alias [{}]!"
                    + "\n\tError details: {}", cert.getIssuerX500Principal().getName(),
                    cert.getSerialNumber().toString(), usage.toString(), alias, ex.getMessage());
            throw new SecurityProcessingException("Can not registered key pair", ex);
        }
    }

    @Override
    public KeyStore.PrivateKeyEntry getKeyPair(final String alias, final String password)
                                                                                   throws SecurityProcessingException {
        // Load keystore and retrieve the entry from it
        KeyStore ks = KeystoreUtils.load(privateKeystorePath, privateKeystorePwd);
        try {
            // Check if the alias exists
            if (!ks.containsAlias(alias))
                return null;
            else
                return (KeyStore.PrivateKeyEntry) ks.getEntry(alias,
                                                              new KeyStore.PasswordProtection(password.toCharArray()));
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            log.error("Problem retrieving key pair with alias {} from keystore!"
                    + "\n\tError details: {}-{}", alias, ex.getClass().getSimpleName(), ex.getMessage());
            throw new SecurityProcessingException("Error retrieving the keypair", ex);
        }
    }

    @Override
    public X509Certificate getCertificate(final CertificateUsage use, final String alias)
                                                                                   throws SecurityProcessingException {
        // Load keystore and retrieve the certificate from it
        KeyStore ks = use == CertificateUsage.Validation ? KeystoreUtils.load(trustKeystorePath, trustKeystorePwd)
                                                    : KeystoreUtils.load(encryptionKeystorePath, encryptionKeystorePwd);
        try {
            // Check if the alias exists, in compatibility mode also encryption certs must be checked for verification
            if (ks.containsAlias(alias))
                return (X509Certificate) ks.getCertificate(alias);
            else if (use == CertificateUsage.Validation && inCompatibilityMode)
                return getCertificate(CertificateUsage.Encryption, alias);
            else
                return null;
        } catch (KeyStoreException ex) {
            log.error("Problem retrieving certificate for {} use with alias {} from keystore!"
                    + "\n\tError details: {}", use, alias, ex.getMessage());
            throw new SecurityProcessingException("Error retrieving the certificate", ex);
        }
    }

    @Override
    public Collection<X509Certificate> getValidationCertificates() throws SecurityProcessingException {
    	// Load keystore and retrieve the certificates from it
        final KeyStore ks = KeystoreUtils.load(trustKeystorePath, trustKeystorePwd);
        try {
	        final Collection<X509Certificate> result = new ArrayList<>(ks.size());
	        Enumeration<String> aliases = ks.aliases();
	        while (aliases.hasMoreElements()) 
	        	 result.add((X509Certificate) ks.getCertificate(aliases.nextElement()));       
	        return result;
        } catch (KeyStoreException ex) {
            log.error("Problem retrieving the trusted certificates from keystore!\n\tError details: {}", 
            			ex.getMessage());
            throw new SecurityProcessingException("Error retrieving the certificates", ex);       	
        }
    }
    
    @Override
    public String getCertificateAlias(final CertificateUsage use, final X509Certificate cert)
                                                                                   throws SecurityProcessingException {
        // Load keystore and retrieve the certificate from it
        KeyStore ks = use == CertificateUsage.Validation ? KeystoreUtils.load(trustKeystorePath, trustKeystorePwd)
                                                    : KeystoreUtils.load(encryptionKeystorePath, encryptionKeystorePwd);
        try {
            // Check if the keystore contains the certificate and return the alias
            return ks.getCertificateAlias(cert);
        } catch (KeyStoreException ex) {
            log.error("Problem finding certificate [Issuer/SerialNo={}/{}] for {} usage in keystore!"
                     + "\n\tError details: {}", cert.getIssuerX500Principal().getName(),
                     cert.getSerialNumber().toString(), use, ex.getMessage());
            throw new SecurityProcessingException("Error retrieving the certificate", ex);
        }
    }

    /**
     * Removes the key pair registered under the given alias.
     * <p>The certificate manager does not check whether the key pair can be safely removed, i.e. if there is no active
     * P-Mode referencing it. It is the responsibility of the caller to make sure it is safe to remove the key pair.
     */
    @Override
    public synchronized void removeKeyPair(String alias) throws SecurityProcessingException {
        try {
            // Load keystore and retrieve the entry from it
            KeyStore ks = KeystoreUtils.load(privateKeystorePath, privateKeystorePwd);
            // Check if the alias exists
            if (ks.containsAlias(alias))
                ks.deleteEntry(alias);
            KeystoreUtils.save(ks, privateKeystorePath, privateKeystorePwd);
            log.info("Removed key pair {}", alias);
        } catch (SecurityProcessingException | KeyStoreException ex) {
            log.error("Problem removing key pair with alias {} from keystore!"
                    + "\n\tError details: {}", alias, ex.getMessage());
            throw new SecurityProcessingException("Error retrieving the keypair", ex);
        }
    }

    /**
     * Removes the certificate registered for the specified usages and under the given alias.
     * <p>The certificate manager does not check whether the certificate can be safely removed, i.e. if there is no
     * active P-Mode referencing it. It is the responsibility of the caller to make sure it is safe to remove the
     * certificate.
     */
    @Override
    public synchronized void removeCertificate(String alias, CertificateUsage[] use)
                                                                                   throws SecurityProcessingException {
        Map<CertificateUsage, KeyStore> modifiedKeystores = new HashMap<>(2);
        CertificateUsage usage = null;
        try {
            for (int i = 0; i < use.length; i++) {
                usage = use[i];
                // Load the keystore and add the entry
                KeyStore ks;
                if (usage == CertificateUsage.Encryption)
                    ks = KeystoreUtils.load(encryptionKeystorePath, encryptionKeystorePwd);
                else
                    ks = KeystoreUtils.load(trustKeystorePath, trustKeystorePwd);

                if (ks.containsAlias(alias)) {
                    ks.deleteEntry(alias);
                    log.debug("Removing certificate with alias {} for {} usage", alias, usage.toString());
                    modifiedKeystores.put(usage, ks);
                }
            }
            // Now save all modified keystores
            for (Map.Entry<CertificateUsage, KeyStore> entry : modifiedKeystores.entrySet()) {
                CertificateUsage u = entry.getKey();
                KeyStore ks = entry.getValue();
                KeystoreUtils.save(ks, u == CertificateUsage.Encryption ? encryptionKeystorePath : trustKeystorePath,
                                       u == CertificateUsage.Encryption ? encryptionKeystorePwd : trustKeystorePwd);
            }
            log.info("Removed certificate with alias {} for {} usage.", alias,
                                                                    use[0] + (use.length == 2 ? " and " + use[1] : ""));
        } catch (KeyStoreException ex) {
            log.error("Problem removing certificate with alias {} for {} usage!"
                    + "\n\tError details: {}", alias, usage.toString(), ex.getMessage());
            throw new SecurityProcessingException("Can not registered key pair", ex);
        }
    }



    /**
     * Gets a WSS4J {@link Crypto} instance that is configured for use in the given action. This means that the returned
     * Crypto instance will have access to the keystore containing the key pairs when the action is <code>SIGN</code> or
     * <code>DECRYPT</code>, to the public keys keystore when the action is <code>ENCRYPT</code> and to the trusted
     * certificates and if <i>compatibility mode</i> is enabled to the public keys keystore when the action is <code>
     * VERIFY</code>.
     *
     * @param action    The action for which the Crypto instance should be configured
     * @return          A configured {@link Crypto} instance ready for use in the specified action
     * @throws SecurityProcessingException When the Crypto instance could not be created.
     */
    Crypto  getWSS4JCrypto(final Action action) throws SecurityProcessingException {
        final Properties cryptoProperties = new Properties();

        // All instances will use Merlin
        cryptoProperties.setProperty("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");

        switch (action) {
            case SIGN  :
            case DECRYPT :
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.file", privateKeystorePath);
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.type", "jks");
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.password", privateKeystorePwd);
                break;
            case VERIFY :
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.truststore.file", trustKeystorePath);
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.truststore.type", "jks");
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.truststore.password", trustKeystorePwd);
                // if not in compatibility mode only the trust store should be used, otherwise also the certificates
                // stored for encryption
                if (!inCompatibilityMode)
                    break;
            case ENCRYPT :
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.file", encryptionKeystorePath);
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.type", "jks");
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.password", encryptionKeystorePwd);
        }

        try {
            // Now create the Crypto instance using the prepared props
            return CryptoFactory.getInstance(cryptoProperties);
        } catch (WSSecurityException ex) {
            log.error("Could not create a WSS4J Crypto instance for performing the {} action", action);
            throw new SecurityProcessingException("Could not instantiate WSS4J Crypto", ex);
        }
    }
}

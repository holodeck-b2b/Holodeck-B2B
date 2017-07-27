/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Properties;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.ICertificateManager;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.IUsernameTokenProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is a container for general security related functions.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SecurityUtils {

    /**
     * Enumerates the certificate types for which crypto configurations can be created by this utility class.
     */
    public enum CertType {
        pub,
        priv,
        trust
    }


    /**
     * Verifies whether a WSS username token found in the message conforms to the configured values.
     * <p>The check on nonce and created timestamp is whether they are contained in the username token as expected
     * because their actual values are dynamic. The expected password must be supplied in clear text to enable
     * recreation of the digest.
     *
     * @param expected  The expected values for the username token
     * @param actual    The actual values of the received username token
     * @return          <code>true</code> if the received username token is successfully verified against the expected
     *                  username token,<br>
     *                  <code>false</code> otherwise
     * @throws SecurityProcessingException When there is a problem in calculating the password digest value
     */
    public static boolean verifyUsernameToken(final IUsernameTokenConfiguration expected,
                                              final IUsernameTokenProcessingResult actual)
                                                                                    throws SecurityProcessingException {
        boolean verified = false;

        if (expected == null && actual == null)
            return true;
        else if (actual == null)
            return false; // A token was expected not found!

        // Compare usernames
        int c = Utils.compareStrings(expected.getUsername(), actual.getUsername());
        verified = (c == -1 || c == 0); // Both must either be empty or equal

        // Check for existence of created timestamp and nonce
        verified &= !expected.includeCreated() || actual.getCreatedTimestamp() != null;
        verified &= !expected.includeNonce() || !Utils.isNullOrEmpty(actual.getNonce());

        // Check password, starting with type
        verified &= (expected.getPasswordType() == actual.getPasswordType());

        if (verified && (expected.getPasswordType() == UTPasswordType.DIGEST)) {
            // Recreate the digest based on expected password and actual created and nonce values
            // Convert to UsernameToken object to get full access
            final String passDigest = calculatePwdDigest(actual.getNonce(), actual.getCreatedTimestamp(),
                                                         expected.getPassword());
            verified = passDigest.equals(actual.getPassword());
        } else if (verified) {
            // Plain text password, compare strings
            c = Utils.compareStrings(expected.getPassword(), actual.getPassword());
            verified = (c == -1 || c == 0); // Both must either be empty or equal
        }

        return verified;
    }

    /**
     * Calculates the password digest as specified in <a href=
     * "http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-UsernameTokenProfile-v1.1.1-os.html#_Toc307415202"> section
     * 3.1 of Web Services Security Username Token Profile Version 1.1.1</a>.
     *
     * @param nonce     The nonce value to include in the digest
     * @param created   The creation timestamp to include in the digest
     * @param password  The password itself to include in the digest
     * @return          The calculated digest: Base64 ( SHA-1 ( nonce + created + password ) )
     * @throws SecurityProcessingException When the input values can not be correctly transformed to a byte array or
     *                                     when there is no SHA-1 digester available
     * @since HB2B_NEXT_VERSION
     */
    private static String calculatePwdDigest(final String nonce, final String created, final String password)
                                                                                    throws SecurityProcessingException {
        try {
            byte[] decodedNonce = nonce != null ? Base64.getDecoder().decode(nonce) : new byte[0];
            byte[] bCreatedPwd  = (created + password).getBytes("UTF-8");
            byte[] bDigestInput = new byte[decodedNonce.length + bCreatedPwd.length];
            int offset = 0;
            System.arraycopy(decodedNonce, 0, bDigestInput, offset, decodedNonce.length);
            offset += decodedNonce.length;
            System.arraycopy(bCreatedPwd, 0, bDigestInput, offset, bCreatedPwd.length);

            byte[] digestBytes = MessageDigest.getInstance("SHA-1").digest(bDigestInput);
            return Base64.getEncoder().encodeToString(digestBytes);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            throw new SecurityProcessingException("Problem calculating password digest", ex);
        }
    }

    /**
     * Verifies whether the X509 certificate used to sign the message is the one that is configured in the P-Mode.
     *
     * @param expected  The signature configuration as defined in the P-Mode
     * @param actual    The meta-data on the signature of the message
     * @return          <code>true</code> if the certificate is successfully verified against the configuration,<br>
     *                  <code>false</code> otherwise
     * @throws SecurityProcessingException  When there is a problem to get the alias of the certificate used to create
     *                                      the signature of the received message
     * @since HB2B_NEXT_VERSION
     */
    public static boolean verifySigningCertificate(final ISigningConfiguration expected,
                                          final ISignatureProcessingResult actual) throws SecurityProcessingException {
        if (expected == null)
            return true;

        final String expAlias = expected.getKeystoreAlias();
        final String actAlias = actual != null ? HolodeckB2BCore.getCertificateManager()
                                                                .getCertificateAlias(
                                                                        ICertificateManager.CertificateUsage.Signing,
                                                                        actual.getSigningCertificate()) : null;
        return expAlias.equals(actAlias);
    }

    /**
     * Creates the set of properties to configure the Crypto provider for signing or encryption.
     *
     * @param   certType    Indicates for which type of certificate (public, private or trust) the Crypto provider must
     *                      be set up.
     * @return  The Crypto configuration for the requested certificate type
     * @since 2.1.0     Added option to create config for trust keystore
     */
    public static Properties createCryptoConfig(final CertType certType) {
        final IConfiguration config = HolodeckB2BCoreInterface.getConfiguration();
        final Properties cryptoProperties = new Properties();
        String     keyStoreFile = null;
        String     keyStorePwd = null;
        String     keyStoreType = "keystore";

        switch (certType) {
            case pub  :
                keyStoreFile = config.getPublicKeyStorePath();
                keyStorePwd = config.getPublicKeyStorePassword();
                break;
            case priv :
                keyStoreFile = config.getPrivateKeyStorePath();
                keyStorePwd = config.getPrivateKeyStorePassword();
                break;
            case trust :
                keyStoreType = "truststore";
                keyStoreFile = config.getTrustKeyStorePath();
                keyStorePwd = config.getTrustKeyStorePassword();
                break;
        }

        cryptoProperties.setProperty("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");

        // TODO This will crash when keyStoreFile is null
        cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".file", keyStoreFile);
        cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".type", "jks");
        // TODO This will crash when keyStorePwd is null
        cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin." + keyStoreType + ".password", keyStorePwd);

        return cryptoProperties;
    }

    /**
     * Converts the X509 key identifier type to the values used by the WSS4J library.
     *
     * @param refType   The key identifier reference type expressed as {@link X509ReferenceType}
     * @return          The key identifier reference type for use with the WSS4J library
     */
    public static String getWSS4JX509KeyId(final X509ReferenceType refType) {
        switch (refType) {
            case BSTReference   : return "DirectReference";
            case KeyIdentifier  : return "SKIKeyIdentifier";
            default             : return "IssuerSerial";
        }
    }

    /**
     * Gets the alias that is used to reference the supplied X509 certificate in the keystore holding the public keys.
     *
     * @param cert  The {@link X509Certificate} to get the alias for
     * @return      The alias for the supplied certificate if found in the keystore, or<br>
     *              <code>null</code> otherwise (not found or error during search)
     */
    public static String getKeystoreAlias(final X509Certificate cert) {
        final IConfiguration config = HolodeckB2BCoreInterface.getConfiguration();
        String alias = null;
        FileInputStream fis = null;
        char[]  keystorePwd;

        try {
            // Get the password for accessing the keystore
            keystorePwd = config.getPublicKeyStorePassword().toCharArray();
            // Create and load the keystore
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            fis = new java.io.FileInputStream(config.getPublicKeyStorePath());
            keyStore.load(fis, keystorePwd);

            // Get alias of certificate
            alias = keyStore.getCertificateAlias(cert);
        } catch (final Exception ex) {
            // Somehow the search for the certificate alias failed, so no reference available
            alias = null;
        } finally {
            if (fis != null)
                try { fis.close(); } catch (final IOException ex) {}
        }

        return alias;
    }

    /**
     * Checks whether the private key referenced by the given alias is available in the private keys keystore.
     *
     * @param alias         The alias to retrieve the private key from the keystore
     * @param keyPassword   The password to access the private key
     * @return      <code>true</code> if there is a private key in the keystore and it can be accessed using the
     *              given password,<br> <code>false</code> otherwise
     * @since  3.0.0
     */
    public static boolean isPrivateKeyAvailable(final String alias, final String keyPassword) {
        final   IConfiguration config = HolodeckB2BCoreInterface.getConfiguration();

        try (FileInputStream fis = new java.io.FileInputStream(config.getPrivateKeyStorePath())) {
            // Get the password for accessing the keystore
            char[] keystorePwd = config.getPrivateKeyStorePassword().toCharArray();
            // Create and load the keystore
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fis, keystorePwd);

            // Check that the alias exists
            if (keyStore.containsAlias(alias)) {
                return keyStore.getKey(alias, keyPassword.toCharArray()) != null;
            } else
                return false;
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException |
                 UnrecoverableKeyException ex) {
            return false;
        }
    }

    /**
     * Checks whether the certificate referenced by the given alias is available in the public keys or optionally the
     * trust keystore.
     *
     * @param alias         The alias to retrieve the certificate from the keystore
     * @param checkTrust    Indicator if the trust store should be check if the certificate is not found in the public
     *                      keys keystore
     * @return       <code>true</code> if there is a certificate in the public keys or trust keystore (if <code>
     *               checkTrust == true</code>,<br> <code>false</code> otherwise
     * @since  3.0.0
     */
    public static boolean isCertificateAvailable(final String alias, final boolean checkTrust) {
        final   IConfiguration config = HolodeckB2BCoreInterface.getConfiguration();
        boolean found = false;

        try (FileInputStream fis = new java.io.FileInputStream(config.getPublicKeyStorePath())) {
            // Get the password for accessing the keystore
            char[] keystorePwd = config.getPublicKeyStorePassword().toCharArray();
            // Create and load the keystore
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fis, keystorePwd);

            // Check that the alias exists
            found = keyStore.containsAlias(alias);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException ex) {
            found = false;
        }

        // Check the trust store if not found in the public keys keystore
        if (!found && checkTrust) {
            try (FileInputStream fis = new java.io.FileInputStream(config.getTrustKeyStorePath())) {
                // Get the password for accessing the keystore
                char[] keystorePwd = config.getTrustKeyStorePassword().toCharArray();
                // Create and load the keystore
                final KeyStore keyStore = KeyStore.getInstance("JKS");
                keyStore.load(fis, keystorePwd);

                // Check that the alias exists
                found = keyStore.containsAlias(alias);
            } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException ex) {
                found = false;
            }
        }

        return found;
    }
}

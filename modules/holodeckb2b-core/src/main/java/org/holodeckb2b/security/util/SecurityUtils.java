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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.security.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.security.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.pmode.security.X509ReferenceType;
import org.holodeckb2b.security.tokens.UsernameToken;

/**
 * Is a container for general security related functions.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
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
     * Verifies whether a WSS username token conforms to the configured values.
     * <p>The check on nonce and created timestamp is whether they are contained in the username token as expected
     * because their actual values are dynamic. The expected password must be supplied in clear text to enable
     * recreation of the digest.
     *
     * @param expected  The expected values for the username token
     * @param actual    The actual values of the received username token
     * @return          <code>true</code> if the received username token is successfully verified against the expected
     *                  username token,<br>
     *                  <code>false</code> otherwise
     */
    public static boolean verifyUsernameToken(final IUsernameTokenConfiguration expected, final UsernameToken actual) {
        boolean verified = false;

        if (expected == null && actual == null)
            return true;
        else if (actual == null)
            return false; // A token was expected not found!

        // Compare usernames
        int c = Utils.compareStrings(expected.getUsername(), actual.getUsername());
        verified = (c == -1 || c == 0); // Both must either be empty or equal

        // Check for existence of created timestamp and nonce
        verified &= (expected.includeCreated() == actual.includesCreated());
        verified &= (expected.includeNonce() == actual.includesNonce());

        // Check password, starting with type
        verified &= (expected.getPasswordType() == actual.getPasswordType());

        if (verified && (expected.getPasswordType() == IUsernameTokenConfiguration.PasswordType.DIGEST)) {
            // Recreate the digest based on expected password and actual created and nonce values
            // Convert to UsernameToken object to get full access
            final String passDigest = org.apache.wss4j.dom.message.token.UsernameToken.doPasswordDigest(actual.getNonce(),
                                                                                                actual.getCreated(),
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
     * Verifies whether the X509 certificate used to sign the message is the one that is configured in the P-Mode.
     *
     * @param expected  The signature configuration as defined in the P-Mode
     * @param actual    The actual certificate that was used to create the signature in the received username
     * @return          <code>true</code> if the certificate is successfully verified against the configuration,<br>
     *                  <code>false</code> otherwise
     */
    public static boolean verifySignature(final ISigningConfiguration expected,
                                          final org.holodeckb2b.security.tokens.X509Certificate actual) {
        final String expAlias = expected != null ? expected.getKeystoreAlias() : null;
        final String actAlias = actual != null ? actual.getKeystoreAlias() : null;

        if (expected == null && actAlias == null)
            return true;
        else if (actual == null)
            return false; // A signature was expected but not there or it was created with an unknown certificate
        else
            return actAlias.equals(expAlias);
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
     * Gets all <code>ds:Reference</code> descendant elements from the signature in the default WS-Security header.
     * <p>In an ebMS there may only be one <code>ds:Signature</code> element, so we can take the<code>
     * ds:SignedInfo</code> of the first one to get access to the <code>ds:Reference</code> elements.
     *
     * @param mc    The {@link MessageContext} of the message to get the reference from
     * @return      The {@link Collection} of <code>ds:Reference</code> elements contained in the signature,<br>
     *              <code>null</code> or an empty collection if there is no signature in the default security header.
     */
    public static Collection<OMElement> getSignatureReferences(final MessageContext mc) {
       // Get all WS-Security headers
        final ArrayList<SOAPHeaderBlock> secHeaders = mc.getEnvelope().getHeader()
                                                        .getHeaderBlocksWithNSURI(SecurityConstants.WSS_NAMESPACE_URI);
        if (secHeaders == null || secHeaders.isEmpty())
            return null; // No security headers in message

        // There can be more than one security header, get the default header
        SOAPHeaderBlock defHeader = null;
        for(final SOAPHeaderBlock h : secHeaders) {
            if (h.getRole() == null)
                defHeader = h;
        }
        if (defHeader == null)
            return null; // No default security header

        // Get the ds:SignedInfo descendant in the default header.
        final Iterator<OMElement> signatureElems = defHeader.getChildrenWithName(
                                                          new QName(SecurityConstants.DSIG_NAMESPACE_URI, "Signature"));
        if (signatureElems == null || !signatureElems.hasNext())
            return null; // No Signature in default header

        // The ds:SignedInfo element is the first child of ds:Signature
        final OMElement signedInfoElement = signatureElems.next().getFirstElement();
        // Collect all ds:Reference contained in it
        Collection<OMElement> references = null;
        if (signedInfoElement != null) {
            references = new ArrayList<>();
            for (final Iterator<OMElement> it =
                    signedInfoElement.getChildrenWithName(new QName(SecurityConstants.DSIG_NAMESPACE_URI, "Reference"))
                ; it.hasNext() ;)
                references.add(it.next());

        }

        return references;
    }

    /**
     * Checks whether the private key referenced by the given alias is available in the private keys keystore.
     *
     * @param alias         The alias to retrieve the private key from the keystore
     * @param keyPassword   The password to access the private key
     * @return      <code>true</code> if there is a private key in the keystore and it can be accessed using the
     *              given password,<br> <code>false</code> otherwise
     * @since HB2B_NEXT_VERSION
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
     * @since HB2B_NEXT_VERSION
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

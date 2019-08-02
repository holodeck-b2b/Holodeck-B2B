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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.ICertificateManager.CertificateUsage;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.IUsernameTokenProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.UTPasswordType;

/**
 * Is a container for general security related functions.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class VerificationUtils {

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
     * @since 4.0.0
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
     * @since 4.0.0
     */
    public static boolean verifySigningCertificate(final ISigningConfiguration expected,
                                          final ISignatureProcessingResult actual) throws SecurityProcessingException {
        if (expected == null)
            return true;

        final String expAlias = expected.getKeystoreAlias();
        final String actAlias = actual != null ? HolodeckB2BCore.getCertificateManager()
                                                                .getCertificateAlias(CertificateUsage.Validation,
                                                                                     actual.getSigningCertificate())
                                               : null;
        return expAlias.equals(actAlias);
    }
}

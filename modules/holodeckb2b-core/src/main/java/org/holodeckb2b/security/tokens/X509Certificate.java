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
package org.holodeckb2b.security.tokens;

import org.holodeckb2b.security.util.SecurityUtils;

/**
 * Is used to represent a X509 Certificate that was used creating a Signature in the security header of the message as
 * an {@link IAuthenticationInfo} so it can be used for the authentication of the sender of the message.
 * <p>The current implementation only contains the reference to the certificate in the keystore, i.e. the alias. This
 * is enough to check if the correct certificate is used as the P-Mode also contains the reference to the keystore. If
 * authentication information will be made available outside the Holodeck B2B Core, i.e. included in delivery of message
 * units, then this class needs to be extended.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class X509Certificate implements IAuthenticationInfo {

   /**
    * The alias used for reference to the certificate in the keystore
    */
   private final String  alias;

   /**
    * Creates a new <code>X509Certificate</code> based on the actual certificate used for signing the message.
    * <p>The constructor searches the keystore for the certificate and if found stores the alias. The certificate itself
    * is not stored.
    *
    * @param cert   The {@link java.security.cert.X509Certificate} that was for signing the message.
    */
   public X509Certificate(final java.security.cert.X509Certificate cert) {
       alias = SecurityUtils.getKeystoreAlias(cert);
   }

   /**
    * @return The alias that is used to reference the certificate in the keystore.
    */
   public String getKeystoreAlias() {
       return alias;
   }
}

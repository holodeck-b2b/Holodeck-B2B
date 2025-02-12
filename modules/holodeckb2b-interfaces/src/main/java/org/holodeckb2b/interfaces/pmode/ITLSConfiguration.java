/**
 * Copyright (C) 2025 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.pmode;

/**
 * Defines the parameters for establishing a secure connection to the communication partner. These consist of the
 * allowed TLS protocols, cipher suites and client certificate that should be used.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 */
public interface ITLSConfiguration {

	/**
	 * Gets the list of allowed protocols that are allowed for use when connecting to the other MSH. If not specified or
	 * empty only TLS 1.2 and TLS 1.3 are allowed.
	 * <p>
	 * The returned list must contain the names of the allowed protocols as specified in the table <i>Standard Names
	 * for a Protocol</i> found in the <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#additional-jsse-standard-names">
	 * Additional JSSE Standard Names</a> section of the Java Cryptography Architecture Standard Algorithm Name
	 * Documentation.
	 *
	 * @return	an array of protocol names
	 */
	String[] getAllowedProtocols();

	/**
	 * Gets the list of cipher suites that are allowed for use when connecting to the other MSH. If not specified or
	 * empty all supported cipher suites are allowed.
	 * <p>
	 * The returned list must contain the names of the allowed cipher suites as specified in the <a
	 * href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#jsse-cipher-suite-names">
	 * JSSE Cipher Suite Names</a> section of the Java Cryptography Architecture Standard Algorithm Name Documentation.
	 *
	 * @return an array of cipher suite names
	 */
	String[] getAllowedCipherSuites();

	/**
	 * Gets the alias of the key pair, as registered with the <i>Certificate Manager</i>, Holodeck B2B has to use for
	 * client authentication during the TLS handshake when establishing connections to the other MSH.
	 *
	 * @return alias of the key pair to use for TLS client authentication
	 */
	String getClientCertificateAlias();

	/**
	 * Gets the password of the key pair, as registered with the <i>Certificate Manager</i>, Holodeck B2B has to use for
	 * client authentication during the TLS handshake when establishing connections to the other MSH.
	 *
	 * @return password of the key pair to use for TLS client authentication
	 */
	String getClientCertificatePassword();
}

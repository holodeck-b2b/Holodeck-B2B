/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.security;

/**
 * Defines the names of custom P-Mode parameters used by the default Security Provider.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public final class PModeParameters {
	/**
	 * The Agreement Method parameter to specify that the receiver's certificate mete-data should be included in a
	 * WS-Security token reference. The value of the parameter is a boolean and should be "true" to include the
	 * certificate data in a <code>wss:SecurityTokenReference</code> element.
	 */
	public static final String	KA_RCPT_CERT_AS_WSSECREF = "useWSSecRef";

	/**
	 * The Key Derivation parameter to specify the value of the <i>AlgorithmID</i> attribute to include in the
	 * <code>ConcatKDFParams</code>.
	 */
	public static final String  CONCAT_KDF_ALGORITHMID = "AlgorithmID";

	/**
	 * The Key Derivation parameter to specify the value of the <i>PartyUInfo</i> attribute to include in the
	 * <code>ConcatKDFParams</code>.
	 */
	public static final String  CONCAT_KDF_PARTY_U = "PartyUInfo";

	/**
	 * The Key Derivation parameter to specify the value of the <i>PartyVInfo</i> attribute to include in the
	 * <code>ConcatKDFParams</code>.
	 */
	public static final String  CONCAT_KDF_PARTY_V = "PartyVInfo";


}

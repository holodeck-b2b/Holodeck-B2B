/**
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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

import java.util.Map;

/**
 * Defines the settings for the key derivation method used within the <i>key agreement</i>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public interface IKeyDerivationMethod {

	/**
	 * Gets the key derivation algorithm.
	 *
	 * @return  URI of the key derivation algorithm to be used as defined in XMLENC-core1
	 */
	String getAlgorithm();

    /**
     * Gets the digest algorithm to be used for key agreement.
     *
     * @return  URI the digest algorithm to be used with the key derivation algorithm
     */
    String getDigestAlgorithm();

    /**
     * Gets the additional parameters of the key derivation method. Depending on the chosen derivation algorithm there
     * may be other parameters needed in addition to digest algorithm.
     *
     * @return Map containing the additional parameters
     */
    Map<String, ?> getParameters();
}

/**
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.pmode;

import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * Is a helper class to convert the value of the {@link X509ReferenceType} enumeration to strings as used in the P-Mode
 * XML document.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class KeyReferenceMethodConverter implements Converter<X509ReferenceType> {

	private static final String ISSUER_SERIAL = "IssuerSerial";
	private static final String BST_REFERENCE = "BSTReference";
	private static final String SKI = "KeyIdentifier";

	@Override
	public X509ReferenceType read(InputNode node) throws Exception {
		switch (node.getValue()) {
		case BST_REFERENCE:
			return X509ReferenceType.BSTReference;
		case SKI:
			return X509ReferenceType.KeyIdentifier;
		default:
			return X509ReferenceType.IssuerAndSerial;
		}
	}

	@Override
	public void write(OutputNode node, X509ReferenceType value) throws Exception {
		switch (value) {
		case BSTReference:
			node.setValue(BST_REFERENCE);
			break;
		case KeyIdentifier:
			node.setValue(SKI);
			break;
		default:
			node.setValue(ISSUER_SERIAL);
		}

	}
}

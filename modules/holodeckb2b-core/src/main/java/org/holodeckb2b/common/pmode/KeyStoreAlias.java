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
package org.holodeckb2b.common.pmode;

import java.io.Serializable;

import org.holodeckb2b.commons.util.Utils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

/**
 * Represents a <code>KeystoreAlias</code> element in the P-Mode XML document that contains a reference to a certificate
 * in one of the keystores. The reference consists of at least the alias that is used to identify the certificate in the
 * keystore and optionally a password to access the private key from the certificate.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
class KeystoreAlias implements Serializable {
	private static final long serialVersionUID = -8670222264115089303L;

	@Text
    String  name;

    @Attribute(required = false)
    String  password;

    @Override
    public boolean equals(Object obj) {
    	if (this == obj)
	    	return true;
    	if (!(obj instanceof KeystoreAlias))
    		return false;
		KeystoreAlias that = (KeystoreAlias) obj;
    	return Utils.nullSafeEqual(this.name, that.name) && Utils.nullSafeEqual(this.password, that.password);
    }
}

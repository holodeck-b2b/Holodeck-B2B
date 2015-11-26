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

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;

/**
 * Is a special implementation of {@link Validator} that does not validate the credential during security processing 
 * but just returns the credential so validation can be done later in the processing chain.
 * <p>This validator is used to prevent the validation of username token passwords. These passwords are configured in
 * the P-Mode. The P-Mode however is not known at the time of processing the security header so the validation is 
 * skipped and executed later.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class NoOpValidator implements Validator {
    
    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        return credential;
    }
}

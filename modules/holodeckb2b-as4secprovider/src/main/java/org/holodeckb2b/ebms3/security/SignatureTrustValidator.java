/*
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
package org.holodeckb2b.ebms3.security;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.ext.WSSecurityException.ErrorCode;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.security.trust.IValidationResult.Trust;
import org.holodeckb2b.interfaces.security.trust.SecurityLevel;

/**
 * Is a WSS4J <code>Validator</code> that uses the <i>Certificate Manager</i>v to verify the trust in the certificate(s)
 * used in a signature. The WSS4J signature processor extracts the certificate(s) from the WS-Security header and
 * provides it/them to this validator.
 * <p>The validator stores there result reported by the <i>Certificate Manager</i> so it can be retrieved by the
 * security header processor to create a complete result.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 * @since 8.0.0 Uses the configuration based trust validation if the <i>Certificate Manager</i> supports it
 */
public class SignatureTrustValidator implements Validator {
	/*
	 * Holds the result of the trust validation check as executed by the <i>Certificate Manager</i>.
	 */
	private IValidationResult	trustCheckResult = null;

	/**
	 * Validates the certificate (path) used in the WS-Security signature using the installed <i>Certificate Manager</i>.
	 *
     * @param credential 	the Credential to be validated, MUST contain at least one certificate
     * @param data 			the RequestData associated with signature verification
     * @throws WSSecurityException When the given credential cannot be validated. This happens when it isn't trusted by
     * 							   the Certificate Manager, when no certificates were present or some other processing
     * 							   error occurs.
	 */
	@Override
	public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        X509Certificate[] certs = credential.getCertificates();

        if (certs == null || certs.length == 0)
        	throw new WSSecurityException(ErrorCode.UNSUPPORTED_SECURITY_TOKEN);

        // Use the installed Certificate Manager to perform the validation and store the result for future use
        ICertificateManager certManager = HolodeckB2BCoreInterface.getCertificateManager();
        try {
    		trustCheckResult = certManager.validateCertificate(Arrays.asList(certs), SecurityLevel.MLS);
		} catch (SecurityProcessingException e) {
			throw new WSSecurityException(ErrorCode.FAILED_CHECK);
		}

        // Check if the certificate (path) was trusted, if not throw exception to signal to WSS4J
        if (trustCheckResult.getTrust() == Trust.NOK)
        	throw new WSSecurityException(ErrorCode.FAILED_AUTHENTICATION);

        return credential;
	}

	/**
	 * Gets the result of the trust validation check as executed by the <i>Certificate Manager</i>.
	 *
	 * @return	The trust validation results
	 */
	public IValidationResult getValidationResult() {
		return trustCheckResult;
	}
}

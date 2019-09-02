/*
 * Copyright (C) 2019 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.security;

import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.security.trust.IValidationResult.Trust;

/**
 * Is a specialised {@link SecurityProcessingException} to indicate that the processing of the signature on a received
 * message failed due to issues with the trust in the certificate(s) used for signing. In addition to the regular 
 * exception it provides the results of the trust validation as reported by the <i>Certificate Manager</i>.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class SignatureTrustException extends SecurityProcessingException {
	/**
	 * The trust validation result as reported by the <i>Certificate Manager</i>  
	 */
	private IValidationResult	validationResult;
	
	/**
	 * Creates a new exception to indicate the signature verification failed due to problems with the trust validation.
	 *  
	 * @param result	The trust validation result as reported by the <i>Certificate Manager</i>. 
	 * 					SHALL NOT be <code>null</code>
	 */
	public SignatureTrustException(final IValidationResult result) {
		this(result, false);
	}

	/**
	 * Creates a new exception to indicate the signature verification failed due to problems with the trust validation.
	 *  
	 * @param result				The trust validation result as reported by the <i>Certificate Manager</i>
	 * 								SHALL NOT be <code>null</code>
	 * @param isPolicyViolation		Indicates whether the failure should be treated as a policy violation.
	 */
	public SignatureTrustException(final IValidationResult result, final boolean isPolicyViolation) {
		super(result.getMessage(), result.getDetails(), isPolicyViolation);
		if (result.getTrust() == Trust.OK)
			throw new IllegalArgumentException("Exception not allowed for successfull trust validation");
		
		this.validationResult = result;
	}
	
	/**
	 * Gets the results of the trust validation as reported by the <i>Certificate Manager</i>.
	 * 
	 * @return	The validation result
	 */
	public IValidationResult getValidationResult() {
		return validationResult;
	}
	
    /**
     * Gets a textual description why the trust validation failed.
     * 
     * @return	The description of the issue as reported by the <i>Certificate Manager</i>
	 */
	@Override
	public String getMessage() {
		return validationResult.getMessage();
	}

	/**
	 * Gets the exception that caused the trust issue.
	 * 
	 * @return The details of the issue as reported by the <i>Certificate Manager</i>
	 */
	@Override
	public Throwable getCause() {
		return validationResult.getDetails();
	}
}

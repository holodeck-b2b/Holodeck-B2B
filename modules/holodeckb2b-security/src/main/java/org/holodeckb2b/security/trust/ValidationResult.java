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
package org.holodeckb2b.security.trust;

import java.security.cert.X509Certificate;
import java.util.List;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;

/**
 * Is the default implementation of the {@link IValidationResult} interface that contains information on the certificate 
 * trust validation performed by the <i>Certificate Manager</i>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class ValidationResult implements IValidationResult {

	private final List<X509Certificate>			certpath;
	private final Trust							trust;
	private final String						message;
	private final SecurityProcessingException	details;
	
	/**
	 * Create a new instance indicating the result of the check on the given certificate path.
	 *   
	 * @param trust		Indication whether the certificate path was trusted
	 * @param certpath	The tested certificate path
	 */
	public ValidationResult(final Trust trust, final List<X509Certificate> certpath) {
		this(trust, certpath, null, null);
	}
	
	/**
	 * Create a new instance indicating the result of the check on the given certificate path and adding a descriptive
	 * text.
	 *   
	 * @param trust		Indication whether the certificate path was trusted
	 * @param certpath	The tested certificate path
	 * @param message	Descriptive text of the result
	 */
	public ValidationResult(final Trust trust, final List<X509Certificate> certpath, final String message) {
		this(trust, certpath, message, null);
	}
	
	/**
	 * Create a new instance indicating that the given certificate path is untrusted and add a exception detailing why.
	 *   
	 * @param certpath	The tested certificate path
	 * @param details   Exception providing details why the trust validation failed or raised warnings
	 */
	public ValidationResult(final List<X509Certificate> certpath, final SecurityProcessingException details) {
		this(Trust.NOK, certpath, null, details);
	}
	
	/**
	 * Create a new instance indicating the result of the check on the given certificate path failed or had warnings and 
	 * sets both a text description and details of the found issues.
	 *   
	 * @param trust		Indication whether the certificate path was trusted
	 * @param certpath	The tested certificate path
	 * @param message	Descriptive text of the result
	 * @param details	Exception providing details why the trust validation failed or raised warnings
	 */
	public ValidationResult(final Trust trust, final List<X509Certificate> certpath, final String message, 
							final SecurityProcessingException details) {
		if (details != null && trust == Trust.OK)
			throw new IllegalArgumentException("Cannot specify exception for successful validation");
		
		this.trust = trust;
		this.message = message;
		this.certpath = certpath;
		this.details = details;
	}
	
	@Override
	public Trust getTrust() {
		return trust;
	}

	@Override
	public String getMessage() {
		return Utils.isNullOrEmpty(message) && details != null ? details.getMessage() : message; 
	}

	@Override
	public List<X509Certificate> getValidatedCertPath() {
		return certpath;
	}

	@Override
	public SecurityProcessingException getDetails() {		
		return details;
	}

}

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
package org.holodeckb2b.interfaces.security.trust;

import java.security.cert.X509Certificate;
import java.util.List;

import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 * Represents the result of the trust validation check performed by the {@link ICertificateManager} on a certificate 
 * (path). The basic result of the check is the indication whether the checked certificate (path) is considered trusted 
 * by the certificate manager. This however is not a simple yes/no as the certificate manager may issue a warning with
 * trust in the given certificate (path). If the validation failed or has warnings the {@link #getMessage()} and {@link 
 * #getDetails()} methods provide a generic and detailed descriptions on why the certificate(s) were not (fully) trusted.
 * <p>Beside the indication of trust the result also includes the certificate path that was validated. This can be just 
 * the path provided to the certificate manager when requesting the check or the actual path that was used by the 
 * certificate manager for the validation of trust.  
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public interface IValidationResult {

	/**
	 * Enumerates the status of the trust validation.   
	 */
	enum Trust { 
		/**
		 * Indicates the certificate manager successfully completed the validation of the given certificate (path) and 
		 * considers it trusted according to the its policy.  
		 */
		OK, 
		/**
		 * Indicates the certificate manager completed the validation of the given certificate (path) and considers 
		 * it as trusted according to its policy but that it could not execute all validation steps.
		 */
		WITH_WARNINGS, 
		/**
		 * Indicates the certificate manager completed the validation of the given certificate (path) and finds it to
		 * be not trusted according to its policy. 
		 */
		NOK }
	
	/**
	 * Gets the certificate path that was validated. This can be just the path provided to the certificate manager when 
	 * requesting the check or the actual path that was used by the certificate manager for the validation of trust.<br> 
	 * For example, when the given certificate was registered as trusted with the Certificate Manager the path could 
	 * consist of only the certificate itself or it could contain a complete certificate path to the root certificate
	 * when that is checked.  
	 * 
	 * @return 	The validated certificate path
	 */
	List<X509Certificate> getValidatedCertPath();

	/**
     * Indicates whether the provided certificate (path) for validation is trusted by the Certificate Manager based on
     * its trust policy. 
     *
     * @return	The level of trust in the given certificate (path)
     * @see Trust  
     */
    Trust getTrust();
      
    /**
     * Gets a textual description about the trust validation.
     * <p>When the result of the validation is {@link Trust#WITH_WARNINGS} or {@link Trust#FAILED} this method provides
     * a textual description on the reason why the provided certificate (path) is considered not or only with warnings 
     * trusted by the certificate manager. 
     *
     * @return	Additional information on the validation result. May only be <code>null</code> if certificate is trusted
     * 			without warnings.
     */
    String getMessage();
    
    /**
     * Gets detailed information about the reason why the trust validation failed (i.e. why there was no trust) or why
     * warnings were raised by the Certificate Manager.
     * <p>The details are provided in the form of a {@link SecurityProcessingException} so it is easy to trace back
     * to the root cause of the problem. Note however that this exception is just included and not thrown.  
     * <p>NOTE: This methods shall only be used when {@link #getTrust()} returns {@link Trust#WITH_WARNINGS} or {@link 
     * Trust#NOK}.
     *   
     * @return	Details why the trust validation failed or raised warnings, may be <code>null</code> if details are not
     * 			available.
     */
    SecurityProcessingException getDetails();
}

/**
 * Copyright (C) 2025 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.axis2;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.ITLSConfiguration;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.security.trust.IValidationResult.Trust;
import org.holodeckb2b.interfaces.security.trust.SecurityLevel;

/**
 * Is an implementation of {@link X509TrustManager} for the validation of TLS server certificates. It will use the
 * installed Holodeck B2B <i>Certificate Manager</i> to perform the actual trust validation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 * @see ICertificateManager
 */
final class TLSCertificateTrustManager implements X509TrustManager {
	private static final Logger log = LogManager.getLogger();

	/*
	 * The specific TLS configuration that will be used for trust validation (if the installed <i>Certificate Manager</i>
	 * supports it).
	 */
	private final ITLSConfiguration	tlsConfiguration;
	/*
	 * The deployed Holodeck B2B <i>Certificate Manager</i> that will perform the actual trust validation.
	 */
	private final ICertificateManager	certManager;

	public TLSCertificateTrustManager() {
		this(null);
	}

	public TLSCertificateTrustManager(ITLSConfiguration tlsConfiguration) {
		this.tlsConfiguration = tlsConfiguration;
		this.certManager = HolodeckB2BCoreInterface.getCertificateManager();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// As this trust manager will only be used to validate server certificates nothing to do here
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		try {
			IValidationResult vResult;
			if (tlsConfiguration != null && tlsConfiguration.getValidationParameters() != null) {
				log.trace("Using config based validation");
				vResult = certManager.validateCertificate(Arrays.asList(chain),
														tlsConfiguration.getValidationParameters(), SecurityLevel.TLS);
			} else {
				log.trace("Using standard validation");
				vResult = certManager.validateCertificate(Arrays.asList(chain), SecurityLevel.TLS);
			}
			if (vResult.getTrust() == Trust.NOK) {
				log.warn("TLS Server Cert (Subj CN={}) is not trusted", CertificateUtils.getSubjectCN(chain[0]));
				throw new CertificateException("TLS Server Cert is not trusted", vResult.getDetails());
			} else
				log.trace("TLS Server Cert (Subj CN={}) is trusted", CertificateUtils.getSubjectCN(chain[0]));
		} catch (SecurityProcessingException validationException) {
			log.error("An error occurred during trust validation of server certificate (Subj CN={}) : {}",
					  CertificateUtils.getSubjectCN(chain[0]), Utils.getExceptionTrace(validationException));
			throw new CertificateException("Error in trust validation", validationException);
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		try {
			Collection<X509Certificate> certs;
			if (tlsConfiguration != null && tlsConfiguration.getValidationParameters() != null)
				certs = certManager.getAllTrustedCertificates(SecurityLevel.TLS,
															  tlsConfiguration.getValidationParameters());
			else
				certs = certManager.getAllTrustedCertificates(SecurityLevel.TLS);
			return certs.toArray(new X509Certificate[certs.size()]);
		} catch (SecurityProcessingException certError) {
			log.error("Could not get CA certificates from CertManager : {}", Utils.getExceptionTrace(certError));
			throw new RuntimeException("Certificate Manager error", certError);
		}
	}

}
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
package org.holodeckb2b.core.pmode;

import java.security.KeyStore.PrivateKeyEntry;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.axis2.description.TransportOutDescription;
import org.apache.logging.log4j.LogManager;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.axis2.HTTPTransportSender;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.holodeckb2b.interfaces.pmode.ITLSConfiguration;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 * Validates the TLS configuration of a P-Mode. The validation consists of a check on the availability of the client TLS
 * certificate, if one is specified and whether the installed <i>Certificate Manager</i> supports parameter based
 * validation of TLS server certificates if the P-Mode specifies such parameters.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 */
public class TLSConfigurationValidator implements IPModeValidator {

	@Override
	public boolean doesValidate(String pmodeType) {
		TransportOutDescription httpTransport = HolodeckB2BCore.getConfiguration().getTransportOut("http");
		return httpTransport != null && httpTransport.getSender() instanceof HTTPTransportSender;
	}

	@Override
	public Collection<PModeValidationError> validatePMode(IPMode pmode) {
		Collection<PModeValidationError>    errors = new ArrayList<>();

		ILeg sendLeg = PModeUtils.getSendLeg(pmode);
		IProtocol protocol = sendLeg != null ? sendLeg.getProtocol() : null;
		ITLSConfiguration tlsConfiguration = protocol != null ? protocol.getTLSConfiguration() : null;

		// Only if there is a TLS configuration we need to validate the P-Mode
		if (tlsConfiguration != null) {
			String clientCertAlias = tlsConfiguration.getClientCertificateAlias();
			if (!Utils.isNullOrEmpty(clientCertAlias)) {
				PrivateKeyEntry clientCert;
				try {
					clientCert = HolodeckB2BCore.getCertificateManager().getKeyPair(clientCertAlias,
																	   tlsConfiguration.getClientCertificatePassword());
				} catch (SecurityProcessingException e) {
					LogManager.getLogger()
						.warn("Unexpected error checking the availability of the TLS client certificate (alias={}) : ",
							 clientCertAlias, Utils.getExceptionTrace(e));
					clientCert = null;
				}
				if (clientCert == null) {
					errors.add(new PModeValidationError("Protocol.TLSConfiguration.ClientCertificate",
							"Specified client certificate (alias=" + clientCertAlias + ") is not available"));
				}
			}
			if (tlsConfiguration.getValidationParameters() != null &&
			   !HolodeckB2BCore.getCertificateManager().supportsConfigBasedValidation())
				errors.add(new PModeValidationError("Protocol.TLSConfiguration.ValidationParameters",
													"Parameter based TLS server certificate validation not supported"));
		}

		return errors;
	}

}

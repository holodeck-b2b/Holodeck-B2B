/**
 * Copyright (C) 2924 The Holodeck B2B Team, Sander Fieten
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

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.ebms3.security.util.EncryptionConfigWithDefaults;
import org.holodeckb2b.ebms3.security.util.SigningConfigWithDefaults;
import org.holodeckb2b.interfaces.pmode.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.IKeyAgreement;
import org.holodeckb2b.interfaces.pmode.IKeyTransport;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 * Is a Holodeck B2B <i>P-Mode Validator/i> that checks if the security configurations contained in an ebMS3/AS4 P-Mode
 * are valid for use with the default Security Provider.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
public class PModeValidator implements IPModeValidator {

	@Override
	public String getName() {
		return "HB2B Default Security Provider Validator";
	}

	@Override
	public boolean doesValidate(String pmodeType) {
		return pmodeType.startsWith("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704");
	}

	@Override
	public Collection<PModeValidationError> validatePMode(IPMode pmode) {
		Collection<PModeValidationError> errors = new ArrayList<>();

		// For checking the key references we need to know whether HB2B acts as initiator or responder
		boolean hb2bIsInitiator = PModeUtils.isHolodeckB2BInitiator(pmode);

		// Initiator
		ITradingPartnerConfiguration tpCfg = pmode.getInitiator();
		ISecurityConfiguration secCfg = tpCfg != null ? tpCfg.getSecurityConfiguration() : null;
		if (secCfg != null) {
			errors.addAll(checkUsernameTokenParameters(secCfg, "PMode.Initiator"));
			errors.addAll(checkSigningParameters(secCfg.getSignatureConfiguration(), "PMode.Initiator", hb2bIsInitiator));
			errors.addAll(checkEncryptionParameters(secCfg.getEncryptionConfiguration(), "PMode.Initiator", hb2bIsInitiator));
		}

		// Responder
		tpCfg = pmode.getResponder();
		secCfg = tpCfg != null ? tpCfg.getSecurityConfiguration() : null;
		if (secCfg != null) {
			errors.addAll(checkUsernameTokenParameters(secCfg, "PMode.Responder"));
			errors.addAll(checkSigningParameters(secCfg.getSignatureConfiguration(), "PMode.Initiator", !hb2bIsInitiator));
			errors.addAll(checkEncryptionParameters(secCfg.getEncryptionConfiguration(), "PMode.Initiator", !hb2bIsInitiator));
		}

		// Pull specific configs
		List<? extends ILeg> legs = pmode.getLegs();
		if (!Utils.isNullOrEmpty(legs))
			for (int i = 0; i < legs.size(); i++) {
				Collection<IPullRequestFlow> pullCfgs = legs.get(i).getPullRequestFlows();
				if (!Utils.isNullOrEmpty(pullCfgs)) {
					String parentParameterName = "PMode[" + i + "]";
					boolean hb2bSends = PModeUtils.doesHolodeckB2BTrigger(legs.get(i));
					for (IPullRequestFlow pullCfg : pullCfgs) {
						parentParameterName += ".Subchannel(" + pullCfg.getMPC() + ")";
						secCfg = pullCfg.getSecurityConfiguration();
						if (secCfg != null) {
							errors.addAll(checkUsernameTokenParameters(secCfg, parentParameterName));
							errors.addAll(checkSigningParameters(secCfg.getSignatureConfiguration(),
																 parentParameterName, hb2bSends));
						}
					}
				}
			}
		return errors;
	}

	/**
	 * Checks the parameters of a Username tokens (there may be two, one targeted at the <i>default</i> actor/role and
	 * one for the <i>ebms</i>. Token configurations must at least contain the username and password.
	 *
	 * @param parentSecurityCfg		the parent security configuration
	 * @param parentParameterName	name of the parent P-Mode parameter
	 * @return	collection of validation errors
	 */
	protected Collection<PModeValidationError> checkUsernameTokenParameters(
			final ISecurityConfiguration parentSecurityCfg, final String parentParameterName) {
		Collection<PModeValidationError> errors = new ArrayList<>();
		IUsernameTokenConfiguration utConfig = parentSecurityCfg
														.getUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT);
		if (utConfig != null) {
			if (Utils.isNullOrEmpty(utConfig.getUsername()))
				errors.add(new PModeValidationError(parentParameterName + ".UsernameToken[default].username",
						"The configuration of a Username token must contain a username"));
			if (Utils.isNullOrEmpty(utConfig.getPassword()))
				errors.add(new PModeValidationError(parentParameterName + ".UsernameToken[default].password",
						"The configuration of a Username token must contain a password"));
		}
		utConfig = parentSecurityCfg.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS);
		if (utConfig != null) {
			if (Utils.isNullOrEmpty(utConfig.getUsername()))
				errors.add(new PModeValidationError(parentParameterName + ".UsernameToken[ebms].username",
						"The configuration of a Username token must contain a username"));
			if (Utils.isNullOrEmpty(utConfig.getPassword()))
				errors.add(new PModeValidationError(parentParameterName + ".UsernameToken[ebms].password",
						"The configuration of a Username token must contain a password"));
		}

		return errors;
	}

	/**
	 * Checks the Signing parameters of the P-Mode. Only the settings for creating the signature are checked to ensure
	 * the security provider is able to sign outgoing messages. It is checked that the configured keypair is available
	 * and compatible with the configured signing algorithm.
	 *
	 * @param signingCfg	the signature configuration
	 * @param tpRoleName	role name of the sending trading partner, e.g. "Initiator"
	 * @param isSender		indicates if this configuration applies to the sender of the message
	 * @return	collection of validation errors
	 */
	protected Collection<PModeValidationError> checkSigningParameters(final ISigningConfiguration signingCfg,
			final String tpRoleName, final boolean isSender) {
		Collection<PModeValidationError> errors = new ArrayList<>();
		if (signingCfg != null && isSender) {
			ISigningConfiguration sigCfg = new SigningConfigWithDefaults(signingCfg);
			String alias = sigCfg.getKeystoreAlias();
			if (Utils.isNullOrEmpty(alias))
				errors.add(new PModeValidationError(tpRoleName + ".Signature.KeyAlias",
						"A reference to the private key must be specified"));
			String password = sigCfg.getCertificatePassword();
			if (Utils.isNullOrEmpty(password))
				errors.add(new PModeValidationError(tpRoleName + ".Signature.KeyPassword",
						"A password for the private key must be specified"));
			if (!Utils.isNullOrEmpty(alias) && !Utils.isNullOrEmpty(password)) {
				PrivateKeyEntry keyPair = getKeyPair(alias, password);
				if (keyPair == null)
					errors.add(new PModeValidationError(tpRoleName + ".Signature",
															"The private key specified for signing is not available"));
				else {
					String sigAlg = sigCfg.getSignatureAlgorithm();
					String keyAlg = keyPair.getPrivateKey().getAlgorithm();
					if ((sigAlg.contains("rsa") && !"RSA".equalsIgnoreCase(keyAlg))
					  || (sigAlg.contains("ecdsa") && !"EC".equalsIgnoreCase(keyAlg)))
						errors.add(new PModeValidationError(tpRoleName + ".Signature",
													"The private key is not compatible with the signature algorithm"));
				}
			}
		}
		return errors;
	}

	/**
	 * Checks the Encryption parameters of the P-Mode. It is checked that the configured keypair (for decryption) or
	 * certificate (for encryption) is available and that the configured certificate and encryption algorithm are
	 * compatible.<br/>
	 * As the supported algorithms for key agreement are restricted, these are also checked.
	 *
	 * @param encConfig		the encryption configuration
	 * @param tpRoleName	role name of the sending trading partner, e.g. "Initiator"
	 * @param forDecryption	indicates if this configuration is used for decryption and therefore requires the private key
	 * @return	collection of validation errors
	 */
	private Collection<PModeValidationError> checkEncryptionParameters(final IEncryptionConfiguration encConfig,
			final String tpRoleName, final boolean forDecryption) {
		Collection<PModeValidationError> errors = new ArrayList<>();
		if (encConfig != null) {
			IEncryptionConfiguration config = new EncryptionConfigWithDefaults(encConfig);
			String alias = config.getKeystoreAlias();
			if (Utils.isNullOrEmpty(alias))
				errors.add(new PModeValidationError(tpRoleName + ".Encryption.KeyAlias", "A reference to the "
									+ (forDecryption ? "private key" : "certificate") + " must be specified"));
			if (!forDecryption) {
				X509Certificate cert = alias != null ? getCertificate(config.getKeystoreAlias()) : null;
				if (cert == null)
					errors.add(new PModeValidationError(tpRoleName + ".Encryption",
							"The specified certificate for encryption is not available"));
				IKeyTransport keyTransport = config.getKeyTransport();
				if (keyTransport != null) {
					if (cert != null && !"RSA".equalsIgnoreCase(cert.getPublicKey().getAlgorithm()))
						errors.add(new PModeValidationError(tpRoleName + ".Encryption",
									"The specified certificate is not compatible with the key transport algorithm"));
				} else {
					IKeyAgreement keyAgreement = config.getKeyAgreement();
					if (!DefaultSecurityAlgorithms.KEY_AGREEMENT.equals(keyAgreement.getAgreementMethod()))
						errors.add(new PModeValidationError(tpRoleName + ".Encryption",
															"The specified key agreement algorithm is not supported"));
					if (!DefaultSecurityAlgorithms.KEY_DERIVATION.equals(
																keyAgreement.getKeyDerivationMethod().getAlgorithm()))
						errors.add(new PModeValidationError(tpRoleName + ".Encryption",
															"The specified key derivation algorithm is not supported"));
					if (cert != null && !"EC".equalsIgnoreCase(cert.getPublicKey().getAlgorithm()))
						errors.add(new PModeValidationError(tpRoleName + ".Encryption",
									"The specified certificate is not compatible with the key agreement algorithm"));
				}
			} else {
				String pwd = config.getCertificatePassword();
				if (Utils.isNullOrEmpty(pwd))
					errors.add(new PModeValidationError(tpRoleName + ".Encryption.KeyPassword",
								"A password for the private key must be specified"));
				else if (!Utils.isNullOrEmpty(alias) && getKeyPair(alias, pwd) == null)
					errors.add(new PModeValidationError(tpRoleName + ".Encryption",
								"The specified key pair for decryption is not available"));
			}
		}
		return errors;
	}

	/**
	 * Gets the key pair from the installed <i>Certificate Manager</i>.
	 *
	 * @param keystoreAlias       The alias the key pair should be registered under
	 * @param certificatePassword The password to get access to the keypair
	 * @return if a keypair exists and is accessible for the given alias and password, that keypair<br>
	 *         <code>null</code> otherwise
	 */
	protected PrivateKeyEntry getKeyPair(String keystoreAlias, String certificatePassword) {
		try {
			return HolodeckB2BCore.getCertificateManager().getKeyPair(keystoreAlias, certificatePassword);
		} catch (SecurityProcessingException ex) {
			return null;
		}
	}

	/**
	 * Gets the partner certificate with the given alias is available the installed <i>Certificate Manager</i>.
	 *
	 * @param keystoreAlias The alias the certificate should be registered under
	 * @return if a certificate for the given usage and with the given alias exists, that certificate<br>
	 *         <code>null</code> otherwise
	 */
	protected X509Certificate getCertificate(String keystoreAlias) {
		try {
			return HolodeckB2BCore.getCertificateManager().getPartnerCertificate(keystoreAlias);
		} catch (SecurityProcessingException ex) {
			return null;
		}
	}
}

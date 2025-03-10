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

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertPathValidatorException.Reason;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.events.security.ISignatureVerifiedWithWarning;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.security.trust.IValidationResult.Trust;
import org.holodeckb2b.security.trust.config.CertManagerConfigurationType;
import org.holodeckb2b.security.trust.config.PasswordType;

/**
 * Is the default implementation of the {@link ICertificateManager} which manages the storage of private keys and
 * certificates needed for the processing of both the transport and message level security. It <b>does not</b> support
 * configuration based trust validation.
 * <p>
 * The certificate manager uses three JKS key stores to store the different type of certificates:<ol>
 * <li>Holding the key pairs used for signing and decrypting messages and for TLS client authentication. Note that a key
 * pair may need to contain more than one certificate if a certificate chain needs to be included in a signature.</li>
 * <li>Holding the trading partner certificates with public keys used for encryption of messages and identification.
 * This means that the certificates in this key store are also used to find the signing certificate when a received
 * message only includes a reference to the certificate, e.g. a Serial Number or SKI.</li>
 * <li>Holding "trust anchors" used for trust validation of certificates used to sign messages and by the server during
 * the TLS handshake. Normally these are the certificates of trusted Certificate Authorities. Certificates on a
 * certificate path are checked up to the first trust anchor found, i.e. for path [<i>c<sub>0</sub></i>,
 * <i>c<sub>1</sub></i>] with <i>c<sub>1</sub></i> registered as trust anchor, it is checked that <i>c<sub>0</sub></i>
 * is issued by <i>c<sub>1</sub></i> and is valid.</li></ol>
 * <p>
 * The location and passwords of the key stores are managed in the <code>certmanager_config.xml</code> configuration
 * file that must be stored in the <code>conf</code> directory of the Holodeck B2B installation. The structure of the
 * configuration file is defined by the XSD with namespace "http://holodeck-b2b.org/schemas/2019/09/config/certmanager"
 * which can be found in <a href="../xsd/certmanager.xsd">certmanager.xsd</a>.
 * <p>
 * The trading partner certificates are by default not used during trust validation. However it can be useful in an
 * environment to directly trust the certificates of the trading partner (for example if there is just one). For this
 * the <i>direct trust</i> configuration setting is available in which case the trading partner certificates are handled
 * like trust anchors.
 * <p>
 * Another feature of this Certificate Manager is the option to perform a revocation check using OCSP on
 * certificates. This check is disabled by default for back-ward compatibility and can be enabled in the configuration.
 * Note however that when enabled and used in an environment where certificates don't provide OSCP information this will
 * result in a lot of {@link ISignatureVerifiedWithWarning} events as the revocation check could not be executed.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0	This class replaces the certificate manager implementation part of the <i>default Security
 * 			Provider</i> in version 4.x (<code>org.holodeckb2b.security.CertificateManager</code>)
 * @since 8.0.0 Added support for handling key pairs and certificates used in transport level security
 */
public class DefaultCertManager implements ICertificateManager {

    private final Logger log = LogManager.getLogger(DefaultCertManager.class);

    /**
     * Path to the keystore holding the key pairs used for signing and decryption
     */
    private Path  privateKeystorePath;
    /**
     * Password to access the keystore holding the key pairs used for signing and decryption
     */
    private String privateKeystorePwd;
    /**
     * Path to the keystore holding the trading partner certificates
     */
    private Path partnerKeystorePath;
    /**
     * Password to access the keystore holding the trading partner certificates
     */
    private String partnerKeystorePwd;
    /**
     * Path to the keystore holding the trusted certificates acting as trust anchors
     */
    private Path trustKeystorePath;
    /**
     * Password to access the keystore holding the trusted certificates
     */
    private String trustKeystorePwd;

    /**
     * Indicator whether a revocation check should be performed
     */
    private boolean performRevocationCheck;
    /**
     * Indicator whether the trading partners' certificates should be used as trust anchors.
     */
    private boolean enableDirectTrust;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "HB2B Default CertManager/" + VersionInfo.fullVersion;
    }

    /**
     * Initializes the certificate manager by reading the parameters from the configuration file and checking that the
     * keystores are available.
     *
     * @param hb2bHome  The home directory of the current HB2B instance
     * @throws SecurityProcessingException  When the cert manager can not be initialized correctly. Probably caused by
     *                                      missing or incorrect references to keystores.
     */
    @Override
    public void init(final IConfiguration config) throws SecurityProcessingException {
    	final Path hb2bHome = config.getHolodeckB2BHome();
        final Path cfgFilePath = hb2bHome.resolve("conf/certmanager_config.xml");
        try (FileInputStream fis = new FileInputStream(cfgFilePath.toFile())) {
            log.debug("Reading configuration file at {}", cfgFilePath.toString());
            JAXBContext jaxbContext = JAXBContext.newInstance("org.holodeckb2b.security.trust.config");
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<CertManagerConfigurationType> rootConfigElement =
            					jaxbUnmarshaller.unmarshal(new StreamSource(fis), CertManagerConfigurationType.class);
            CertManagerConfigurationType certMgrConfig = rootConfigElement.getValue();
            // Check revocation check and direct trust parameters
            performRevocationCheck = certMgrConfig.isPerformRevocationCheck() == null ? false :
            															certMgrConfig.isPerformRevocationCheck();
            enableDirectTrust = certMgrConfig.isDirectTrustPartnerCertificates() == null ? false :
            														certMgrConfig.isDirectTrustPartnerCertificates();
            // Load key store configs
            privateKeystorePath = ensureAbsolutePath(certMgrConfig.getKeystores().getPrivateKeys().getPath(), hb2bHome);
            privateKeystorePwd = getPassword(certMgrConfig.getKeystores().getPrivateKeys().getPassword());
            partnerKeystorePath = ensureAbsolutePath(certMgrConfig.getKeystores()
            													  .getTradingPartnerCertificates().getPath(), hb2bHome);
            partnerKeystorePwd = getPassword(certMgrConfig.getKeystores().getTradingPartnerCertificates().getPassword());
            trustKeystorePath = ensureAbsolutePath(certMgrConfig.getKeystores().getTrustedCertificates().getPath(),
            										hb2bHome);
            trustKeystorePwd = getPassword(certMgrConfig.getKeystores().getTrustedCertificates().getPassword());
        } catch (JAXBException | NullPointerException | IOException e) {
            log.error("Problem reading the configuration file [{}]! Details: {}", cfgFilePath, e.getMessage());
            throw new SecurityProcessingException("Configuration file not available or invalid!");
        }
        log.trace("Check availability of configured keystores");
        if (!KeystoreUtils.check(privateKeystorePath, privateKeystorePwd)
        	|| !KeystoreUtils.check(partnerKeystorePath, partnerKeystorePwd)
        	|| !KeystoreUtils.check(trustKeystorePath, trustKeystorePwd)) {
        	log.fatal("One or more of the configured key stores are not available!");
        	throw new SecurityProcessingException("Invalid configuration!");
        }
        // We enable OCSP by default, even if revocation checking is disabled
        Security.setProperty("ocsp.enable", "true");

        if (log.isDebugEnabled()) {
            StringBuilder logMsg = new StringBuilder("Completed initialisation of the Default Certificate Manager:\n");
            logMsg.append("\tRevocation check : ").append(performRevocationCheck).append('\n')
            	  .append("\tDirect trust     : ").append(enableDirectTrust).append('\n')
                  .append("\tKey stores: ").append('\n')
                  .append("\t\tPrivate keys  :").append(privateKeystorePath).append('\n')
                  .append("\t\tPartner certs :").append(partnerKeystorePath).append('\n')
                  .append("\t\tTrust anchors :").append(trustKeystorePath).append('\n');
            log.debug(logMsg.toString());
        } else
        	log.info("Completed initialisation");
    }

    @Override
    public void shutdown() {
    }

    /**
     * Helper method to ensure that the paths to the Java keystores are absolute when handed over to the Certificate
     * Manager. If the gieven path is a relative path it will be prefixed with the hb2bHome path.
     *
     * @param sPath     The path to check
     * @param hb2bHome  The HB2B home directory
     * @return          The absolute path
     */
    private Path ensureAbsolutePath(String sPath, Path hb2bHome) {
        if (Utils.isNullOrEmpty(sPath))
            return null;

        Path path = Paths.get(sPath);
        return path.isAbsolute() ? path : hb2bHome.resolve(path);
    }

    /**
     * Helper method to retrieve the password of a key store. The password can be included directly in the configuration
     * or it may need to be retrieved from the application's runtime context, i.e. a Java system property or OS
     * environment variable.
     *
     * @param pwdConfig		the password configuration
     * @return	the password to use
     */
    private String getPassword(PasswordType pwdConfig) {
    	if (pwdConfig == null)
    		return null;

    	switch (pwdConfig.getType()) {
    	case "sys" : return System.getProperty(pwdConfig.getValue());
    	case "env" : return System.getenv(pwdConfig.getValue());
    	default: return pwdConfig.getValue();
    	}
    }

    @Override
    public String findKeyPair(X509Certificate cert) throws SecurityProcessingException {
    	return findKeyPair("certificate", c -> c.equals(cert));
    }

    @Override
    public String findKeyPair(PublicKey key) throws SecurityProcessingException {
		return findKeyPair("public key", c -> c.getPublicKey().equals(key));
    }

    @Override
    public String findKeyPair(byte[] skiBytes) throws SecurityProcessingException {
    	return findKeyPair("SKI", c -> CertificateUtils.hasSKI(c, skiBytes));
    }

    @Override
    public String findKeyPair(X500Principal issuer, BigInteger serial) throws SecurityProcessingException {
    	return findKeyPair("IssuerAndSerial", c -> CertificateUtils.hasIssuerSerial(c, issuer, serial));
    }

    @Override
    public String findKeyPair(byte[] hash, MessageDigest digester) throws SecurityProcessingException {
    	return findKeyPair("thumbprint", c -> CertificateUtils.hasThumbprint(c, hash, digester));
    }

    /**
     * Helper method to search the registered key pairs for an entry that has a certificate that matches to the given
     * condition.
     *
     * @param descr		description of search condition, used for logging
     * @param cond		the condition to match
     * @return			the alias of the first entry that holds a certificate that matches the condition,
     * 					<code>null</code> if no matching entry is found
     * @throws SecurityProcessingException when an error occurs searching the key store
     */
    private String findKeyPair(String descr, CertCondition cond) throws SecurityProcessingException {
    	try {
    		return findEntry(KeystoreUtils.load(privateKeystorePath, privateKeystorePwd), cond);
		} catch (KeyStoreException ex) {
	        log.error("Problem searching for key pair based on {}!\n\tError details: {}", descr, ex.getMessage());
	        throw new SecurityProcessingException("Error searching for keypair", ex);
		}
    }

    @Override
    public KeyStore.PrivateKeyEntry getKeyPair(final String alias, final String password)
                                                                                   throws SecurityProcessingException {
        try {
        	KeyStore ks = KeystoreUtils.load(privateKeystorePath, privateKeystorePwd);
        	final char[] pwd = !Utils.isNullOrEmpty(password) ? password.toCharArray() : new char[] {};
        	return !ks.containsAlias(alias) ? null : (KeyStore.PrivateKeyEntry) ks.getEntry(alias,
                                                              					new KeyStore.PasswordProtection(pwd));
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            log.error("Problem retrieving key pair with alias {} from keystore!"
                    + "\n\tError details: {}-{}", alias, ex.getClass().getSimpleName(), ex.getMessage());
            throw new SecurityProcessingException("Error retrieving the keypair", ex);
        }
    }

    @Override
    public List<X509Certificate> getKeyPairCertificates(final String alias) throws SecurityProcessingException {
    	try {
			Certificate[] cc = KeystoreUtils.load(privateKeystorePath, privateKeystorePwd).getCertificateChain(alias);
			if (cc != null && cc.length > 0) {
				List<X509Certificate> result = new ArrayList<>(cc.length);
				for(Certificate c : cc)
					result.add((X509Certificate) c);
				return result;
			} else
				return null;
		} catch (KeyStoreException e) {
			log.error("Could not access the private keystore! Error details: {}", e.getMessage());
			throw new SecurityProcessingException("Unable to get certificate of key pair", e);
		}
    }

    @Override
    public X509Certificate getPartnerCertificate(final String alias) throws SecurityProcessingException {
    	try {
    		return (X509Certificate)
    				KeystoreUtils.load(partnerKeystorePath, partnerKeystorePwd).getCertificate(alias);
		} catch (KeyStoreException e) {
			log.error("Could not access the partner keystore! Error details: {}", e.getMessage());
			throw new SecurityProcessingException("Unable to get partner certificate", e);
		}
    }

    @Override
    public Collection<X509Certificate> getAllTlsCACertificates() throws SecurityProcessingException {
    	try {
    		KeyStore ks = KeystoreUtils.load(trustKeystorePath, trustKeystorePwd);
    		Collection<X509Certificate> certs = new ArrayList<>();
    		for (Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements();)
	    		certs.add((X509Certificate) ks.getCertificate(aliases.nextElement()));
    		return certs;
    	} catch (KeyStoreException kse) {
	    	log.error("Could not access the trust keystore! Error details: {}", kse.getMessage());
	    	throw new SecurityProcessingException("Unable to get trust certificates", kse);
    	}
    }

    @Override
    public String findCertificate(final X509Certificate cert) throws SecurityProcessingException {
    	try {
    		return findEntry(KeystoreUtils.load(partnerKeystorePath, partnerKeystorePwd), c -> c.equals(cert));
    	} catch (KeyStoreException e) {
    		log.error("Could not access the partner keystore! Error details: {}", e.getMessage());
    		throw new SecurityProcessingException("Unable to get partner certificate", e);
    	}
    }

    @Override
    public X509Certificate findCertificate(final X500Principal issuer, final BigInteger serial)
    																				throws SecurityProcessingException {
    	return getCertificate("IssuerAndSerial", c -> CertificateUtils.hasIssuerSerial(c, issuer, serial));
    }

    @Override
    public X509Certificate findCertificate(final byte[] skiBytes) throws SecurityProcessingException {
    	return getCertificate("SKI", c -> CertificateUtils.hasSKI(c, skiBytes));
    }

    @Override
    public X509Certificate findCertificate(byte[] hash, MessageDigest digester) throws SecurityProcessingException {
    	return getCertificate("thumbprint", c -> CertificateUtils.hasThumbprint(c, hash, digester));
    }

    /**
     * Helper method to search the registered partner certificates for a certificate that matches the given condition.
     *
     * @param descr		description of search condition, used for logging
     * @param cond		the condition to match
     * @return			the alias of the first entry that holds a certificate that matches the condition,
     * 					<code>null</code> if no matching entry is found
     * @throws SecurityProcessingException when an error occurs searching the key store
     */
    private X509Certificate getCertificate(String descr, CertCondition cond) throws SecurityProcessingException {
    	try {
    		KeyStore ks = KeystoreUtils.load(partnerKeystorePath, partnerKeystorePwd);
    		String alias = findEntry(ks, cond);
    		return alias != null ? (X509Certificate) ks.getCertificate(alias) : null;
    	} catch (KeyStoreException e) {
    		log.error("Problem finding the trading partner certificate based on {}!\n\tError details: {}", descr,
    					e.getMessage());
    		throw new SecurityProcessingException("Error retrieving the certificate", e);
    	}
    }

    /**
     * Helper method to search a key store for an entry that holds a certificate matching a given condition.
     *
     * @param kt	indicator which key store to search
     * @param cond	the condition to check
     * @return		the alias of the matching entry if found, <code>null</code> if not found
     * @throws KeyStoreException when an error occurs searching the key store
     */
    private String findEntry(KeyStore ks, CertCondition cond) throws KeyStoreException {
		for (Enumeration<String> e = ks.aliases(); e.hasMoreElements();) {
            String a = e.nextElement();
            Certificate c = ks.getCertificate(a);
            if (c != null && cond.matches((X509Certificate) c))
            	return a;
		}
		return null;
    }

    /**
     * Functional interface used to determine whether the given Certificate matches a given condition, e.g. has the
     * specified SKI.
     */
    private interface CertCondition {
    	boolean matches(X509Certificate c);
    }

    @Override
    public IValidationResult validateMlsCertificate(List<X509Certificate> certs) throws SecurityProcessingException {
    	return validateTrust(certs);
    }

    @Override
    public IValidationResult validateTlsCertificate(List<X509Certificate> certs) throws SecurityProcessingException {
    	return validateTrust(certs);
    }

    /**
     * Helper method to perform the actual trust validation of a certificate chain.
     *
     * @param certs	the certificate chain to validate
     * @return	An instance of {@link IValidationResult} describing the validation result
     * @throws SecurityProcessingException when an error occurs during the validation process
     */
	private IValidationResult validateTrust(List<X509Certificate> certs) throws SecurityProcessingException {
		if (Utils.isNullOrEmpty(certs)) {
			log.error("Cannot validate an empty certificate path!");
			throw new SecurityProcessingException("Empty certificate path");
		}

		if (log.isDebugEnabled()) {
			if (certs.size() == 1)
				log.debug("Validate trust of single certificate for Subject: {}",
																				certs.get(0).getSubjectDN().getName());
			else {
				StringBuilder sb = new StringBuilder("Validate trust in cert path: Leaf cert for Subject: ");
				sb.append(certs.get(0).getSubjectDN().getName());
				for (int i = 1; i < certs.size(); i++)
					sb.append("\n\tNext: ").append(certs.get(i).getSubjectDN().getName());
				log.debug(sb.toString());
			}
		}

		log.trace("Create the set of trust anchors");
		Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
		trustAnchors.addAll(getTrustAnchorsFromKeyStore(trustKeystorePath, trustKeystorePwd));
		if (enableDirectTrust) {
			log.debug("Direct trust in partner certificates is enabled, add as trust anchors");
			trustAnchors.addAll(getTrustAnchorsFromKeyStore(partnerKeystorePath, partnerKeystorePwd));
		}

		log.trace("Calculate cert path to validate (i.e. find first trust anchor)");
		// We only validate the given certificate path up to the first certificate that is listed as a trust anchor,
		// so remove any certificate from the given path that is already in the set of trust anchors
		List<X509Certificate> cpToCheck = new ArrayList<>();
		boolean foundAnchor = false;
		for(int i = 0; !foundAnchor && i < certs.size(); i++) {
			X509Certificate c = certs.get(i);
			cpToCheck.add(c);
			foundAnchor = trustAnchors.parallelStream().anyMatch(a -> a.getTrustedCert().equals(c));
		}

		if (cpToCheck.isEmpty()) {
			log.debug("Leaf certificate (Subject={}) is directly trusted", certs.get(0).getSubjectDN().getName());
			return new ValidationResult(Trust.OK, cpToCheck, "Leaf certificate is registered a trust anchor");
		}

		if (log.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder("Cert path to check: [");
			sb.append(cpToCheck.get(0).getSubjectDN().getName());
			for (int i = 1; i < cpToCheck.size(); i++)
				sb.append(" << ").append(cpToCheck.get(i).getSubjectDN().getName());
			sb.append(']');
			log.trace(sb.toString());
		}

		try {
			CertPath cp = CertificateFactory.getInstance("X.509").generateCertPath(cpToCheck);
			PKIXParameters params = new PKIXParameters(trustAnchors);
			params.setRevocationEnabled(performRevocationCheck);

			CertPathValidator validator = CertPathValidator.getInstance("PKIX", BouncyCastleProvider.PROVIDER_NAME);
			try {
				PKIXCertPathValidatorResult validation = (PKIXCertPathValidatorResult) validator.validate(cp, params);
				// Add the found trust anchor to cert path to include in result
				cpToCheck.add(validation.getTrustAnchor().getTrustedCert());
				if (log.isDebugEnabled())
					log.debug("Certificate path is trusted! {}", getValidatedPath(cpToCheck));
				else
					log.info("Certficate path is trusted!");
				return new ValidationResult(Trust.OK, cpToCheck);
			} catch (CertPathValidatorException validationException) {
				// If reason is "unspecified" or "undetermined" this could be caused by a problem in the OCSP check, so
				// try again without
				Reason reason = validationException.getReason();
				if (performRevocationCheck
					&& (reason == BasicReason.UNSPECIFIED || reason == BasicReason.UNDETERMINED_REVOCATION_STATUS)) {
					try {
						log.debug("Validation with revocation check failed ({}), retry without",
									validationException.getMessage());
						params.setRevocationEnabled(false);
						PKIXCertPathValidatorResult validation = (PKIXCertPathValidatorResult) validator.validate(cp, params);
						// Add the found trust anchor to cert path to include in result
						cpToCheck.add(validation.getTrustAnchor().getTrustedCert());
						log.warn("Certificate path could only be validated without revocation check! {}",
						getValidatedPath(cpToCheck));
						return new ValidationResult(Trust.WITH_WARNINGS, cpToCheck, "Revocation could not be checked",
													new SecurityProcessingException("Revocaction check failed",
																					validationException));
					} catch (CertPathValidatorException persistentError) {
						// Even without revocation check it failed...
					}
				}
				log.error("Trust validation failed! Details: {}", validationException.getMessage());
				return new ValidationResult(cpToCheck, new SecurityProcessingException("Untrusted cert path",
																							validationException));

			}
		} catch (InvalidAlgorithmParameterException | CertificateException | NoSuchAlgorithmException
				| NoSuchProviderException ex) {
			// These indicate some generic problem occured during the validation which is not related to the trust,
			// so report as exception too
			throw new SecurityProcessingException("Error during trust validation", ex);
		}
	}

	/**
	 * Helper method to load the certificates from a key store as trust anchors for validation.
	 *
	 * @param ksPath	Path to key store
	 * @param ksPwd		Password to access key store
	 * @return			Set of trust anchors
	 * @throws SecurityProcessingException	If the key store cannot be accessed or certificates cannot be read
	 */
	private Set<TrustAnchor> getTrustAnchorsFromKeyStore(final Path ksPath, final String ksPwd)
																					throws SecurityProcessingException {
		HashSet<TrustAnchor>	trustanchors = new HashSet<TrustAnchor>();
		try {
			KeyStore ks = KeystoreUtils.load(ksPath, ksPwd);
			Enumeration<String> aliases = ks.aliases();
			while (aliases.hasMoreElements())
				trustanchors.add(new TrustAnchor((X509Certificate) ks.getCertificate(aliases.nextElement()), null));
		} catch (KeyStoreException kse) {
			log.error("Could not retrieve trust anchors from key store ({})", ksPath);
			throw new SecurityProcessingException("Could not retrieve trust anchors");
		}
		return trustanchors;
	}

	/**
	 * Helper method to create log message with the certificate path that was validated.
	 *
	 * @param cp	List of certificate representing the validated cert path with the last one the found trust anchor
	 * @return		The log message with the validated path
	 */
	private String getValidatedPath(final List<X509Certificate> cp) {
		StringBuilder sb = new StringBuilder("Validated path = [");
		sb.append(cp.get(0).getSubjectDN().getName());
		for (int i = 1; i < cp.size() - 1; i++)
			sb.append(" << ").append(cp.get(i).getSubjectDN().getName());
		sb.append(" <{trustanchor}< ")
		  .append(cp.get(cp.size() -1).getSubjectDN().getName())
		  .append(']');
		return sb.toString();
	}

	/*
	 * The following methods are used by the user interface module to display the registered certificates
	 */

    /**
     * Gets all the certificates of all registered key pairs together with the alias their registered under.
     *
     * @return	Map of alias and certificate
     * @throws SecurityProcessingException	When the certificates could not be retrieved from the key store.
     */
    public Map<String, X509Certificate> getPrivateKeyCertificates() throws SecurityProcessingException {
    	return getCertificates(privateKeystorePath, privateKeystorePwd);
    }

    /**
     * Gets all the certificates of all registered key pairs together with the alias their registered under.
     *
     * @return	Map of alias and certificate
     * @throws SecurityProcessingException	When the certificates could not be retrieved from the key store.
     */
    public Map<String, X509Certificate> getPartnerCertificates() throws SecurityProcessingException {
    	return getCertificates(partnerKeystorePath, partnerKeystorePwd);
    }

    /**
     * Gets all the certificates of all registered key pairs together with the alias their registered under.
     *
     * @return	Map of alias and certificate
     * @throws SecurityProcessingException	When the certificates could not be retrieved from the key store.
     */
    public Map<String, X509Certificate> getTrustedCertificates() throws SecurityProcessingException {
    	return getCertificates(trustKeystorePath, trustKeystorePwd);
    }

    /**
     * Helper method to get all the certificates together with their alias from the specified key store.
     *
	 * @param ksPath	Path to key store
	 * @param ksPwd		Password to access key store
     * @return	Map of alias and certificate
     * @throws SecurityProcessingException	When the certificates could not be retrieved from the key store.
     */
    private Map<String, X509Certificate> getCertificates(final Path ksPath, final String ksPwd)
    																				throws SecurityProcessingException {
	    try {
	    	KeyStore ks = KeystoreUtils.load(ksPath, ksPwd);
	        Map<String, X509Certificate> result = new HashMap<>(ks.size());
	        for(Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements();) {
	        	String alias = aliases.nextElement();
	        	result.put(alias, (X509Certificate) ks.getCertificate(alias));
	        }
	        return result;
	    } catch (KeyStoreException ex) {
	        log.error("Problem retrieving the certificates from keystore!\n\tError details: {}", ex.getMessage());
	        throw new SecurityProcessingException("Error retrieving the certificates", ex);
	    }
    }

}

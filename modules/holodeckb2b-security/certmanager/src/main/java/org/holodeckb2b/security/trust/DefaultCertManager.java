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

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertPathValidatorException.Reason;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.events.security.ISignatureVerifiedWithWarning;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.security.trust.IValidationResult.Trust;
import org.holodeckb2b.security.trust.config.CertManagerConfigurationType;

/**
 * Is the default implementation of the {@link ICertificateManager} which manages the storage of private keys and 
 * certificates needed for the processing of the message level security. This default implementation is closely coupled
 * to the default security provider and has some special methods to facilitate the integration. Since the default 
 * security provide builds on WSS4J and WSS4J by default uses JKS key stores to get access to the keys and certificates 
 * here we also uses JKS key stores for managing the different keys and certificates:
 * <li>Holding the key pairs used for signing and decrypting messages. Note that a key pair may need to contain more 
 * than one certificate if a certificate chain needs to be included in a signature.</li>
 * <li>Holding the trading partner certificates with public keys used for encryption of messages and identification. 
 * This means that the certificates in this key store are also used to find the signing certificate when a received 
 * message only includes a reference to the certificate, e.g. a Serial Number or SKI.</li>
 * <li>Holding "trust anchors" used for trust validation of certificates used to sign messages. Normally these are the 
 * certificates of trusted Certificate Authorities. Certificates on a certificate path are checked up to the first trust 
 * anchor found, i.e. for path [<i>c<sub>0</sub></i>, <i>c<sub>1</sub></i>] with <i>c<sub>1</sub></i> registered as 
 * trust anchor, it is checked that <i>c<sub>0</sub></i> is issued by <i>c<sub>1</sub></i> and is valid.</li></ol>
 * <p>The use of these three key stores is similar to earlier versions of Holodeck B2B, but as in version 4 the trading 
 * partner certificates are by default not used during trust validation like they were in earlier versions. In version 
 * 4.x a so-called "compatibility mode" was offered so it could handle the configuration of earlier version. Since it 
 * can be useful in a environment where the certificates of trading partner are trusted this mode has now been replaced
 * by a new <i>direct trust</i> configuration setting in which case the trading partner certificates are handled like
 * trust anchors, with the exception that they are for validity. 
 * <p>Another new feature of this Certificate Manager is the option to perform a revocation check using OCSP on 
 * certificates. This check is disabled by default for back-ward compatibility and can be enabled in the configuration. 
 * Note however that when enabled and used in an environment where certificates don't provide OSCP information this will
 * result in a lot of {@link ISignatureVerifiedWithWarning} events as the revocation check could not be executed.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0	This class replaces the certificate manager implementation part of the <i>default Security
 * 			Provider</i> in version 4.x (<code>org.holodeckb2b.security.CertificateManager</code>)
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
    public void init(final Path hb2bHome) throws SecurityProcessingException {
        final Path cfgFilePath = hb2bHome.resolve("conf/certmanager_config.xml");
        try {
            log.debug("Reading configuration file at {}", cfgFilePath.toString());
            JAXBContext jaxbContext = JAXBContext.newInstance("org.holodeckb2b.security.trust.config");
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<CertManagerConfigurationType> rootConfigElement =
                                        (JAXBElement<CertManagerConfigurationType>) jaxbUnmarshaller.unmarshal(
                                        														cfgFilePath.toFile());
            CertManagerConfigurationType certMgrConfig = rootConfigElement.getValue();
            // Check revocation check and direct trust parameters                       
            performRevocationCheck = certMgrConfig.isPerformRevocationCheck() == null ? false :
            															certMgrConfig.isPerformRevocationCheck();
            enableDirectTrust = certMgrConfig.isDirectTrustPartnerCertificates() == null ? false :
            														certMgrConfig.isDirectTrustPartnerCertificates();
            // Load key store configs
            privateKeystorePath = ensureAbsolutePath(certMgrConfig.getKeystores().getPrivateKeys().getPath(), hb2bHome);
            privateKeystorePwd = certMgrConfig.getKeystores().getPrivateKeys().getPassword();
            partnerKeystorePath = ensureAbsolutePath(certMgrConfig.getKeystores()
            													  .getTradingPartnerCertificates().getPath(), hb2bHome);
            partnerKeystorePwd = certMgrConfig.getKeystores().getTradingPartnerCertificates().getPassword();
            trustKeystorePath = ensureAbsolutePath(certMgrConfig.getKeystores().getTrustedCertificates().getPath(), 
            										hb2bHome);
            trustKeystorePwd = certMgrConfig.getKeystores().getTrustedCertificates().getPassword();
        } catch (JAXBException | NullPointerException e) {
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

    @Override
    public KeyStore.PrivateKeyEntry getKeyPair(final String alias, final String password)
                                                                                   throws SecurityProcessingException {
        try {
        	KeyStore ks = KeystoreUtils.load(privateKeystorePath, privateKeystorePwd);
        	return !ks.containsAlias(alias) ? null : (KeyStore.PrivateKeyEntry) ks.getEntry(alias,
                                                              new KeyStore.PasswordProtection(password.toCharArray()));
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            log.error("Problem retrieving key pair with alias {} from keystore!"
                    + "\n\tError details: {}-{}", alias, ex.getClass().getSimpleName(), ex.getMessage());
            throw new SecurityProcessingException("Error retrieving the keypair", ex);
        }
    }
    
    /**
     * Gets only the certificate part of the key pair registered under the given alias. This method is used by the UI
     * for display information about the private key without exposing it.
     * 
     * @param alias		The alias of the key pair
     * @return			The certificate of the key pair registered under this alias, or<br>
     * 					<code>null</code> when no key pair is found 
     * @throws SecurityProcessingException When there is an error retrieving the certificate
     */
    public X509Certificate getPrivateKeyCertificate(final String alias) throws SecurityProcessingException {
    	try {
			return this.getCertificate(KeystoreUtils.load(privateKeystorePath, privateKeystorePwd), alias);
		} catch (KeyStoreException e) {
			log.error("Could not access the private keystore! Error details: {}", e.getMessage()); 
			throw new SecurityProcessingException("Unable to get certificate of key pair", e);
		}
    }
    
    @Override
    public X509Certificate getCertificate(final String alias) throws SecurityProcessingException {
    	try {
    		return this.getCertificate(KeystoreUtils.load(partnerKeystorePath, partnerKeystorePwd), alias);
		} catch (KeyStoreException e) {
			log.error("Could not access the partner keystore! Error details: {}", e.getMessage()); 
			throw new SecurityProcessingException("Unable to get partner certificate", e);
		}
    		
    }
    
    /**
     * Helper method to retrieve a certificate from a key store.
     * 
     * @param ks		Key store to retrieve certificate from
     * @param alias		Alias of certificate to retrieve
     * @return			The certificate or <code>null</code> if not found
     * @throws SecurityProcessingException	If there is an error retrieving the certificate
     */
    private X509Certificate getCertificate(final KeyStore ks, final String alias) throws SecurityProcessingException {
        try {
            // Check if the alias exists
            if (ks.containsAlias(alias))
                return (X509Certificate) ks.getCertificate(alias);
            else
                return null;
        } catch (KeyStoreException ex) {
            log.error("Problem retrieving certificate with alias {} from partner keystore!"
                    + "\n\tError details: {}", alias, ex.getMessage());
            throw new SecurityProcessingException("Error retrieving the certificate", ex);
        }
    }

    @Override
    public String findCertificate(final X509Certificate cert) throws SecurityProcessingException {
        try {
        	KeyStore ks = KeystoreUtils.load(partnerKeystorePath, partnerKeystorePwd);
            return ks.getCertificateAlias(cert);
        } catch (KeyStoreException ex) {
            log.error("Problem finding the trading partner certificate [Issuer/SerialNo={}/{}] in keystore!"
                     + "\n\tError details: {}", cert.getIssuerX500Principal().getName(),
                     cert.getSerialNumber().toString(), ex.getMessage());
            throw new SecurityProcessingException("Error retrieving the certificate", ex);
        }
    }
    
    @Override
    public X509Certificate findCertificate(final X500Principal issuer, final BigInteger serial)
    																				throws SecurityProcessingException {
    	try {
    		KeyStore ks = KeystoreUtils.load(partnerKeystorePath, partnerKeystorePwd);
	        Enumeration<String> aliases = ks.aliases();
	        X509Certificate cert = null;
	        while (aliases.hasMoreElements() && cert == null) {
	        	final X509Certificate c = (X509Certificate) ks.getCertificate(aliases.nextElement());
	        	if (c.getIssuerX500Principal().equals(issuer) && c.getSerialNumber().equals(serial))
	        		cert = c;	        			
	        }    		     		
    		return cert;
    	} catch (KeyStoreException ex) {
    		log.error("Problem finding the trading partner certificate [Issuer/SerialNo={}/{}] in keystore!"
    				+ "\n\tError details: {}", issuer.getName(),serial.toString(), ex.getMessage());
    		throw new SecurityProcessingException("Error retrieving the certificate", ex);
    	}
    }

    @Override
    public X509Certificate findCertificate(final byte[] skiBytes) throws SecurityProcessingException {
    	try {
    		KeyStore ks = KeystoreUtils.load(partnerKeystorePath, partnerKeystorePwd);
    		Enumeration<String> aliases = ks.aliases();
    		X509Certificate cert = null;
    		while (aliases.hasMoreElements() && cert == null) {
    			final X509Certificate c = (X509Certificate) ks.getCertificate(aliases.nextElement());
    			byte[] skiExtValue = c.getExtensionValue("2.5.29.14");
				if (skiExtValue != null) {
					byte[] ski = Arrays.copyOfRange(skiExtValue, 4, skiExtValue.length);    			
					if (Arrays.equals(ski, skiBytes))
						cert = c;
				}
    		}    		     		
    		return cert;
    	} catch (KeyStoreException ex) {
    		log.error("Problem finding the trading partner certificate [SKI={}] in keystore!"
    				+ "\n\tError details: {}", Hex.encodeHexString(skiBytes), ex.getMessage());
    		throw new SecurityProcessingException("Error retrieving the certificate", ex);
    	}
    }    

	@Override
	public IValidationResult validateTrust(List<X509Certificate> certs) throws SecurityProcessingException {
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
					sb.append("\n\tNext :").append(certs.get(i).getSubjectDN().getName());
				log.debug(sb.toString());
			}							
		}
		
		log.trace("Create the set of trust anchors");
		final Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();		
		trustAnchors.addAll(getTrustAnchorsFromKeyStore(trustKeystorePath, trustKeystorePwd));
		if (enableDirectTrust) {
			log.debug("Direct trust in partner certificates is enabled, add as trust anchors");
			trustAnchors.addAll(getTrustAnchorsFromKeyStore(partnerKeystorePath, partnerKeystorePwd));
		}
		
		log.trace("Calculate cert path to validate (i.e. find first trust anchor)");
		// We only validate the given certificate path up to the first certificate that is listed as a trust anchor,
		// so remove any certificate from the given path that is already in the set of trust anchors
		final List<X509Certificate> cpToCheck = new ArrayList<>();		
		boolean foundAnchor = false;		
		for(int i = 0; !foundAnchor && i < certs.size(); i++) {
			X509Certificate c = certs.get(i);
			if (!(foundAnchor = trustAnchors.parallelStream().anyMatch(a -> a.getTrustedCert().equals(c))))
				cpToCheck.add(c);
		}

		if (cpToCheck.isEmpty()) {
			log.debug("Leaf certificate (Subject={}) is directly trusted", certs.get(0).getSubjectDN().getName());
			return new ValidationResult(Trust.OK, cpToCheck, "Leaf certificate is registered a trust anchor");		
		}
		
		if (log.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder("Cert path to check: [");
			sb.append(cpToCheck.get(0).getSubjectDN().getName());
			for (int i = 1; i < certs.size(); i++)
				sb.append(" << ").append(certs.get(i).getSubjectDN().getName());
			sb.append(']');
			log.trace(sb.toString());
		}		
		
		try {
			final CertPath cp = CertificateFactory.getInstance("X.509").generateCertPath(cpToCheck);
			final PKIXParameters params = new PKIXParameters(trustAnchors);
			params.setRevocationEnabled(performRevocationCheck);
			Security.setProperty("ocsp.enable", "true");
			final CertPathValidator validator = CertPathValidator.getInstance("PKIX");			
			try {
				final PKIXCertPathValidatorResult validation = (PKIXCertPathValidatorResult) 
																						validator.validate(cp, params);
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
						final PKIXCertPathValidatorResult validation = (PKIXCertPathValidatorResult) 
								validator.validate(cp, params);
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
		} catch (InvalidAlgorithmParameterException | CertificateException | NoSuchAlgorithmException ex) {
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
	
    /**
     * Gets the WSS4J {@link Crypto} instance configured for use in the given action. This means that the returned
     * Crypto instance will have access to the keystore containing the key pairs when the action is <code>SIGN</code> or
     * <code>DECRYPT</code>, to the public keys keystore when the action is <code>ENCRYPT</code>. Since the trust 
     * validation of certificates is not handled by WSS4J it should not access to any of the key stores when the action 
     * is <code>VERIFY</code>.   
     *
     * @param action    The action for which the Crypto instance should be configured
     * @return          A configured {@link Crypto} instance ready for use in the specified action
     * @throws SecurityProcessingException When the Crypto instance could not be created.
     */
    public Crypto getWSS4JCrypto(final String action) throws SecurityProcessingException {
        final Properties cryptoProperties = new Properties();
        // All instances will use Merlin
        cryptoProperties.setProperty("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
        switch (action) {
            case "SIGN"  :
            case "DECRYPT" :
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.file", 
                								privateKeystorePath.toString());
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.type", "jks");
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.password", privateKeystorePwd);
                break;
            case "VERIFY" :
            case "ENCRYPT" :            	
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.file", 
                								partnerKeystorePath.toString());
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.type", "jks");
                cryptoProperties.setProperty("org.apache.wss4j.crypto.merlin.keystore.password", partnerKeystorePwd);
            default: // do nothing
        }

        try {
            // Now create the Crypto instance using the prepared props
            return CryptoFactory.getInstance(cryptoProperties);
        } catch (WSSecurityException ex) {
            log.error("Could not create a WSS4J Crypto instance for performing the {} action", action);
            throw new SecurityProcessingException("Could not instantiate WSS4J Crypto", ex);
        }
    }
    
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
	    	final KeyStore ks = KeystoreUtils.load(ksPath, ksPwd);
	        final Map<String, X509Certificate> result = new HashMap<>(ks.size());
	        Enumeration<String> aliases = ks.aliases();
	        while (aliases.hasMoreElements()) {
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

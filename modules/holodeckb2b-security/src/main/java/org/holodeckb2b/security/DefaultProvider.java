/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.security;

import java.io.File;
import java.nio.file.Paths;
import java.security.Provider;
import java.security.Security;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.common.config.ConfigXmlFile;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.security.ICertificateManager;
import org.holodeckb2b.interfaces.security.ISecurityHeaderCreator;
import org.holodeckb2b.interfaces.security.ISecurityHeaderProcessor;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.security.config.ProviderConfigurationType;

/**
 * Is the default implementation of a Holodeck B2B <i>Security Provider</i> as specified by {@link ISecurityProvider}.
 * <p>The provider uses the WSS4J library for the actual processing of the WS-Security headers in the messages. Because
 * WSS4J by default uses JKS keystore to get access to the keys and certificates the provider also uses JKS keystores
 * for managing the different keys and certificates. It uses one for the key pairs used for encryption, one for
 * certificates used for decryption and one for certificates used in signature verification.
 * <p>Note that the use of the three keystores is similar to earlier versions of Holodeck B2B but their use slightly
 * different since the provider differentiates between certificates used for encryption and signature verification and
 * stores them in separate keystores whereas in the former versions of Holodeck B2B the certificates stored in the
 * "public keys" keystore were used for both encryption and signature verification.<br>
 * For migration purposes the current version of the provider offers a <i>"compatibility mode"</i> in which it will
 * use the "old" configuration and also use the certificates registered in the <i>public</i> keystore are for signature
 * verification. This mode is automatically used when the provider's normal configuration file is not found. It must be
 * noted that this mode will be offered only temporarily and will be removed in a future version of the provider!
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class DefaultProvider implements ISecurityProvider {

    private final Logger  log = LogManager.getLogger(DefaultProvider.class);

    /**
     * The certificate manager in use by this provider
     */
    private CertificateManager  certManager;
    /**
     * Indicator whether providers runs in compatibility mode
     */
    private boolean inCompatibilityMode = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "HB2B Default Security/" + VersionInfo.fullVersion;
    }

    /**
     * Initializes the default security provider. It starts by reading the parameters from its own configuration file
     * and fall backs to the "old" configuration method if that file is not available or can not be read.
     *
     * @param hb2bHome  The home directory of the current HB2B instance
     * @throws SecurityProcessingException  When the provider can not be initialized correctly. Probably caused by
     *                                      missing or incorrect references to keystores.
     */
    @Override
    public void init(final String hb2bHome) throws SecurityProcessingException {
        String privStorePath = null, privStorePwd = null, encryptStorePath = null, encryptStorePwd = null,
               trustStorePath = null, trustStorePwd = null;
        final String cfgFilePath = hb2bHome + "/conf/securityprovider.xml";
        boolean fallback = false;
        try {
            log.debug("Reading configuration file at {}", cfgFilePath);
            File file = new File(cfgFilePath);
            if (file.exists() && file.isFile() && file.canRead()) {
                JAXBContext jaxbContext = JAXBContext.newInstance("org.holodeckb2b.security.config");
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                JAXBElement<ProviderConfigurationType> providerConfigElement =
                                            (JAXBElement<ProviderConfigurationType>) jaxbUnmarshaller.unmarshal(file);
                ProviderConfigurationType providerConfig = providerConfigElement.getValue();
                log.debug("Configure provider using settings from config file: {}", cfgFilePath);
                privStorePath = ensureAbsolutePath(providerConfig.getKeystores().getPrivateKeys().getPath(), hb2bHome);
                privStorePwd = providerConfig.getKeystores().getPrivateKeys().getPassword();
                encryptStorePath = ensureAbsolutePath(providerConfig.getKeystores().getPublicEncryptionKeys().getPath(),
                                                      hb2bHome);
                encryptStorePwd = providerConfig.getKeystores().getPublicEncryptionKeys().getPassword();
                trustStorePath = ensureAbsolutePath(providerConfig.getKeystores().getTrustedCerts().getPath(),
                                                    hb2bHome);
                trustStorePwd = providerConfig.getKeystores().getTrustedCerts().getPassword();
                inCompatibilityMode = false;
            } else {
                log.warn("No configuration file found!");
                fallback = true;
            }
        } catch (JAXBException | NullPointerException e) {
            // If specific configuration is not available, fall back to using the "old" config
            log.error("Problem reading the configuration file [{}]! Details: {}", cfgFilePath, e.getMessage());
            fallback = true;
        }
        if (fallback) {
            log.warn("Using fall back configuration mechanism. NOTE that this will be removed in a future version!");
            final ConfigXmlFile configFile;
            try {
                configFile = ConfigXmlFile.loadFromFile(Paths.get(hb2bHome, "conf", "holodeckb2b.xml").toString());
                privStorePath = ensureAbsolutePath(configFile.getParameter("PrivateKeyStorePath"), hb2bHome);
                if (Utils.isNullOrEmpty(privStorePath))
                    privStorePath = hb2bHome + "/repository/certs/privatekeys.jks";
                privStorePwd = configFile.getParameter("PrivateKeyStorePassword");
                encryptStorePath = ensureAbsolutePath(configFile.getParameter("PublicKeyStorePath"), hb2bHome);
                if (Utils.isNullOrEmpty(encryptStorePath))
                    encryptStorePath = hb2bHome + "/repository/certs/publickeys.jks";
                encryptStorePwd = configFile.getParameter("PublicKeyStorePassword");
                trustStorePath = ensureAbsolutePath(configFile.getParameter("TrustKeyStorePath"), hb2bHome);
                if (Utils.isNullOrEmpty(trustStorePath))
                    trustStorePath = hb2bHome + "/repository/certs/trustedcerts.jks";
                trustStorePwd = configFile.getParameter("TrustKeyStorePassword");
            } catch (Exception ex) {
                log.error("Could not read the main Holodeck B2B configuration file [{}/conf/holodeckb2b.xml]",
                          hb2bHome);
                // Although here it is already clear we can not initialize the provider successfully, we just continue
                // and let the CertificateManager conclude this
            }
            // When using fall back mechanism we are automatically running in compatibility mode
            inCompatibilityMode = true;
        }
        // Create the Certificate Manager
        try {
            log.debug("Creating the Certificate Manager");
            certManager = new CertificateManager(privStorePath, privStorePwd, encryptStorePath, encryptStorePwd,
                                                 trustStorePath, trustStorePwd, inCompatibilityMode);
            log.debug("Certificate Manager is now ready for use");
        } catch (SecurityProcessingException spe) {
            log.fatal("Could not create the Certificate Manager! Error message: {}", spe.getMessage());
            throw spe;
        }
		// Make sure that BouncyCastle is the preferred security provider
		final Provider[] providers = Security.getProviders();
		if (providers != null && providers.length > 0)
			Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);		
		log.debug("Registering BouncyCastle as preferred Java security provider");
		Security.insertProviderAt(new BouncyCastleProvider(), 1);		        
        
        // Initialization done
        log.info("Default Security Provider version {} is ready!", VersionInfo.fullVersion);
    }

    @Override
    public ISecurityHeaderProcessor getSecurityHeaderProcessor() {
        return new SecurityHeaderProcessor(certManager);
    }

    @Override
    public ISecurityHeaderCreator getSecurityHeaderCreator() {
        return new SecurityHeaderCreator(certManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICertificateManager getCertificateManager() {
        return certManager;
    }

    /**
     * Returns the indicator whether the default security provider is running in <i>"compatibility"</i> mode. In this
     * mode the certificates stored in the "encryption" keystore are also used for signature verification.
     * <p><b>NOTE:</b> Compatibility mode is only provided for easy migration of extension that depend on the "old"
     * interfaces and will be removed in a future version!
     *
     * @return <code>true</code> when running in compatibility mode,<br><code>false</code> if running normally.
     */
    @Deprecated
    public boolean inCompatibilityMode() {
        return inCompatibilityMode;
    }

    /**
     * Helper method to ensure that the paths to the Java keystores are absolute when handed over to the Certificate
     * Manager. If the gieven path is a relative path it will be prefixed with the hb2bHome path.
     *
     * @param path      The path to check
     * @param hb2bHome  The HB2B home directory
     * @return          The absolute path
     */
    private String ensureAbsolutePath(String path, String hb2bHome) {
        if (Utils.isNullOrEmpty(path))
            return null;

        if (Paths.get(path).isAbsolute())
            return path;
        else
            return Paths.get(hb2bHome, path).toString();
    }

}

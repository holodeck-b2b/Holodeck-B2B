/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.axis2.context.ConfigurationContext;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventProcessor;

/**
 * Contains the configuration of the Holodeck B2B Core.
 * <p>The Holodeck B2B configuration is located in the <code><b>«HB2B_HOME»</b>/conf</code> directory where
 * <b>HB2B_HOME</b> is the directory where Holodeck B2B is installed or the system property <i>"holodeckb2b.home"</i>.
 * <p>The structure of the config file is defined by the XML Schema Definition
 * <code>http://holodeck-b2b.org/schemas/2015/10/config</code> which can be found in <code>
 * src/main/resources/xsd/hb2b-xsd</code>.
 * <p>NOTE: Current implementation does not encrypt the keystore passwords! Therefore access to the config file
 * SHOULD be limited to the account that is used to run Holodeck B2B.
 * @todo Encryption of keystore passwords
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Config implements InternalConfiguration {

    /*
     * The Holodeck B2B home directory
     */
    private String holodeckHome = null;

    /*
     * Name of the JPA persistency unit to get database access
     */
    private String  persistencyUnit = null;

    /*
     * The host name
     */
    private String  hostName = null;

    /*
     * The location of the workerpool configuration
     */
    private String  workerConfigFile = null;

    /*
     * The directory where files can be temporarily stored
     */
    private String  tempDir = null;

    /*
     * Indication whether bundling of signals is allowed
     */
    private boolean allowSignalBundling = false;

    /*
     * Default setting whether errors on errors should be reported to sender or not
     */
    private boolean defaultReportErrorOnError = false;

    /*
     * Default setting whether errors on receipts should be reported to sender or not
     */
    private boolean defaultReportErrorOnReceipt = false;

    /*
     * Indication whether the strict error reference check from the spec should be used
     */
    private boolean useStrictErrorReferencesCheck = false;

    /*
     * The path to the keystore holding the private keys and certificates
    */
    private String privKeyStorePath = null;

    /*
     * The password of the keystore that holds the certificates with the private keys
     */
    private String  privKeyStorePassword = null;

    /*
     * The path to the keystore holding the public keys and certificates
    */
    private String pubKeyStorePath = null;

    /*
     * The password of the keystore that holds the certificates with the public keys
     */
    private String  pubKeyStorePassword = null;

    /*
     * The path to the keystore holding the trusted certificates
    */
    private String trustKeyStorePath = null;

    /*
     * The password of the keystore that holds the trusted certificates
     */
    private String  trustKeyStorePassword = null;

    /*
     * Default setting whether the revocation of a certificate should be checked
     */
    private boolean defaultRevocationCheck = false;

    /*
     * The Axis2 configuration context that is used to process the messages
     */
    private ConfigurationContext    axisCfgCtx = null;

    /*
     * The class name of the event processor that is to be used for handling message processing events
     * @since 2.1.0
     */
    private String messageProcessingEventProcessorClass = null;

    /*
     * The class name of the component that should be used to validate P-Modes before deploying them
     * @since HB2B_NEXT_VERSION
     */
    private String pmodeValidatorClass = null;

    /*
     * The class name of the component that should be used to store deployed P-Modes
     * @since HB2B_NEXT_VERSION
     */
    private String pmodeStorageClass = null;

    private boolean isTrue (final String s) {
      return "on".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s) || "1".equalsIgnoreCase(s);
    }

    /**
     * Initializes the configuration object using the Holodeck B2B configuration file located in <code>
     * «HB2B_HOME»/conf/holodeckb2b.xml</code> where <b>HB2B_HOME</b> is the directory where Holodeck B2B is installed
     * or the system property <i>"holodeckb2b.home"</i>.
     *
     * @param configCtx     The Axis2 configuration context containing the Axis2 settings
     * @throws Exception    When the configuration can not be initialized
     */
    public Config(final ConfigurationContext configCtx) throws Exception {
        // The Axis2 configuration context
        axisCfgCtx = configCtx;

        // Detect the Holodeck B2B home directory
        this.holodeckHome = detectHomeDir(configCtx);

        // Read the configuration file
        final ConfigXmlFile configFile = ConfigXmlFile.loadFromFile(
                                                        Paths.get(holodeckHome, "conf", "holodeckb2b.xml").toString());

        // The JPA persistency unit to get database access
        //@TODO: REQUIRED, check and throw exception when not available!
        persistencyUnit = configFile.getParameter("PersistencyUnit");

        // The hostname to use in message processing
        hostName = configFile.getParameter("ExternalHostName");
        if (Utils.isNullOrEmpty(hostName)) {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (final UnknownHostException e) {}

            if (Utils.isNullOrEmpty(hostName)) {
                // If we still have no hostname, we just generate a random hex number to use as hostname
                final Random r = new Random();
                hostName = Long.toHexString((long) r.nextInt() * (long) r.nextInt()) + ".generated";
            }
        }

        /* The configuration of the workerpool. By default the "workers.xml" in the
         * conf directory is used. But it is possible to specify another location
         * using the "WorkerConfig" parameter
         */
        String workerCfgFile = configFile.getParameter("WorkerConfig");
        if (workerCfgFile == null || workerCfgFile.isEmpty())
            // Not specified, use default
            workerCfgFile = holodeckHome + "/conf/workers.xml";

        workerConfigFile = workerCfgFile;

        // The temp dir
        tempDir = configFile.getParameter("TempDir");
        if (Utils.isNullOrEmpty(tempDir))
            // Not specified, use default
            tempDir = holodeckHome + "/temp/";

        // Ensure the path ends with a folder separator
        tempDir = (tempDir.endsWith(FileSystems.getDefault().getSeparator()) ? tempDir
                            : tempDir + FileSystems.getDefault().getSeparator());

        // Option to enable signal bundling
        final String bundling = configFile.getParameter("AllowSignalBundling");
        allowSignalBundling = isTrue (bundling);

        // Default setting for reporting Errors on Errors
        String defErrReporting = configFile.getParameter("ReportErrorOnError");
        defaultReportErrorOnError = isTrue(defErrReporting);
        // Default setting for reporting Errors on Receipts
        defErrReporting = configFile.getParameter("ReportErrorOnReceipt");
        defaultReportErrorOnReceipt = isTrue(defErrReporting);

        // Option to use strict error references check
        final String strictErrorRefCheck = configFile.getParameter("StrictErrorReferencesCheck");
        useStrictErrorReferencesCheck = isTrue(strictErrorRefCheck);

        // The location of the keystore holding the private keys, if not provided the default location «HB2B_HOME»/
        // repository/certs/privatekeys.jks is used
        privKeyStorePath = configFile.getParameter("PrivateKeyStorePath");
        if (Utils.isNullOrEmpty(privKeyStorePath))
            privKeyStorePath = holodeckHome + "/repository/certs/privatekeys.jks";

        // The password for the keystore holding the private keys
        privKeyStorePassword = configFile.getParameter("PrivateKeyStorePassword");

        // The location of the keystore holding the certificate (public keys), if not provided the default location
        // «HB2B_HOME»/repository/certs/publickeys.jks is used
        pubKeyStorePath = configFile.getParameter("PublicKeyStorePath");
        if (Utils.isNullOrEmpty(pubKeyStorePath))
            pubKeyStorePath = holodeckHome + "/repository/certs/publickeys.jks";

        // The password for the keystore holding the public keys
        pubKeyStorePassword = configFile.getParameter("PublicKeyStorePassword");

        // The location of the keystore holding the trusted certificate, if not provided the default location
        // «HB2B_HOME»/repository/certs/trustedcerts.jks is used
        trustKeyStorePath = configFile.getParameter("TrustKeyStorePath");
        if (Utils.isNullOrEmpty(trustKeyStorePath))
            trustKeyStorePath = holodeckHome + "/repository/certs/trustedcerts.jks";

        // The password for the keystore holding the public keys
        trustKeyStorePassword = configFile.getParameter("TrustKeyStorePassword");

        // Default setting for certificate revocation check
        final String certRevocationCheck = configFile.getParameter("CertificateRevocationCheck");
        defaultRevocationCheck = isTrue(certRevocationCheck);

        // The class name of the event processor
        messageProcessingEventProcessorClass = configFile.getParameter("MessageProcessingEventProcessor");

        // The class name of the P-Mode validator
        pmodeValidatorClass = configFile.getParameter("PModeValidator");

        // The class name of the component to store P-Modes
        pmodeStorageClass = configFile.getParameter("PModeStorageImplementation");
    }

    /**
     * Helper method to detect what the Holodeck B2B home directory is.
     *
     * @param configContext     The Axis2 <code>ConfigurationContext</code> in which Holodeck B2B operates
     * @return                  The Holodeck B2B home directory
     */
    private String detectHomeDir(final ConfigurationContext configContext) {
        String hb2b_home_dir = System.getProperty("holodeckb2b.home");
        if (!Utils.isNullOrEmpty(hb2b_home_dir) && !Files.isDirectory(Paths.get(hb2b_home_dir))) {
            Logger.getLogger(Config.class.getName())
                        .log(Level.WARNING, "Specified Holodeck B2B HOME does not exists, reverting to default home");
            hb2b_home_dir = null;
        }

        return Utils.isNullOrEmpty(hb2b_home_dir) ? configContext.getRealPath("").getParent()
                                                               : hb2b_home_dir;
    }


    /**
     * Gets the Axis2 configuration context. This context is used for processing messages.
     *
     * @return The Axis2 configuration context.
     */
    @Override
    public ConfigurationContext getAxisConfigurationContext() {
        return axisCfgCtx;
    }

    /**
     * Gets the name of the JPA persistency unit to use for accessing the database
     *
     * @return The name of the persistency unit
     */
    @Override
    public String getPersistencyUnit() {
        return persistencyUnit;
    }

    /**
     * Gets the host name. During the message processing a host name may be needed,
     * for example for generating a message id. Because the host name of the machine
     * Holodeck B2B runs on may be for internal use only it is possible to set an
     * <i>external</i> host name using the <i>ExternalHostName</i> parameter.
     * <p>When no host name is specified in the configuration the first host name bound to
     * a network interface (not being the loopback adapter) will be used. If there
     * is still no host name a random id will be used.
     *
     * @return  The host name
     */
    @Override
    public String getHostName () {
        return hostName;
    }

    /**
     * Gets the Holodeck B2B home directory.
     *
     * @return  The Holodeck B2B home directory.
     */
    @Override
    public String getHolodeckB2BHome () {
        return holodeckHome;
    }

    /**
     * Gets the location of the workerpool configuration file.
     * <p>By default the configuration file named <code>workers.xml</code> in the
     * <code>conf</code> directory is used. But it is possible to specify another
     * location using the <i>WorkerConfig</i> parameter.
     *
     * @return The absolute path to the worker pool configuration file.
     */
    @Override
    public String getWorkerPoolCfgFile() {
        return workerConfigFile;
    }

    /**
     * Gets the directory to use for temporarily storing files.
     * <p>By default this the <code>temp</code> directory in the Holodeck B2B
     * installation. The directory to use can also be specified using the
     * <i>TempDir</i> parameter.
     * <p>It is RECOMMENDED to create a subdirectory in this directory when
     * regularly storing files in the temp directory.
     *
     * @return  The absolute path to the temp directory. Ends with a directory
     *          separator.
     */
    @Override
    public String getTempDirectory() {
        return tempDir;
    }

    /**
     * Indicates whether bundling of signal message units in a response message is allowed. When enabled Holodeck B2B
     * can add multiple signal message units generated during the processing of the request message to the response.
     * This however will create ebMS messages that DO NOT conform to the ebMS v3 Core Spec and AS4 profile.
     * <p>The default setting is not to allow this bundling to ensure Core Spec and AS4 compliant ebMS messages. To
     * enable the feature set the <i>AllowSignalBundling</i> to "on" or "true".
     *
     * @return Indication whether bundling of signals in a response is allowed
     */
    @Override
    public boolean allowSignalBundling() {
        return allowSignalBundling;
    }

    /**
     * Gets the default setting whether Errors on Errors should be reported to the sender of the faulty error. This
     * setting can be overriden in the P-Mode configuration. However the problem that causes an error to be in error is
     * often an invalid message reference. In such cases the error can not be assigned a P-Mode, so the P-Mode can not
     * configure the behaviour.
     *
     * @return <code>true</code> if generated errors on errors should by default be reported to the sender,<br>
     *         <code>false</code> otherwise
     */
    @Override
    public boolean shouldReportErrorOnError() {
        return defaultReportErrorOnError;
    }

    /**
     * Gets the default setting whether Errors on Receipts should be reported to the sender of the faulty receipt. This
     * setting can be overriden in the P-Mode configuration. However the problem that causes an error to be in error is
     * often an invalid message reference. In such cases the receipt can not be assigned a P-Mode, so the P-Mode can not
     * configure the behaviour.
     *
     * @return <code>true</code> if generated errors on receipts should by default be reported to the sender,<br>
     *         <code>false</code> otherwise
     */
    @Override
    public boolean shouldReportErrorOnReceipt() {
        return defaultReportErrorOnReceipt;
    }

    /**
     * Indicates if the references in an Error signal message unit should be checked using the strict requirements
     * defined in the Core Specification (all references equal) or if a bit more relaxed check can be used (signal level
     * reference empty, but individual errors have the same reference).
     * <p>Default the more relaxed check is used. To change this set the value of the <i>StrictErrorReferencesCheck<i>
     * parameter to <i>"true"</i>.
     *
     * @return <code>true</code> if generated errors on receipts should by default be reported to the sender,<br>
     *         <code>false</code> otherwise
     */
    @Override
    public boolean useStrictErrorRefCheck() {
        return useStrictErrorReferencesCheck;
    }

    /**
     * Gets the path to the keystore containing the private keys and related certificates that are used for signing
     * and decryption of messages.
     *
     * @return The path to the <i>"private"</i> keystore.
     */
    @Override
    public String getPrivateKeyStorePath() {
        return privKeyStorePath;
    }

    /**
     * Gets the password for the keystore that holds the certificates with the private keys.
     *
     * @return  The password for accessing the keystore with the private keys
     */
    @Override
    public String getPrivateKeyStorePassword() {
        return privKeyStorePassword;
    }

    /**
     * Gets the path to the keystore containing the certificates (i.e. public keys) that are used for encrypting
     * messages and verification of a signed messages.
     *
     * @return The path to the <i>"public"</i> keystore.
     */
    @Override
    public String getPublicKeyStorePath() {
        return pubKeyStorePath;
    }

    /**
     * Gets the password for the keystore that holds the certificates with the public keys.
     *
     * @return  The password for accessing the keystore with the public keys
     */
    @Override
    public String getPublicKeyStorePassword() {
        return pubKeyStorePassword;
    }

    /**
     * Gets the path to the keystore containing the CA certificates that should be trusted when validating the
     * certificate that was used for signing a received message.
     * <p>This "trust" keystore should be used to store certificates that are not directly related to a specific trading
     * partner but are needed to validate the trading partners certificate. By using the trust store the trading
     * partners certificate doesn't need to be registered in Holodeck B2B but can be included in the message itself
     * using a <code>BinarySecurityToken</code> element.
     *
     * @return The path to the <i>"trust"</i> keystore.
     * @since 2.1.0
     */
    @Override
    public String getTrustKeyStorePath() {
        return trustKeyStorePath;
    }

    /**
     * Gets the password for the keystore that holds the trusted CA certificates.
     *
     * @return  The password for accessing the keystore with the trusted certificates.
     * @since 2.1.0
     */
    @Override
    public String getTrustKeyStorePassword() {
        return trustKeyStorePassword;
    }

    /**
     * Gets the global setting whether Holodeck B2B should check if a certificate is revoked. As an error that occurs
     * during the revocation check will result in rejection of the complete ebMS message the default value is <i>false
     * </i>. If required the revocation check can be enable in the P-Mode configuration.
     * <p>To change the default setting for the revocation check change the value of the <i>CertificateRevocationCheck
     * </i> parameter to <i>"true"</i>.
     *
     * @return <code>true</code> if the revocation of certificates should by default be checked,<br>
     *         <code>false</code> otherwise
     */
    @Override
    public boolean shouldCheckCertificateRevocation() {
        return defaultRevocationCheck;
    }

    /**
     * Gets the configured class name of the component that is responsible for the processing of event that are raised
     * during the message processing. This is an optional configuration parameter and when not set the Holodeck B2B Core
     * will use a default implementation. To use a custom implementation set class name in the
     * <i>MessageProcessingEventProcessor</i> parameter
     *
     * @return String containing the class name of the {@link IMessageProcessingEventProcessor} implementation to use
     * @since 2.1.0
     */
    @Override
    public String getMessageProcessingEventProcessor() {
        return messageProcessingEventProcessorClass;
    }

    /**
     * Gets the class name of the {@link IPModeValidator} implementation that the Holodeck B2B Core's <code>PModeManager
     * </code> must use to validate P-Modes before they are deployed. This is an optional configuration parameter and
     * when not set the Holodeck B2B Core will use a default implementation. To use a custom implementation set class
     * name in the <i>PModeValidator</i> parameter.
     * <b>NOTE:</b> The configured validator will be used to validate all P-Modes and should therefore only be used if
     * Holodeck B2B is deployed in a specific domain.
     *
     * @return  The class name of the {@link IPModeValidator} implementation
     * @since  HB2B_NEXT_VERSION
     */
    @Override
    public String getPModeValidatorImplClass() {
        return pmodeValidatorClass;
    }

    /**
     * Gets the class name of the {@link IPModeSet} implementation that the Holodeck B2B Core's <code>PModeManager
     * </code> must use to store the set of deployed P-Modes. This is an optional configuration parameter and when not
     * set the Holodeck B2B Core will use a default implementation. To use a custom implementation set class name in
     * the <i>PModeStorageImplementation</i> parameter.
     *
     * @return  The class name of the {@link IPModeSet} implementation to use for storing deployed P-Modes
     * @since  HB2B_NEXT_VERSION
     */
    @Override
    public String getPModeStorageImplClass() {
        return pmodeStorageClass;
    }
}

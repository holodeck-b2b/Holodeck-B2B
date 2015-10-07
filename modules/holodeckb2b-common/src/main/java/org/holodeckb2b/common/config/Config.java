/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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

/**
 * Contains the configuration of the Holodeck B2B Core.
 * <p>The Holodeck B2B configuration is located in the <code><b>«HB2B_HOME»</b>/conf</code> directory where 
 * <b>HB2B_HOME</b> is the directory where Holodeck B2B is installed or the system property <i>"holodeckb2b.home"</i>.
 * <p>The structure of the config file is defined by the XML Schema Definition 
 * <code>http://holodeck-b2b.org/schemas/2015/10/config</code> which can be found in <code>
 * src/main/resources/xsd/hb2b-config.xsd</code>.
 * <p>NOTE: Current implementation does not encrypt the keystore passwords! Therefore access to the config file 
 * SHOULD be limited to the account that is used to run Holodeck B2B.
 * @todo Encryption of keystore passwords
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Config {
    
    /*
     * Singleton pattern is used for configuration as we only need it once for
     * each instance
     */
    private static Config   config = null;

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
     * Default setting whether the revocation of a certificate should be checked 
     */
    private boolean defaultRevocationCheck = false;
    
    /*
     * The Axis2 configuration context that is used to process the messages
     */
    private ConfigurationContext    axisCfgCtx = null;
    
    /**
     * Initializes the configuration object using the Holodeck B2B configuration file located in <code>
     * «HB2B_HOME»/conf/holodeckb2b.xml</code> where <b>HB2B_HOME</b> is the directory where Holodeck B2B is installed 
     * or the system property <i>"holodeckb2b.home"</i>.
     * 
     * @param configContext     The Axis2 configuration context
     * @param module            The module configuration
     * @throws Exception    When the configuration can not be initialized
     */
    public static void init(ConfigurationContext configContext) throws Exception {
        // Alway start with a new and empty configuration
        config = new Config(configContext);        
    }
    
    /**
     * Initializes the singleton object.
     * 
     * @param configCtx     The Axis2 configuration context containing the Axis2 settings
     * @throws Exception    When the configuration can not be initialized
     */
    private Config(ConfigurationContext configCtx) throws Exception {        
        // The Axis2 configuration context
        axisCfgCtx = configCtx;

        // Read the configuration file
        ConfigXmlFile configFile = loadConfiguration(configCtx);
        
        // The JPA persistency unit to get database access
        //@TODO: REQUIRED, check and throw exception when not available!
        persistencyUnit = configFile.getParameter("PersistencyUnit");
        
        // The hostname to use in message processing
        String hostName = configFile.getParameter("ExternalHostName");
        if (hostName == null || hostName.isEmpty()) {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {}
        
            if (hostName == null || hostName.isEmpty()) {
                // If we still have no hostname, we just generate a random hex number to use as hostname
                Random r = new Random();
                hostName = Long.toHexString((long) r.nextInt() * (long) r.nextInt()) + ".generated";
            }
        }
        hostName = hostName;
        
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
        String tempDir = configFile.getParameter("TempDir");
        if (tempDir == null || tempDir.isEmpty())
            // Not specified, use default
            tempDir = holodeckHome + "/temp/";
        
        // Ensure the path ends with a folder separator
        tempDir = (tempDir.endsWith(FileSystems.getDefault().getSeparator()) ? tempDir 
                            : tempDir + FileSystems.getDefault().getSeparator());
        
        // Option to enable signal bundling
        String bundling = configFile.getParameter("AllowSignalBundling");
        allowSignalBundling = "on".equalsIgnoreCase(bundling) || "true".equalsIgnoreCase(bundling) 
                                        || "1".equalsIgnoreCase(bundling);        

        // Default setting for reporting Errors on Errors
        String defErrReporting = configFile.getParameter("ReportErrorOnError");
        defaultReportErrorOnError = "on".equalsIgnoreCase(defErrReporting) 
                                            || "true".equalsIgnoreCase(defErrReporting) 
                                            || "1".equalsIgnoreCase(defErrReporting);        
        // Default setting for reporting Errors on Receipts
        defErrReporting = configFile.getParameter("ReportErrorOnReceipt");
        defaultReportErrorOnReceipt = "on".equalsIgnoreCase(defErrReporting) 
                                            || "true".equalsIgnoreCase(defErrReporting) 
                                            || "1".equalsIgnoreCase(defErrReporting);        
        
        // Option to use strict error references check 
        String strictErrorRefCheck = configFile.getParameter("StrictErrorReferencesCheck");
        useStrictErrorReferencesCheck = "on".equalsIgnoreCase(strictErrorRefCheck) 
                                                || "true".equalsIgnoreCase(strictErrorRefCheck) 
                                                || "1".equalsIgnoreCase(strictErrorRefCheck);        
        
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
        
        // Default setting for certificate revocation check
        String certRevocationCheck = configFile.getParameter("CertificateRevocationCheck");
        defaultRevocationCheck = "on".equalsIgnoreCase(certRevocationCheck) 
                                                || "true".equalsIgnoreCase(certRevocationCheck) 
                                                || "1".equalsIgnoreCase(certRevocationCheck);                
    }

    /**
     * Helper method to load the Holodeck B2B XML document from file.
     * 
     * @param configContext     The Axis2 <code>ConfigurationContext</code> in which Holodeck B2B operates
     * @return                  The Holodeck B2B configuration parameters read from the config file.
     * @throws Exception When the configuration file can not be read.
     */
    private ConfigXmlFile loadConfiguration(ConfigurationContext configContext) throws Exception {
        String hb2b_home_dir = System.getProperty("holodeckb2b.home");
        if (!Utils.isNullOrEmpty(hb2b_home_dir) && !Files.isDirectory(Paths.get(hb2b_home_dir))) {
            Logger.getLogger(Config.class.getName())
                        .log(Level.WARNING, "Specified Holodeck B2B HOME does not exists, reverting to default home");
            hb2b_home_dir = null;
        } 
        holodeckHome = Utils.isNullOrEmpty(hb2b_home_dir) ? configContext.getRealPath("").getParent()
                                                               : hb2b_home_dir;
               
        return ConfigXmlFile.loadFromFile(Paths.get(holodeckHome, "conf", "holodeckb2b.xml").toString());                    
    }

    
    /**
     * Gets the Axis2 configuration context. This context is used for processing messages.
     * 
     * @return The Axis2 configuration context.
     */
    public static ConfigurationContext getAxisConfigurationContext() {
        assertInitialized();        
        return config.axisCfgCtx;
    }
    
    /**
     * Gets the name of the JPA persistency unit to use for accessing the database
     * 
     * @return The name of the persistency unit
     */
    public static String getPersistencyUnit() {
        assertInitialized();
        
        return config.persistencyUnit;
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
    public static String getHostName () {
        assertInitialized();
        
        return config.hostName;
    }
    
    /**
     * Gets the location of the workerpool configuration file. 
     * <p>By default the configuration file named <code>workers.xml</code> in the 
     * <code>conf</code> directory is used. But it is possible to specify another 
     * location using the <i>WorkerConfig</i> parameter.
     * 
     * @return The absolute path to the worker pool configuration file.
     */
    public static String getWorkerPoolCfgFile() {
        assertInitialized();
        
        return config.workerConfigFile;
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
    public static String getTempDirectory() {
        assertInitialized();
        
        return config.tempDir;
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
    public static boolean allowSignalBundling() {
        assertInitialized();
        
        return config.allowSignalBundling;
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
    public static boolean shouldReportErrorOnError() {
        assertInitialized();        
        return config.defaultReportErrorOnError;
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
    public static boolean shouldReportErrorOnReceipt() {
        assertInitialized();        
        return config.defaultReportErrorOnReceipt;
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
    public static boolean useStrictErrorRefCheck() {
        assertInitialized();
        return config.useStrictErrorReferencesCheck;
    }

    /**
     * Gets the path to the keystore containing the private keys and related certificates that are used for signing
     * and decryption of messages.
     * 
     * @return The path to the <i>"private"</i> keystore.
     */
    public static String getPrivateKeyStorePath() {
        assertInitialized();
        return config.privKeyStorePath;
    }
    
    /**
     * Gets the password for the keystore that holds the certificates with the private keys. 
     * 
     * @return  The password for accessing the keystore with the private keys
     */
    public static String getPrivateKeyStorePassword() {
        assertInitialized();
        return config.privKeyStorePassword;
    }
    
    /**
     * Gets the path to the keystore containing the certificates (i.e. public keys) that are used for encrypting 
     * messages and verification of a signed messages.
     * 
     * @return The path to the <i>"public"</i> keystore.
     */
    public static String getPublicKeyStorePath() {
        assertInitialized();
        return config.pubKeyStorePath;
    }

    /**
     * Gets the password for the keystore that holds the certificates with the public keys. 
     * 
     * @return  The password for accessing the keystore with the public keys
     */
    public static String getPublicKeyStorePassword() {
        assertInitialized();
        return config.pubKeyStorePassword;
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
    public static boolean shouldCheckCertificateRevocation() {
        assertInitialized();
        return config.defaultRevocationCheck;
    }
    
    /**
     * Checks if the configuration object was correctly initialized
     */
    protected static void assertInitialized() {
        if (config == null)
            throw new IllegalStateException("Configuration should be initialized first.");
    }
}

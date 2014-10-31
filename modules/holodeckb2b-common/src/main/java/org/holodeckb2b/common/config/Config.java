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
import java.util.Random;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.Parameter;

/**
 * Contains the configuration of the Holodeck B2B Core.
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
     * Indication whether more extensive bundling of message units is allowed
     */
    private boolean bundling = false;
    
    /*
     * Default setting whether errors on errors should be reported to sender or not
     */
    private boolean defaultReportErrorOnError = false;
    
    /*
     * Default setting whether errors on receipts should be reported to sender or not
     */
    private boolean defaultReportErrorOnReceipt = false;
    
    /*
     * The Axis2 configuration context that is used to process the messages
     */
    private ConfigurationContext    axisCfgCtx = null;
    
    /**
     * Initializes the configuration based on the Holodeck B2B module configuration.
     * 
     * @param configContext     The Axis2 configuration context
     * @param module            The module configuration
     */
    public static void init(ConfigurationContext configContext, AxisModule module) {
        // Alway start with a new and empty configuration
        config = new Config();
        
        // The Axis2 configuration context
        config.axisCfgCtx = configContext;
        
        // The JPA persistency unit to get database access
        //@TODO: REQUIRED, check and throw exception when not available!
        config.persistencyUnit = readModuleParameter(module, "PersistencyUnit");
        
        // The hostname to use in message processing
        String hostName = readModuleParameter(module, "ExternalHostName");
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
        config.hostName = hostName;
        
        /* The configuration of the workerpool. By default the "workers.xml" in the
         * conf directory is used. But it is possible to specify another location
         * using the "WorkerConfig" parameter
         */  
        String workerCfgFile = readModuleParameter(module, "WorkerConfig");
        if (workerCfgFile == null || workerCfgFile.isEmpty())
            // Not specified, use default
            workerCfgFile = configContext.getRealPath("../conf/workers.xml").getAbsolutePath();
        
        config.workerConfigFile = workerCfgFile;
    
        // The temp dir
        String tempDir = readModuleParameter(module, "TempDir");
        if (tempDir == null || tempDir.isEmpty())
            // Not specified, use default
            // @todo: Use system temp as default
            tempDir = configContext.getRealPath("/temp/").getAbsolutePath();
        
        // Ensure the path ends with a folder separator
        config.tempDir = (tempDir.endsWith(FileSystems.getDefault().getSeparator()) ? tempDir 
                            : tempDir + FileSystems.getDefault().getSeparator());
        
        // Option to enable bundling feature
        String bundling = readModuleParameter(module, "BundlingFeature");
        config.bundling = "on".equalsIgnoreCase(bundling) || "true".equalsIgnoreCase(bundling) || "1".equalsIgnoreCase(bundling);        

        // Default setting for reporting Errors on Errors
        String defErrReporting = readModuleParameter(module, "ReportErrorOnError");
        config.defaultReportErrorOnError = "on".equalsIgnoreCase(defErrReporting) 
                                            || "true".equalsIgnoreCase(defErrReporting) 
                                            || "1".equalsIgnoreCase(defErrReporting);        
        // Default setting for reporting Errors on Receipts
        defErrReporting = readModuleParameter(module, "ReportErrorOnReceipt");
        config.defaultReportErrorOnReceipt = "on".equalsIgnoreCase(defErrReporting) 
                                            || "true".equalsIgnoreCase(defErrReporting) 
                                            || "1".equalsIgnoreCase(defErrReporting);        
        
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
     * Indicates whether more extensive bundling of message units is allowed. When enabled Holodeck B2B
     * will add message units in one ebMS message when it detects they are for the same destination MSH.
     * This however will create ebMS messages that DO NOT conform to the ebMS v3 Core Spec and AS4 profile.
     * <p>The default setting is not to use extensive bundling to ensure Core Spec and AS4 compliant ebMS 
     * messages. To enable the feature set the <i>BundlingFeature</i> to "on" or "true".
     * 
     * @return Indication whether bundling feature is enabled to allow for more extensive bundling
     */    
    public static boolean isBundlingFeatureEnabled() {
        assertInitialized();
        
        return config.bundling;
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
     * Checks if the configuration object was correctly initialized
     */
    protected static void assertInitialized() {
        if (config == null)
            throw new IllegalStateException("Configuration should be initialized first.");
    }
    
    /*
     * Helper to read a parameter from the module.xml
     */
    protected static String readModuleParameter(AxisModule m, String n) {
        Parameter p = m.getParameter(n);
        
        if (p == null)
            return null;
        else {
            Object v = p.getValue();
            if (v == null)
                return null;
            else
                return v.toString();
        }
    }
}

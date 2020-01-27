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
package org.holodeckb2b.core.config;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Random;

import org.apache.axis2.context.ConfigurationContext;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;

/**
 * Contains the configuration of the Holodeck B2B Core.
 * <p>The Holodeck B2B configuration is located in the <code><b>«HB2B_HOME»</b>/conf</code> directory where
 * <b>HB2B_HOME</b> is the directory where Holodeck B2B is installed or the system property <i>"holodeckb2b.home"</i>.
 * <p>The structure of the config file is defined by the XML Schema Definition
 * <code>http://holodeck-b2b.org/schemas/2015/10/config</code> which can be found in <code>
 * src/main/resources/xsd/hb2b-config.xsd</code>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class Config implements InternalConfiguration {

    /*
     * The Holodeck B2B home directory
     */
    private String holodeckHome = null;

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
     * The Axis2 configuration context that is used to process the messages
     */
    private ConfigurationContext    axisCfgCtx = null;
    
    /**
     * The indicator whether the Core should fall back to the default event processor implementation in case the 
     * configured implementation cannot be loaded.
     * @since 5.0.0
     */
    private boolean eventProcessorFallback = true;
        
    /**
     * The indicator whether P-Modes for which no {@link IPModeValidator} implementation is available should be rejected 
     * or still be loaded. 
     * @since 5.0.0
     */
    private boolean acceptNonValidablePModes = false;

    /*
     * Indicator whether strict header validation should be applied to all messages
     * @since 4.0.0
     */
    private boolean useStrictHeaderValidation = false;

    private boolean isTrue (final String s) {
      return "on".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s) || "1".equalsIgnoreCase(s);
    }

    /**
     * Initializes the configuration object using the Holodeck B2B configuration file located in <code>
     * «HB2B_HOME»/conf/holodeckb2b.xml</code> where <b>HB2B_HOME</b> is the directory where Holodeck B2B is installed.
     *
     * @param configCtx     The Axis2 configuration context containing the Axis2 settings
     * @throws Exception    When the configuration can not be initialized
     */
    public Config(final ConfigurationContext configCtx) throws Exception {
        // The Axis2 configuration context
        axisCfgCtx = configCtx;

        // Set the Holodeck B2B home directory
        this.holodeckHome = axisCfgCtx.getRealPath("").getParent() + File.separatorChar; 

        // Read the configuration file
        final ConfigXmlFile configFile = ConfigXmlFile.loadFromFile(
                                                        Paths.get(holodeckHome, "conf", "holodeckb2b.xml").toString());

        // The hostname to use in message processing
        hostName = configFile.getParameter("ExternalHostName");
        if (Utils.isNullOrEmpty(hostName)) {
            try {
            	//TODO: Getting the hostname this way can be slow, can we change?
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
     
        // The class name of the event processor
        final String disableFallback = configFile.getParameter("DisableEventProcessorFallback");
        eventProcessorFallback = !isTrue(disableFallback);

        // Indicator whether to accept non validable P-Modes, default false
        final String acceptNVPMode = configFile.getParameter("AcceptNonValidablePModes");
        acceptNonValidablePModes = isTrue(acceptNVPMode); 
        
        // Indicator whether strict header validation should be performed
        final String strictHeaderValidation = configFile.getParameter("StrictHeaderValidation");
        useStrictHeaderValidation = isTrue(strictHeaderValidation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationContext getAxisConfigurationContext() {
        return axisCfgCtx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostName () {
        return hostName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHolodeckB2BHome () {
        return holodeckHome;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWorkerPoolCfgFile() {
        return workerConfigFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTempDirectory() {
        return tempDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public boolean allowSignalBundling() {
        return allowSignalBundling;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldReportErrorOnError() {
        return defaultReportErrorOnError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldReportErrorOnReceipt() {
        return defaultReportErrorOnReceipt;
    }

    /**
     * Indicates whether Holodeck B2B Core should not fall back to the default event processor in case the configured
     * event processor fails to load. The default behaviour is to fall back but in certain deployment it may be required
     * to use the configured implementation.   
     *  
     * @return	<code>true</code> if Core should fall back to default implementation,<br>
     * 			<code>false</code> if startup of the gateway should be aborted in case the configured event processor
     * 							   cannot be loaded
     * @since 5.0.0
     */
    @Override
    public boolean eventProcessorFallback() {
    	return eventProcessorFallback;
    }
    
    /**
     * {@inheritDoc}
     * @since 5.0.0
     */
    @Override
    public boolean acceptNonValidablePMode() {
    	return acceptNonValidablePModes;
    }

    /**
     * {@inheritDoc}
     * @since 4.0.0
     */
    @Override
    public boolean useStrictHeaderValidation() {
        return useStrictHeaderValidation;
    }
}

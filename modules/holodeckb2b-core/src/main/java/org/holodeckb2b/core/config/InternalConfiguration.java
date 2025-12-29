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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisConfiguration;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;

/**
 * Contains the configuration of the Holodeck B2B Core as defined by the {@link IConfiguration} interface and adds the
 * settings that are only to be used by the Holodeck B2B Core internally.
 * <p>It is based on {@link AxisConfiguration} so we can reuse the Axis2 configuration file also for the configuration
 * of the Holodeck B2B gateway.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class InternalConfiguration extends AxisConfiguration implements IConfiguration {

    /*
     * The Holodeck B2B home directory
     */
    private Path holodeckHome = null;

    /*
     * The host name
     */
    private String  hostName = null;

    /*
     * The location of the workerpool configuration
     */
    private Path  workerConfigFile = null;

    /*
     * The directory where files can be temporarily stored
     */
    private Path  tempDirectory = null;

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

    /**
     * Indicator whether strict header validation should be applied to all messages
     * @since 4.0.0
     */
    private boolean useStrictHeaderValidation = false;

    /**
     * Creates a new Holodeck B2B configuration instance that uses the given path as its home directory.
     *
     * @param hb2bHomePath	path of the HB2B home directory
     */
    public InternalConfiguration(final Path hb2bHomePath) {
    	holodeckHome = hb2bHomePath.toAbsolutePath();
    }

    /**
     * Gets the location of the worker pool configuration file. This an optional configuration parameter and when not
     * specified the default location of worker configuration file is <code>«HB2B_HOME»/conf/workers.xml</code>.
     *
     * @return The absolute path to the worker pool configuration file.
     */
    public Path getWorkerPoolCfgFile() {
    	return workerConfigFile;
    }

    /**
     * Sets the location of the worker pool configuration file.
     *
     * @param path location of the worker pool config file
     */
    public void setWorkerPoolCfgFile(final Path path) {
    	workerConfigFile = path;
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
    public boolean eventProcessorFallback() {
    	return eventProcessorFallback;
    }

    /**
     * Sets whether Holodeck B2B Core should not fall back to the default event processor in case the configured event
     * processor fails to load.
     *
     * @param fallback	<code>true</code> if Core should fall back to default implementation,<br>
     * 					<code>false</code> if startup of the gateway should be aborted in case the configured event
     * 					processor cannot be loaded
     */
    public void setEventProcessorFallback(final boolean fallback) {
    	eventProcessorFallback = fallback;
    }

    /**
     * Indicates whether a P-Mode for which no {@link IPModeValidator} implementation is available to check it before
     * loading it should be rejected or still be loaded.
     *
     * @return <code>true</code> if the P-Mode should still be loaded,<br><code>false</code> if it should be rejected
     * @since 5.0.0
     */
    public boolean acceptNonValidablePMode() {
    	return acceptNonValidablePModes;
    }

    /**
     * Sets whether a P-Mode for which no {@link IPModeValidator} implementation is available to check it before
     * loading it should be rejected or still be loaded.
     *
     * @param accept <code>true</code> if the P-Mode should still be loaded,<br>
     * 				 <code>false</code> if it should be rejected
     */
    public void setAcceptNonValidablePMode(final boolean accept) {
    	acceptNonValidablePModes = accept;
    }

	@Override
	public String getHostName() {
		return hostName;
	}

	/**
	 * Sets the host name.
	 *
	 * @param host	Host name to use
	 */
	public void setHostName(final String host) {
		hostName = host;
	}

	@Override
	public Path getHolodeckB2BHome() {
		return holodeckHome;
	}

	/**
	 * Sets the Holodeck B2B home directory.
	 *
	 * @param homedir	Path to the Holodeck B2B home directory
	 */
	public void setHolodeckB2BHome(final Path homedir) {
		holodeckHome = homedir.toAbsolutePath();
	}

	@Override
	public Path getTempDirectory() {
		return tempDirectory;
	}

	/**
	 * Sets the Holodeck B2B temp directory.
	 *
	 * @param tempdir	Path to the Holodeck B2B temp directory
	 */
	public void setTempDirectory(final Path tempdir) {
		tempDirectory = tempdir;
	}

	@Override
	public boolean shouldReportErrorOnError() {
		return defaultReportErrorOnError;
	}

	/**
	 * Sets the default whether Errors on Errors should be reported to the sender of the faulty error.
	 *
	 * @param report <code>true</code> if generated errors on errors should by default be reported to the sender,<br>
     *        		 <code>false</code> otherwise
	 */
	public void setReportErrorOnError(final boolean report) {
		defaultReportErrorOnError = report;
	}

	@Override
	public boolean shouldReportErrorOnReceipt() {
		return defaultReportErrorOnReceipt;
	}

	/**
	 * Sets the default whether Errors on Receipts should be reported to the sender of the faulty error.
	 *
	 * @param report <code>true</code> if generated errors on receipts should by default be reported to the sender,<br>
     *        		 <code>false</code> otherwise
	 */
	public void setReportErrorOnReceipts(final boolean report) {
		defaultReportErrorOnReceipt = report;
	}

	@Override
	public boolean useStrictHeaderValidation() {
		return useStrictHeaderValidation;
	}

	/**
	 * Sets the global setting for whether Holodeck B2B should perform a strict validation of the message unit header
	 * meta-data.
	 *
	 * @param strict 	<code>true</code> if strict validation should be performed,<br>
	 * 					<code>false</code> if a basic validation is enough
	 */
	public void setStrictHeaderValidation(final boolean strict) {
		useStrictHeaderValidation = strict;
	}

	@Override
	public HashMap<String, AxisModule> getModules() {
		Set<Entry<String, AxisModule>> cfgdModules = super.getModules().entrySet();
		HashMap<String, AxisModule> allModules = new LinkedHashMap<>(cfgdModules.size());
		cfgdModules.stream().filter(e -> isGlobalModulesRegistered(e.getValue().getName()))
							.forEach(e -> allModules.put(e.getKey(), e.getValue()));
		cfgdModules.parallelStream().forEach(e -> allModules.putIfAbsent(e.getKey(), e.getValue()));
		return allModules;
	}
}

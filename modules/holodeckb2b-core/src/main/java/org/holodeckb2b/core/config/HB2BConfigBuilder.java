/**
 * Copyright (C) 2020 The Holodeck B2B Team, Sander Fieten
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

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.deployment.AxisConfigBuilder;
import org.apache.axis2.deployment.DeploymentConstants;
import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.XMLUtils;
import org.holodeckb2b.commons.util.Utils;

/**
 * Is a specialised {@link AxisConfigBuilder} that will not only build the Axis2 configuration but also read the 
 * Holodeck B2B specific configuration parameters from the configuration file. This configuration file is a Axis2
 * configuration file with root element <code>holodeckb2b-config</code> and includes the Holodeck B2B as <code>parameter
 * </code> elements.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  5.0.0
 */
public class HB2BConfigBuilder extends AxisConfigBuilder {

	/**
	 * The [local] name of the root element of the Holodeck B2B configuration XML file
	 */
	private static final String	HB2B_CONFIG_ROOT = "holodeckb2b-config";
	
	public HB2BConfigBuilder(InputStream serviceInputStream, AxisConfiguration axisConfiguration,
			DeploymentEngine deploymentEngine) {
		super(serviceInputStream, axisConfiguration, deploymentEngine);
	}

    /**
     * Creates the {@link OMElement} representation for the Holodeck B2B/Axis2 configuration file. 
     * <p>Due to private methods in {@link AxisConfigBuilder} we cannot override the {@link #populateConfig()} method
     * to process a renamed root element (<code>holodeckb2b-config</code> instead of <code>axisconfig</code>). Therefore 
     * we override this method and rename the read root element so <code>AxisConfigBuilder</code> can process it.   
     *
     * @return Returns <code>OMElement</code> for the Holodeck B2B config file.
     * @throws javax.xml.stream.XMLStreamException  when the XML document could not be read or the root element is not
     * 												<code>holodeckb2b-config</code>
     */
    @Override
	public OMElement buildOM() throws XMLStreamException {
        OMElement element = (OMElement) XMLUtils.toOM(descriptionStream);        
        element.build();
        
        final String elName = element.getLocalName();
        if (HB2B_CONFIG_ROOT.equals(elName))
        	element.setLocalName(DeploymentConstants.TAG_AXISCONFIG);
        else
        	throw new XMLStreamException("Unexpected root element encountered");
        
        return element;
    }	
    
    
    @Override
    public void populateConfig() throws DeploymentException {
    	// First process the Axis2 configuration
    	super.populateConfig();
    	
    	// Then process Holodeck B2B specific settings
    	InternalConfiguration hb2bConfig = (InternalConfiguration) axisConfig;
    	// The hostname to use in message processing
    	String hostName = (String) axisConfig.getParameterValue("ExternalHostName");    	
		if (Utils.isNullOrEmpty(hostName)) {
			try {
				hostName = InetAddress.getLocalHost().getCanonicalHostName();
			} catch (final UnknownHostException e) {
			}
			if (Utils.isNullOrEmpty(hostName)) {
				// If we still have no host name, we just generate a random hex number to use as host name
				final Random r = new Random();
				hostName = Long.toHexString((long) r.nextInt() * (long) r.nextInt()) + ".generated";
			}
		}
		hb2bConfig.setHostName(hostName);

		final Path hb2bHome = hb2bConfig.getHolodeckB2BHome();
		// The configuration of the worker pool. By default the "workers.xml" in the conf directory is used. But it is
		// possible to specify another location using the "WorkerConfig" parameter
		final String workerCfgFile = (String) axisConfig.getParameterValue("WorkerConfig");
		hb2bConfig.setWorkerPoolCfgFile(!Utils.isNullOrEmpty(workerCfgFile) ? Paths.get(workerCfgFile) 
																		    : hb2bHome.resolve("conf/workers.xml"));
		
		// The temp dir. By default it is set to «HB2B_HOME»/temp but a specific directory can be assigned using the
		// "TempDir" parameter
		final String tempDirectory = (String) axisConfig.getParameterValue("TempDir");
		hb2bConfig.setTempDirectory(!Utils.isNullOrEmpty(tempDirectory) ? Paths.get(tempDirectory) 
																	    : hb2bHome.resolve("temp"));

        // Global setting for reporting Errors on Errors
		hb2bConfig.setReportErrorOnError(Utils.isTrue((String) axisConfig.getParameterValue("ReportErrorOnError")));

		// Global setting for reporting Errors on Receipts
		hb2bConfig.setReportErrorOnReceipts(Utils.isTrue((String) axisConfig.getParameterValue("ReportErrorOnReceipt")));
		
        // Indicator whether a fall back to the default event processor is allowed
        hb2bConfig.setEventProcessorFallback(!Utils.isTrue((String) axisConfig
        														.getParameterValue("DisableEventProcessorFallback")));
        
        // Indicator whether to accept non validable P-Modes, default false       
        hb2bConfig.setAcceptNonValidablePMode(Utils.isTrue((String) axisConfig
        															 .getParameterValue("AcceptNonValidablePModes")));
        
        // Indicator whether strict header validation should be performed
        hb2bConfig.setStrictHeaderValidation(Utils.isTrue((String) axisConfig
        															   .getParameterValue("StrictHeaderValidation")));    	
    }
}

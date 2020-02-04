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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.axis2.AxisFault;
import org.apache.axis2.deployment.AxisConfigBuilder;
import org.apache.axis2.deployment.DeploymentConstants;
import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.deployment.ModuleDeployer;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisConfigurator;
import org.holodeckb2b.common.util.Utils;

/**
 * Is a specialised {@link AxisConfigurator} that will uses the {@link HB2BConfigBuilder} to build a configuration that
 * does not only include the Axis2 settings but also the Holodeck B2B ones.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  5.0.0
 */
public class HB2BAxis2Configurator extends DeploymentEngine implements AxisConfigurator {

	/**
	 * Path of the Holodeck B2B home directory
	 */
	private Path	hb2bHomeDirectory;
	/**
	 * Path to the Holodeck B2B / Axis2 configuration file
	 */
	private Path 	configFile;
	/**
	 * Path to the repository directory
	 */
	private Path 	repositoryPath;		

	/**
	 * Creates a new configuration builder using the given path as the Holodeck B2B home directory. The configuration
	 * should then be located in <code>«HB2B_HOME»/conf/holodeckb2b.xml</code> and the default repository directory is
	 * <code>«HB2B_HOME»/repository</code>. However both locations can be specifically set, with the location of the 
	 * configuration file in a [Java] system property "hb2b.config" and the repository directory in the configuration
	 * file itself using the "repository" parameter.
	 *  
	 * @param configPath	path of the Holodeck B2B home directory 
	 * @throws AxisFault	when either the configuration file or repository is not available at the given location 
	 */
	public HB2BAxis2Configurator(final String hb2bHome) throws AxisFault {
		hb2bHomeDirectory = Paths.get(hb2bHome);
		if (!Files.isDirectory(hb2bHomeDirectory) || !Files.isReadable(hb2bHomeDirectory))
			throw new AxisFault("HB2B home directory (" + hb2bHome + ") does not exist or is not accessible");
		
		// Check if system property for config file is set
		final String sysCfgProp = System.getProperty("hb2b.config");
		configFile = !Utils.isNullOrEmpty(sysCfgProp) ? Paths.get(sysCfgProp) :
													   hb2bHomeDirectory.resolve("conf/holodeckb2b.xml");
		if (!Files.isReadable(configFile))
			throw new AxisFault("Configuration file not found at " + configFile.toString());
		
		repositoryPath = hb2bHomeDirectory.resolve("repository");
		if (!Files.isDirectory(repositoryPath) || !Files.isReadable(repositoryPath))
			throw new AxisFault("Specified repository path (" + repositoryPath.toString() 
																			+ ") does not exist or is not accessible");			
	}
	
	@Override
	public AxisConfiguration getAxisConfiguration() throws AxisFault {
		// If we have already built the configuration, just return it
		if (axisConfig != null)
			return axisConfig;
		
        try (InputStream configStream = new FileInputStream(configFile.toFile())) {
        	axisConfig = new InternalConfiguration(hb2bHomeDirectory);
            AxisConfigBuilder builder = new HB2BConfigBuilder(configStream, axisConfig, this);
            builder.populateConfig();
        } catch (IOException e) {
            throw new AxisFault("Cannot access the configuration file " + configFile.toString());
		} 
        moduleDeployer = new ModuleDeployer(axisConfig);
        
        // Check if a specific repository path is specified in the configuration file and use it when the specified 
        // directory is accessible
        Parameter axis2repoPara = axisConfig.getParameter(DeploymentConstants.AXIS2_REPO);
        if (axis2repoPara != null) {
            final Path repoLocation = Paths.get((String) axis2repoPara.getValue());
            if (Files.isDirectory(repoLocation) && Files.isReadable(repoLocation))
            	repositoryPath = repoLocation;            
        }
        
        loadRepository(repositoryPath.toString());
        axisConfig.setConfigurator(this);
        return axisConfig;		
	}
    
	@Override
	public void engageGlobalModules() throws AxisFault {
		engageModules();
	}

}

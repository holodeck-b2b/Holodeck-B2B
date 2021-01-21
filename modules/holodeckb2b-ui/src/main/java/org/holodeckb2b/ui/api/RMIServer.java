/**
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
package org.holodeckb2b.ui.api;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.workers.AbstractWorkerTask;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Is the Holodeck B2B <i>worker</i> to initialise the RMI server that the default User Interface uses to retrieve 
 * information from the gateway. The worker has one optional parameter <i>port</i> that can be used to specify the port
 * number that should be used by the RMI server. The default port is set to 1701.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class RMIServer extends AbstractWorkerTask {
	private static final Logger	log = LogManager.getLogger(RMIServer.class);

	/**
	 * The name of the parameter to be used to specify the port number for the RMI server
	 */
	public static final String P_PORT = "port";
	
	/**
	 * The actual port the RMI server uses
	 */
	private int	port;
	
	/**
	 * The implementation of the interface
	 */
	private static CoreInfo serverImpl;  
	
	@Override
	public void setParameters(Map<String, ?> parameters) throws TaskConfigurationException {
		setName("Holodeck B2B UI API");
		
		port = CoreInfo.DEFAULT_PORT;
		try {
			port = Integer.parseInt((String) parameters.get(P_PORT));
		} catch (NumberFormatException invalidPort) { 
			log.warn("Invalid or no value provided for the \"port\" parameter ({}). Using default port {}", 
					parameters.get(P_PORT), CoreInfo.DEFAULT_PORT);
		} catch (Exception noPort) {};
	}

	@Override
	public void doProcessing() throws InterruptedException {

        try {
        	log.trace("Creating the API implementation");
			serverImpl = new CoreInfoImpl();
        	log.trace("Creating RMI stub for UI access");
        	Remote stub = UnicastRemoteObject.exportObject(serverImpl, 0);
        	log.trace("Creating the RMI registry on port {}", port);
        	Registry registry = LocateRegistry.createRegistry(port);
        	log.trace("Binding stub in RMI registry");
        	registry.rebind(CoreInfo.RMI_SVC_NAME, stub);
        	log.info("Registered the RMI service for the local UI");        	
        } catch (Exception e) {
            log.fatal("Could not start the UI API! Error details: " + e.getMessage());
        }		
	}
}

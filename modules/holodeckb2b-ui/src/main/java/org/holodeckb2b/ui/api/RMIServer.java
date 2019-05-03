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
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Is the Holodeck B2B <i>worker</i> to initialise the RMI server that the default User Interface uses to retrieve 
 * information from the gateway. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class RMIServer extends AbstractWorkerTask {
	private static final Logger	log = LogManager.getLogger(RMIServer.class);

	private static Registry registry;
	
	private static CoreInfo serverImpl;
	
	@Override
	public void setParameters(Map<String, ?> parameters) throws TaskConfigurationException {
		setName("Holodeck B2B UI API");
	}

	@Override
	public void doProcessing() throws InterruptedException {

        try {
        	log.trace("Creating the API implementation");
        	serverImpl = new CoreInfoImpl();
        	log.trace("Creating RMI stub for UI access");
        	Remote stub = UnicastRemoteObject.exportObject(serverImpl, 0);
        	log.trace("Creating the RMI registry");
        	registry = LocateRegistry.createRegistry(10888);
        	log.trace("Binding stub in RMI registry");
        	registry.rebind(CoreInfo.RMI_SVC_NAME, stub);
        	log.info("Registered the RMI service for the local UI");        	
        } catch (Exception e) {
            log.fatal("Could not start the UI API! Error details: " + e.getMessage());
        }		
	}
}

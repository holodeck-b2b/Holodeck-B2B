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
package org.holodeckb2b.core;

import java.util.Hashtable;
import java.util.Map.Entry;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.engine.AxisServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.config.HB2BAxis2Configurator;
import org.holodeckb2b.core.config.InternalConfiguration;

/**
 * Is the Java application to start the Holodeck B2B Server.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  5.0.0
 */
public class HolodeckB2BServer extends AxisServer {

	private static final Logger log = LogManager.getLogger(HolodeckB2BServer.class);
	
	private static final String A_HOME = "-home";
	
	/**
	 * The server configuration read from the <code>holodeckb2b.xml</code> file
	 */
	private final  InternalConfiguration	serverConfig;
	
	/**
	 * Creates a new server instance and initialises the configuration context.
	 * 
	 * @param hb2bHome		path to the Holodeck B2B home directory
	 * @throws AxisFault	when there is a problem in initialising the configuration
	 */
    public HolodeckB2BServer(final String hb2bHome) throws AxisFault {
        super(false);
        final HB2BAxis2Configurator configurator = new HB2BAxis2Configurator(hb2bHome);
        configContext = ConfigurationContextFactory.createConfigurationContext(configurator);
        serverConfig = (InternalConfiguration) configurator.getAxisConfig();
    }

    /**
     * Main script to start the Holodeck B2B server.
     *  
     * @param args			command line args     
     */
    public static void main(String[] args) {
        
    	if (args.length == 0) {
    		log.fatal("Missing required argument \"{}\" defining Holodeck B2B's root directory", A_HOME);
    		printUsage();
    	}
    	String hb2bHome = null;
    	for(int i = 0; i < args.length - 1 && hb2bHome == null; i++) 
    		if (A_HOME.equalsIgnoreCase(args[i]))
    			hb2bHome = args[i+1];
    	
    	if (hb2bHome == null) {
    		log.fatal("No value specified for argument \"{}\" defining Holodeck B2B's root directory", A_HOME);
    		printUsage();    		
    	}
        
        log.info("Starting the server");
        System.out.println("[HolodeckB2BServer] Starting server");
        HolodeckB2BServer server = null;
        try {
            server = new HolodeckB2BServer(hb2bHome);
            server.start();            
        } catch (Throwable t) {
            log.fatal("Error during startup of the server: {}", Utils.getExceptionTrace(t));
            System.err.println("[HolodeckB2BServer] Aborted startup due to error. See log for details.");
            System.exit(-1);
        }

        // Check if all modules and services have correctly started
        if (server != null) {
        	final Hashtable<String,String> faultyModules = server.serverConfig.getFaultyModules();
        	final Hashtable<String,String> faultyServices = server.serverConfig.getFaultyServices();        	
        	if (!Utils.isNullOrEmpty(faultyModules) || !Utils.isNullOrEmpty(faultyServices)) {
        		final StringBuilder logMsg = new StringBuilder();
        		logMsg.append("Holodeck B2B cannot be started because one or more modules or services failed to start!");
        		if (!Utils.isNullOrEmpty(faultyModules)) {
        			  logMsg.append("\tList of modules that failed to start:");
        			  for(Entry<String, String> fm : faultyModules.entrySet())
        				  logMsg.append("\t\t").append(fm.getKey()).append(" - ").append(fm.getValue()).append('\n');
        		}
        		if (!Utils.isNullOrEmpty(faultyServices)) {
        			logMsg.append("\tList of services that failed to start:");
        			for(Entry<String, String> fs : faultyServices.entrySet())
        				logMsg.append("\t\t").append(fs.getKey()).append(" - ").append(fs.getValue()).append('\n');
        		}
        		log.fatal(logMsg.toString());
                System.err.println("[HolodeckB2BServer] Aborted startup due to error. See log for details.");
                System.exit(-1);        			
        	}
        } 
        
        log.info("Server started successfully");
        System.out.println("[HolodeckB2BServer] Started server");        
    }

    /**
     * Prints a simple "usage" instruction.
     */
    public static void printUsage() {
        System.out.println("Usage: HolodeckB2B -home <HB2B root directory>");
        System.out.println();
        System.exit(-1);
    }	
}

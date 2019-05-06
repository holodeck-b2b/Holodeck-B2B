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
package org.holodeckb2b.ui.app.cli;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.Date;

import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.IAgreement;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.ui.api.CoreInfo;

/**
 * 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class HB2BInfoTool {
	
	/**
	 * The API to access the information in the Holodeck B2B instance
	 */
	private static CoreInfo		coreAPI;
	/**
	 * The host name used by this Holodeck B2B instance
	 */
	private static String 		hb2bHostName;
	
	public static void main(String[] args) {
		CommandLineArguments clArgs = null;
		try {
			clArgs = new CommandLineArguments(args);
		} catch (IllegalArgumentException invalidInvocation) {
			CommandLineArguments.printUsage();
			System.exit(-1);
		}
				
        try {
			coreAPI = (CoreInfo) LocateRegistry.getRegistry(getServerPort(clArgs)).lookup(CoreInfo.RMI_SVC_NAME);
			hb2bHostName = coreAPI.getHostName();
		} catch (RemoteException | NotBoundException e) {
			System.err.println("Could not connect the Holodeck B2B instance on port " + getServerPort(clArgs) + "!");
			System.exit(-2);
		}            
				
		switch (clArgs.getAction()) {
		case LIST_PMODES :
			listPModes(); break;
		case PRINT_PMODE :
			printPMode(clArgs.getParameter(CommandLineArguments.PRT_PMODE_ID)); break;
		}
	}

	/**
	 * Gets the server port to use for retrieving info from the Holodeck B2B instance
	 * 
	 * @param clArgs	The parsed command line arguments
	 * @return			The port to connect to
	 */
	private static int getServerPort(CommandLineArguments clArgs) {
		try {
			return Integer.parseInt(clArgs.getParameter(CommandLineArguments.PORT_OPTION));
		} catch (Exception useDefault) {
			return CoreInfo.DEFAULT_PORT;
		}
	}

	/**
	 * Gets all the P-Modes currently configured in the Holodeck B2B instance and prints a list of PMode.id, MEP and
	 * PartyIds used for each P-Mode.
	 */
	private static void listPModes() {
		PMode[] pmodes = null;
		try {
			pmodes = coreAPI.getPModes();
		} catch (Exception e) {
		System.err.println("Could not retrieve the P-Mode from Holodeck B2B instance. See error details below:");
			e.printStackTrace(System.err);
			System.exit(-3);
		}
		
		if (pmodes == null || pmodes.length == 0) { 
			System.out.println("There are currently no P-Modes configured on this Holodeck B2B instance.");
			return;
		}
		
		System.out.printf("There are currently %d P-Modes installed:%n%n", pmodes.length);		
		for(PMode p : pmodes) {			
			System.out.println("=======================");
			System.out.println("P-Mode.id   : " + p.getId());
			IAgreement agreement = p.getAgreement();
			System.out.println("Agreement   : " + (agreement == null ? "N/A" : agreement.getName()));
			System.out.println("MEP-binding : " + p.getMepBinding());
			ITradingPartnerConfiguration initiator = p.getInitiator();
			if (initiator != null) { 
				System.out.println("Initiator   : ");
				System.out.println("\tRole   : " + 
											(Utils.isNullOrEmpty(initiator.getRole()) ? "N/A" : initiator.getRole()));
				Collection<IPartyId> partyIds = initiator.getPartyIds();
				if (!Utils.isNullOrEmpty(partyIds)) {
					IPartyId pid = partyIds.iterator().next();
					System.out.println("\tPartyId: " + 
											(!Utils.isNullOrEmpty(pid.getType()) ? pid.getType() + "::" : "") 
											+ pid.getId());
				} else
					System.out.println("\tPartyId: N/A");
			} else
				System.out.println("Initiator   : N/A");
			ITradingPartnerConfiguration responder = p.getResponder();
			if (responder != null) { 
				System.out.println("Responder   : ");
				System.out.println("\tRole   : " + 
											(Utils.isNullOrEmpty(responder.getRole()) ? "N/A" : responder.getRole()));
				Collection<IPartyId> partyIds = responder.getPartyIds();
				if (!Utils.isNullOrEmpty(partyIds)) {
					IPartyId pid = partyIds.iterator().next();
					System.out.println("\tPartyId: " + 
											(!Utils.isNullOrEmpty(pid.getType()) ? pid.getType() + "::" : "") 
											+ pid.getId());
				} else
					System.out.println("\tPartyId: N/A");
			} else
				System.out.println("Responder   : N/A");
		}
	}
	
	/**
	 * Checks if a P-Mode with the given id is configured on this Holodeck B2B instance and outputs the complete P-Mode
	 * as a XML document. 
	 * <p>NOTE: As the RMI API for the UI only supports the default P-Mode implementation as defined by the <i>interface
	 * </i> module custom parameters will not be included in the output. 
	 * 
	 * @param pmodeId	identifier of the P-Mode to print
	 */
	private static void printPMode(final String pmodeId) {
		PMode[] pmodes = null;
		try {
			pmodes = coreAPI.getPModes();
			
		} catch (Exception e) {
		System.err.println("Could not retrieve the P-Mode from Holodeck B2B instance. See error details below:");
			e.printStackTrace(System.err);
			System.exit(-3);
		}		
		if (pmodes == null || pmodes.length == 0) { 
			System.out.println("There are currently no P-Modes configured on this Holodeck B2B instance.");
			return;
		}
		PMode p = null;
		for (int i = 0; i < pmodes.length && p == null; i++) {
			if (pmodes[i].getId().equals(pmodeId))
				p = pmodes[i];
		}
		if (p == null) {
			System.out.println("There is no P-Mode configured on this Holodeck B2B instance with id= " + pmodeId);
			return;			
		}
		
		try {
			System.out.printf("<!--%nThis P-Mode was extracted from Holodeck B2B instance %s on %tc%n-->%n",
							  hb2bHostName, new Date());   
			p.writeAsXMLTo(System.out);
			System.out.println();
		} catch (Exception e) {
			System.err.println("An error occurred while creating the P-Mode XML document! Error details:\n" 
								+ Utils.getExceptionTrace(e));
		}		
	}
}

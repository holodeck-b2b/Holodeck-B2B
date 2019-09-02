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
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.Arrays;
import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.IAgreement;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.ui.api.CertType;
import org.holodeckb2b.ui.api.CoreInfo;

/**
 * Is the CLI application for monitoring a Holodeck B2B instance. 
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
		case LIST_CERTS :
			listCerts(clArgs.getParameter(CommandLineArguments.CERT_TYPE),
					  clArgs.getParameter(CommandLineArguments.FORMAT)); break;
		case PRINT_CERT :
			printCert(clArgs.getParameter(CommandLineArguments.CERT_TYPE), 
					  clArgs.getParameter(CommandLineArguments.CERT_ALIAS)); break;
		case MSG_STATUS :
			getMsgStatus(clArgs.getParameter(CommandLineArguments.MESSAGE_ID)); break;
		case STATUS_LIST :
			getMsgStatusList(clArgs.getParameter(CommandLineArguments.MESSAGE_ID)); break;
		case HISTORY :
			getHistory(clArgs.getParameter(CommandLineArguments.FROM),
					   clArgs.getParameter(CommandLineArguments.MAX)); break;
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
	
	/**
	 * Prints the list of certificates trusted by this Holodeck B2B instance.
	 * 
	 * @param certType	The certificates' type, must be a name from {@link CertType}
	 * @param format	Specifies how much detailed information should be provided:<ul>
	 * 					<li><i>simple</i> : shows DN of both subject and issuer</li>
	 * 					<li><i>detail</i> : in addition shows SKI and Serial Number</li></ul>
	 */
	private static void listCerts(final String certType, final String format) {
		CertType type = null;
		try {
			type = CertType.valueOf(certType);
		} catch (Exception invalidType) {
			System.err.println("The specified certificate type [" + certType + "] is unknown!");
			System.exit(-1);
		}
		
		Map<String, X509Certificate> certs = null;
		try { 
			certs = coreAPI.getCertificates(type);
		} catch (Exception e) {
			System.err.println(
					"Could not retrieve the list of certificates from Holodeck B2B instance. See error details below:");
			e.printStackTrace(System.err);
			System.exit(-3);			
		}
		
		boolean detailed = "detail".equalsIgnoreCase(format);
		
		System.out.printf("There are currently %d %s certificates configured:%n%n", certs.size(), type.name());
		for(Entry<String, X509Certificate> e : certs.entrySet()) {
			System.out.println("=======================");
			System.out.println("Alias: " + e.getKey());
			X509Certificate c = e.getValue();
			boolean isCA = c.getBasicConstraints() > 0;
			boolean isRootCA = isCA && c.getSubjectDN().getName().equals(c.getIssuerDN().getName());
			if (isRootCA) 
				System.out.print("Trusted root CA : ");
			else if (isCA)
				System.out.print("Trusted intermediate CA : ");
			else				
				System.out.print("Subject : ");
			System.out.println(c.getSubjectDN().getName());
			if (!isRootCA)
				System.out.println("\tIssued by : " + c.getIssuerDN().getName());
			if (detailed) {
				System.out.println("\tSerial No : " + c.getSerialNumber().toString(16));
				byte[] skiExtValue = c.getExtensionValue("2.5.29.14");
				byte[] ski = null;
				if (skiExtValue != null)
					ski = Arrays.copyOfRange(skiExtValue, 4, skiExtValue.length);
				System.out.println("\tSKI       : " + (ski != null ? Hex.encodeHexString(ski) : "N/A"));
			}
		}
	}
	
	/**
	 * Prints detailed information about the certificate registered in the Holodeck B2B instance under the given alias 
	 * and of the specified type.
	 * 
	 * @param certType	The certificate's type, must be a name from {@link CertType}
	 * @param alias		The alias under which certificate is registered
	 */
	private static void printCert(final String certType, final String alias) {
		CertType type = null;
		try {
			type = CertType.valueOf(certType);
		} catch (Exception invalidType) {
			System.err.println("The specified certificate type [" + certType + "] is unknown!");
			System.exit(-1);
		}
		
		Map<String, X509Certificate> certs = null;
		try { 
			certs = coreAPI.getCertificates(type);
		} catch (RemoteException e) {
			System.err.println(
			"An error occurred while getting the certificate from the Holodeck B2B instance. See error details below:");
			e.printStackTrace(System.err);
			System.exit(-3);						
		}
		if (Utils.isNullOrEmpty(certs) || !certs.containsKey(alias)) {
			System.out.println("There is no " + type.toString() 
							   + " certificate configured on this Holodeck B2B instance with alias= " + alias);
			return;			
		}
		
		X509Certificate cert = certs.get(alias);	
		System.out.println("Subject     : " + cert.getSubjectDN().getName());
		System.out.println("Issuer      : " + cert.getIssuerDN().getName());
		System.out.println("Serial no   : " + cert.getSerialNumber().toString(16));
		byte[] skiExtValue = cert.getExtensionValue("2.5.29.14");
		byte[] ski = null;
		if (skiExtValue != null)
			ski = Arrays.copyOfRange(skiExtValue, 4, skiExtValue.length);
		System.out.println("SKI         : " + (ski != null ? Hex.encodeHexString(ski) : "N/A"));		
		System.out.println("Valid from  : " + cert.getNotBefore().toString());
		System.out.println("     until  : " + cert.getNotAfter().toString());
		System.out.println("Fingerprints:");
		System.out.println("     SHA256 : " + getCertFingerPrint("SHA-256", cert));
		System.out.println("     SHA1   : " + getCertFingerPrint("SHA-1", cert));
	}	

	/**
	 * Prints the current processing state of the message unit with the given MessageId. As it is possible that there 
	 * exist multiple message units with the same id beside the status also the type of message unit and direction is
	 * printed.
	 * 
	 * @param messageId 
	 */
	private static void getMsgStatus(String messageId) {
		MessageUnit[] msgUnits = null;
		try {
			msgUnits = coreAPI.getMessageUnitInfo(messageId);
		} catch (RemoteException e) {
			System.err.println(
			"An error occurred while getting the status from the Holodeck B2B instance. See error details below:");
			e.printStackTrace(System.err);
			System.exit(-3);						
		}
		if (msgUnits == null || msgUnits.length == 0) {
			System.out.println("No message unit with messageId=" + messageId + " could be found!");
			return;			
		}
		
		if (msgUnits.length > 1) {
			System.out.println("More than one message unit with MessageId [" + messageId 
								+ "] were found. Listing all:");
			System.out.println();
		}
		
		for(MessageUnit m : msgUnits) {
			System.out.print("Current processing status (since " + m.getCurrentProcessingState().getStartTime() + ") of ");
			System.out.print(m.getDirection() == Direction.IN ? "received " : "outgoing ");
			System.out.print(MessageUnitUtils.getMessageUnitName(m) 
								+ " is " + m.getCurrentProcessingState().getState().name());
			final String desc =  m.getCurrentProcessingState().getDescription();
			System.out.println(!Utils.isNullOrEmpty(desc) ? (" (" + desc + ")") : ""); 				
		}		
	}
	
	/**
	 * Prints the list of processing states of the message unit with the given MessageId. As it is possible that there 
	 * exist multiple message units with the same id beside the status also the type of message unit and direction is
	 * printed.
	 * 
	 * @param messageId 
	 */
	private static void getMsgStatusList(String messageId) {
		MessageUnit[] msgUnits = null;
		try {
			msgUnits = coreAPI.getMessageUnitInfo(messageId);
		} catch (RemoteException e) {
			System.err.println(
			"An error occurred while getting the status from the Holodeck B2B instance. See error details below:");
			e.printStackTrace(System.err);
			System.exit(-3);						
		}
		if (msgUnits == null || msgUnits.length == 0) {
			System.out.println("No message unit with messageId=" + messageId + " could be found!");
			return;			
		}
		
		if (msgUnits.length > 1) {
			System.out.println("More than one message unit with MessageId [" + messageId 
								+ "] were found. Listing all:");
			System.out.println();
		}
		
		for(MessageUnit m : msgUnits) {
			System.out.print("Processing states of "); 
			System.out.print(m.getDirection() == Direction.IN ? "received " : "outgoing ");
			System.out.println(MessageUnitUtils.getMessageUnitName(m) + ":");
			
			for(int i = m.getProcessingStates().size() - 1; i >= 0; i--) {
				IMessageUnitProcessingState s = m.getProcessingStates().get(i);			
				System.out.print(s.getStartTime() + " : " + s.getState().name());			
				final String desc =  s.getDescription();				
				System.out.println(!Utils.isNullOrEmpty(desc) ? (" (" + desc + ")") : "");
			}
		}		
	}	
	
	/**
	 * Shows an summary overview of message units processed by the Holodeck B2B instance. The overview can be limited
	 * to show only message units which processing started before a certain time stamp and to a maximum number of 
	 * message units to include. When no parameters are provided the 10 most recent message units are shown.
	 * 
	 * @param from	Time stamp up to which processing of the message unit should have been started to include it	 * 				
	 * @param max	Maximum number of message units to show 
	 */
	private static void getHistory(final String from, final String max) {
		Date upto = new Date();
		try {
			if (!Utils.isNullOrEmpty(from))
				upto = Date.from(LocalDateTime.parse(from).atZone(ZoneId.systemDefault()).toInstant());
		} catch (DateTimeParseException invaliddate) {
			System.err.println("Invalid time stamp specified as start of overview : " + from);
			System.exit(-1);
		}
		int maxMsgs = 10;
		try {
			if (!Utils.isNullOrEmpty(max))
				maxMsgs = Integer.parseInt(max);
		} catch (NumberFormatException invalidMax) {
			System.err.println("Invalid maximum specified : " + max);
			System.exit(-1);
		}
		
		MessageUnit[] msgUnits = null;
		try {
			msgUnits = coreAPI.getMessageUnitLog(upto, maxMsgs);
		} catch (RemoteException e) {
			System.err.println(
			"An error occurred while getting the message meta-data from the Holodeck B2B instance. See error details below:");
			e.printStackTrace(System.err);
			System.exit(-3);						
		}
		if (msgUnits == null || msgUnits.length == 0) {
			System.out.print("No message units found");
			if (!Utils.isNullOrEmpty(from))
				System.out.print(" before " + from);
			System.out.println("!");
			return;			
		}
		
		int mxMsgId = 9, mxRefTo = 12, mxPMode = 8, mxStatus = 13;
		for(MessageUnit m : msgUnits) {
			mxMsgId  = Math.max(mxMsgId , m.getMessageId().length());
			mxRefTo  = Math.max(mxRefTo , Utils.isNullOrEmpty(m.getRefToMessageId()) ? 0 : m.getRefToMessageId().length());
			mxPMode  = Math.max(mxPMode , Utils.isNullOrEmpty(m.getPModeId()) ? 0 : m.getPModeId().length());
			mxStatus = Math.max(mxStatus, m.getCurrentProcessingState().getState().name().length());
		}
		
		final String template = "| %-24s | %-" + mxStatus + "s | %1s | %1s | %-" + mxMsgId + "s | %-" + mxRefTo + "s | %-" + mxPMode + "s |";		
		final String line = String.format(template, "", "", "", "", "", "", "").replace(" ", "-").replace("|", "+");
		
		System.out.println(line);
		System.out.println(String.format(template, "Timestamp", "Current state", "T", "D", "MessageId", "RefMessageId", "PMode.id"));
		System.out.println(line);
		for(MessageUnit m : msgUnits) 
			System.out.println(String.format(template, Utils.toXMLDateTime(m.getTimestamp()), 
											 m.getCurrentProcessingState().getState().name(),
										     MessageUnitUtils.getMessageUnitName(m).charAt(0),
										     m.getDirection() == Direction.IN ? "R" : "S",
										     m.getMessageId(), 
										     !Utils.isNullOrEmpty(m.getRefToMessageId()) ? m.getRefToMessageId() : "", 
								    		 !Utils.isNullOrEmpty(m.getPModeId()) ? m.getPModeId() : ""));
		System.out.println(line);
	}	
	
    /**
     * Gets the finger print of the certificate using the specified hash algorithm.
     * 
     * @param hashAlg The hash algorithm to calculate the finger print
     * @param cert	  The certificate
     * @return  	  Hex encoded finger print
     */
    private static String getCertFingerPrint(final String hashAlg, final X509Certificate cert) 
    {
    	try {
	        byte[] encCertInfo = cert.getEncoded();
	        MessageDigest md = MessageDigest.getInstance(hashAlg);
	        byte[] digest = md.digest(encCertInfo);
	        return Hex.encodeHexString(digest);	
    	} catch (Exception e) {
    		e.printStackTrace();
    		return "N/A";
    	}
    }	
}

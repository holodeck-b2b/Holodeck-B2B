/*
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
package org.holodeckb2b.ui.app.gui;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ui.api.CertType;
import org.holodeckb2b.ui.api.CoreInfo;
import org.holodeckb2b.ui.app.gui.models.CertificatesData;
import org.holodeckb2b.ui.app.gui.models.MessageHistoryData;
import org.holodeckb2b.ui.app.gui.models.MessageUnitStatesData;
import org.holodeckb2b.ui.app.gui.models.PModesData;
import org.holodeckb2b.ui.app.gui.views.MainWindow;
import org.holodeckb2b.ui.app.gui.views.MessageStatusPanel;
import org.holodeckb2b.ui.app.gui.views.SplashScreen;

/**
 * Is the main component of the GUI application for monitoring a Holodeck B2B instance. It acts as the controller 
 * getting all the data from the instance and creating the windows.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class HB2BMonitoringApp {
	/**
	 * The API to access the information in the Holodeck B2B instance
	 */
	private static CoreInfo		coreAPI;
	/**
	 * The main window, needed to show error dialogs
	 */
	private static MainWindow	mainWindow;
	/**
	 * The host name used by this Holodeck B2B instance
	 */
	private String 		hb2bHostName;	
	/**
	 * The message unit history data to show
	 */
	private MessageHistoryData	messageHistory;	
	/**
	 * The meta-data for displaying the processing states of a message unit
	 */
	private MessageUnitStatesData  msgUnitStatus;
	/**
	 * The P-Mode data 
	 */
	private PModesData pmodes;
	/**
	 * The three sets of certificates
	 */
	private Map<CertType, CertificatesData>	certificates; 
	
	/**
	 * Creates a new instance that acts as controller for the application.
	 * 
	 * @param hostName	The host name of the connected HB2B instance 
	 */
	public HB2BMonitoringApp(final String hostName) {
		hb2bHostName = hostName;
		messageHistory = new MessageHistoryData();
		msgUnitStatus = new MessageUnitStatesData();
		pmodes = new PModesData(hostName);
		certificates = new HashMap<>();
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
				
		int port = CoreInfo.DEFAULT_PORT;
		if (args.length > 0) {
			// The RMI port where the HB2B instance listens may be specified as argument
			try {
				port = Integer.parseInt(args[0]);
			} catch (Exception useDefault) {
				port = CoreInfo.DEFAULT_PORT;
			}
		}
		SplashScreen splash = new SplashScreen(port);		
		try {			
			String hb2bHostName;
			try {
				coreAPI = (CoreInfo) LocateRegistry.getRegistry(port).lookup(CoreInfo.RMI_SVC_NAME);
				hb2bHostName = coreAPI.getHostName();
			} catch (RemoteException | NotBoundException e) {
				throw new Exception("Could not connect to Holodeck B2B on port " + port, e);
			}            
			
			// Initialise the controller
			HB2BMonitoringApp controller = new HB2BMonitoringApp(hb2bHostName);			
			
			// Get the last 10 messages from the gateway
			splash.updateStatus("Retrieving message list...");
			try {
				controller.messageHistory.setMessageUnits(coreAPI.getMessageUnitLog(new Date(), 10));
			} catch (RemoteException e) {
				throw new Exception("Could not retrieve the message history from Holodeck B2B", e);
			}
		
			splash.updateStatus("Retrieving P-Modes...");
			try {
				controller.pmodes.setPModes(coreAPI.getPModes());
			} catch (RemoteException e) {
				throw new Exception("Could not retrieve the P-Modes from Holodeck B2B", e);
			}
	
			splash.updateStatus("Retrieving certificates...");	
			boolean certsAvailable = true;
			try {		
				controller.certificates.put(CertType.Private, 
													new CertificatesData(coreAPI.getCertificates(CertType.Private)));
				controller.certificates.put(CertType.Partner, 
													new CertificatesData(coreAPI.getCertificates(CertType.Partner)));
				controller.certificates.put(CertType.Trusted, 
													new CertificatesData(coreAPI.getCertificates(CertType.Trusted)));
			} catch (RemoteException e) {
				// Probably another Certificate Manager implementation is used, therefore disable Certficates tab
				certsAvailable = false;
			}		

			// Done with initialisation, close splash, show main window
			splash.close();		
			mainWindow = new MainWindow(controller, certsAvailable);
			mainWindow.setVisible(true);
		} catch (Throwable t) {
			// Something went wrong, just show error message and exit
			t.printStackTrace(System.err);
			JOptionPane.showMessageDialog(mainWindow != null ? mainWindow : splash, 
										  "An error occurred during processing:\n" + t.getMessage(), 
										  "Error", JOptionPane.ERROR_MESSAGE);			
			if (mainWindow != null && mainWindow.isVisible()) {
				mainWindow.setVisible(false);
				mainWindow.dispose();
			}
		} finally {
			splash.dispose();
		}
	}
	
	/**
	 * Gets the Holodeck B2B instance that the app is connected to.
	 * 
	 * @return	Host name of the Holodeck B2B instance
	 */
	public String getMonitoredInstance() {
		return hb2bHostName;
	}	

	/**
	 * Gets the meta-data of the message units for displaying the message history.
	 * 
	 * @return	The data model
	 */
	public MessageHistoryData getMessageHistoryData() {		
		return messageHistory;
	}
	
	/**
	 * Gets the meta-data of the message units for displaying the message status.
	 * 
	 * @return	The data model
	 */
	public MessageUnitStatesData getMessageUnitStatus() {
		return msgUnitStatus;
	}	

	/**
	 * Gets the data of the P-Modes for displaying.
	 * 
	 * @return	The data model
	 */
	public PModesData getPModes() {
		return pmodes;
	}	
	
	/**
	 * Gets the data of the certificates of the specified type to display them.
	 * 
	 * @param type	The certificate type
	 * @return		The data model
	 */
	public CertificatesData getCertificates(final CertType type) {
		return !Utils.isNullOrEmpty(certificates) ? certificates.get(type) : null; 
	}
	
	/**
	 * Retrieves a list of the given maximum number of message units starting from the given time stamp back-ward.
	 * 
	 * @param from	The time stamp to use a starting point
	 * @param max  	Maximum number of message units to retrieve
	 */
	public void retrieveMessageUnitHistory(final Date from, final int max) {
		try {
			messageHistory.setMessageUnits(coreAPI.getMessageUnitLog(from, max));
		} catch (RemoteException e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(mainWindow, 
										  "An error occurred while retrieving the message history:\n" + e.getMessage(), 
										  "Error", JOptionPane.ERROR_MESSAGE);				
			msgUnitStatus.setMessageUnits(null);
		}
	}	
	
	/**
	 * Retrieves the list of processing states for all message units with the given MessageId from the monitored 
	 * Holodeck B2B instance and fills the {@link #msgUnitStatus} data model with the result so the {@link 
	 * MessageStatusPanel} gets updated.
	 * 
	 * @param messageId		MessageId of the message units to get processing states of
	 */
	public void retrieveProcessingStates(final String messageId) {
		try {
			msgUnitStatus.setMessageUnits(coreAPI.getMessageUnitInfo(messageId));
		} catch (RemoteException e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(mainWindow, 
										  "An error occurred while retrieving the message status:\n" + e.getMessage(), 
										  "Error", JOptionPane.ERROR_MESSAGE);				
			msgUnitStatus.setMessageUnits(null);
		}
	}	
}

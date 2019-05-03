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
import java.rmi.RemoteException;

import org.holodeckb2b.common.pmode.PMode;

/**
 * Defines the interface of the RMI server that this extension provides for the default User Interface to retrieve 
 * information from the gateway.
 * <p>NOTE: The default UI is intended for use with the default Holodeck B2B implementation and does not support 
 * custom functionality that may have been added to an instance using extensions.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface CoreInfo extends Remote {

	/**
	 * The name used for the RMI Server
	 */
	final String RMI_SVC_NAME = "HB2B_UI_API";
	
	/**
	 * Gets all currently loaded P-Modes. 
	 *  
	 * @return The collection of configured P-Modes
	 */
	public PMode[] getPModes() throws RemoteException;
	
	/**
	 * Gets the host name as used by Holodeck B2B when constructing identifiers.
	 * 
	 * @return	The host name 
	 */
	public String getHostName() throws RemoteException;
}

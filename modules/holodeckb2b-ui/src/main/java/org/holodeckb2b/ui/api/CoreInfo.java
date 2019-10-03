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
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;

import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Defines the interface of the RMI server that this extension provides for the default User Interface to retrieve 
 * information from the gateway.
 * <p>NOTE: The default UI is intended for use with the default Holodeck B2B implementation and does not support 
 * custom functionality that may have been added to an instance using extensions.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public interface CoreInfo extends Remote {

	/**
	 * The name used for the RMI Server
	 */
	String RMI_SVC_NAME = "HB2B_UI_API";

	/**
	 * The default port number used for the RMI server
	 */
	int	DEFAULT_PORT = 1701;
	
	/**
	 * Gets the host name as used by this Holodeck B2B instance. It is used when constructing identifiers and intended
	 * for identifying the gateway externally. Can therefore be useful for display purposes as well.
	 * 
	 * @return	The host name 
	 */
	String getHostName() throws RemoteException;

	/**
	 * Gets all currently loaded P-Modes. 
	 *  
	 * @return The collection of configured P-Modes
	 */
	PMode[] getPModes() throws RemoteException;

	/**
	 * Gets all the certificates of the given type together with the alias they are registered under. 
	 * 
	 * @param type		The type of certificate
	 * @return			The certificates of the specified type registered in the Holodeck B2B instance, or<br>
	 * 					<code>null</code> if none are registered
	 * @throws RemoteException When an error occurs while retrieving the certificate from the Holodeck B2B instance
	 */
	Map<String, X509Certificate> getCertificates(final CertType type) throws RemoteException;
	
	/**
     * Gets the meta-data of the message unit with the given <i>MessageId</i>. Although the ebMS Specification requires 
     * the <i>MessageId</i> to be universally unique the result of this method is an array of {@link IMessageUnit} 
     * objects as message units can resend and therefore exist multiple times in the message database. Another reason
     * multiple occurrences may exist is that uniqueness is not enforced.
     *
  	 * @param messageId			The <i>MessageId</i> of the message unit to get information about
	 * @return					Array of message unit meta-data objects for message units with given id. Empty if no
	 * 							message units with the give id are found
	 * @throws RemoteException	When an error occurs in retrieving the meta-data of the requested message unit
	 */
	MessageUnit[] getMessageUnitInfo(final String messageId) throws RemoteException;
	
	/**
     * Gets the meta-data of message units which processing started before and up to the given time stamp and limited to 
     * the indicated maximum of message units. The result is ordered descendingly on the start of processing time stamp.
     * 
     * @param upto		Most recent time stamp to include
     * @param max		Maximum number of results
     * @return			Array of message units which processing started before the given time stamp, limited to the 
     * 					given maximum number of entries 
	 * @throws RemoteException When an error occurs in retrieving the meta-data of the message units
	 */
	MessageUnit[] getMessageUnitLog(final Date upto, final int max) throws RemoteException;
}

/*
 * Copyright (C) 2023 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.persistency.entities;

import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;

/**
 * Defines the interface of the persistent entity object that is used by the Holodeck B2B to store the payload 
 * meta-data.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 * @see IUserMessageEntity
 * @see IUpdateManager#updatePayload(IPayloadEntity)
 */
public interface IPayloadEntity extends IPayload {
	
	/**
	 * Sets the path where the payload content is saved.
	 * 
	 * @param path	to the file in which the payload content is saved
	 */
	void setContentLocation(String path);
		
	/**
	 * Sets the Mime type of the payload.
	 * 
	 * @param mt	the Mime type of the payload
	 */
	void setMimeType(String mt);
	
	/**
	 * Sets the reference to the payload content in the message.
	 * <p>Corresponds to the <code>href</code> attribute of the <code>//eb:UserMessage/eb:IPayloadInfo/eb:PartInfo</code>
	 * element in the ebMS header of the message.
	 * 
	 * @param uri 	the URI that references the payload content within the context of the message
	 */ 
	void setPayloadURI(String uri);
	
	/**
	 * Adds the given <i>Part Property</i> to the payload meta-data.
	 * 
	 * @param p	property to add as part property  
	 */
	void addProperty(IProperty p);

	/**
	 * Removes the given property from the set of <i>Part Properties</i> of this payload.
	 *   
	 * @param p	property to remove as part property
	 */
	void removeProperty(IProperty p);	
}

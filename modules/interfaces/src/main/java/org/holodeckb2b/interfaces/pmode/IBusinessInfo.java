/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.pmode;

import java.util.Collection;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.IService;

/**
 * Describes the P-Mode parameters that contain the meta-data about the <i>business transaction</i> the message exchange
 * is part of. This meta-data can be used by the business application to determine how the message should be processed.
 * This information is also used by Holodeck B2B to determine which P-Mode defines the processing of a received message,
 * especially if they do not contain the P-Mode id.
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IBusinessInfo {
 
    /**
     * Gets the business level operation/activity requested to be executed.
     * 
     * @return The name of the business level operation to handle the message.
     */
    public String getAction();
    
    /**
     * Gets the message partition channel the user message to sent or pull is assigned to. As described in the ebMS Core 
     * Specification it is not required to specify the MPC. If no MPC is given in either the P-Mode or when the message
     * is submitted it assumed to be assigned to the default MPC. 
     * 
     * @return The URI identifying the MPC that the message to sent or pull is assigned to.
     */
    public String getMpc();
    
    /**
     * Gets the business service that is [supposed] to handle the user message.
     * 
     * @return An {@link IService} object containing the meta-data on the service
     */
    public IService getService();
    
    /**
     * Gets the set of additional meta-data properties required to handle the message.
     * <p>Note that properties defined in the P-Mode will be included in all user messages. If properties depend on the 
     * actual content of the message they should be supplied when the message is submitted. To enable specifying both 
     * generic and specific properties Holodeck B2B will combine the property sets from P-Mode and submission when 
     * processing a message. If both sets contain a property with the same name, the value given at message submission 
     * will be used.
     * 
     * @return The set of user defined meta-data properties
     */
    public Collection<IProperty> getProperties();    
}

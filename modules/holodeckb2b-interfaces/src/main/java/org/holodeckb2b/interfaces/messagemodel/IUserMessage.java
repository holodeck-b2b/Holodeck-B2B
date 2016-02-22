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
package org.holodeckb2b.interfaces.messagemodel;


import java.util.Collection;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;

/**
 * Represents the ebMS user message type of message unit and defines the interface to get access to the meta-data and
 * content of the user message. This interface follows the message model as defined in section 5.2.2 of the ebMS Core 
 * Specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IMessageUnit
 */
public interface IUserMessage extends IMessageUnit {
   
    /**
     * Gets the MPC this message is submitted to. 
     * <p>Corresponds to <code>//eb:UserMessage/@mpc</code> in the ebMS messaging header. See section 5.2.2 of the ebMS 
     * Core specification. 
     * <p><b>NOTE:</b> Every user message will be exchanged over a MPC. If no specific MPC is defined for a user message
     * the default MPC with value <i>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC</i> is 
     * used. So this function SHOULD always return a value.
     * 
     * @return The MPC of this message.  
     */
    public String getMPC();
    
    /**
     * Gets the trading partner that is the sender of this message.
     * <p>Corresponds to the <code>//eb:UserMessage/eb:PartyInfo/eb:From</code> element in the ebMS messaging header. 
     * See section 5.2.2.3 of the ebMS Core Specification.
     * 
     * @return An {@link ITradingPartner} object containing the information on the sender of the message 
     */
    public ITradingPartner getSender();
    
    /**
     * Gets the trading partner that is the [intended] receiver of this message.
     * <p>Corresponds to the <code>//eb:UserMessage/eb:PartyInfo/eb:To</code> element in the ebMS messaging header. See 
     * section 5.2.2.5 of the ebMS Core Specification.
     * 
     * @return An {@link ITradingPartner} object containing the information on the sender of the message 
     */
    public ITradingPartner getReceiver();
    
    /**
     * Gets the collaboration information. This set of information is intended for determining how messages should be 
     * processed, both by Holodeck B2B as well as the business applications.
     * <p>Corresponds to the <code>//eb:UserMessage/eb:CollaborationInfo</code> element and its children in the ebMS 
     * messaging header. See section 5.2.2.6 of the ebMS Core Specification.
     * 
     * @return An {@link ICollaborationInfo} object containing the collaboration information
     */
    public ICollaborationInfo getCollaborationInfo();
    
    /**
     * Gets the user specified message properties. These properties are intended for use by the business applications 
     * involved in the message exchange. Holodeck B2B will ignore these properties.
     * <p>Corresponds to the <code>//eb:UserMessage/eb:MessageProperties</code> and <code>eb:Property</code> child 
     * elements. See section 5.2.2.11 of the ebMS Core Specification.
     * 
     * @return  The collection of properties specified in the message header.
     */
    public Collection<IProperty> getMessageProperties();
    
    /**
     * Gets the information about the payloads of the user message. The payloads are the actual business documents being 
     * exchanged. This method returns the information <b>about</b> the payloads, not their actual content. Using the
     * returned {@link IPayload} objects the actual content can be accessed.
     * <p>Corresponds to the <code>//eb:UserMessage/eb:PayloadInfo</code> and <code>eb:PartInfo</code> child elements
     * and with information to retrieve the actual content of the payload. See sections 5.2.2.12 and 5.2.2.13 of the
     * ebMS Core Specification.
     * 
     * @return  A collection of {@link IPayload} objects containing the information on the payloads contained in the
     *          user message.<br>
     *          <b>NOTE: </b> A user message is not required to contain payloads, so the returned collection can be 
     *          empty!
     */
    public Collection<IPayload>  getPayloads();
}

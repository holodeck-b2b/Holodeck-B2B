/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.interfaces.messagemodel;


import org.holodeckb2b.interfaces.general.IService;

/**
 * Represents the collaboration info available for an (or to be) exchanged message. This set of information is intended 
 * for determining how messages should be processed, both by Holodeck B2B as well as by the business applications. Based
 * on this information Holodeck B2B will determine which P-Mode defines the message processing.
 * <p>The information accessed through this interface corresponds with the <code>//eb:UserMessage/eb:CollaborationInfo</code>
 * and child elements. See sections 5.2.2.6 to 5.2.2.10 of the ebMS Core Specification for more information.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface ICollaborationInfo {
    
    /**
     * Gets the information about the <i>business</i> service that should handle the message.
     * <p>Corresponds to the <code>eb:Service</code> child element of <code>eb:CollaborationInfo</code>
     * 
     * @return An {@link IService} object containing meta-data on the service
     */
    public IService  getService();
    
    /**
     * Gets the action that requested to be executed by the service.
     * <p>Corresponds to the <code>eb:Action</code> child element of <code>eb:CollaborationInfo</code>
     * 
     * @return  The [name of] action to execute
     */
    public String getAction();
    
    /**
     * Gets the identification of the conversation the message unit is part of. A conversation can consist of multiple 
     * message exchanges between business applications. The conversation id can be used to group the messages.
     * Holodeck B2B does not use this information during message processing, but as it is a required element it will
     * generate a conversation id when none is supplied by the business application during submission of the message
     * unit.
     * <p>Corresponds to the <code>eb:ConversationId</code> child element of <code>eb:CollaborationInfo</code>
     * 
     * @return  The conversation id of the conversation the message unit belongs to.
     */
    public String getConversationId();
    
    /**
     * Gets the information about the agreement under which the message exchange takes place. Basicly this refers to the
     * agreement between the <i>trading partners</i>. Part of the information can however be the id of the P-Mode that 
     * defines how the message unit should be processed. 
     * <p>Corresponds to the <code>eb:AgreementRef</code> child element of <code>eb:CollaborationInfo</code>
     * <p><b>NOTE: </b>The set of information does only include meta-data on the agreement, not the actual agreement.  
     * 
     * @return An {@link IAgreementReference} object that contains the information on the agreement.
     */
    public IAgreementReference getAgreement();
}

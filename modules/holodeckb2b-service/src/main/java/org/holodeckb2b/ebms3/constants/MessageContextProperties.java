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
package org.holodeckb2b.ebms3.constants;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;

/**
 * Defines constants for {@link MessageContext} properties that Holodeck B2B 
 * uses to store information about the processed ebMS message.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageContextProperties {
    
    /**
     * Holds the <i>primary</i> outgoing user message. The P-Mode of this 
     * message unit determines the configuration that is used for sending the
     * complete message.
     */
    public static final String OUT_USER_MESSAGE = "org:holodeckb2b:out-user-msg";

    /**
     * Holds the received user message. 
     */
    public static final String IN_USER_MESSAGE = "org:holodeckb2b:in-user-msg";

    /**
     * Holds information on the received pull request as {@link PullRequest}
     */
    public static final String IN_PULL_REQUEST = "org:holodeckb2b:in-pullrequest";
    
    /**
     * Holds the meta-data on the received Receipts 
     */
    public static final String IN_RECEIPTS = "org:holodeckb2b:in-receipts";

    /**
     * Holds the meta-data on the received Errors 
     */
    public static final String IN_ERRORS = "org:holodeckb2b:in-errors";
    
    /**
     * Holds the list of PModes a received PullRequest is authorized for
     */
    public static final String PULL_AUTH_PMODES = "org:holodeckb2b:pull-from-pmodes";
   
    /**
     * Holds the signal to the Holodeck B2B message receiver that a direct 
     * response should be sent back
     */
    public static final String RESPONSE_REQUIRED = "org:holodeckb2b:response-req";
    
    /**
     * Holds an array of {@link org.holodeckb2b.ebms3.persistency.entities.EbmsError} 
     * objects that contain information on errors that occurred during message 
     * processing
     */
    public static final String GENERATED_ERRORS = "org:holodeckb2b:gen-errors";
    
    /**
     * Holds an array of {@link org.holodeckb2b.ebms3.persistency.entities.ErrorMessage} 
     * objects that contain error signals that should be sent.
     */
    public static final String OUT_ERROR_SIGNALS = "org:holodeckb2b:send-errors";

    /**
     * Holds the Receipt signal data of the receipt that must be sent as response.
     *
     * @see  Receipt
     */
    public static final String RESPONSE_RECEIPT = "org:holodeckb2b:resp-receipt";

    /**
     * Holds the Receipt signal data of the receipts that must be sent.
     *
     * @see  Receipt
     */
    public static final String OUT_RECEIPTS = "org:holodeckb2b:out-receipts";
        
    /**
     * Holds the PullRequest signal data that must be sent.
     *
     * @see  PullRequest
     */
    public static final String OUT_PULL_REQUEST = "org:holodeckb2b:out-pullrequest";
    
    /**
     * Holds the indicator whether the received user message message unit was successfully delivered 
     * to the business application, i.e. the {@link IMessageDeliverer} did throw an exception.
     */
    public static final String DELIVERED_USER_MSG = "org:holodeckb2b:usrmsg-delivered";
}

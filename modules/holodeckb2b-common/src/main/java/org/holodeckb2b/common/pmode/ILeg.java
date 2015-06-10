/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.pmode;

import java.util.List;
import org.holodeckb2b.common.delivery.IDeliverySpecification;
import org.holodeckb2b.common.delivery.IMessageDeliverer;

/**
 * Describes the parameters of one leg in the message exchange. When a leg uses pulling for the exchange of the user
 * message there are two ebMS messages that can trigger responses: the pull request signal and the user message. These
 * [responses] both need their own configuration. Therefor the leg configuration is split into <i>flows</i> that 
 * configure the pull request and user message. The leg defines the common parameters like the protocol to use and 
 * if and how receipts should be used.
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface ILeg {
    
    /**
     * Enumeration of the allowed <i>"labels"</i> of a leg. 
     */
    public enum Label  { REQUEST, RESPONSE };
    
    /**
     * Gets the <i>label</i> of this leg. Used in a Two-Way MEP to identify whether leg contains the <i>request</i> or
     * </i>response</i> user message.
     * 
     * @return The {@link Label} of this leg. Can be <code>null</code> if this leg is part of a One-Way MEP.
     */
    public Label getLabel();
    
    /**
     * Gets the configuration data for the transport protocol used to exchange the messages of this leg.
     * 
     * @return An {@link IProtocol} object containing the configuration for the transport protocol to use.
     */
    public IProtocol getProtocol();
    
    /**
     * Gets the general configuration for the use of <i>Receipt signals</i> to confirm that the user message was 
     * received. The use of receipts is optional so this method should only return configuration data when receipt
     * must be used. 
     * 
     * @return An {@link IReceiptConfiguration} object containing the configuration for the receipt, or<br>
     *         <code>null</code> when no receipt should be used
     */
    public IReceiptConfiguration getReceiptConfiguration();
    
    /**
     * Gets the default <i>delivery specification</i> for this leg which defines how message units should be delivered
     * to the connected business application. Holodeck B2B itself does not deliver the message, the actual delivery is 
     * done through {@link IMessageDeliverer}s. The delivery specification tells Holodeck B2B how to construct the 
     * correct deliverer for messages exchanged on this leg. 
     * <p>If configured not only user message message units will be delivered but also Receipt and Error signal message
     * units, see {@link IReceiptConfiguration} and {@link IErrorHandling}. The signals can each use a specific delivery
     * specification but will use this delivery as the default.
     * 
     * @return An {@link IDeliverySpecification} object containing the configuration of the message delivery. MUST NOT
     *         be <code>null</code> for the receiving side. Can be <code>null</code> on the sending side when errors
     *         and receipts responses do not need to be delivered to the business application or use their own 
     *         delivery specification.
     */
    public IDeliverySpecification getDefaultDelivery();
    
    /**
     * Returns the configuration of the pull request processing. The configuration for the pull request is limited to 
     * the MPC which is pulled, security and error handling. Only applicable when the leg is bound to pulling, 
     * implementations SHOULD return <code>null</code> if pulling is not used for this leg.
     * <p>To enable sub-channel MPC functionality as described in part 2 of the ebMS V3 Specification and section 3.5 of 
     * the AS4 profile a leg can contain multiple <i>pull request flows</i>. As required by the specification the MPC
     * specified in the pull request flows MUST start with MPC used in the user message, i.e. the one defined in the
     * <i>user message flow</i> or if not specified in the P-Mode the MPC provided when the user message is submitted.
     * <p><b>NOTE: </b>When this P-Mode configures an Holodeck B2B instance that sends the PullRequest the list
     * MUST include at most 1 flow. Otherwise Holodeck B2B can not determine for which MPC the pull request should be
     * sent.
     * 
     * @return A list of  {@link IFlow} objects containing the configuration of the pull request, one for each 
     *         sub-channel MPC,<br>
     *         or <code>null</code> when pulling is not used.
     */
    public List<IPullRequestFlow> getPullRequestFlows();
    
    /**
     * Returns the configuration of the user message processing. As target of the leg is to exchange a user message this
     * flow is alway available. 
     * 
     * @return An {@link IFlow} object containing the configuration of the user message
     */
    public IUserMessageFlow getUserMessageFlow();
}

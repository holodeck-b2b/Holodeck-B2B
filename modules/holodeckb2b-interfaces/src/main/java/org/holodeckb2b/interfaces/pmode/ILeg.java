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
import java.util.List;

import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;

/**
 * Represents the P-Mode parameters of one leg, i.e. the exchange of a user message, in a message exchange.
 * <p>When a leg uses pulling for the exchange of the user message there are two ebMS messages that can trigger
 * responses: the pull request signal and the user message. These [responses] both need their own configuration.
 * Therefore the leg configuration is split into <i>flows</i> that configure the pull request and user message.<br>
 * The leg defines the common and default parameters like the protocol to use and if and how receipts should be used.
 *
 * @author Bram Bakx (bram at holodeck-b2b.org)
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ILeg {

    /**
     * Enumeration of the allowed <i>"labels"</i> of a leg.
     */
    public enum Label  {
    	/** Indicates the "request" leg, **/
    	REQUEST,
    	/** Indicates the "reply" leg, **/
    	REPLY
    }

    /**
     * Gets the <i>label</i> of this leg.
     * <p>Used in a Two-Way MEP to identify whether leg contains the <i>request</i> or <i>reply</i> user message.
     *
     * @return The {@link Label} of this leg. Can be <code>null</code> if this leg is part of a One-Way MEP.
     */
    public Label getLabel();

    /**
     * Gets the configuration data for the transport protocol used to exchange the messages of this leg.
     *
     * @return  An {@link IProtocol} object containing the configuration for the transport protocol to use. MUST NOT be
     *          <code>null</code> if Holodeck B2B sends the first message of the leg.
     */
    public IProtocol getProtocol();

    /**
     * Gets the general configuration for the use of <i>Receipt signals</i> to confirm that the user message was
     * received. The use of receipts is optional so this method should only return configuration data when receipts
     * must be used.
     * <p>Note that in appendix D of the ebMS V3 Core Specification the P-Mode parameters related to receipts are part
     * of the security parameter group. Because receipts can be used independent of the security features like signing
     * and encryption the configuration is split.
     *
     * @return An {@link IReceiptConfiguration} object containing the configuration for the receipt, or<br>
     *         <code>null</code> when no receipt should be used
     */
    public IReceiptConfiguration getReceiptConfiguration();

    /**
     * Gets the default <i>delivery specification</i> for this leg.
     * <p>This default delivery specification is not only used for the delivery of user message message units but also
     * for received Receipt and Error signal message units if they do not have their own delivery specification, see
     * {@link IReceiptConfiguration} and {@link IErrorHandling}.
     *
     * @return An {@link IDeliverySpecification} object containing the configuration of the message delivery. MUST NOT
     *         be <code>null</code> for the receiving side. Can be <code>null</code> on the sending side when errors
     *         and receipts responses do not need to be delivered to the business application or use their own
     *         delivery specification
     * @see    org.holodeckb2b.interfaces.delivery
     */
    public IDeliverySpecification getDefaultDelivery();

    /**
     * Returns the configuration of the pull request processing.
     * <p>The configuration for the pull request is limited to the MPC which is pulled, security and error handling.
     * <p>To enable sub-channel MPC functionality as described in part 2 of the ebMS V3 Specification and section 3.5 of
     * the AS4 profile a leg can contain multiple <i>pull request flows</i>, each representing a specific sub-channel
     * MPC.
     * <p>As required by the specification the MPC specified in the pull request flows MUST start with MPC used in the
     * user message, i.e. the one defined in the <i>user message flow</i> or if not specified in the P-Mode the MPC
     * provided when the user message is submitted.
     * <p><b>NOTE: </b>When this P-Mode configures an Holodeck B2B instance that sends the <code>PullRequest</code> the
     * collection MUST include at most 1 flow. Otherwise Holodeck B2B can not determine for which MPC the pull request
     * should be sent.
     *
     * @return  A collection of {@link IPullRequestFlow} objects containing the configuration for the the pull request,
     *          one for each sub-channel MPC, or <br>
     *          <code>null</code> when pulling is not used.
     */
    public Collection<IPullRequestFlow> getPullRequestFlows();

    /**
     * Returns the configuration of the user message processing.
     * <p>As target of the leg is to exchange a user message this flow should be available.
     *
     * @return An {@link IUserMessageFlow} object containing the configuration of the user message
     */
    public IUserMessageFlow getUserMessageFlow();

    /**
     * Returns the configuration for handling <i>"events"</i> that occur during the processing of message units on this
     * leg. These <i>message processing events</i> are used to provide additional information to the business
     * application about the processing of a message unit in addition to the formally specified <i>Submit</i>,
     * <i>Deliver</i> and <i>Notify</i> operations. An example of an event is that a message unit has been (re)sent.
     *
     * @return A {@link List} of {@link IMessageProcessingEventConfiguration}s that specify which event handlers should
     *         be used for events that occur while processing message units of this leg.
     * @see IMessageProcessingEvent
     * @see org.holodeckb2b.interfaces.eventprocessing
     * @since 2.1.0
     */
    public List<IMessageProcessingEventConfiguration> getMessageProcessingEventConfiguration();

    /**
     * Gets the P-Mode parameters for the reception awareness feature.
     *
     * @return  An {@link IReceptionAwareness} object containing the reception awareness parameters, or<br>
     *          <code>null</code> if reception awareness is not used on this leg.
     * @since 5.0.0
     */
    public IReceptionAwareness getReceptionAwareness();
}

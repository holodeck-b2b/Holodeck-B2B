/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.multihop;

import java.util.Collection;

import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.holodeckb2b.common.handler.AbstractBaseHandler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.pmode.PModeUtils;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for adding the necessary WS-Addressing headers to the message to sent it
 * using AS4 multi-hop.
 * <p>
 * AS4 Multi-hop is a profiled version of the ebMS V3 multi-hop feature defined in part 2 of the specifications. Where
 * the complete feature from part 2 has many parameters the AS4 multi-hop profile is very restricted and much simpler.
 * For details see section 4 of the AS4 Profile.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ConfigureMultihop extends AbstractBaseHandler {

    // Ensure that the I-Cloud URI that is set in the wsa:To header is interpreted as anonymous by the Axis2 addressing
    // module
    static {
        EndpointReference.addAnonymousEquivalentURI(MultiHopConstants.WSA_TO_ICLOUD);
    }

    /**
     * Checks whether the message is sent through the I-Cloud and adds necessary routing information if so. The routing
     * information is simple in case the primary message unit in the message is a User Message because this message unit
     * already contains all the info. For Receipt and Error signals the routing info is retrieved from the User Message
     * that the signal is a response to. See section 4.4 of the AS4 profile how the WS-A headers are constructed.
     */
    @Override
    protected InvocationResponse doProcessing(final MessageProcessingContext procCtx, final Log log) throws Exception {
        // For routing signals through the I-Cloud WS-A headers are used. We use the Axis2 addressing module to create
        // the headers. But we don't need these headers normally, so disable the module by default
        procCtx.getParentContext().setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);

    	// If the primary message unit is a user message it can be multi-hop in itself. Receipt and/or Error signals
        // depends on whether the original message was sent using multi-hop.
        final IMessageUnitEntity primMU = procCtx.getPrimaryMessageUnit();
        if (primMU instanceof IUserMessage) {
            // Whether the user message is sent using multi-hop is defined by P-Mode parameter
            // PMode[1].Protocol.AddActorOrRoleAttribute
        	final IProtocol prot = PModeUtils.getLeg(primMU).getProtocol();
            if (prot == null || !prot.shouldAddActorOrRoleAttribute())
                log.trace("Primary message is a non multi-hop UserMessage");
            else {
                // This is a multi-hop message, set the multi-hop target on the eb:Messaging element
                log.debug("Primary message is a multi-hop UserMessage -> set multi-hop target");
                final SOAPHeaderBlock ebHeader = Messaging.getElement(procCtx.getParentContext().getEnvelope());
                ebHeader.setRole(MultiHopConstants.NEXT_MSH_TARGET);
            }
        } else if (primMU instanceof IPullRequest) {
            // If the primary message unit is a PullRequest the message is not sent using multi-hop as this is not
            // supported in AS4 multi-hop
            log.trace("Primary message unit is a PullRequest -> no multi-hop");
        } else {
            // If the primary message unit is a Receipt or Error signal additional WS-A headers must be provided with
            // the necessary routing info. This info is retrieved from the UserMessage the signal is a response to.
            final IUserMessageEntity usrMessage = getReferencedUserMsg(procCtx, (ISignalMessage) primMU);

            // Check if the user message was received over multi-hop
            if (usrMessage != null && usrMessage.usesMultiHop()) {
                log.debug("Primary message unit is response signal to multi-hop User Message -> add routing info");
                addRoutingInfo(procCtx.getParentContext(), usrMessage, (ISignalMessage) primMU);
            } else
                log.trace("Primary message unit is response signal to non (multi-hop) User Message");
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Gets the UserMessage that is referenced by a signal message.
     * <p>If the signal is sent as a response the related User Message is retrieved from the in flow message context,
     * otherwise it is retrieved from the database. If multiple entity objects with the same <i>messageId</i> exist the
     * first one is used as it assumed that all entity object represent the same User Message (based on the ebMS V3
     * Specification's requirement that <i>messageId</i>s must be unique this is a safe assumption).
     *
     * @param procCtx   The Holodeck B2B message processing context
     * @param signal    The signal message unit to get the reference user message for
     * @return          The referenced {@link UserMessage} or <code>null</code> if no unique referenced user message
     *                  can be found
     */
    private IUserMessageEntity getReferencedUserMsg(final MessageProcessingContext procCtx, final ISignalMessage signal)
                                                                                        throws PersistenceException {
        String refToMsgId = MessageUnitUtils.getRefToMessageId(signal);
        // If the signal does not contain a reference to another message unit there is nothing to do here
        if (Utils.isNullOrEmpty(refToMsgId))
            return null;

        // If the signal is the primary message unit in a response it is sent synchronously and the related user
        // message must be available in the context 
        for(IMessageUnitEntity mu : procCtx.getReceivedMessageUnits())
            if (mu instanceof IUserMessage && refToMsgId.equals(mu.getMessageId()))
                return (IUserMessageEntity) mu;

        // If not sent as response, get the information from the database
        final Collection<IMessageUnitEntity> refdMessages = HolodeckB2BCore.getQueryManager()
                                                                         .getMessageUnitsWithId(refToMsgId, 
                                                                        		 				Direction.OUT);
        if (!Utils.isNullOrEmpty(refdMessages)) {
            IMessageUnitEntity sentMsgUnit = refdMessages.iterator().next();
            return (sentMsgUnit instanceof IUserMessageEntity) ? (IUserMessageEntity) sentMsgUnit : null;
        } 
        
        return null;        
    }

    /**
     * Adds the required routing information to the message by adding the <code>wsa:To</code>, <code>wsa:Action</code>
     * and <code>ebint:RoutingInput</code> EPR parameter WS-A headers to the message.
     * <p>NOTE: The actual elements are added to the SOAP envelop later by the WS-A module.
     *
     * @param mc            The current message context
     * @param usrMessage    The User Message the routing input has to be deferred from
     * @param signal        The Signal message for which the routing input must be added
     */
    private void addRoutingInfo(final MessageContext mc, final IUserMessage usrMessage, final ISignalMessage signal) {
        // wsa:To
        final EndpointReference toEpr = new EndpointReference(MultiHopConstants.WSA_TO_ICLOUD);

        /* ebint:RoutingInput EPR parameter
        *  The info is the same as contained in the referenced user message but with the To and From swapped and the
        *  MPC and Action extended.
        *  To create this parameter we first create a MessageMetaData object based on the UserMessage and then change
        *  the required fields
        */
        final UserMessage routingInputUsrMsg = new UserMessage(usrMessage);
        // Swap To and From
        routingInputUsrMsg.setReceiver(usrMessage.getSender());
        routingInputUsrMsg.setSender(usrMessage.getReceiver());
        // Extend Action
        routingInputUsrMsg.getCollaborationInfo().setAction(usrMessage.getCollaborationInfo().getAction()
                                                            + MultiHopConstants.GENERAL_RESP_SUFFIX);
        // Extend MPC
        if (signal instanceof IReceipt)
            routingInputUsrMsg.setMPC(usrMessage.getMPC() + MultiHopConstants.RECEIPT_SUFFIX);
        else
            routingInputUsrMsg.setMPC(usrMessage.getMPC() + MultiHopConstants.ERROR_SUFFIX);

        // Create the ebint:RoutingInput element and add it as reference parameter to the To EPR
        toEpr.addReferenceParameter(RoutingInput.createElement(mc.getEnvelope(), routingInputUsrMsg));

        mc.setTo(toEpr);

        // wsa:Action header
        if (signal instanceof IReceipt)
            mc.getOptions().setAction(MultiHopConstants.ONE_WAY_ACTION + MultiHopConstants.RECEIPT_SUFFIX);
        else
            mc.getOptions().setAction(MultiHopConstants.ONE_WAY_ACTION + MultiHopConstants.ERROR_SUFFIX);

        // Set nextMSH as target for the WS-A headers
        mc.setProperty(AddressingConstants.SOAP_ROLE_FOR_ADDRESSING_HEADERS, MultiHopConstants.NEXT_MSH_TARGET);

        // Enable use of WS-A, but prevent optional WS-A headers to be added
        mc.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.FALSE);
        mc.setProperty(AddressingConstants.INCLUDE_OPTIONAL_HEADERS, Boolean.FALSE);
    }
}

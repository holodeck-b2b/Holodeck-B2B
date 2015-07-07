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
import java.util.List;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.IEbmsError;
import org.holodeckb2b.common.messagemodel.IReceipt;
import org.holodeckb2b.common.pmode.IProtocol;
import org.holodeckb2b.ebms3.mmd.xml.CollaborationInfo;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.persistent.message.SignalMessage;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.MessageContextUtils;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for adding the necessary WS-Addressing headers to the message to sent it
 * using AS4 multi-hop.
 * <p>
 * AS4 Multi-hop is a profiled version of the ebMS V3 multi-hop feature defined in part 2 of the specifications. Where 
 * the complete feature from part 2 has many parameters the AS4 multi-hop profile is very restricted and much simpler.
 * For details see section 4 of the AS4 Profile.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ConfigureMultihop extends BaseHandler {

    // Ensure that the I-Cloud URI that is set in the wsa:To header is interpreted as anonymous by the Axis2 addressing
    // module
    static {
        EndpointReference.addAnonymousEquivalentURI(MultiHopConstants.WSA_TO_ICLOUD);    
    }
    
    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW;
    }

    /**
     * Checks whether the message is sent through the I-Cloud and adds necessary routing information if so. The routing
     * information is simple in case the primary message unit in the message is a User Message because this message unit
     * already contains all the info. For Receipt and Error signals the routing info is retrieved from the User Message
     * that the signal is a response to. See section 4.4 of the AS4 profile how the WS-A headers are constructed.
     */
    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {
        // For routing signals through the I-Cloud WS-A headers are used. We use the Axis2 addressing module to create
        // the headers. But this only needed for signals, so we disable the module by default
        mc.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
        
        // If the primary message unit is a user message it can be multi-hop in itself. Receipt and/or Error signals
        // depends on whether the original message was sent using multi-hop.
        MessageUnit primMU = MessageContextUtils.getPrimaryMessageUnit(mc);
        if (primMU instanceof UserMessage) {
            // Whether the user message is sent using multi-hop is defined by P-Mode parameter 
            // PMode[1].Protocol.AddActorOrRoleAttribute
            IProtocol prot = HolodeckB2BCore.getPModeSet().get(primMU.getPMode()).getLegs().iterator().next().getProtocol();            
            if (prot == null || !prot.shouldAddActorOrRoleAttribute()) 
                log.debug("Primary message is a non multi-hop UserMessage");
            else {
                // This is a multi-hop message, set the multi-hop target on the eb:Messaging element
                log.debug("Primary message is a multi-hop UserMessage -> set multi-hop target");
                SOAPHeaderBlock ebHeader = Messaging.getElement(mc.getEnvelope());
                ebHeader.setRole(MultiHopConstants.NEXT_MSH_TARGET);
            }
        } else if (primMU instanceof PullRequest) {
            // If the primary message unit is a PullRequest the message is not sent using multi-hop as this is not
            // supported in AS4 multi-hop
            log.debug("Primary message unit is a PullRequest -> no multi-hop");
        } else {
            // If the primary message unit is a Receipt or Error signal additional WS-A headers must be provided with
            // the necessary routing info. This info is retrieved from the UserMessage the signal is a response to.
            UserMessage usrMessage = getReferencedUserMsg((SignalMessage) primMU);
            
            // Check if the user message was received over multi-hop
            if (usrMessage != null && usrMessage.usesMultiHop()) {
                log.debug("Primary message unit is response signal to multi-hop User Message -> add routing info");
                addRoutingInfo(mc, usrMessage, (SignalMessage) primMU);
            } else
                log.debug("Primary message unit is response signal to non multi-hop User Message");            
        }
        
        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Gets the UserMessage that is referenced by a signal message. 
     * <p>The reference will be retrieved from the signal message unit itself or when in case of an Error signal that
     * does not directly reference a message the first error contained in the signal.
     * 
     * @param signal    The signal message unit to get the reference user message for
     * @return          The referenced {@link UserMessage} or <code>null</code> if no unique referenced user message
     *                  can be found
     */
    private UserMessage getReferencedUserMsg(SignalMessage signal) {
        UserMessage refdUM = null;
        String refToMsgId = signal.getRefToMessageId();
        
        if ((refToMsgId == null || refToMsgId.isEmpty()) && signal instanceof ErrorMessage) {
            // For errors the reference can also be included in the Error element
            Collection<IEbmsError> errors = ((ErrorMessage) signal).getErrors();
            refToMsgId = errors.isEmpty() ? null : errors.iterator().next().getRefToMessageInError();
        }
        
        if (refToMsgId != null && !refToMsgId.isEmpty()) {
            List<MessageUnit> refdMessages = null;
            try {
                refdMessages = MessageUnitDAO.getReceivedMessageUnitsWithId(refToMsgId);
                if (refdMessages != null && refdMessages.size() >= 1) {
                    // Signal refers to one other message unit, check that it is a User Message
                    MessageUnit refdMU = refdMessages.get(0);
                    if (refdMU instanceof UserMessage)
                        // Load the object completely
                        refdUM = (UserMessage) MessageUnitDAO.loadCompletely(refdMU);
                    else
                        log.debug("Referenced message unit is not a UserMessage!");
                } else
                   log.debug("Signal message refers to no or multiple message units in database");
            } catch (DatabaseException dbe) {};           
        } else 
            log.debug("Signal message did not reference another message!");
            
        return refdUM;
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
    private void addRoutingInfo(MessageContext mc, UserMessage usrMessage, SignalMessage signal) {
    
        // wsa:To
        EndpointReference toEpr = new EndpointReference(MultiHopConstants.WSA_TO_ICLOUD);        
        
        /* ebint:RoutingInput EPR parameter
        *  The info is the same as contained in the referenced user message but with the To and From swapped and the
        *  MPC and Action extended. 
        *  To create this parameter we first create a MessageMetaData object based on the UserMessage and then change
        *  the required fields
        */
        MessageMetaData tmpMMD = new MessageMetaData(usrMessage);
        // Swap To and From
        tmpMMD.setReceiver(usrMessage.getSender());
        tmpMMD.setSender(usrMessage.getReceiver());
        // Extend Action
        ((CollaborationInfo) tmpMMD.getCollaborationInfo()).setAction(
                                usrMessage.getCollaborationInfo().getAction() + MultiHopConstants.GENERAL_RESP_SUFFIX);
        // Extend MPC
        if (signal instanceof IReceipt)
            tmpMMD.setMPC(usrMessage.getMPC() + MultiHopConstants.RECEIPT_SUFFIX);
        else
            tmpMMD.setMPC(usrMessage.getMPC() + MultiHopConstants.ERROR_SUFFIX);
        
        // Create the ebint:RoutingInput element and add it as reference parameter to the To EPR
        toEpr.addReferenceParameter(RoutingInput.createElement(mc.getEnvelope(), tmpMMD));

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

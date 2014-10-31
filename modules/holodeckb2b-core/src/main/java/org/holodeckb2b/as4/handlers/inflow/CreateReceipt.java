/*
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

package org.holodeckb2b.as4.handlers.inflow;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.general.ReplyPattern;
import org.holodeckb2b.common.pmode.ILeg;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.common.pmode.IReceiptConfiguration;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN_FLOW</i> handler responsible for creating a <i>Receipt</i> signal for the received user message. 
 * <p>The basic structure of the <code>eb:Receipt</code> element is defined in section 5.2.3.3 of the ebMS V3 Core 
 * Specification. The actual content, i.e. the child elements of the <code>eb:Receipt</code> element are not specified. 
 * This handler uses the specification given in the <i>AS4 profile</i> to fill the receipt signal. 
 * <p>It can create both the receipt for <i>reception awareness</i> and <i>non repudiation</i>. For the latter also 
 * signing must be enabled.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CreateReceipt extends AbstractUserMessageHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc, UserMessage um) throws AxisFault {
        // Only when user message was successfully delivered to business application the Receipt should be created
        Boolean delivered = (Boolean) mc.getProperty(MessageContextProperties.DELIVERED_USER_MSG);
        
        if (delivered != null && delivered) {
            log.debug("User message was succesfully delivered, check if Receipt is needed");
            
            IPMode pmode = HolodeckB2BCore.getPModeSet().get(um.getPMode());
            if (pmode == null) {
                // The P-Mode configurations has changed and does not include this P-Mode anymore, assume no receipt
                // is needed
                log.error("P-Mode " + um.getPMode() + " not found in current P-Mode set!" 
                            + "Unable to determine if receipt is needed for message [msgId=" + um.getMessageId() + "]");
                return InvocationResponse.CONTINUE;
            }
            
            // Currently we only support one-way MEPs so the leg is always the first one
            ILeg leg = pmode.getLegs().iterator().next();
            IReceiptConfiguration rcptConfig = leg.getReceiptConfiguration();
            
            if (rcptConfig == null) {
                log.debug("No receipts requested for this message exchange");
                return InvocationResponse.CONTINUE;
            }
            
            log.debug("Receipt requested for this message exchange, create new Receipt signal");
            Receipt rcptData = new Receipt();

            log.debug("Create reception awareness Receipt for User message with msg-id=" + um.getMessageId());
            // Copy some meta-data to receipt
            rcptData.setRefToMessageId(um.getMessageId());
            rcptData.setPMode(um.getPMode());

            //
            // FOR NOW WE ALWAYS CREATE A RECEIPT FOR RECEPTION AWARENESS @todo: Change when security is include
            //
            log.debug("A reception awareness Receipt should be sent");
            rcptData.setContent(createRARContent(mc, um));
            
            // Check reply patten to see if receipt should be sent as response
            boolean asResponse = rcptConfig.getPattern() == ReplyPattern.RESPONSE;
            
            log.debug("Store the receipt signal in database");
            try {
                //@todo: Use same pattern for creating the receipt as other message unit (let MsgDAO factor object)
                rcptData = MessageUnitDAO.storeOutgoingReceiptMessageUnit(rcptData, asResponse);
                if (asResponse) {
                    log.debug("Store the receipt in the MessageContext");
                    mc.setProperty(MessageContextProperties.RESPONSE_RECEIPT, rcptData);
                    mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                } else {
                    log.debug("The Receipt should be sent separately, change its processing state to READY_TO_PUSH");
                    MessageUnitDAO.setReadyToPush(rcptData);
                }
                log.debug("Receipt for message [msgId=" + um.getMessageId() + "] created successfully");
            } catch (DatabaseException ex) {
                // Storing the new Receipt signal failed! This is a severe problem, but it does not
                // need to stop processing because the user message is already delivered. The receipt
                // can be regenerated when a retry is received.
                log.error("Creating the Receipt signal in repsonse to user message [msgId=" 
                            + um.getMessageId() + "] failed! Details: " + ex.getMessage());
            }
        } else {
            // The user message is not delivered to the business application, so do not create a receipt
            log.debug("User message is not delivered successfully, so no Receipt possible");
        }
        
        return InvocationResponse.CONTINUE;
    }

    /**
     * Creates the content of a Reception Awareness Receipt as defined in section 5.1.8 of the AS4 profile. This type
     * of receipt is specified in section of the AS4 profile.
     * <p>NOTE: The first element returned by {@link org.holodeckb2b.ebms3.packaging.UserMessage#getElements(org.apache.axiom.om.OMElement)} 
     * is included in the <code>eb:Receipt</code> element. The given {@link UserMessage} object therefor MUST represent 
     * this element.  
     * 
     * @param mc    The current {@link MessageContext}, used to get copy of <code>eb:UserMessage</code> element
     * @param um    The data on the current User Message being processed, used for getting <i>refToMessageId</i> and
     *              <i>P-Mode</i> info.
     * @return      The content for the new Receipt represented as an iteration of <code>OMElement</code>s
     */
    protected Iterator<OMElement> createRARContent(MessageContext mc, UserMessage um) {
        ArrayList<OMElement>    rcptContent = new ArrayList<OMElement>();
        // Get the UserMessage element from the message header 
        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        OMElement umElement = (OMElement) org.holodeckb2b.ebms3.packaging.UserMessage.getElements(messaging).next();
        // and add it to receipt content
        rcptContent.add(umElement);
        
        return rcptContent.iterator();
    }
}

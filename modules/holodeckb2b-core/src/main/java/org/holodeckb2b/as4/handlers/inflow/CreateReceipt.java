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
package org.holodeckb2b.as4.handlers.inflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistency.entities.Receipt;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.events.ReceiptCreatedEvent;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;
import org.holodeckb2b.security.tokens.IAuthenticationInfo;
import org.holodeckb2b.security.util.SecurityUtils;

/**
 * Is the <i>IN_FLOW</i> handler responsible for creating a <i>Receipt</i> signal for the received user message.
 * <p>The basic structure of the <code>eb:Receipt</code> element is defined in section 5.2.3.3 of the ebMS V3 Core
 * Specification. The actual content, i.e. the child elements of the <code>eb:Receipt</code> element are not specified.
 * This handler uses the specification given in the <i>AS4 profile</i> to fill the receipt signal.
 * <p>It can create both the receipt for <i>reception awareness</i> and <i>non repudiation</i> (NRR).For the latter also
 * signing must be enabled. Which type of receipt is created is determined by whether the received message is signed or
 * not. If it is signed the NRR receipt is created, as specified in section 5.1.8 of the AS4 Profile.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CreateReceipt extends AbstractUserMessageHandler {

    /**
     * The namespace of the XML schema that defines the elements that must be inserted into a NRR receipt
     */
    private static final String EBBP_NS = "http://docs.oasis-open.org/ebxml-bp/ebbp-signals-2.0";

    /**
     * The namespace prefix for the XML schema that defines the elements that must be inserted into a NRR receipt
     */
    private static final String EBBP_NS_PREFIX = "ebbp";

    /**
     * The fully qualified name of the <code>ebbp:NonRepudiationInformation</code> element that is the main element
     * for the NRR Receipt
     */
    private static final QName QNAME_NRI_ELEM = new QName(EBBP_NS, "NonRepudiationInformation", EBBP_NS_PREFIX);
    /**
     * The fully qualified name of the <code>ebbp:MessagePartNRInformation</code> element that contains a
     * <code>ds:Reference</code> element from the original message
     */
    private static final QName  QNAME_MSG_PART_ELEM = new QName(EBBP_NS, "MessagePartNRInformation", EBBP_NS_PREFIX);


    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc, final EntityProxy<UserMessage> umProxy) throws AxisFault {
        // Only when user message was successfully delivered to business application the Receipt should be created
        final Boolean delivered = (Boolean) mc.getProperty(MessageContextProperties.DELIVERED_USER_MSG);

        if (delivered != null && delivered) {
            // Extract the entity object from the proxy
            final UserMessage um = umProxy.entity;
            log.debug("User message was succesfully delivered, check if Receipt is needed");

            final IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(um.getPModeId());
            if (pmode == null) {
                // The P-Mode configurations has changed and does not include this P-Mode anymore, assume no receipt
                // is needed
                log.error("P-Mode " + um.getPModeId() + " not found in current P-Mode set!"
                            + "Unable to determine if receipt is needed for message [msgId=" + um.getMessageId() + "]");
                return InvocationResponse.CONTINUE;
            }

            // Currently we only support one-way MEPs so the leg is always the first one
            final ILeg leg = pmode.getLegs().iterator().next();
            final IReceiptConfiguration rcptConfig = leg.getReceiptConfiguration();

            if (rcptConfig == null) {
                log.debug("No receipts requested for this message exchange");
                return InvocationResponse.CONTINUE;
            }

            log.debug("Receipt requested for this message exchange, create new Receipt signal");
            final Receipt rcptData = new Receipt();
            // Copy some meta-data to receipt
            rcptData.setRefToMessageId(um.getMessageId());
            rcptData.setPMode(um.getPModeId());

            log.debug("Determine type of Receipt that should be sent");
            // Check if message was signed, done by checking if Signature info was available in default WS-Sec header
            final Map<String, IAuthenticationInfo> authInfo = (Map<String, IAuthenticationInfo>)
                                                            mc.getProperty(SecurityConstants.MC_AUTHENTICATION_INFO);
            if (authInfo != null && authInfo.containsKey(SecurityConstants.SIGNATURE)) {
                log.debug("Received message was signed, created NRR receipt");
                rcptData.setContent(createNRRContent(mc));
            } else {
                log.debug("Received message not signed, create reception awareness receipt");
                rcptData.setContent(createRARContent(mc, um));
            }

            // Check reply patten to see if receipt should be sent as response
            final boolean asResponse = rcptConfig.getPattern() == ReplyPattern.RESPONSE;

            log.debug("Store the receipt signal in database");
            try {
                //@todo: Use same pattern for creating the receipt as other message unit (let MsgDAO factor object)
                final EntityProxy<Receipt> receipt = MessageUnitDAO.storeOutgoingReceiptMessageUnit(rcptData, asResponse);
                if (asResponse) {
                    log.debug("Store the receipt in the MessageContext");
                    mc.setProperty(MessageContextProperties.RESPONSE_RECEIPT, receipt);
                    mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                } else {
                    log.debug("The Receipt should be sent separately, change its processing state to READY_TO_PUSH");
                    MessageUnitDAO.setReadyToPush(receipt);
                }
                log.debug("Receipt for message [msgId=" + um.getMessageId() + "] created successfully");
                // Trigger event to signal that the event was created
                HolodeckB2BCoreInterface.getEventProcessor().raiseEvent(
                   new ReceiptCreatedEvent(um, receipt.entity,
                                           um.getCurrentProcessingState().getName().equals(ProcessingStates.DUPLICATE)),
                   mc);
            } catch (final DatabaseException ex) {
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
     * Creates the content of a Reception Awareness Receipt as defined in section 5.1.8 of the AS4 profile.
     * <p>NOTE: The first element returned by {@link org.holodeckb2b.ebms3.packaging.UserMessage#getElements(org.apache.axiom.om.OMElement)}
     * is included in the <code>eb:Receipt</code> element. The given {@link UserMessage} object therefor MUST represent
     * this element.
     *
     * @param mc    The current {@link MessageContext}, used to get copy of <code>eb:UserMessage</code> element
     * @param um    The data on the current User Message being processed, used for getting <i>refToMessageId</i> and
     *              <i>P-Mode</i> info.
     * @return      The content for the new Receipt represented as an iteration of <code>OMElement</code>s
     */
    protected Iterator<OMElement> createRARContent(final MessageContext mc, final UserMessage um) {
        final ArrayList<OMElement>    rcptContent = new ArrayList<>();
        // Get the UserMessage element from the message header
        final SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        final OMElement umElement = (OMElement) org.holodeckb2b.ebms3.packaging.UserMessage.getElements(messaging).next();
        // and add it to receipt content
        rcptContent.add(umElement);

        return rcptContent.iterator();
    }

    /**
     * Creates the content of a Non-Repudiation Of Receipt (NRR) Receipt as defined in section 5.1.8 of the AS4 profile.
     *
     * @param mc    The current {@link MessageContext}, used to get copies of the <code>ds:Reference</code> elements
     * @return      The content for the new Receipt represented as an iteration of <code>OMElement</code>s
     */
    protected Iterator<OMElement> createNRRContent(final MessageContext mc) {
        final ArrayList<OMElement>    rcptContent = new ArrayList<>();
        // Get element factory for creating the elements in the Receipt content
        final OMFactory elemFactory = mc.getEnvelope().getOMFactory();
        // Create the ebbp:NonRepudiationInformation container element
        final OMElement ebbpNRIElement = elemFactory.createOMElement(QNAME_NRI_ELEM);
        ebbpNRIElement.declareNamespace(EBBP_NS, EBBP_NS_PREFIX);

        // Add a ebbp:MessagePartNRInformation for each reference found in Signature element of the received message
        for (final OMElement ref : SecurityUtils.getSignatureReferences(mc)) {
            final OMElement ebbpMsgPart = elemFactory.createOMElement(QNAME_MSG_PART_ELEM);
            ebbpMsgPart.addChild(ref.cloneOMElement());
            ebbpNRIElement.addChild(ebbpMsgPart);
        }
        rcptContent.add(ebbpNRIElement);

        return rcptContent.iterator();
    }

}

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
import java.util.Collection;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.as4.receptionawareness.ReceiptCreatedEvent;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN_FLOW</i> handler responsible for creating a <i>Receipt</i> signal for the received user message.
 * <p>The basic structure of the <code>eb:Receipt</code> element is defined in section 5.2.3.3 of the ebMS V3 Core
 * Specification. The actual content, i.e. the child elements of the <code>eb:Receipt</code> element are not specified.
 * This handler uses the specification given in the <i>AS4 profile</i> to fill the receipt signal.
 * <p>It can create both the receipt for <i>reception awareness</i> and <i>non repudiation</i> (NRR).For the latter also
 * signing must be enabled. Which type of receipt is created is determined by whether the received message is signed or
 * not. If it is signed the NRR receipt is created, as specified in section 5.1.8 of the AS4 Profile.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CreateReceipt extends AbstractUserMessageHandler {

    /**
     * The WS-Security namespace URI
     */
    private static final String WSS_NAMESPACE_URI =
                                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    /**
     * The namespace URI for XML Signatures
     */
    public static final String DSIG_NAMESPACE_URI = "http://www.w3.org/2000/09/xmldsig#";
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
    public static final QName QNAME_NRI_ELEM = new QName(EBBP_NS, "NonRepudiationInformation", EBBP_NS_PREFIX);
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
    protected InvocationResponse doProcessing(final MessageContext mc, final IUserMessageEntity um) {
        // Only when user message was successfully delivered to business application the Receipt should be created
        final Boolean delivered = (Boolean) mc.getProperty(MessageContextProperties.DELIVERED_USER_MSG);

        if (delivered != null && delivered) {
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
            final ILeg leg = pmode.getLeg(um.getLeg() != null ? um.getLeg() : ILeg.Label.REQUEST);
            final IReceiptConfiguration rcptConfig = leg.getReceiptConfiguration();

            if (rcptConfig == null) {
                log.debug("No receipts requested for this message exchange");
                return InvocationResponse.CONTINUE;
            }

            log.debug("Receipt requested for this message exchange, create new Receipt signal");
            final Receipt rcptData = new Receipt();
            // Copy some meta-data to receipt
            rcptData.setRefToMessageId(um.getMessageId());
            rcptData.setPModeId(um.getPModeId());

            log.debug("Determine type of Receipt that should be sent");
            // Check if message was signed,
            if (mc.getProperty(MessageContextProperties.SIG_VERIFICATION_RESULT) != null) {
                log.debug("Received message was signed, created NRR receipt");
                rcptData.setContent(createNRRContent(mc));
            } else {
                log.debug("Received message not signed, create reception awareness receipt");
                rcptData.setContent(createRARContent(mc));
            }

            // Check reply patten to see if receipt should be sent as response
            final boolean asResponse = rcptConfig.getPattern() == ReplyPattern.RESPONSE;
            log.debug("Store the receipt signal in database");
            try {
                IReceiptEntity receipt = (IReceiptEntity) HolodeckB2BCore.getStorageManager()
                                                                                .storeOutGoingMessageUnit(rcptData);
                if (asResponse) {
                    log.debug("Store the receipt in the MessageContext");
                    mc.setProperty(MessageContextProperties.RESPONSE_RECEIPT, receipt);
                    mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                } else {
                    log.debug("The Receipt should be sent separately");
                    HolodeckB2BCore.getStorageManager().setProcessingState(receipt, ProcessingState.READY_TO_PUSH);
                }
                log.debug("Receipt for message [msgId=" + um.getMessageId() + "] created successfully");
                // Trigger event to signal that the event was created
                HolodeckB2BCore.getEventProcessor().raiseEvent(
                   new ReceiptCreatedEvent(um, receipt,
                                           um.getCurrentProcessingState().getState() == ProcessingState.DUPLICATE),
                   mc);
            } catch (final PersistenceException ex) {
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
     * <p>The first element returned by {@link UserMessageElement#getElements(org.apache.axiom.om.OMElement)}
     * applied to the received message is included in the <code>eb:Receipt</code> element. This implies that bundling
     * of User Message units is NOT supported by this handler.
     *
     * @param mc    The current {@link MessageContext}, used to get copy of <code>eb:UserMessage</code> element
     * @return      The content for the new Receipt represented as an iteration of <code>OMElement</code>s
     */
    protected Iterator<OMElement> createRARContent(final MessageContext mc) {
        final ArrayList<OMElement>    rcptContent = new ArrayList<>();
        // Get the UserMessage element from the message header
        final SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        final OMElement umElement = UserMessageElement.getElements(messaging).next();
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
        for (final OMElement ref : getSignatureReferences(mc)) {
            final OMElement ebbpMsgPart = elemFactory.createOMElement(QNAME_MSG_PART_ELEM);
            ebbpMsgPart.addChild(ref.cloneOMElement());
            ebbpNRIElement.addChild(ebbpMsgPart);
        }
        rcptContent.add(ebbpNRIElement);

        return rcptContent.iterator();
    }

    /**
     * Gets all <code>ds:Reference</code> descendant elements from the signature in the default WS-Security header.
     * <p>In an ebMS there may only be one <code>ds:Signature</code> element, so we can take the<code>
     * ds:SignedInfo</code> of the first one to get access to the <code>ds:Reference</code> elements.
     *
     * @param mc    The {@link MessageContext} of the message to get the reference from
     * @return      The {@link Collection} of <code>ds:Reference</code> elements contained in the signature,<br>
     *              <code>null</code> or an empty collection if there is no signature in the default security header.
     */
    private Collection<OMElement> getSignatureReferences(final MessageContext mc) {
       // Get all WS-Security headers
        final ArrayList<SOAPHeaderBlock> secHeaders = mc.getEnvelope().getHeader()
                                                        .getHeaderBlocksWithNSURI(WSS_NAMESPACE_URI);
        if (secHeaders == null || secHeaders.isEmpty())
            return null; // No security headers in message

        // There can be more than one security header, get the default header
        SOAPHeaderBlock defHeader = null;
        for(final SOAPHeaderBlock h : secHeaders) {
            if (h.getRole() == null)
                defHeader = h;
        }
        if (defHeader == null)
            return null; // No default security header

        // Get the ds:SignedInfo descendant in the default header.
        final Iterator<OMElement> signatureElems = defHeader.getChildrenWithName(
                                                          new QName(DSIG_NAMESPACE_URI, "Signature"));
        if (signatureElems == null || !signatureElems.hasNext())
            return null; // No Signature in default header

        // The ds:SignedInfo element is the first child of ds:Signature
        final OMElement signedInfoElement = signatureElems.next().getFirstElement();
        // Collect all ds:Reference contained in it
        Collection<OMElement> references = null;
        if (signedInfoElement != null) {
            references = new ArrayList<>();
            for (final Iterator<OMElement> it =
                    signedInfoElement.getChildrenWithName(new QName(DSIG_NAMESPACE_URI, "Reference"))
                ; it.hasNext() ;)
                references.add(it.next());
        }

        return references;
    }
}

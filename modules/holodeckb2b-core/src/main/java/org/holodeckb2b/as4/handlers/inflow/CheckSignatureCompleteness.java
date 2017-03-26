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

import java.util.Collection;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.errors.ValueInconsistent;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.security.util.SecurityUtils;

/**
 * Is the <i>IN_FLOW</i> handler that checks whether all payloads in the user message are signed. That all payloads
 * must be signed follows from profiling rule (b) defined in section 5.1.8 of the AS4 Profile:<br>
 * <i>"When signed receipts are requested in AS4 that make use of default conventions, the Sending message handler
 * (i.e. the MSH sending messages for which signed receipts are expected) MUST identify message parts (referenced in
 * eb:PartInfo elements in the received User Message) and MUST sign the SOAP body and all attachments"</i>
 * <p>If a payload is not signed a <i>ValueInconsistent</i> error will be generated and reported to the sender of the
 * user message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CheckSignatureCompleteness extends AbstractUserMessageHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc, final IUserMessageEntity um) throws Exception {
        // First check if this message needs a Receipt and is signed
        final IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(um.getPModeId());
        if (pmode == null) {
            // The P-Mode configurations has changed and does not include this P-Mode anymore, assume no receipt
            // is needed
            log.error("P-Mode " + um.getPModeId() + " not found in current P-Mode set!"
                     + "Unable to determine if receipt is needed for message [msgId=" + um.getMessageId() + "]");
            return InvocationResponse.CONTINUE;
        }

        // Currently we only support one-way MEPs so the leg is always the first one
        if (pmode.getLegs().iterator().next().getReceiptConfiguration() == null) {
            // No receipt requested for this message
            return InvocationResponse.CONTINUE;
        }

        // Check for signed message by retrieving the ds:References from the signature
        final Collection<OMElement> references = SecurityUtils.getSignatureReferences(mc);
        if (Utils.isNullOrEmpty(references))
            // No Signature, nothing to check
            return InvocationResponse.CONTINUE;

        // Message is signed, check that each payload has a ds:Reference in the Signature
        boolean allRefd = true;
        if (!Utils.isNullOrEmpty(um.getPayloads())) {
            for(final IPayload payload : um.getPayloads()) {
                String plRef = payload.getPayloadURI();
                // If the payload has no reference the SOAP Body is implicitly referenced. So set the reference to the id
                // from de Body element
                if (plRef == null || plRef.isEmpty())
                    plRef = getSOAPBodyIdRef(mc);
                else {
                    // Add prefix to the reference
                    switch (payload.getContainment()) {
                        case BODY :
                            plRef = "#" + plRef; break;
                        case ATTACHMENT :
                            plRef = "cid:" + plRef;
                    }
                }
                boolean found = false;
                for(final Iterator<OMElement> it = references.iterator(); it.hasNext() && !found ; ) {
                    final OMElement ref = it.next();
                    found = plRef.equals(ref.getAttributeValue(new QName("URI")));
                }
                allRefd &= found;
                if (!found) {
                    log.warn("Payload with reference [" + plRef + "] is not signed in user message ["
                                + um.getMessageId() + "]");
                    // If no ds:Reference is found for this payload the message does not conform to the AS4 requirements
                    // that all payloads should be signed. Therefore create the ValueInconsistent error
                    createValueInconsistentError(mc, um.getMessageId(), payload.getPayloadURI());
                }
            }
        }
        // If not all payloads are referenced the UserMessage should not be processed further, so change it processing
        // state to failed
        if (!allRefd)
            HolodeckB2BCore.getStoreManager().setProcessingState(um, ProcessingState.FAILURE);

        return InvocationResponse.CONTINUE;
    }

    /**
     * Helper method to retrieve the value of the id of the SOAP <code>Body</code> element.
     * <a>As described in the WS-Security spec valid id attributes are:<ul>
     * <li>Local ID attributes on XML Signature elements</li>
     * <li>Local ID attributes on XML Encryption elements</li>
     * <li>Global wsu:Id attributes (described below) on elements</li>
     * <li>Profile specific defined identifiers</li>
     * <li>Global xml:id attributes on elements</li>
     * </ul>
     * So for the SOAP <code>Body</code> element the possible id attributes are <code>wsu:Id</code> and
     * <code>xml:id</code>.
     *
     * @param mc    The current message context
     * @return      The id reference for the SOAP Body element if it has an id attribute, or<br>
     *              <code>null</code> if no id attribute is found on the SOAP Body element
     */
    private String getSOAPBodyIdRef(final MessageContext mc) {
        String  bodyId = null;

        final SOAPBody body = mc.getEnvelope().getBody();
        for(final Iterator<OMAttribute> attributes = body.getAllAttributes(); attributes.hasNext() && bodyId == null;) {
            final OMAttribute attr = attributes.next();

            if (SecurityConstants.QNAME_WSU_ID.equals(attr.getQName()))
                bodyId = attr.getAttributeValue();
            else if (EbMSConstants.QNAME_XMLID.equals(attr.getQName()))
                bodyId = attr.getAttributeValue();
        }

        // As we need to return the reference the id must be prefixed with a '#'
        return bodyId != null ? "#" + bodyId : null;
    }

    /**
     * Helper method to create a <i>ValueInconsistent</i> error to indicate that a payload is not signed.
     *
     * @param mc        The current message context
     * @param msgId     The message id of the user message for which the signature is not complete
     * @param plRef     The reference for which no ds:Reference was found in the signature
     */
    private void createValueInconsistentError(final MessageContext mc, final String msgId, final String plRef) {
        final ValueInconsistent   error = new ValueInconsistent();
        error.setRefToMessageInError(msgId);
        error.setErrorDetail("Payload with href=" + plRef + " is not included in Signature");

        MessageContextUtils.addGeneratedError(mc, error);
    }

}

/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.security.handlers;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.security.PayloadDigest;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.events.SignatureCreatedEvent;
import org.holodeckb2b.interfaces.events.types.ISignatureCreatedEvent;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.security.util.SecurityUtils;

/**
 * Is the <i>OUT_FLOW</i> handler that will create and raise the {@link ISignatureCreatedEvent} message processing event
 * when the sent User Message is signed.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */
public class RaiseSignatureCreatedEvent extends AbstractUserMessageHandler {

    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc, final IUserMessageEntity um) throws Exception {

        // First check if the message should have been signed
        if (mc.getProperty(SecurityConstants.SIGNATURE) == null) {
            log.debug("Message was not signed, nothing to do");
            return InvocationResponse.CONTINUE;
        }

        log.debug("Message should have been signed, get ds:References from the signature");
        final Collection<OMElement>   sigReferences = SecurityUtils.getSignatureReferences(mc);

        if (Utils.isNullOrEmpty(sigReferences)) {
            // There are no ds:Reference elements found in the message. This is clearly an error, but we assume here
            // this error will be handled elsewhere
            log.warn("No ds:Reference elements found in message [msgId=" + um.getMessageId()
                        + "] that should have been signed!");
            return InvocationResponse.CONTINUE;
        }

        log.debug("Convert the OMElements into PayloadDigest objects");
        final Collection<PayloadDigest>  plDigests = new ArrayList<>();
        final String soapBodyId = mc.getEnvelope().getBody().getAttributeValue(SecurityConstants.QNAME_WSU_ID);
        for(final OMElement ref : sigReferences)
            if (isReferenceToPayload(ref, um, soapBodyId))
                plDigests.add(new PayloadDigest(ref));

        log.debug(plDigests.size() + " references to pyaloads were found");

        // Create and raise the SignatureCreatedEvent
        final SignatureCreatedEvent event = new SignatureCreatedEvent(um, plDigests);
        log.debug("Raising new SignatureCreatedEvent [id=" + event.getId() + "]");
        HolodeckB2BCore.getEventProcessor().raiseEvent(event, mc);
        log.info("Raised SignatureCreatedEvent [id=" + event.getId() + "] for UserMessage with msgId="
                   + um.getMessageId());

        return InvocationResponse.CONTINUE;
    }

    /**
     * Helper method to check whether the given <code>ds:Reference</code> element applies to a payload.
     *
     * @param ref           The {@link OMElement} representing the <code>ds:Reference</code> element
     * @param um            The {@link IUserMessage} for which the signature was created
     * @param soapBodyId    The <i>id</i> of the SOAP Body element. Needed to check if reference applies to payload in
     *                      SOAP Body
     * @return              <code>true</code> if this reference applies to a payload of the message,<br>
     *                      <code>false</code> otherwise
     */
    private boolean isReferenceToPayload(final OMElement ref, final IUserMessage um, final String soapBodyId) {
        boolean isPlRef = false;
        boolean hasBodyPl = false;
        final String refURI = ref.getAttributeValue(new QName("URI"));

        // First check if the uri directly equals a payload URI
        for (final IPayload pl : um.getPayloads()) {
            final String plURI = pl.getPayloadURI();
            // Because the reference in payload object does not contain prefix, use endsWith instead of equals
            isPlRef |= plURI != null && refURI.endsWith(plURI);
            hasBodyPl |= pl.getContainment() == IPayload.Containment.BODY;
        }
        // If not it can still refer to the payload in the SOAP body. The URI from the reference should then be equal
        // to the wsu:Id (but again ref URI has '#' prefix, so use endsWith)
        isPlRef |= hasBodyPl && refURI.endsWith(soapBodyId);

        log.debug(refURI + " does" + (isPlRef ? " " : " not ") + "reference a payload");
        return isPlRef;
    }
}

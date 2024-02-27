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
package org.holodeckb2b.ebms3.handlers.outflow;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.axiom.soap.SOAP11Version;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.ebms3.packaging.ErrorSignalElement;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for creating the <code>eb:SignalMessage</code> and child element for an
 * Error Signal in the ebMS messaging header when Error Signals must be sent.
 * <p>Section 5.2.4 of the ebMS Core specification specifies that a message MUST NOT contain more than one <code>
 * SignalMessage</code> message per signal type. This handler does however support adding multiple error signals to the
 * message, which would create an ebMS message that <b>does not conform</b> to the ebMS V3 Core and AS4 specifications.
 * It is the responsibility of the other handlers not to insert more than one error signal in the message context
 * property.
 * <p>When one of the errors added to the message has severity <i>Failure</i> the ebMS specification states that the
 * SOAP message SHOULD also contains a <i>SOAPFault</i> (see section 6.6 of ebMS V3 Core Specification). This may cause
 * WS-I basic profile conformance problems when the message also contains another message unit, especially when the
 * error signal is bundled with a user message with a body payload as there SHALL be at most one child element of the
 * SOAP body.<br>
 * It is also unclear which HTTP status code should be used in such cases. The SOAP Fault implies using a non 2xx range
 * code, but the bundled message units indicate successful processing which requires a 200 HTTP status code.<br>
 * The resolution to this issue as noted in <a href="https://issues.oasis-open.org/browse/EBXMLMSG-4">issue #4</a> in
 * the issue tracker of the ebMS TC is that the insertion of the SOAP Fault is optional.<br>
 * Holodeck B2B will therefore by default not add a SOAP Fault to an ebMS message containing an Error Signal with errors
 * of severity <i>FAILURE</i>. If in a message exchange it is preferred to add the SOAP Fault it should be configured
 * in the P-Mode using the parameter returned by {@link IErrorHandling#shouldAddSOAPFault()}. Note however that Holodeck
 * B2B will only add the SOAP Fault when the error signal(s) is/are not bundled with another message unit to prevent
 * interop issues mentioned above.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IErrorHandling
 */
public class PackageErrorSignals extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log)
    																					throws StorageException {
        // First check if there are any errors to include
        final Collection<IErrorMessageEntity> errors = procCtx.getSendingErrors();

        if (Utils.isNullOrEmpty(errors))
            // No errors in this message, continue processing
            return InvocationResponse.CONTINUE;

        // There are error signals to be sent, add them to the message
        log.debug("Adding " + errors.size() + " error signal(s) to the message");

        log.trace("Get the eb:Messaging header from the message");
        final SOAPHeaderBlock messaging = Messaging.getElement(procCtx.getParentContext().getEnvelope());

        // If one of the errors is of severity FAILURE a SOAP may be added
        boolean addSOAPFault = false;
        for(final IErrorMessageEntity e : errors) {
            log.trace("Add eb:SignalMessage element to the existing eb:Messaging header");
            ErrorSignalElement.createElement(messaging, e);
            log.debug("eb:SignalMessage element for Error Signal [msgId=" + e.getMessageId()
            			+ "] succesfully added to header");
            // Check if a SOAPFault should be added
            addSOAPFault |= e.shouldHaveSOAPFault();
        }

        // If SOAP Fault should be added, check if possible
        if(addSOAPFault) {
            log.debug("A SOAPFaults should be added to message, check if possible due to UserMessage with body payload");
            if (procCtx.getSendingMessageUnits().parallelStream().allMatch(m -> m instanceof IErrorMessage)) {
                log.trace("SOAPFault can be added to message");
                addSOAPFault(procCtx.getParentContext().getEnvelope());
                log.trace("SOAPFault added to message");
            }
        }
        return InvocationResponse.CONTINUE;
    }

    /**
     * Adds or replaces an existing <i>SOAP Fault</i> to the message indicating that there was a problem during ebMS
     * processing.
     * <p>The added fault refers to the ebMS Error signals included in the message and does not contain detailed
     * error information.
     *
     * @param env    The SOAP envelope of the message the fault must be added to
     */
    protected void addSOAPFault(final SOAPEnvelope env) {
        final SOAPFactory factory = (SOAPFactory) env.getOMFactory();
        final SOAPBody    body = env.getBody();
        SOAPFault   fault = body.getFault();

        if (fault == null)
            fault = factory.createSOAPFault(body);

        // The content of the SOAP Fault differs between SOAP 1.1 and 1.2, so check version to use
        final boolean isSoap11 = env.getVersion() instanceof SOAP11Version;

        final SOAPFaultCode  fCode = factory.createSOAPFaultCode(fault);
        if (isSoap11)
            fCode.setText(new QName(env.getNamespaceURI(), "Client"));
        else
            factory.createSOAPFaultValue(fCode).setText(new QName(env.getNamespaceURI(), "Sender"));

        final SOAPFaultReason fReason = factory.createSOAPFaultReason(fault);
        final String          reason =
                        "An error occurred while processing the received ebMS message. Check ebMS errors for details.";
        if (isSoap11) {
            fReason.setText(reason);
        } else {
            final SOAPFaultText fReasonText = factory.createSOAPFaultText(fReason);
            fReasonText.setText(reason);
            fReasonText.setLang("en");
        }
    }
}

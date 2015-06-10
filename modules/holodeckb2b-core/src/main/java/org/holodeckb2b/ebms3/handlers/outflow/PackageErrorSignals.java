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
package org.holodeckb2b.ebms3.handlers.outflow;

import java.util.ArrayList;
import javax.xml.namespace.QName;
import org.apache.axiom.soap.SOAP11Version;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPFaultValue;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.IEbmsError;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;

/**
 * If there are <i>Error signals </i> that must be sent, this handler adds the
 * <code>eb:SignalMessage</code> elements to the ebMS header (which is created
 * by {@link CreateSOAPEnvelopeHandler}).
 * <p>If there are error signal message units to be sent, the corresponding 
 * {@link ErrorMessage} objects MUST be included in the Axis2 <code>MessageContext</code> 
 * property {@link MessageContextProperties#SEND_ERROR_SIGNALS}.
 * <p><b>NOTE 1:</b> Section 5.2.4 of the ebMS Core specification specifies that
 * a message MUST NOT contain more than one <code>SignalMessage</code> message
 * per signal type. This handler however supports adding multiple error signals
 * to the message. It is the responsibility of the other handlers not to insert
 * more than one error signal in the {@link MessageContextProperties#SEND_ERROR_SIGNALS}
 * message context property.
 * <p><b>NOTE 2:</b> When one of the errors added to the message has severity 
 * <i>Failure</i> the ebMS specification states that the SOAP message SHOULD also
 * contains a <i>SOAPFault</i> (see section 6.6). This may cause WS-I basic profile
 * conformance problems when the message also contains another message unit, especially 
 * when the error signal is bundled with a user message with a body payload as there 
 * SHALL be at most one child element of the SOAP body.<br>
 * It is also unclear which HTTP status code should be used in such cases. The SOAP Fault
 * implies using a non 2xx range code, but the bundled message units indicate successful 
 * processing which requires a 200 HTTP status code.<br>
 * Therefor this handler will not insert a SOAP Fault when the error signal(s) is/are 
 * bundled with another message unit.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PackageErrorSignals extends BaseHandler {

    /**
     * This handler will run in both the regular as well as the fault out flow
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) {
        // First check if there are any errors to include
        ArrayList<ErrorMessage> errors = (ArrayList<ErrorMessage>) mc.getProperty(MessageContextProperties.OUT_ERROR_SIGNALS);
        
        if (errors == null || errors.isEmpty())
            // No errors in this message, continue processing
            return InvocationResponse.CONTINUE;
        
        // There are error signals to be sent, add them to the message
        log.debug("Adding " + errors.size() + " error signal(s) to the message");
        
        log.debug("Get the eb:Messaging header from the message");
        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        
        // If one of the errors is of severity FAILURE a SOAP may be added
        boolean addSOAPFault = false;
        for(ErrorMessage e : errors) {
            log.debug("Add eb:SignalMessage element to the existing eb:Messaging header");
            org.holodeckb2b.ebms3.packaging.ErrorSignal.createElement(messaging, e);
            log.debug("eb:SignalMessage element succesfully added to header");
            
            if (!addSOAPFault) {
                log.debug("Check if a SOAPFault should be added");
                for(IEbmsError err : e.getErrors())
                    addSOAPFault = err.getSeverity() == IEbmsError.Severity.FAILURE;
                log.debug("Error signal contains " + (addSOAPFault ? " one or more " : " no ") 
                        + " errors with severity FAILURE: " + (addSOAPFault ? "" : "no") + " SOAPFault neeeded.");
            }
        }
        
        // If SOAP Fault should be added, check if possible
        if(addSOAPFault) {
            log.debug("A SOAPFaults should be added to message, check if possible due to UserMessage with body payload");
            if (isSOAPFaultAllowed(mc)) {
                log.debug("SOAPFault can be added to message");
                addSOAPFault(mc.getEnvelope());
                log.debug("SOAPFault added to message");
            }
        }
        
        return InvocationResponse.CONTINUE;
    }   
    
    /**
     * Checks if a SOAPFault can be added to the current message. This is allowed when there is no
     * other message unit that must be contained in the SOAP body.
     * 
     * @param mc        The current message context
     * @return          <code>true</code> if a SOAPFault can be added, <code>false</code> otherwise
     */
    protected boolean isSOAPFaultAllowed(MessageContext mc) {
        log.debug("Check if message contains another message unit");
        
        try {
            log.debug("Check for UserMessage message unit");
            UserMessage um = (UserMessage) mc.getProperty(MessageContextProperties.OUT_USER_MESSAGE);
            
            if (um != null) {
                log.debug("Message does contain an UserMessage for sending, SOAPFault should not be added");
                return false;
            }
        } catch (Exception e) {}
        
        log.debug("Message does not contain an UserMessage, check for PullRequest");
        try {
            PullRequest pr = (PullRequest) mc.getProperty(MessageContextProperties.OUT_PULL_REQUEST);
            
            if (pr != null) {
                log.debug("Message does contain a PullRequest for sending, SOAPFault should not be added");
                return false;
            }
        } catch (Exception e) {}
        
        log.debug("Message does not contain a PullRequest, check for Receipt");
        try {
            ArrayList<Receipt> rcpts = (ArrayList<Receipt>) mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
            
            if (rcpts != null && !rcpts.isEmpty()) {
                log.debug("Message does contain a Receipt for sending, SOAPFault should not be added");
                return false;
            }
        } catch (Exception e) {}

        log.debug("Message does not contain another message unit, SOAP Fault can be added");
        return true;
    }

    /**
     * Adds or replaces an existing <i>SOAP Fault</i> to the message indicating that there was a problem during ebMS 
     * processing. 
     * <p>The added fault refers to the ebMS Error signals included in the message and does not contain detailed
     * error information.
     * 
     * @param env    The SOAP envelope of the message the fault must be added to
     */
    protected void addSOAPFault(SOAPEnvelope env) {
        SOAPFactory factory = (SOAPFactory) env.getOMFactory();
        SOAPBody    body = env.getBody();
        SOAPFault   fault = body.getFault();
        
        if (fault == null)
            fault = factory.createSOAPFault(body);

        // The content of the SOAP Fault differs between SOAP 1.1 and 1.2, so check version to use
        boolean isSoap11 = env.getVersion() instanceof SOAP11Version;

        SOAPFaultCode  fCode = factory.createSOAPFaultCode(fault);
        QName   faultValue = new QName(env.getNamespaceURI(), "Client");
        if (isSoap11) {
            fCode.setText(faultValue);
        } else {
            SOAPFaultValue fValue = factory.createSOAPFaultValue(fCode);
            fValue.setText(faultValue);
        }

        SOAPFaultReason fReason = factory.createSOAPFaultReason(fault);
        String          reason = "An error occurred while processing the received ebMS message. Check ebMS errors for details.";
        if (isSoap11) {
            fReason.setText(reason);
        } else {
            SOAPFaultText fReasonText = factory.createSOAPFaultText(fReason);
            fReasonText.setText(reason);
            fReasonText.setLang("en");
        }
    }
}

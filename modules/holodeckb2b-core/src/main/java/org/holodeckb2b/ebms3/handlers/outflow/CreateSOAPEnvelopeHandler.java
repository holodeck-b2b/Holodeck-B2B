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

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.holodeckb2b.common.handler.AbstractBaseHandler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.pmode.PModeUtils;

/**
 * Is the <i>OUT_FLOW</i> handler that creates an empty ebMS message, i.e. a SOAP Envelope containing only an <code>
 * eb:Messaging</code> element. When running in a <i>response</i> flow the SOAP Envelope probably already  exists and
 * creation can therefor be skipped.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CreateSOAPEnvelopeHandler extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final MessageProcessingContext procCtx, final Log log) throws AxisFault {
        final MessageContext mc = procCtx.getParentContext();
    	SOAPEnvelope    env = mc.getEnvelope();
        
        // SOAP Envelope only needs to be created when Holodeck B2B initiates the exchange
        //  or when response does not yet contain one
        if (procCtx.isHB2BInitiated() || env == null) {
            // For request use P-Mode, for response use SOAP version from request
            log.trace("Check for SOAP version");
            SOAPEnv.SOAPVersion version;
            if (procCtx.isHB2BInitiated()) {
                log.trace("Use P-Mode of primary message unit to get SOAP version");
                final IMessageUnitEntity primaryMU = procCtx.getPrimaryMessageUnit();
                if (primaryMU == null) {
                    log.debug("No message unit in this response, no envelope needed");
                    return InvocationResponse.CONTINUE;
                }                
                ILeg leg;
                try {
                	leg = PModeUtils.getLeg(primaryMU);
                } catch (IllegalStateException pmodeNotAvailable) {
                	log.error("P-Mode of primary message unit [" + primaryMU.getMessageId() + "] is not available");
                	leg = null;
                }
                version = leg != null && leg.getProtocol() != null && "1.1".equals(leg.getProtocol().getSOAPVersion()) ?
                                    SOAPEnv.SOAPVersion.SOAP_11 : SOAPEnv.SOAPVersion.SOAP_12;
            } else {
                log.trace("Get version from request context");
                final OperationContext opContext = mc.getOperationContext();
                final MessageContext reqMsgContext = opContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
                version = (reqMsgContext.isSOAP11() ? SOAPEnv.SOAPVersion.SOAP_11 : SOAPEnv.SOAPVersion.SOAP_12);
            }

            log.debug("Create SOAP " + (version == SOAPEnv.SOAPVersion.SOAP_11 ? "1.1" : "1.2") + " envelope");
            env = SOAPEnv.createEnvelope(version);

            try {
                // Add the new SOAP envelope to the message context and continue processing
                mc.setEnvelope(env);
            } catch (final AxisFault ex) {
                log.fatal("Could not add the SOAP envelope to the message!");
                throw new AxisFault("Could not add the SOAP envelope to the message!");
            }
            log.trace("Added SOAP envelope to message context");
        } else {
            log.trace("Check that ebMS namespace is declared on the SOAP envelope");
            SOAPEnv.declareNamespaces(env);
        }

        log.trace("Add empty eb3:Messaging element");
        Messaging.createElement(env);

        return InvocationResponse.CONTINUE;
    }

}

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
package org.holodeckb2b.common.axis2;

import static org.apache.axis2.client.ServiceClient.ANON_OUT_IN_OP;

import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.logging.Log;
import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;

/**
 * Is a helper class that handles the sending of a message unit using the Axis2 framework.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class Axis2Sender {

    /**
     * Sends the given message unit to the other MSH.
     *
     * @param messageUnit   The message unit to send
     * @param log           The log to use for writing log information
     */
    public static void sendMessage(final IMessageUnitEntity messageUnit, final AxisService svcConfig, final Log log) {
        ServiceClient sc;
        OperationClient oc;
        final MessageContext msgCtx = new MessageContext();
        msgCtx.setFLOW(MessageContext.OUT_FLOW);
        
        // For routing signals through the I-Cloud WS-A headers are used. We use the Axis2 addressing module to create
        // the headers. But we don't need these headers normally, so disable the module by default
        msgCtx.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);

        try {
            log.debug("Prepare Axis2 client to send " + MessageUnitUtils.getMessageUnitName(messageUnit)
                        + " with msgId: " + messageUnit.getMessageId());
            sc = new ServiceClient(((InternalConfiguration) HolodeckB2BCoreInterface.getConfiguration())
                                                                                    .getAxisConfigurationContext(),
                                   svcConfig);
            // Engage all modules required by the service
            for(String module : svcConfig.getModules())
                sc.engageModule(module);
            oc = sc.createClient(ANON_OUT_IN_OP);
            oc.addMessageContext(msgCtx);

            // This dummy EPR has to be provided to be able to trigger message sending. It will be replaced later
            // with the correct URL defined in the P-Mode
            final EndpointReference targetEPR = new EndpointReference("http://holodeck-b2b.org/transport/dummy");
            final Options options = new Options();
            options.setTo(targetEPR);
            options.setExceptionToBeThrownOnSOAPFault(false);
            oc.setOptions(options);

            final HttpClient httpClient = new HttpClient();
            httpClient.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
            msgCtx.setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpClient);
            log.trace("Axis2 client configured for sending ebMS message");
        } catch (final AxisFault af) {
            // Setting up the Axis environment failed. As it prevents sending the message it is logged as a fatal error
            log.fatal("Setting up Axis2 to send message failed! Details: " + af.getReason());
            return;
        }

        try {
            log.debug("Create an empty MessageProcessingContext for message with current configuration");
            final MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(msgCtx);
            if (messageUnit instanceof IUserMessage)
                procCtx.setUserMessage((IUserMessageEntity) messageUnit);
            else if (messageUnit instanceof IPullRequest)
            	procCtx.setPullRequest((IPullRequestEntity) messageUnit);
            else if (messageUnit instanceof IErrorMessage)
                procCtx.addSendingError((IErrorMessageEntity) messageUnit);
            else if (messageUnit instanceof IReceipt)
                procCtx.addReceivedReceipt((IReceiptEntity) messageUnit);
            log.trace("Start the message send process");
            oc.execute(true);
        } catch (final AxisFault af) {
            /* An error occurred while sending the message, it should however be already processed by one of the
               handlers. In that case the message context will not contain the failure reason. To prevent redundant
               logging we check if there is a failure reason before we log the error here.
            */
            final List<Throwable> errorStack = Utils.getCauses(af);
            final StringBuilder logMsg = new StringBuilder("\n\tError stack: ")
                                                    .append(errorStack.get(0).getClass().getSimpleName());
            for(int i = 1; i < errorStack.size(); i++) {
                logMsg.append("\n\t    Caused by: ").append(errorStack.get(i).getClass().getSimpleName());
            }
            logMsg.append(" {").append(errorStack.get(errorStack.size() - 1).getMessage()).append('}');
            log.error("An error occurred while sending the message [" + messageUnit.getMessageId() + "]!"
                     + logMsg.toString());
        } finally {
            try {
                sc.cleanupTransport();
                sc.cleanup();
            } catch (final AxisFault af2) {
                log.error("Clean up of Axis2 context to send message failed! Details: " + af2.getReason());
            }
        }
    }
}

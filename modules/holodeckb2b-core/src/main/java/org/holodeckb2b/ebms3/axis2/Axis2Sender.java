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
package org.holodeckb2b.ebms3.axis2;

import java.util.List;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import static org.apache.axis2.client.ServiceClient.ANON_OUT_IN_OP;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.logging.Log;
import org.holodeckb2b.axis2.Axis2Utils;
import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.module.HolodeckB2BCoreImpl;

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
    public static void sendMessage(final IMessageUnitEntity messageUnit, final Log log) {
        ServiceClient sc;
        OperationClient oc;
        final MessageContext msgCtx = new MessageContext();

        try {
            log.debug("Prepare Axis2 client to send " + MessageUnitUtils.getMessageUnitName(messageUnit)
                        + " with msgId: " + messageUnit.getMessageId());
            sc = new ServiceClient(((InternalConfiguration) HolodeckB2BCoreInterface.getConfiguration())
                                                                                    .getAxisConfigurationContext(),
                                   Axis2Utils.createAnonymousService());
            sc.engageModule(HolodeckB2BCoreImpl.HOLODECKB2B_CORE_MODULE);
            oc = sc.createClient(ANON_OUT_IN_OP);

            log.debug("Create an empty MessageContext for message with current configuration");

            if (messageUnit instanceof IUserMessage)
                msgCtx.setProperty(MessageContextProperties.OUT_USER_MESSAGE, messageUnit);
            else if (messageUnit instanceof IPullRequest)
                msgCtx.setProperty(MessageContextProperties.OUT_PULL_REQUEST, messageUnit);
            else if (messageUnit instanceof IErrorMessage)
                MessageContextUtils.addErrorSignalToSend(msgCtx, (IErrorMessageEntity) messageUnit);
            else if (messageUnit instanceof IReceipt)
                MessageContextUtils.addReceiptToSend(msgCtx, (IReceiptEntity) messageUnit);

            oc.addMessageContext(msgCtx);

            // This dummy EPR has to be provided to be able to trigger message sending. It will be replaced later
            // with the correct URL defined in the P-Mode
            final EndpointReference targetEPR = new EndpointReference("http://holodeck-b2b.org/transport/dummy");
            final Options options = new Options();
            options.setTo(targetEPR);
            options.setExceptionToBeThrownOnSOAPFault(false);
            oc.setOptions(options);

            final HttpClient httpClient = new HttpClient();
            httpClient.getParams().setIntParameter(HttpConnectionManagerParams.CONNECTION_TIMEOUT, 5000);
            msgCtx.setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpClient);
            log.debug("Axis2 client configured for sending ebMS message");
        } catch (final AxisFault af) {
            // Setting up the Axis environment failed. As it prevents sending the message it is logged as a fatal error
            log.fatal("Setting up Axis2 to send message failed! Details: " + af.getReason());
            return;
        }

        try {
            log.debug("Start the message send process");
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

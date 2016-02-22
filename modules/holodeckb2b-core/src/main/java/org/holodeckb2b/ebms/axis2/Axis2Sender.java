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
package org.holodeckb2b.ebms.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import static org.apache.axis2.client.ServiceClient.ANON_OUT_IN_OP;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.holodeckb2b.axis2.Axis2Utils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.persistency.entities.ErrorMessage;
import org.holodeckb2b.ebms3.persistency.entities.MessageUnit;
import org.holodeckb2b.ebms3.persistency.entities.PullRequest;
import org.holodeckb2b.ebms3.persistency.entities.Receipt;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.module.HolodeckB2BCoreImpl;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Axis2Sender {

    /**
     * Send the message unit to the other MSH. 
     * 
     * @param message   The MessageUnit to send 
     * @param log       The log to use for writing log information
     */
    public static void sendMessage(EntityProxy msgProxy, Log log) {
        ServiceClient sc;
        OperationClient oc;

        MessageUnit message = msgProxy.entity;
        try {
            log.debug("Prepare Axis2 client to send " + message.getClass().getSimpleName());
            sc = new ServiceClient(HolodeckB2BCoreInterface.getConfiguration().getAxisConfigurationContext(), 
                                   Axis2Utils.createAnonymousService());
            sc.engageModule(HolodeckB2BCoreImpl.HOLODECKB2B_CORE_MODULE);
            oc = sc.createClient(ANON_OUT_IN_OP);

            log.debug("Create an empty MessageContext for message with current configuration");
            MessageContext msgCtx = new MessageContext();

            if (message instanceof UserMessage) {
                log.debug("Message to send is a UserMessage");
                msgCtx.setProperty(MessageContextProperties.OUT_USER_MESSAGE, msgProxy);
            } else if (message instanceof PullRequest) {
                log.debug("Message to send is a PullRequest");
                msgCtx.setProperty(MessageContextProperties.OUT_PULL_REQUEST, msgProxy);
            } else if (message instanceof ErrorMessage) {
                log.debug("Message to send is a ErrorMessage");
                MessageContextUtils.addErrorSignalToSend(msgCtx, msgProxy);
            } else if (message instanceof Receipt) {
                log.debug("Message to send is a Receipt");
                MessageContextUtils.addReceiptToSend(msgCtx, msgProxy);
            }
            oc.addMessageContext(msgCtx);

            // This dummy EPR has to be provided to be able to trigger message sending. It will be replaced later
            // with the correct URL defined in the P-Mode
            EndpointReference targetEPR = new EndpointReference("http://holodeck-b2b.org/transport/dummy");
            Options options = new Options();
            options.setTo(targetEPR);
            options.setExceptionToBeThrownOnSOAPFault(false);
            oc.setOptions(options);

            log.debug("Axis2 client configured for sending ebMS message");
        } catch (AxisFault af) {
            // Setting up the Axis environment failed. As it prevents sending the message it is logged as a fatal error 
            log.fatal("Setting up Axis2 to send message failed! Details: " + af.getReason());
            return;
        }

        try {
            log.debug("Start the message send process");
            oc.execute(true);
        } catch (AxisFault af) {
            // An error occurred while sending the message, 
            log.error("An unexpected error occurred while sending the " + message.getClass().getSimpleName()
                        + " with msg-id: [" + message.getMessageId() + "] Details: " + af.getReason());
        } finally {
            try {
                sc.cleanupTransport();
                sc.cleanup();
            } catch (AxisFault af2) {
                log.error("Clean up of Axis2 context to send message failed! Details: " + af2.getReason());
            }
        }
    }
    
}

/*
 * Copyright (C) 2009, 2012 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.workers;

import java.util.List;
import java.util.Map;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.config.Config;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.common.workerpool.TaskConfigurationException;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.MessageContextUtils;

/**
 * Is responsible for starting the send process of User Message message units. It 
 * looks for all messages waiting in the database to get send and starts an Axis2
 * client for each of them. The ebMS specific handlers in the Axis2 handler chain
 * will then take over and do the actual message processing. This worker is only
 * to kick-off the process.
 * <p>This worker does not need configuration to run. As this worker is needed 
 * for Holodeck B2B to work properly it is included in the default worker pool. 
 * 
 * @author Sander Fieten
 */
public class SenderWorker extends AbstractWorkerTask {

    private static final Log log = LogFactory.getLog(SenderWorker.class.getName());

    /**
     * Looks for message units that are for sending and kicks off the send process
     * for each of them. To prevent a message from being send twice the send process
     * is only started if the processing state can be succesfully changed.
     */
    @Override
    public void doProcessing() {
        try {
            log.debug("Getting list of user messages to send");

            List<MessageUnit> newMsgs = MessageUnitDAO.getMessageUnitsInState(MessageUnit.class,
                                                                        new String[] {ProcessingStates.READY_TO_PUSH});

            if (newMsgs != null && newMsgs.size() > 0) {
                log.info("Found " + newMsgs.size() + " messages to send");
                for (MessageUnit message : newMsgs) {
                    // Indicate that processing will start
                    MessageUnit muInProcess = MessageUnitDAO.startProcessingMessageUnit(message);
                    if (muInProcess != null) {
                        // only when we could succesfully set processing state really start processing
                        log.debug("Start processing message [" + muInProcess.getMessageId() + "]");
                        send(muInProcess);
                        log.info("Successfully processed message [" + muInProcess.getMessageId() + "]");
                    }else {
                        // Message probably already in process
                        log.debug("Could not start processing message [" + message.getMessageId() 
                                    + "] because switching to processing state was unsuccesful");
                    }
                }
            } else
                log.info("No messages found that are ready for sending");
        } catch (DatabaseException dbError) {
            log.error("Could not process message because a database error occurred. Details:" 
                        + dbError.toString() + "\n");
        }
    }

    /*
     * Helper method to start the send process. The actual ebMS processing will take place 
     * in the specific handlers. 
     */
    private void send(MessageUnit message) {
        ServiceClient sc;
        OperationClient oc;
            
        try {
            log.debug("Prepare Axis2 client to send message");
            sc = new ServiceClient(Config.getAxisConfigurationContext(), null);
            sc.engageModule(Constants.HOLODECKB2B_CORE_MODULE);
            oc = sc.createClient(ServiceClient.ANON_OUT_IN_OP);
            
            log.debug("Create an empty MessageContext for message with current configuration");
            MessageContext msgCtx = new MessageContext();
            if (message instanceof UserMessage) {
                log.debug("Message to send is a UserMessage");
                msgCtx.setProperty(MessageContextProperties.OUT_USER_MESSAGE, message);
            } else if (message instanceof ErrorMessage) {
                log.debug("Message to send is a ErrorMessage");
                MessageContextUtils.addErrorSignalToSend(msgCtx, (ErrorMessage) message);
            } else if (message instanceof Receipt) {
                log.debug("Message to send is a Receipt");
                MessageContextUtils.addReceiptToSend(msgCtx, (Receipt) message);                
            }   
            oc.addMessageContext(msgCtx);
            
            EndpointReference targetEPR = new EndpointReference("http://holodeck-b2b.org/transport/dummy");
            Options options = new Options();
            options.setTo(targetEPR);
            oc.setOptions(options);
            
            log.debug("Axis2 client configured for sending ebMS message");
            
        } catch (AxisFault af) {
            // Setting up the Axis environment failed. Return processing state to SUBMITTED so that it will be resend
            // Signal this in the log as a fatal error
            log.fatal("Setting up Axis2 to send message failed! Details: " + af.getReason());
            //@todo: Message has to be resend after some delay
            
            return;
        }
        
        try {
            log.debug("Start the message send process");
            oc.execute(true);
            
        } catch (AxisFault af) {
            // An error occurred while sending the message, 
            
        } finally {
            try { 
                sc.cleanup();
            } catch (AxisFault af) {
                
            }
        }
    }

    /**
     * This worker does not take any configuration. Therefor the implementation of this method is empty.
     */
    @Override
    public void setParameters(Map<String, ?> parameters) throws TaskConfigurationException {
    }
}
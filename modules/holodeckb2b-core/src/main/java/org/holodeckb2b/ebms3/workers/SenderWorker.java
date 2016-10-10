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
package org.holodeckb2b.ebms3.workers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.ebms3.axis2.Axis2Sender;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistency.entities.ErrorMessage;
import org.holodeckb2b.ebms3.persistency.entities.Receipt;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Is responsible for starting the send process of message units. It looks for all messages waiting in the database to
 * get send and starts an Axis2 client for each of them. The ebMS specific handlers in the Axis2 handler chain will then
 * take over and do the actual message processing. This worker is only to kick-off the process.
 * <p>This worker does not need configuration to run. As this worker is needed for Holodeck B2B to work properly it is
 * included in the default worker pool.
 *
 * @author Sander Fieten
 */
public class SenderWorker extends AbstractWorkerTask {

    private static final Log log = LogFactory.getLog(SenderWorker.class.getName());

    /**
     * Looks for message units that are for sending and kicks off the send process
     * for each of them. To prevent a message from being send twice the send process
     * is only started if the processing state can be successfully changed.
     */
    @Override
    public void doProcessing() {
        try {
            log.debug("Getting list of message units to send");
            final List<EntityProxy> newMsgs = new ArrayList<>();
            // Add all User Messages waiting to be sent
            newMsgs.addAll(MessageUnitDAO.getSentMessageUnitsInState(UserMessage.class,
                                                                 new String[] {ProcessingStates.READY_TO_PUSH}));
            // Add all Receipts waiting to be sent
            newMsgs.addAll(MessageUnitDAO.getSentMessageUnitsInState(Receipt.class,
                                                                 new String[] {ProcessingStates.READY_TO_PUSH}));
            // Add all Errors waiting to be sent
            newMsgs.addAll(MessageUnitDAO.getSentMessageUnitsInState(ErrorMessage.class,
                                                                 new String[] {ProcessingStates.READY_TO_PUSH}));

            if (!Utils.isNullOrEmpty(newMsgs)) {
                log.info("Found " + newMsgs.size() + " message units to send");
                for (final EntityProxy message : newMsgs) {
                    // Indicate that processing will start
                    if (MessageUnitDAO.startProcessingMessageUnit(message)) {
                        // only when we could succesfully set processing state really start processing
                        log.debug("Start processing message [" + message.entity.getMessageId() + "]");
                        Axis2Sender.sendMessage(message, log);
                    } else
                        // Message probably already in process
                        log.debug("Could not start processing message [" + message.entity.getMessageId()
                                    + "] because switching to processing state was unsuccesful");
                }
            } else
                log.info("No messages found that are ready for sending");
        } catch (final DatabaseException dbError) {
            log.error("Could not process message because a database error occurred. Details:"
                        + dbError.toString() + "\n");
        }
        catch (final Throwable t) {
            log.error ("Internal error in SenderWorker", t);
            return;
        }
    }

    /**
     * This worker does not take any configuration. Therefor the implementation of this method is empty.
     */
    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
    }
}
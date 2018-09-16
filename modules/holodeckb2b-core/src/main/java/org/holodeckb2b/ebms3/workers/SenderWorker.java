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

import java.util.List;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.ebms3.axis2.Axis2Sender;
import org.holodeckb2b.ebms3.axis2.ebMS3SendService;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.module.HolodeckB2BCore;

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
            List<IMessageUnitEntity> msgUnitsToSend = HolodeckB2BCore.getQueryManager()
                                                               .getMessageUnitsInState(IMessageUnit.class,
                                                                Direction.OUT,
                                                                new ProcessingState[] {ProcessingState.READY_TO_PUSH});

            if (!Utils.isNullOrEmpty(msgUnitsToSend)) {
                log.info("Found " + msgUnitsToSend.size() + " message units to send");
                for (final IMessageUnitEntity msgUnit : msgUnitsToSend) {
                    // Only message units associated with a P-Mode can be send
                    if (Utils.isNullOrEmpty(msgUnit.getPModeId())) {
                        log.error("Can not sent message [" + msgUnit.getMessageId()
                                    + "] because it has no associated P-Mode");
                        HolodeckB2BCore.getStorageManager().setProcessingState(msgUnit, ProcessingState.FAILURE);
                        continue;
                    }

                    // Indicate that processing will start
                    if (HolodeckB2BCore.getStorageManager().setProcessingState(msgUnit, ProcessingState.READY_TO_PUSH,
                                                                              ProcessingState.PROCESSING)) {
                        // only when we could succesfully set processing state really start processing
                        log.debug("Start processing " + MessageUnitUtils.getMessageUnitName(msgUnit)
                                    + "[" + msgUnit.getMessageId() + "]");
                        // Ensure all data is available for processing
                        HolodeckB2BCore.getQueryManager().ensureCompletelyLoaded(msgUnit);
                        Axis2Sender.sendMessage(msgUnit, new ebMS3SendService(), log);
                    } else
                        // Message probably already in process
                        log.debug("Could not start processing message [" + msgUnit.getMessageId()
                                    + "] because switching to processing state was unsuccesful");
                }
            } else
                log.info("No messages found that are ready for sending");
        } catch (final PersistenceException dbError) {
            log.error("Could not process message because a database error occurred. Details:"
                        + dbError.toString() + "\n");
        } catch (AxisFault sendFailure) {
            log.error("An unhandled error occurred when sending a message. Details:" + sendFailure.getMessage());
		}
    }

    /**
     * This worker does not take any configuration. Therefor the implementation of this method is empty.
     */
    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
    }
}

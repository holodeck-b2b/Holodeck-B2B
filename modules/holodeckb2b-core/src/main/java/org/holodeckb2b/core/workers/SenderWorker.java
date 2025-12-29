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
package org.holodeckb2b.core.workers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.common.workers.AbstractWorkerTask;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.axis2.Axis2Sender;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.StorageException;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Is responsible for selecting the message units to be send. It looks for all messages waiting in the database to
 * get send and starts the send process for each of them.
 * <p>This worker does not need configuration to run. As this worker is needed for Holodeck B2B to work properly it is
 * included in the default worker pool.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SenderWorker extends AbstractWorkerTask {

    private static final Logger log = LogManager.getLogger(SenderWorker.class.getName());

    /**
     * Looks for message units that are for sending and kicks off the send process
     * for each of them. To prevent a message from being send twice the send process
     * is only started if the processing state can be successfully changed.
     */
    @Override
    public void doProcessing() {
        try {
            log.trace("Getting list of message units to send");
            List<IMessageUnitEntity> msgUnitsToSend = HolodeckB2BCore.getQueryManager()
                                                               .getMessageUnitsInState(IMessageUnit.class,
                                                                Direction.OUT,
                                                                Set.of(ProcessingState.READY_TO_PUSH));

            if (!Utils.isNullOrEmpty(msgUnitsToSend)) {
                log.trace("Found {} message units to send",  msgUnitsToSend.size());
                for (final IMessageUnitEntity msgUnit : msgUnitsToSend) {
                    // Only message units associated with a P-Mode can be send
                    if (Utils.isNullOrEmpty(msgUnit.getPModeId())) {
                        log.error("Can not sent message [{}] because it has no P-Mode", msgUnit.getMessageId());
                        HolodeckB2BCore.getStorageManager().setProcessingState(msgUnit, ProcessingState.FAILURE);
                        continue;
                    }

                    // Indicate that processing will start
                    if (HolodeckB2BCore.getStorageManager().setProcessingState(msgUnit, ProcessingState.PROCESSING)) {
                    	// only when we could succesfully set processing state really start processing
                        log.trace("Trigger send process for {} [{}]", MessageUnitUtils.getMessageUnitName(msgUnit),
                        			msgUnit.getMessageId());
                        Axis2Sender.sendMessage(msgUnit);
                    } else
                        // Message probably already in process
                        log.trace("Could not start sending [{}] because processing state was already changed",
                        		  msgUnit.getMessageId());
                }
            }
        } catch (final StorageException dbError) {
            log.error("Could not process messages because a database error occurred. Details: {}",
                        Utils.getExceptionTrace(dbError));
        }
    }

    /**
     * This worker does not take any configuration. Therefor the implementation of this method is empty.
     */
    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
    }
}

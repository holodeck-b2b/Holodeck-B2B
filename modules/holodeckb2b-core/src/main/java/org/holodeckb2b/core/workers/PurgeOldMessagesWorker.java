/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import org.holodeckb2b.common.events.impl.MessageUnitPurged;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.events.IMessageUnitPurged;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;

/**
 * Is the default <i>purge worker</i> responsible for cleaning up information on old and processed messages, i.e. remove
 * the meta-data information from the database and delete associated payloads from the file system.
 * <p>Currently only the number of days after which the message information should be removed can be configured. This is
 * done through the optional <i>purgeAfterDays</i> parameter. If not specified 30 days is used as the default setting.
 * <p>This implementation will trigger {@link IMessageUnitPurged} events only for <i>User Message</i> message units and
 * it will only provide the meta-data to the event handler. The payload data associated with the User Message message
 * unit will already be deleted by the worker.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PurgeOldMessagesWorker extends AbstractWorkerTask {

    /**
     * Name of the configuration parameter that must be used to set the number of days after which message information
     * should be purged.
     */
    public static final String P_PURGE_AFTER_DAYS = "purgeAfterDays";

    /**
     * The number of days after which message information will be purged
     */
    private int purgeAfterDays;

    @Override
    public void doProcessing() throws InterruptedException {
        final IQueryManager queryManager = HolodeckB2BCore.getQueryManager();
        Collection<IMessageUnitEntity> experidMsgUnits = null;

        // Calculate the experition time
        final Calendar expirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.DAY_OF_YEAR, -purgeAfterDays);
        final String expDateString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS.sss").format(expirationDate.getTime());

        try {
            log.trace("Get all message units that changed state before " + expDateString);
             experidMsgUnits = queryManager.getMessageUnitsWithLastStateChangedBefore(expirationDate.getTime());
        } catch (final PersistenceException dbe) {
            log.error("Could not get the list of expired message units from database! Error details: "
                     + dbe.getMessage());
        }
        if (Utils.isNullOrEmpty(experidMsgUnits)) {
            log.trace("No expired message unist found, nothing to do");
            return;
        }

        log.debug("Removing " + experidMsgUnits.size() + " expired message units.");
        for(final IMessageUnitEntity msgUnit : experidMsgUnits) {
            log.trace("Removing " + MessageUnitUtils.getMessageUnitName(msgUnit) + " with msgId: " 
            			+ msgUnit.getMessageId());
            try {
            	// Complete loading is needed as it is not done when querying
            	log.trace("Load all meta-data of message unit");
                queryManager.ensureCompletelyLoaded(msgUnit);
                // If the message unit to delete is a User Message we need to have a temp object so we can change the
                // payload location info as we may need to trigger purge event.
                UserMessage tmpUserMessage = null;
                if (msgUnit instanceof IUserMessage) {
                    log.trace("Delete the payload data of the User Message");
                    tmpUserMessage = new UserMessage((IUserMessage) msgUnit);
                    final Collection<IPayload> payloads = tmpUserMessage.getPayloads();
                    if (!Utils.isNullOrEmpty(payloads)) {
                        for (final IPayload pl : payloads) {
                            if (Utils.isNullOrEmpty(pl.getContentLocation()))
                                log.trace("No payload location provided for payload [" + pl.getPayloadURI() + "]");
                            else {
                                final File plFile = new File(pl.getContentLocation());
                                if (plFile.exists() && plFile.delete()) {
                                    log.trace("Removed payload data file " + pl.getContentLocation());
                                    // Clear the payload location
                                    ((Payload) pl).setContentLocation(null);
                                }  else if (plFile.exists())
                                    log.error("Could not remove payload data file " + pl.getContentLocation()
                                                + ". Remove manually");
                            }
                        }
                    } else
                        log.trace("User Message [" + msgUnit.getMessageId() + "] has no payloads");
                }
                log.trace("Removing meta-data of message unit [" + msgUnit.getMessageId() + "] from database.");
                HolodeckB2BCore.getStorageManager().deleteMessageUnit(msgUnit);
                log.debug(MessageUnitUtils.getMessageUnitName(msgUnit) + " [msgId=" + msgUnit.getMessageId() 
                			+ "] is removed");

                // Raise event so extension can process purge actions (for User Messages only)
                if (msgUnit instanceof IUserMessage)
                    HolodeckB2BCore.getEventProcessor().raiseEvent(new MessageUnitPurged(tmpUserMessage));
            } catch (final PersistenceException dbe) {
                log.error("Could not remove the meta-data of " + MessageUnitUtils.getMessageUnitName(msgUnit)
                        + " [msgId=" + msgUnit.getMessageId() + "]. Error details: " + dbe.getMessage());
            }
        }
    }

    /**
     * Configures the worker by setting the number of days after which messages should be purged using the
     * <i>purgeAfterDays</i> parameter. If not specified 30 days is used as the default setting.
     *
     * @param parameters    A <code>Map</code> containing the configuration of the worker
     */
    @Override
    public void setParameters(final Map<String, ?> parameters) {
        if (Utils.isNullOrEmpty(parameters))
            // No parameters given, use default
            purgeAfterDays = 30;
        else {
            final Object pPurgeDays = parameters.get(P_PURGE_AFTER_DAYS);
            if (pPurgeDays == null)
                // No "purgeAfterDays" parameter given, use default
                purgeAfterDays = 30;
            else {
                try {
                    purgeAfterDays = Integer.parseInt(pPurgeDays.toString());
                } catch (final NumberFormatException NaN) {
                    // Could not convert the given value for "purgeAfterDays" parameter to a int, used default
                    log.warn("Illegal value [" + pPurgeDays.toString() + "] used for \""
                                + P_PURGE_AFTER_DAYS + "\" parameter! Using default.");
                    purgeAfterDays = 30;
                }
            }
        }
        log.info("Message information will be deleted after " + purgeAfterDays + " days.");
    }

}

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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.common.workers.AbstractWorkerTask;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.core.IQueryManager;
import org.holodeckb2b.interfaces.events.IMessageUnitPurged;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

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
        } catch (final StorageException dbe) {
            log.error("Could not get the list of expired message units from database! Error details: "
                     + dbe.getMessage());
        }
        if (Utils.isNullOrEmpty(experidMsgUnits)) {
            log.trace("No expired message unist found, nothing to do");
            return;
        }

        log.debug("Removing {} expired message units", experidMsgUnits.size());
        for(final IMessageUnitEntity msgUnit : experidMsgUnits) {
        	try {
        		HolodeckB2BCore.getStorageManager().deleteMessageUnit(msgUnit);
            } catch (final StorageException dbe) {
                log.error("Could not remove data of {} (msgId={})", MessageUnitUtils.getMessageUnitName(msgUnit), 
                			msgUnit.getMessageId());
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

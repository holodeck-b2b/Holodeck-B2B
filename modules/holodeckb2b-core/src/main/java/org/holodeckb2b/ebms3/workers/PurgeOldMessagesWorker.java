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
package org.holodeckb2b.ebms3.workers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.ebms3.persistency.entities.MessageUnit;
import org.holodeckb2b.ebms3.persistency.entities.Payload;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.events.MessageUnitPurgedEvent;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.types.IMessageUnitPurgedEvent;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is the default <i>purge worker</i> responsible for cleaning up information on old and processed messages, i.e. remove
 * the meta-data information from the database and delete associated payloads from the file system.
 * <p>Currently the worker has two optional parameters that set the number of days after which the message information
 * should be removed. The first parameter, <i>purgeAfterDays</i> sets the default purge time for all message units. If
 * not specified 30 days is used as the default setting. Using the second parameter, <i>purgePullDataAfter</i> a
 * different period can be set for <i>Pull Request</i> and related "EmptyMPC" <i>Error Message</i> message units. If not
 * set the same period as for the other message units will be used.
 * <p>This implementation will trigger {@link IMessageUnitPurgedEvent}s only for <i>User Message</i> message units and
 * it will only provide the meta-data to the event handler. The payload data associated with the User Message message
 * unit will already be deleted by the worker.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PurgeOldMessagesWorker extends AbstractWorkerTask {

    /**
     * Name of the configuration parameter that must be used to set the number of days after which message information
     * should be purged.
     */
    public static final String P_PURGE_AFTER_DAYS = "purgeAfterDays";
    /**
     * Name of the configuration parameter that must be used to set the number of days after which message information
     * related to pull requests should be purged.
     */
    public static final String P_PURGE_PULL_AFTER_DAYS = "purgePullDataAfter";

    /**
     * The number of days after which message information will be purged by default
     */
    private int purgeAfterDays;
    /**
     * The number of days after which message information related to pulling will be purged
     */
    private int purgePullAfterDays;

    @Override
    public void doProcessing() throws InterruptedException {
        List<EntityProxy<MessageUnit>> experidMsgUnits = null;

        final Calendar expirationDate = Calendar.getInstance();
        final Calendar pullExpirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.DAY_OF_YEAR, -purgeAfterDays);
        String expDateString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS.sss").format(expirationDate.getTime());
        try {
            log.debug("Get all message units that changed state before " + expDateString);
            experidMsgUnits = MessageUnitDAO.getMessageUnitsLastChangedBefore(expirationDate.getTime());

        } catch (final DatabaseException dbe) {
            log.error("Could not get the list of expired message units from database! Error details: "
                     + dbe.getMessage());
        }
        if (purgePullAfterDays != purgeAfterDays) {
            pullExpirationDate.add(Calendar.DAY_OF_YEAR, -purgePullAfterDays);
            expDateString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS.sss").format(expirationDate.getTime());
            try {
                log.debug("Get all pull message units that changed state before " + expDateString);
                experidMsgUnits.addAll(MessageUnitDAO.getPullingMessageUnitsLastChangedBefore(
                                                                                    pullExpirationDate.getTime()));
            } catch (final DatabaseException dbe) {
                log.error("Could not get the list of expired pull message units from database! Error details: "
                        + dbe.getMessage());
            }
        }
        if (Utils.isNullOrEmpty(experidMsgUnits)) {
            log.debug("No expired message units found, nothing to do");
            return;
        }

        log.debug("Removing " + experidMsgUnits.size() + " expired message units.");
        int u = 0, p = 0, e = 0, r = 0;
        for(final EntityProxy<MessageUnit> ep : experidMsgUnits) {
            final MessageUnit mu = ep.entity;
            log.debug("Removing " + MessageUnitUtils.getMessageUnitName(mu) + " with msgId: " + mu.getMessageId());
            try {
                if (mu instanceof UserMessage) {
                    log.debug("Delete the payload data of the User Message");
                    // Complete loading is needed as it is not done when querying
                    MessageUnitDAO.loadCompletely(ep);
                    final Collection<IPayload> payloads = ((UserMessage) ep.entity).getPayloads();
                    if (!Utils.isNullOrEmpty(payloads)) {
                        for (final IPayload pl : payloads) {
                            if (Utils.isNullOrEmpty(pl.getContentLocation()))
                                log.debug("No payload location provided for payload [" + pl.getPayloadURI() + "]");
                            else {
                                final File plFile = new File(pl.getContentLocation());
                                if (plFile.exists() && plFile.delete()) {
                                    log.debug("Removed payload data file " + pl.getContentLocation());
                                    // Clear the payload location
                                    ((Payload) pl).setContentLocation(null);
                                }  else if (plFile.exists())
                                    log.error("Could not remove payload data file " + pl.getContentLocation()
                                                + ". Remove manually");
                            }
                        }
                    } else
                        log.debug("User Message [" + mu.getMessageId() + "] has no payloads");
                }
                // Remove meta-data from database
                MessageUnitDAO.deleteMessageUnit(ep);
                if (mu instanceof IUserMessage)
                    u++;
                else if (mu instanceof IPullRequest)
                    p++;
                else if (mu instanceof IReceipt)
                    r++;
                else
                    e++;
                log.debug(MessageUnitUtils.getMessageUnitName(mu) + " [msgId=" + mu.getMessageId() + "] is removed");

                // Raise event so extension can process purge actions (for User Messages only)
                if (mu instanceof UserMessage)
                    HolodeckB2BCoreInterface.getEventProcessor().raiseEvent(new MessageUnitPurgedEvent(mu), null);
            } catch (final DatabaseException dbe) {
                log.error("Could not remove the meta-data of " + MessageUnitUtils.getMessageUnitName(mu)
                        + " [msgId=" + mu.getMessageId() + "]. Error details: " + dbe.getMessage());
            }
        }
        log.info("Completed purge process. Removed " + u + " User Messages, " + p + " Pull Requests, "
                + r + " Receipts and " + e + " Error Message");
    }

    /**
     * Configures the worker by setting the number of days after which messages should be purged using the
     * <i>purgeAfterDays</i> parameter. If not specified 30 days is used as the default setting.
     *
     * @param parameters    A <code>Map</code> containing the configuration of the worker
     */
    @Override
    public void setParameters(final Map<String, ?> parameters) {
        if (Utils.isNullOrEmpty(parameters)) {
            // No parameters given, use default
            purgeAfterDays = 30;
            purgePullAfterDays = 30;
        } else {
            Object pPurgeDays = parameters.get(P_PURGE_AFTER_DAYS);
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
            pPurgeDays = parameters.get(P_PURGE_PULL_AFTER_DAYS);
            if (pPurgeDays == null)
                // No "purgePullDataAfter" parameter given, use default
                purgePullAfterDays = purgeAfterDays;
            else {
                try {
                    purgePullAfterDays = Math.min(Integer.parseInt(pPurgeDays.toString()), purgeAfterDays);
                } catch (final NumberFormatException NaN) {
                    // Could not convert the given value for "purgePullDataAfter" parameter to a int, used default
                    log.warn("Illegal value [" + pPurgeDays.toString() + "] used for \""
                                + P_PURGE_PULL_AFTER_DAYS + "\" parameter! Using default.");
                    purgePullAfterDays = purgeAfterDays;
                }
            }
        }
        if (purgePullAfterDays != purgeAfterDays)
            log.info("Pull related message will de deleted after " + purgePullAfterDays
                     + "days. Other message units after " + purgeAfterDays + " days.");
        else
            log.info("Message information will be deleted after " + purgeAfterDays + " days.");
    }

}

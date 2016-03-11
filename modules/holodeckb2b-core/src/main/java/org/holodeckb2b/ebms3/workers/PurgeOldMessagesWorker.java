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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.ebms3.constants.ProcessingStates;

/**
 * Is responsible for cleaning up information on old and processed messages, i.e. remove the information from the 
 * database and delete associated payloads from the file system (if not done already by another process).
 * <p>Currently only the number of days after which the message information should be removed can be configured. This is
 * done through the optional <i>purgeAfterDays</i> parameter. If not specified 30 days is used as the default setting.
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
     * The set of processing states that are considered "final", i.e. indicate when message processing has been 
     * completed and message information can be safely removed.
     */
    private static Set<String>   FINAL_STATES;
    static {
        Set<String> aSet = new HashSet<String>();
        aSet.add(ProcessingStates.DELIVERED);
        aSet.add(ProcessingStates.FAILURE);
        FINAL_STATES = Collections.unmodifiableSet(aSet);
    }

    /**
     * The number of days after which message information will be purged
     */
    private int purgeAfterDays;        
    
    @Override
    public void doProcessing() throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Configures the worker by setting the number of days after which messages should be purged using the 
     * <i>purgeAfterDays</i> parameter. If not specified 30 days is used as the default setting.
     * 
     * @param parameters    A <code>Map</code> containing the configuration of the worker
     */
    @Override
    public void setParameters(Map<String, ?> parameters) {
        if (Utils.isNullOrEmpty(parameters)) 
            // No parameters given, use default
            purgeAfterDays = 30;
        else {
            Object pPurgeDays = parameters.get(P_PURGE_AFTER_DAYS);
            if (pPurgeDays == null)
                // No "purgeAfterDays" parameter given, use default
                purgeAfterDays = 30;
            else {
                try { 
                    purgeAfterDays = Integer.parseInt(pPurgeDays.toString());                    
                } catch (NumberFormatException NaN) {
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

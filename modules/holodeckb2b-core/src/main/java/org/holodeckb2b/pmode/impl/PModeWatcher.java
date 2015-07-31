/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.pmode.impl;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workers.DirWatcher;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 *
 *
 * @see PMode
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PModeWatcher extends DirWatcher {

    /**
     * Internal mapping from filename to P-Mode id for keeping track of changes. We need to have a reference to deleted
     * files in order to keep track of what has changed. Since if PMode definition file is gone from disk there is
     * nothing more to see or known for this file.
     */
    protected Map<String, String> fileToPModeMap = new HashMap<String, String>();

    /**
     * List of new P-Mode files
     */
    protected List<File> newPModes = null;
    /**
     * List of modified P-Mode files
     */
    protected List<File> chgPModes = null;
    /**
     * List of deleted P-Mode files
     */
    protected List<File> delPModes = null;

    /**
     * Prepares for processing all changes in the P-Mode files. Because a P-Mode change can be done either by changing
     * the file as well as deleting the old version and adding a new file we first collect all changes and then process
     * them.
     */
    @Override
    protected void doPreProcessing() {
        newPModes = new LinkedList<File>();
        chgPModes = new LinkedList<File>();
        delPModes = new LinkedList<File>();
    }

    /**
     * On change controller, called when a PMode definition file has changed. This method only stores the change in the
     * correct list, the actual processing of changes is done in {@link #doPostProcessing()}
     *
     * @param f is the file which is changed.
     * @param event is the event that occurred on the file f.
     */
    @Override
    protected void onChange(File f, Event event) {
        switch (event) {
            case ADDED :
                newPModes.add(f); break;
            case CHANGED : 
                chgPModes.add(f); break;
            case REMOVED :
                delPModes.add(f);
        }
    }

    /**
     * Processes all the changes, first the removed files, then the new files and last the changed ones.
     */
    @Override
    protected void doPostProcessing() {
        // Process removed P-Mode files
        for (File f : delPModes) {
            String pmodeId = removePMode(f);
            log.info("Removed P-Mode " + (pmodeId != null ? "[" + pmodeId + "] " : "") 
                        + " contained in '" + f.getName()+ "'");
        }
        // Process new P-Mode files
        for (File f : newPModes) {
            String pmodeId = addPMode(f);
            if (pmodeId != null) 
                log.info("Added P-Mode [" + pmodeId + "] from file '" + f.getName()+ "'");
        }
        // Process changed P-Mode files
        for (File f : chgPModes) {
            /* When the file has changed it does not necessarily mean that the P-Mode contained in the original file has 
              changed. It is also possible that the changed file contains a completely new P-Mode. Therefore the changed
              file is processed by first removed the old P-Mode and then adding a new one.
              Note that this can result in removal of a P-Mode if the new file contains a modified version with an error
            */
            String oldPModeId = removePMode(f);
            String newPModeId = addPMode(f);
            if (newPModeId == null) {
                log.error("The P-Mode originally contained in '" + f.getName() + "' with ID [" + oldPModeId 
                            + "] is removed, but the new P-Mode could not be loaded!");
            } else if (newPModeId.equals(oldPModeId)) 
                log.info("Loaded changed P-Mode [" + newPModeId + "] from file '" + f.getName()+ "'");
            else     
                log.info("P-Mode [" + oldPModeId + "] originally contained in '" + f.getName() + "' is replaced by" 
                            + " new P-Mode with ID [" + newPModeId + "]");        
        }
    }

    /**
     * Handles a new PMode file.
     *
     * @param f The file containing the PMode definition in XML.
     * @return String containing the P-Mode Id of the new P-Mode if it is successfully added to the P-Mode set,<br>
     *         <code>null</code> if the P-Mode could not be added
     */
    protected String addPMode(File f) {
        // Get the PMode from the file, based on file f
        PMode pmode = new PMode();
        try {
            pmode = pmode.createFromFile(f);
        } catch (Exception ex) {
            // The XML contained an error and could not be tranformed into a PMode object
            //
            log.error("PMode from '" + f.getAbsolutePath() + " could not be read." 
                        + " Error details: " + ex.getMessage());            
            return null;
        }

        // Get the current set of P-Modes. 
        // Check if Holodeck B2B has just started and we do not have any P-Mode yet in the internal reference. In that
        // case create the P-Mode set.
        PModeSet pms = null;
        if (fileToPModeMap.isEmpty()) {
            pms = new PModeSet();
        } else {
            // get the current PModeSet from the Holodeck core.
            pms = (PModeSet) HolodeckB2BCore.getPModeSet();
        }

        // we have a new file which is previously unknown to the system
        // check if new PMode is currently known based on it's PMode-ID.
        String pmodeId = pmode.getId();
        if (this.fileToPModeMap.containsValue(pmodeId)) {
            // There is already a file that containing a P-Mode with the same id. P-Mode.ids must however be unique! 
            // Log error about this and stop processing this file
            log.error("DUPLICATE P-Mode.ID detected! The P-Mode.ID ["+ pmodeId + "] of P-Mode in '" 
                        + f.getAbsolutePath() + "' is also registered in '" 
                        + Utils.getKeyByValue(fileToPModeMap, pmodeId) + "'!");
            return null;
        } else {
            // Add the new P-Mode to the set and mapping
            pms.set(pmode.getId(), pmode);
            // pass the new PModeSet to the Holodeck core
            HolodeckB2BCore.setPModeSet(pms);
            
            // add new entry to the internal mapping of this class
            fileToPModeMap.put(f.getAbsolutePath(), pmode.getId());
            
            return pmodeId;
        }
    }

    /**
     * Handles a removed PMode file.
     *
     * @param f Reference to the removed file. Note that this file no longer exists and the reference can not be used
     *          for access the file.
     * @return The id of the removed P-Mode, can be <code>null</code> if the P-Mode was already removed earlier.
     */
    protected String removePMode(File f) {
        // get the PModeID from the internal mapping (since the file which contained the PModeID is removed from disk).
        String PModeID = fileToPModeMap.get(f.getAbsolutePath());
        if ((PModeID != null) && (!PModeID.isEmpty())) {
            // remove the P-Mode from both set and mapping
            // get the current PModeSet from the Holodeck core.
            PModeSet pms = (PModeSet) HolodeckB2BCore.getPModeSet();
            // remove existing entry from PModeSet
            pms.remove(PModeID);
            // pass the new PModeSet to the Holodeck B2B core
            HolodeckB2BCore.setPModeSet(pms);

            fileToPModeMap.remove(f.getAbsolutePath());
        } 

        return PModeID;
    }

}

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
import java.util.Map;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workers.DirWatcher;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class PModeWatcher extends DirWatcher {


    /**
     * Internal mapping from pmodeID to filename for keeping track of changes, we need to have a reference to deleted
     * files in order to keep track of what has changed. Since if PMode definition file is gone from disk there is
     * nothing more to see or known for this file.
     */
    protected Map<String, String> pModeIDToFilenameMapping = new HashMap<String, String>();

    /**
     * On change controller, called when a PMode definition file has changed.
     *
     * @param f is the file which is changed.
     * @param event is the event that occurred on the file f.
     */
    @Override
    protected void onChange(File f, Event event) {

        if (event == Event.ADDED) {
            onChangeAdd(f);
        }

        if (event == Event.CHANGED) {
            onChangeChanged(f);
        }

        if (event == Event.REMOVED) {
            onChangeRemove(f);
        }

    }

    /**
     * Handle the PMode add event.
     * New file describing the PMode found on file system.
     *
     * @param f XML file containing the PMode definition in XML.
     */
    protected void onChangeAdd(File f) {

        try {
            PModeSet pms = null;

            // Proces new PMode xml file(s).
            log.debug("New PMode file '" + f.getName() + "' found.");

            // get the PMode from the file, based on file f
            PMode pm = new PMode();
            pm = pm.createFromFile(f);

            // Check if Holodeck B2B has just started and we do not have any P-Mode yet
            // in the internal reference
            if (this.pModeIDToFilenameMapping.isEmpty()) {
                pms = new PModeSet();
            } else {
                // get the current PModeSet from the Holodeck core.
                pms = (PModeSet) HolodeckB2BCore.getPModeSet();
            }

            // we have a new file which is previously unknown to the system
            
            // check if new PMode is currently known based on it's PMode-ID.
            if (this.pModeIDToFilenameMapping.containsKey(pm.getId())) {

                log.debug("An error occured when processing new pmode XML file from '" + f.getAbsolutePath() + "'.");
                log.debug("PMode already known based on the PMode-ID.");
                log.debug("The defined PModeID ('" + pm.getId() + "') is already in use and PMode ID's must be UNIQUE.");
                log.debug("PMode with this PMode-ID already defined in file '" + this.pModeIDToFilenameMapping.get(pm.getId()) + "'");

            } else {

                // add new entry to the internal mapping of this class
                this.pModeIDToFilenameMapping.put(pm.getId(), f.getAbsolutePath());

                // add new entry to the PModeSet
                pms.set(pm.getId(), pm);

                // pass the new PModeSet to the Holodeck core
                HolodeckB2BCore.setPModeSet(pms);

            }

        } catch (Exception e) {
            // Something went wrong on while reading the PMode XML file.
            log.error("An error occured when reading pmode XML file from '" + f.getAbsolutePath() + "' when adding file'. Details: " + e.getMessage());
        }
    }

    /**
     * Handle the PMode changed event. 
     * Previous existing file describing a PMode found on file system.
     *
     * @param f XML file containing the PMode definition in XML.
     */
    protected void onChangeChanged(File f) {

        try {

            // Proces changed PMode xml file(s).
            log.debug("Changed PMode file '" + f.getAbsolutePath() + "' found.");

            // get the PMode from the file
            PMode pm = new PMode();
            pm = pm.createFromFile(f);

            // get the current PModeSet from the Holodeck core.
            PModeSet pms = (PModeSet) HolodeckB2BCore.getPModeSet();

            // check if PMode is currently known based on the location of the PMode definition file.
            // if file is found, replace the definition by remove and add operations
            if (this.pModeIDToFilenameMapping.containsValue(f.getAbsolutePath())) {

                String PModeID = "";
                
                // get the PMode ID corresponding to the old PMode definition file
                PModeID = Utils.getKeyByValue(pModeIDToFilenameMapping, f.getAbsolutePath());

                if ((PModeID != null) && (!PModeID.isEmpty())) {

                    // remove the old entry from the internal mapping of this class
                    this.pModeIDToFilenameMapping.remove(PModeID);
                    
                    // remove existing entry from PModeSet
                    pms.remove(PModeID);
                } 
                
                // add new entry to the internal mapping of this class
                this.pModeIDToFilenameMapping.put(pm.getId(), f.getAbsolutePath());

                // add new entry to the PModeSet
                pms.set(pm.getId(), pm);

                // pass the new PModeSet to the Holodeck core
                HolodeckB2BCore.setPModeSet(pms);

            } else {

                // we have a new file now let's check it's PMode-ID.
                // check PMode is currently known based on the PMode-ID.
                if (this.pModeIDToFilenameMapping.containsKey(pm.getId())) {

                    log.debug("An error occured when processing new pmode XML file from '" + f.getName() + "'.");
                    log.debug("PMode already known based on the PMode-ID.");
                    log.debug("The defined PModeID ('" + pm.getId() + "') is already in use and PMode ID's must be UNIQUE.");
                    log.debug("PMode with this PMode-ID already defined in file '" + this.pModeIDToFilenameMapping.get(pm.getId()) + "'");

                } else {

                    // add new entry to the internal mapping of this class
                    this.pModeIDToFilenameMapping.put(pm.getId(), f.getAbsolutePath());

                    // add new entry to the PModeSet
                    pms.set(pm.getId(), pm);

                    // pass the new PModeSet to the Holodeck core
                    HolodeckB2BCore.setPModeSet(pms);

                }
            } 

        } catch (Exception e) {
            // Something went wrong on while reading the PMode XML file.
            log.error("An error occured when reading pmode XML file from '" + f.getAbsolutePath() + "' on file change'. Details: " + e.getMessage());
        }
    }

    /**
     * Handle the PMode remove event.
     *
     * @param f XML file containing the PMode definition in XML.
     *
     */
    protected void onChangeRemove(File f) {
        try {

            String PModeID = "";

            // Check which PMode file is removed
            log.debug("Removed PMode file '" + f.getName() + "' no longer exist.");

            // check if we already know a PMode with the same file location.
            // note that we cannot retrieve the PMode ID any longer since
            // the file is gone on disk which defined the PMode ID.
            if (this.pModeIDToFilenameMapping.containsValue(f.getAbsolutePath())) {

                // get the PModeID from the internal mapping (since the file
                // which contained the PModeID is removed from disk).
                PModeID = Utils.getKeyByValue(pModeIDToFilenameMapping, f.getAbsolutePath());

                if ((PModeID != null) && (!PModeID.isEmpty())) {

                    // remove existing entry
                    this.pModeIDToFilenameMapping.remove(PModeID);

                    // get the current PModeSet from the Holodeck core.
                    PModeSet pms = (PModeSet) HolodeckB2BCore.getPModeSet();

                    // remove existing entry from PModeSet
                    pms.remove(PModeID);

                    // pass the new PModeSet to the Holodeck B2B core
                    HolodeckB2BCore.setPModeSet(pms);
                }

            }

        } catch (Exception e) {
            // Something went wrong on while reading the PMode XML file.
            log.error("An error occured when pmode XML file was removed from '" + f.getAbsolutePath() + "'. Details: " + e.getMessage());
        }
    }

}

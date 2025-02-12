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
package org.holodeckb2b.common.pmode;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeStorage;
import org.holodeckb2b.interfaces.pmode.PModeSetException;

/**
 * Is the default implementation of {@link IPModeStorage} that maintains the set of P-Modes in memory.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class InMemoryPModeSet implements IPModeStorage {

    /**
     * The actual storage of the P-Mode set, for easy access on PMode.id we use a HashMap
     */
    private HashMap<String, IPMode>     pmodeSet = new HashMap<>();

    @Override
    public void init(IConfiguration config) throws PModeSetException {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public IPMode get(final String id) {
        if (this.containsId(id))
            return pmodeSet.get(id);
        else
            return null;
    }

    @Override
    public Collection<IPMode> getAll() {
        return pmodeSet.values();
    }

    @Override
    public boolean containsId(final String id) {
        return (pmodeSet.keySet() != null ? pmodeSet.keySet().contains(id) : false);
    }

    @Override
    public String add(final IPMode pmode) throws PModeSetException {
        // Check whether the provided P-Mode has been assigned an id
        String pmodeId = pmode.getId();

        if (Utils.isNullOrEmpty(pmodeId)) {
            // No id provided, generate one now
            pmodeId = generatePModeId(pmode);
        }

        synchronized (this.pmodeSet) {
            // Ensure that the P-Mode id is unique and does not already exist
            if (pmodeSet.containsKey(pmodeId))
                throw new PModeSetException("A P-Mode with id " + pmodeId + " already exists!");

            pmodeSet.put(pmodeId, pmode);

            return pmodeId;
        }
    }

    @Override
    public void replace(final IPMode pmode) throws PModeSetException {
        final String pmodeId = pmode.getId();
        // The given P-Mode must contain an id
        if (Utils.isNullOrEmpty(pmodeId))
            throw new PModeSetException("The P-Mode MUST have an id!");

        synchronized (this.pmodeSet) {
            if (!containsId(pmodeId))
                throw new PModeSetException("There is no P-Mode with the given id!");

            this.pmodeSet.put(pmodeId, pmode);
        }
    }

    @Override
    public void remove(final String id) throws PModeSetException {
        if (containsId(id)) {
            synchronized (this.pmodeSet) {
                this.pmodeSet.remove(id);
            }
        }
    }

    @Override
    public void removeAll() throws PModeSetException {
        this.pmodeSet = new HashMap<>();
    }

    /**
     * Helper method to create a unique P-Mode id when no id is specified when adding a P-Mode
     *
     * @param pmode The P-Mode to generate the id for
     * @return The new id which is guaranteed to be unique in the current set
     */
    private String generatePModeId(final IPMode pmode) {
        final StringBuffer pmodeId = new StringBuffer("pm-");

        // For now we just use an id based on the current time and a sequence number
        pmodeId.append(new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));
        pmodeId.append('-');
        int i = 0;
        while (this.containsId(pmodeId.toString() + i))
            i += 1;
        pmodeId.append(i);

        return pmodeId.toString();
    }

}

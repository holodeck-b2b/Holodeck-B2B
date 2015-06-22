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
package org.holodeckb2b.common.pmode;

import java.util.Set;

/**
 * Represents a set of {@link IPMode}s that configure how Holodeck B2B should process the ebMS messages. The set of 
 * P-Modes is therefore the most important configuration item in Holodeck B2B, without P-Modes it will not be possible 
 * to send and receive messages! Within Holodeck B2B there is always just one <code>IPModeSet</code> instance.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IPModeSet {

    /**
     * Gets a list of the <b>PMode.id</b>s of all P-Modes in the set.
     * 
     * @return A <code>String[]</code> containing the P-Mode id's
     */
    public String[] listPModeIds();
    
    /**
     * Gets the P-Mode with the given <b>PMode.id</b>
     * 
     * @param id    The <b>PMode.id</b> as a <code>String</code> of the P-Mode to retrieve from the set
     * @return      The {@link IPMode} with the given id if it exists in the set, or<br>
     *              <code>null</code> when there is no P-Mode in the set with the given id.
     */
    public IPMode get(String id);
    
    /**
     * Gets all the {@link IPMode}s in the set.
     * 
     * @return The complete {@link Set} of {@link IPMode}s
     */
    public Set<IPMode> getAll();
}

/*
 * Copyright (C) 2013-2014 The Holodeck B2B Team, Sander Fieten
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.common.pmode.IPModeSet;

/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */

public class PModeSet implements IPModeSet {
    
    /**
     * Internal map used to store PModes.
     */
    private Map<String, PMode> pmodeSet = null;
    
    /**
     * Constructor
     */
    public PModeSet() {
        
        // initialize the internal PModeSet, contains PModeID and PMode object.
        this.pmodeSet = new HashMap<String, PMode>();
    }
    
    /**
     * List all PMode ID's for all known PModes
     * @return  String[] with all the PMode ID's or an empty String[] if an error occurred.
     */
    @Override
    public String[] listPModeIds() {
                
        List<String> result = new ArrayList<String>();
        String[] retVal =  new String[0];
        
        try {
            
            if (!pmodeSet.isEmpty()) {
                
                // loop trough the know set of PModes and get their PMode ID
                for (Map.Entry<String, PMode> entry : pmodeSet.entrySet()) {
                    result.add(entry.getKey());
                }
                
                if ( result.size() > 0 ) {
                    // convert List<String> to String[]
                    retVal = result.toArray(new String[0]);
                } else {
                    retVal =  new String[0];
                }
                
            }
           
            
        } catch (Exception ex) {
            Logger.getLogger(PModeSet.class.getName()).log(Level.SEVERE, null, ex);
        }  
        return retVal;
    }

    /**
     * Get the PMode based on the PMode ID
     * 
     * @param id The PMode ID
     * @return IPMode or null if an error occurred.
     */
    @Override
    public IPMode get(String id) {
        return pmodeSet.get(id);
    }

    /**
     * Get all the PMode's contained by the PModeSet.
     * 
     * @return Set of <code>IPMode</code> containing all the PMode's.
     */
    @Override
    public Set<IPMode> getAll() {
        
        Set<IPMode> theSet = new HashSet<IPMode>();
        
        if (!pmodeSet.isEmpty()) {
                
                // loop trough the know set and retrieve the PModes
                for (Map.Entry<String, ?> entry : pmodeSet.entrySet()) {
                    theSet.add((PMode) entry.getValue());
                }
         }
        
        return theSet;
    }
    

    /**
     * Add a PMode to the PModeSet
     * 
     * @param pmodeID The PMode ID which uniquely identifies the PMode.
     * @param pmode The PMode to add to the PModeSet.
     * @return void
     */
    void set(String pmodeID, IPMode pmode) {
            
        this.pmodeSet.put(pmodeID, (PMode) pmode);
    }

    /**
     * Remove PMode from the PModeSet
     * 
     * @param pmodeID The PMode ID which uniquely identifies the PMode.
     * @return void
     */
    void remove(String pmodeID) {
        this.pmodeSet.remove(pmodeID);
    }
}

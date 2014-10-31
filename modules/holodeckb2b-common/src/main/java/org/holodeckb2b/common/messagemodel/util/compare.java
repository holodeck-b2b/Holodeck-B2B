/*
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

package org.holodeckb2b.common.messagemodel.util;

import java.util.Collection;
import java.util.Iterator;
import org.holodeckb2b.common.general.IPartyId;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.general.IService;
import org.holodeckb2b.common.general.ITradingPartner;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public final class compare {
    
    /**
     * Checks if two {@link ITradingPartner} objects are equal. Two <code>ITradingPartner</code> objects are equal
     * when their <i>Role</i>s are equal and both have the same set of {@link IPartyId}s.
     * 
     * @param tp1
     * @param tp2
     * @return      <code>true</code> if the {@link ITradingPartner} object are equal,
     *              <code>false</code> otherwise
     */
    public static boolean TradingPartner(ITradingPartner tp1, ITradingPartner tp2) {
        boolean equal = true;
        
        String r1 = tp1.getRole(), r2 = tp2.getRole();
        equal = (r1 == null ? r2 == null : r1.equals(r2));
        
        Collection<IPartyId> pids1 = tp1.getPartyIds(), pids2 = tp2.getPartyIds();
        equal &= pids1.size() == pids2.size();
        
        // Check every PartyId from the first collection to exist in the second and ensure all id's from the second
        // collection have been checked
        boolean[] checked = new boolean[pids2.size()]; // have all items in second collection been checked?
        for(Iterator<IPartyId> it1 = pids1.iterator() ; equal && it1.hasNext() ;) {
            IPartyId pi1 = it1.next();
            // Check if this PartyId exists in the second collection
            Iterator<IPartyId> it2 = pids2.iterator(); int i = 0;
            for(; equal && it2.hasNext() ; i++)
                if (equal = compare.PartyId(pi1, it2.next()))
                    checked[i] = true; // This item in the second collection is succesfully compared            
        }
        // Check that every id in second collection was found in first collection
        for(boolean b : checked)
            equal &= b;
        
        return equal;
    }
    
    /**
     * Checks if two {@link IPartyId} objects are equal. Two <code>IPartyId</code> object are equal when there values
     * and types are equal.
     * 
     * @param id1
     * @param id2
     * @return  <code>true</code> if the party id are equal,
     *          <code>false</code> otherwise
     */
    public static boolean PartyId(IPartyId id1, IPartyId id2) {
        boolean equal = true;
        
        String v1 = id1.getId(), v2 = id2.getId();
        String t1 = id1.getType(), t2 = id2.getType();
        
        equal = (v1 == null ? v2 == null : v1.equals(v2));
        equal &= (t1 == null ? t2 == null : t1.equals(t2));
        
        return equal;
    }
    
    /**
     * Checks if two {@link IProperty} objects are equal. Two <code>IProperty</code> objects are equal when there names,
     * values and types are equal.
     * 
     * @param p1
     * @param p2
     * @return  <code>true</code> if the properties are equal,
     *          <code>false</code> otherwise     
     */
    public static boolean Property(IProperty p1, IProperty p2) {
        boolean equal = true;
        
        String n1 = p1.getName(), n2 = p2.getName();
        String v1 = p1.getValue(), v2 = p2.getValue();
        String t1 = p1.getType(), t2 = p2.getType();
        
        equal = (n1 == null ? n2 == null : n1.equals(n2));
        equal &= (v1 == null ? v2 == null : v1.equals(v2));
        equal &= (t1 == null ? t2 == null : t1.equals(t2));
        
        return equal;
    }
    
    /**
     * Checks if two {@link IService} objects are equal. Two <code>IService</code> objects are equal when there names
     * and types are equal.
     * 
     * @param svc1
     * @param svc2
     * @return  <code>true</code> if the services are equal,
     *          <code>false</code> otherwise     
     */
    public static boolean Service(IService svc1, IService svc2) {
        boolean equal = true;
        String n1 = svc1.getName(), n2 = svc2.getName();
        String t1 = svc1.getType(), t2 = svc2.getType();
        
        equal = (n1 == null ? n2 == null : n1.equals(n2));
        equal &= (t1 == null ? t2 == null : t1.equals(t2));
        
        return equal;
    }
    
    /*
     * This class only has static utility methods and should not be instantiated!
     */
    private compare() {}
}

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
package org.holodeckb2b.common.messagemodel.util;

import java.util.Collection;
import java.util.Iterator;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.general.ITradingPartner;

/**
 * Is a utility class that offers methods to check whether two message model objects are equal, i.e. refer to the same
 * business information.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public final class CompareUtils {
    
    /**
     * Checks if two {@link ITradingPartner} objects are equal. Two <code>ITradingPartner</code> objects are equal
     * when their <i>Role</i>s are equal and both have the same set of {@link IPartyId}s.
     * 
     * @param tp1 first trading partner
     * @param tp2 second trading partner
     * @return      <code>true</code> if the {@link ITradingPartner} object are equal,
     *              <code>false</code> otherwise
     */
    public static boolean areEqual(ITradingPartner tp1, ITradingPartner tp2) {
        boolean equal = true;
        
        String r1 = tp1.getRole(), r2 = tp2.getRole();
        equal = (r1 == null ? r2 == null : r1.equals(r2));
        
        if (equal) {
            // Evaluate only if the roles are identical
            equal = CompareUtils.areEqual(tp1.getPartyIds(), tp2.getPartyIds());        
        }
        
        return equal;
    }
    
    /**
     * Checks if two <code>Collection</code>s of {@link IPartyId} are equal.
     * 
     * @param pids1 First collection
     * @param pids2 Second collection
     * @return      <code>true</code> if both collection contain the same party ids,
     *              <code>false</code> otherwise
     */
    public static boolean areEqual(Collection<IPartyId> pids1, Collection<IPartyId> pids2) {
        boolean equal = pids1.size() == pids2.size();
        
        if (equal)
        {
            // Evaluate only if the roles are identical
            // Check every areEqual from the first collection to exist in the second and ensure all id's from the second
            // collection have been checked
            boolean[] checked = new boolean[pids2.size()]; // have all items in second collection been checked?
            for(Iterator<IPartyId> it1 = pids1.iterator() ; equal && it1.hasNext() ;) {
                IPartyId pi1 = it1.next();
                // Check if this areEqual exists in the second collection
                Iterator<IPartyId> it2 = pids2.iterator(); int i = 0;
                for(; equal && it2.hasNext() ; i++)
                    if (equal = CompareUtils.areEqual(pi1, it2.next()))
                        checked[i] = true; // This item in the second collection is successfully compared            
            }
            // Check that every id in second collection was found in first collection
            for(boolean b : checked)
                equal &= b;
        }
        
        return equal;
    }
    
    /**
     * Checks if two {@link IPartyId} objects are equal. Two <code>IPartyId</code> object are equal when there values
     * and types are equal.
     * 
     * @param id1 first Id
     * @param id2 second Id
     * @return  <code>true</code> if the party id are equal,
     *          <code>false</code> otherwise
     */
    public static boolean areEqual(IPartyId id1, IPartyId id2) {
        boolean equal = true;
        
        String v1 = id1.getId(), v2 = id2.getId();
        String t1 = id1.getType(), t2 = id2.getType();
        
        equal = (v1 == null ? v2 == null : v1.equals(v2));
        if (equal) {
          // Evaluate only if the roles are identical
          equal = (t1 == null ? t2 == null : t1.equals(t2));
        }
        return equal;
    }
    
    /**
     * Checks if two {@link IProperty} objects are equal. Two <code>IProperty</code> objects are equal when there names,
     * values and types are equal.
     * 
     * @param p1 first property
     * @param p2 second property
     * @return  <code>true</code> if the properties are equal,
     *          <code>false</code> otherwise     
     */
    public static boolean areEqual(IProperty p1, IProperty p2) {
        boolean equal = true;
        
        String n1 = p1.getName(), n2 = p2.getName();
        String v1 = p1.getValue(), v2 = p2.getValue();
        String t1 = p1.getType(), t2 = p2.getType();
        
        equal = (n1 == null ? n2 == null : n1.equals(n2));
        if (equal) {
          // Evaluate only if the roles are identical
          equal = (v1 == null ? v2 == null : v1.equals(v2));
          if (equal)
            equal = (t1 == null ? t2 == null : t1.equals(t2));
        }
        
        return equal;
    }
    
    /**
     * Checks if two {@link IService} objects are equal. Two <code>IService</code> objects are equal when there names
     * and types are equal.
     * 
     * @param svc1 first service
     * @param svc2 second service
     * @return  <code>true</code> if the services are equal,
     *          <code>false</code> otherwise     
     */
    public static boolean areEqual(IService svc1, IService svc2) {
        boolean equal = true;
        String n1 = svc1.getName(), n2 = svc2.getName();
        String t1 = svc1.getType(), t2 = svc2.getType();
        
        equal = (n1 == null ? n2 == null : n1.equals(n2));
        if (equal)
          equal = (t1 == null ? t2 == null : t1.equals(t2));
        
        return equal;
    }
    
    /*
     * This class only has static utility methods and should not be instantiated!
     */
    private CompareUtils() {}
}

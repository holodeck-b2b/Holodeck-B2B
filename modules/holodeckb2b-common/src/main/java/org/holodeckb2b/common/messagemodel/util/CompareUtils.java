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
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Is a utility class that offers methods to check whether two message model objects are equal, i.e. refer to the same
 * business information.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
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
    public static boolean areEqual(final ITradingPartner tp1, final ITradingPartner tp2) {
        return Utils.nullSafeEqual (tp1.getRole (), tp2.getRole ()) &&
               areEqual(tp1.getPartyIds(), tp2.getPartyIds());
    }

    /**
     * Checks if two <code>Collection</code>s of {@link IPartyId} are equal.
     *
     * @param pids1 First collection
     * @param pids2 Second collection
     * @return      <code>true</code> if both collection contain the same party ids,
     *              <code>false</code> otherwise
     */
    public static boolean areEqual(final Collection<? extends IPartyId> pids1,
                                   final Collection<? extends IPartyId> pids2) {
        boolean equal = pids1.size() == pids2.size();

        if (equal) {
            // Check every PartyId from the first collection to exist in the second and ensure all id's from the second
            // collection have been checked
            final boolean[] checked = new boolean[pids2.size()]; // have all items in second collection been checked?
            for(final Iterator<? extends IPartyId> it1 = pids1.iterator() ; equal && it1.hasNext() ;) {
                final IPartyId pi1 = it1.next();
                // Check if this areEqual exists in the second collection
                int i = 0; boolean exists = false;
                for (final Iterator<? extends IPartyId> it2 = pids2.iterator() ; !exists && it2.hasNext() ; i++)
                    if (exists = areEqual(pi1, it2.next()))
                        checked[i] = true; // This item in the second collection is successfully compared
                equal &= exists;
            }
            // Check that every id in second collection was found in first collection
            for(final boolean b : checked)
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
    public static boolean areEqual(final IPartyId id1, final IPartyId id2) {
        return Utils.nullSafeEqual (id1.getId(), id2.getId()) &&
               Utils.nullSafeEqual (id1.getType(), id2.getType());
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
    public static boolean areEqual(final IProperty p1, final IProperty p2) {
      return Utils.nullSafeEqual (p1.getName(), p2.getName()) &&
             Utils.nullSafeEqual (p1.getValue(), p2.getValue()) &&
             Utils.nullSafeEqual (p1.getType(), p2.getType());
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
    public static boolean areEqual(final IService svc1, final IService svc2) {
      return Utils.nullSafeEqual (svc1.getName(), svc2.getName()) &&
             Utils.nullSafeEqual (svc1.getType(), svc2.getType());
    }

    /**
     * Checks if two {@link IPayload} objects are equals, i.e. if they represent the same payload data.
     *
     * @param pl1   first payload
     * @param pl2   second payload
     * @return      <code>true</code> if the payloads are equal,
     *              <code>false</code> otherwise
     * @since 4.0.0
     */
    public static boolean areEqual(final IPayload pl1, final IPayload pl2) {
        return Utils.nullSafeEqual(pl1.getContainment(), pl2.getContainment()) &&
               Utils.nullSafeEqual(pl1.getPayloadURI(), pl2.getPayloadURI()) &&
               Utils.nullSafeEqual(pl1.getContentLocation(), pl2.getContentLocation());
    }
    
    /*
     * This class only has static utility methods and should not be instantiated!
     */
    private CompareUtils() {}
}

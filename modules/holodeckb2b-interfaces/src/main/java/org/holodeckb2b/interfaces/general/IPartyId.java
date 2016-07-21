/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.general;


/**
 * Is used to identify a business partner. Consist of an id value and an optional id type. Corresponds to the
 * <code>eb:PartyId</code> element in the ebMS header and is defined in section 5.2.2.4 of the ebMS Core Specification.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IPartyId {

    /**
     * Get the identification of the business partner
     *
     * @return  The identification of the business partner as a <code>String</code>
     */
    public String getId();

    /**
     * Get the type of identification that is used to identify the business partner. This is a <i>"business"</i> type
     * that identifies the naming scheme used for identification.
     *
     * @return  The <i>businnes</i> type of the party id
     */
    public String getType();
}

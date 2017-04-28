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


import java.util.Collection;

/**
 * Represents a business partner involved in a message exchange. The partner is identified by one or more party ids and
 * the business role the partner is acting in. Corresponds with the information contained in the <code>eb:From</code>
 * and <code>eb:To</code> elements in the ebMS message header. See also sections 5.2.2.3 and 5.2.2.4 of the ebMS Core
 * Specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ITradingPartner {

    /**
     * Gets the list of party ids used to identify the trading partner
     *
     * @return A collection of {@link IPartyId} objects representing the party ids that identify the partner
     */
    public Collection<IPartyId> getPartyIds();

    /**
     * Gets the <i>business</i> role the trading partner is acting in. The role is defined in a business level agreement
     * and probably different from the role the MSH that processes the message is acting in.
     *
     * @return The business role the trading partner is playing
     */
    public String getRole();
}

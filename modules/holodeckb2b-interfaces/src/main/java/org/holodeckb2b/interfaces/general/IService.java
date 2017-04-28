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
 * Represents a <i>business service</i>, i.e. a service of the business application, that will process a message.
 * Holodeck B2B itself will not use this information to determine message processing other than that it might be used
 * to find the correct P-Mode. Corresponds with the information contained in the <code>eb:Service</code> element in the
 * ebMS message header. See also section 5.2.2.8 of the ebMS Core Specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IService {

    /**
     * Gets the service name.
     * <p>Corresponds to the content of the <code>eb:Service</code> element in the ebMS message header.
     *
     * @return  The service name
     */
    public String getName();

    /**
     * Gets the service type
     * <p>Corresponds to the <code>type</code> attribute of the <code>eb:Service</code> element in the ebMS message
     * header.
     *
     * @return The service type
     */
    public String getType();
}

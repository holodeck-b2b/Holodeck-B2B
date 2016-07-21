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
 *
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public interface IAgreement {

    /**
     * Gets the agreement name.
     *
     * @return The agreement name
     */
    public String getName();

    /**
     * Gets the agreement type.
     *
     * @return The agreement type
     */
    public String getType();

    public void setName(String name);

    public void setType(String type);

}

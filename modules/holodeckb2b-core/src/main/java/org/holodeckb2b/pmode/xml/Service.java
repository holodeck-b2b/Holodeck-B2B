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
package org.holodeckb2b.pmode.xml;

import org.holodeckb2b.interfaces.general.IService;
import org.simpleframework.xml.Element;

/**
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
class Service implements IService {


    @Element (name = "type", required = false)
    private String type;

    @Element (name = "name", required = true)
    private String name;


    /**
     * Get the Service type.
     * @return String Service type.
     */
    @Override
    public String getType() {
        return this.type;
    }

    /**
     * Get the Service name.
     * @return String Service name
     */
    @Override
    public String getName() {
        return this.name;
    }
}

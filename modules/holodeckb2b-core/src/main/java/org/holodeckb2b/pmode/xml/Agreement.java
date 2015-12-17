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

import org.holodeckb2b.interfaces.general.IAgreement;
import org.simpleframework.xml.Element;

/**
 * Implements a PMode agreement.
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class Agreement implements IAgreement {
    
    @Element (name = "name", required = true)
    private String name = "";
    
    @Element (name = "type", required = false)
    private String type = "";
    
    
    /**
     * Default constructor.
     */
    public Agreement() {}
    
    /**
     * Gets the agreement name.
     * 
     * @return The agreement name
     */
    @Override
    public String getName() {
        return this.name;
    }
    
    /**
     * Gets the agreement type.
     * 
     * @return The agreement type
     */
    @Override
    public String getType() {
        return this.type;
    }

    /**
     * Set the name of the agreement.
     * @param name The agreement name to set
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Set the type of the agreement.
     * @param type The agreement type to set
     */
    @Override
    public void setType(String type) {
        this.type = type;
    }
    
}

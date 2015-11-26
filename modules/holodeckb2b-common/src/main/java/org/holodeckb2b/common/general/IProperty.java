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
package org.holodeckb2b.common.general;

/**
 * A simple name value pair representing a property assigned to an entity. To facilitate further processing of the 
 * property also the value type can be given. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IProperty {
   
    /**
     * Gets the name of the property.
     * 
     * @return  The name of the property. As name is a required attribute for a property, the result SHOULD NOT be 
     *          <code>null</code>
     */
    public String getName();
    
    /**
     * Gets the value of the property
     * 
     * @return  The value of the property as a <code>String</code>.
     */
    public String getValue();
    
    /**
     * Gets the value type. This does not need to be a data type but can also indicate a <i>"business"</i> type that
     * indicates how the property should be used.
     * 
     * @return  The type of the property as a <code>String</code>.
     */
    public String getType();
}

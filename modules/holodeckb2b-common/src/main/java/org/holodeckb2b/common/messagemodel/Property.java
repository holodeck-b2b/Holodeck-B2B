/**
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.messagemodel;

import org.holodeckb2b.interfaces.general.IProperty;

/**
 * Is an in memory only implementation of {@link IProperty} to temporarily store a property that was contain in the ebMS
 * message header of a message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class Property implements IProperty {

    private String  name;
    private String  value;
    private String  type;

    /**
     * Default constructor creates empty object
     */
    public Property() {}

    /**
     * Creates a new <code>Property</code> object based on data given in the provided object
     *
     * @param source     The property info to use
     */
    public Property(final IProperty source) {
        this.name = source.getName();
        this.value = source.getValue();
        this.type = source.getType();
    }

    /**
     * Creates a new <code>Property</code> object for a property with the specified name and value.
     *
     * @param   name    Name of the property
     * @param   value   The value of the property
     */
    public Property(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Creates a new <code>Property</code> object for a property with the specified name, value and type.
     *
     * @param   name    Name of the property
     * @param   value   The value of the property
     * @param   type    The type of the property value
     */
    public Property(final String name, final String value, final String type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}

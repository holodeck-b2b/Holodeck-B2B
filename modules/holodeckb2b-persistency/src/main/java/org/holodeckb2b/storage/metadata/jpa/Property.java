/**
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.storage.metadata.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

import org.holodeckb2b.interfaces.general.IProperty;

/**
 * Is an <i>embeddable</i> JPA persistency class used to store a generic property as described by {@link IProperty}
 * interface.
 * <p>This class is <i>embeddable</i> as a property is always bound specifically to one instance of an object.
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 * @since  3.0.0
 */
@Embeddable
public class Property implements IProperty, Serializable {
	private static final long serialVersionUID = -2688175299690561424L;

    /*
     * Getters and setters
     */

	@Override
    public String getName() {
        return NAME;
    }

    public void setName(final String name) {
        NAME = name;
    }

    @Override
    public String getValue() {
        return VALUE;
    }

    public void setValue(final String value) {
        VALUE = value;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public void setType(final String type) {
        TYPE = type;
    }

    /*
     * Constructors
     */
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
        this(source.getName(), source.getValue(), source.getType());
    }

    /**
     * Creates a new <code>Property</code> object for a property with the specified name and value.
     *
     * @param   name    Name of the property
     * @param   value   The value of the property
     */
    public Property(final String name, final String value) {
        this(name, value, null);
    }

    /**
     * Creates a new <code>Property</code> object for a property with the specified name, value and type.
     *
     * @param   name    Name of the property
     * @param   value   The value of the property
     * @param   type    The type of the property value
     */
    public Property(final String name, final String value, final String type) {
        this.NAME = name;
        this.VALUE = value;
        this.TYPE = type;
    }

    /*
     * Fields
     *
     * NOTE: The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    @Lob
    @Column(length = 1024)
    private String  NAME;

    @Lob
    @Column(length = 3092)
    private String  VALUE;

    @Lob
    @Column(length = 1024)
    private String  TYPE;

}

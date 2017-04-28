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
package org.holodeckb2b.persistency.jpa;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;
import org.holodeckb2b.interfaces.general.IService;

/**
 * Is an <i>embeddable</i> JPA persistency class used to store the service information as described by {@link IService}
 * interface in the Holodeck B2B messaging model.
 * <p>This class is <i>embeddable</i> as the service meta-data is always specific to one instance of a  User Message.
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 * @since  3.0.0
 */
@Embeddable
public class Service implements IService, Serializable {

    /*
     * Getters and setters
     */

    @Override
    public String getName() {
        return S_NAME;
    }

    public void setName(final String name) {
        S_NAME = name;
    }

    @Override
    public String getType() {
        return S_TYPE;
    }

    public void setType(final String type) {
        S_TYPE = type;
    }

    /*
     * Constructors
     */
    /**
     * Default constructor creates empty object
     */
    public Service() {}

    /**
     * Creates a <code>Service</code> object with the provided name
     *
     * @param name      The name of the service
     */
    public Service(final String name) {
        this(name, null);
    }

    /**
     * Creates a <code>Service</code> object using the meta-data provided in the given source object
     *
     * @param source   The data to use
     */
    public Service(final IService source) {
        this(source.getName(), source.getType());
    }

    /**
     * Creates a <code>Service</code> object with the provided meta-data
     *
     * @param name      The name of the service
     * @param type      The type of the service
     */
    public Service(final String name, final String type) {
        this.S_NAME = name;
        this.S_TYPE = type;
    }

    /*
     * Fields
     */

    /*
     * The service name is REQUIRED
     */
    @Lob
    @Column(length = 1024)
    private String  S_NAME;

    /*
     * The service type is optional
     */
    @Lob
    @Column(length = 1024)
    private String  S_TYPE;
 }

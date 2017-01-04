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
package org.holodeckb2b.persistency.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

import org.holodeckb2b.interfaces.general.IService;

/**
 * Is the JPA persistency class for storing information about the <i>Service</i> that is addressed by a User Message. As
 * this is always part of a larger information structure (like the {@link CollaborationInfo} data part of a {@link
 * UserMessage}) it is defined as <i>embeddable</i>.
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@Embeddable
public class Service implements Serializable, IService {

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
    public Service() {}

    /**
     * Construct a new Service object with the given name and type
     */
    public Service(final String name, final String type) {
        S_NAME = name;
        S_TYPE = type;
    }

    /*
     * Constructs a new Service object which only has a service name
     */
    public Service(final String name) {
        this(name, null);
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

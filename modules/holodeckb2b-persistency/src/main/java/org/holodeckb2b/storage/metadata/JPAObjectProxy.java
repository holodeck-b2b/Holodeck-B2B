/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.storage.metadata;

import org.holodeckb2b.storage.metadata.jpa.JPAEntityObject;

/**
 * Is the base class for the implementation of the entity interfaces defined in {@link
 * org.holodeckb2b.interfaces.storage}. As the entity objects may not change during the message processing, but the JPA
 * implementations used here do, we need to create a stable proxy object that hides the change of JPA object from the
 * Core.
 *
 * @param <T>	the actual entity class being proxied
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
abstract class JPAObjectProxy<T extends JPAEntityObject> {
    /**
     * The JPA entity object that is being proxied
     */
    protected T   jpaEntityObject;

    /**
     * Indicates whether all the meta-data on the Message Unit has been loaded
     */
    protected boolean allMetadataLoaded = false;

    /**
     * Creates a new <code>MessageUnitEntity</code> object for the given JPA entity object.
     *
     * @param jpaObject     The JPA entity object to create a proxy for
     */
    protected JPAObjectProxy(final T jpaObject) {
        this.jpaEntityObject = jpaObject;
    }

    /**
     * @return	the proxied JPA object
     */
    public T getJPAObject() {
    	return jpaEntityObject;
    }

    /**
     * Updates the JPA object that is being proxied.
     *
     * @param jpaObject
     */
    public void updateJPAObject(final T jpaObject) {
        this.jpaEntityObject = jpaObject;
    }

    /**
     * Indicates whether all meta-data of the Message Unit has been loaded from the database.
     *
     * @return <code>true</code> if all meta-data on the User Message has been loaded, <code>false</code> otherwise
     */
    public boolean isLoadedCompletely() {
        return allMetadataLoaded;
    }

    /**
     * Loads all meta-data of the meta-data of the proxied JPA object so it available outside of the JPA persistence
     * context. After this method is called {@link #isLoadedCompletely()} must return <code>true</code>.
     */
    abstract public void loadCompletely();

    /**
     * Gets the OID of the JPA object that is being proxied. This OID is needed to retrieve the actual meta-data from
     * the database.
     *
     * @return  The OID of the proxied message unit JPA object
     */
    public long getOID() {
        return jpaEntityObject.getOID();
    }
}

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
package org.holodeckb2b.storage.metadata.jpa.wrappers;

import jakarta.persistence.Embedded;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
abstract class BaseEmbedableWrapper<E> {

    @Id
    @GeneratedValue
    protected long id;

    @Embedded
    protected E  embeddedObj;

    public long id() {
    	return id;
    }
    
    public E object() {
    	return embeddedObj;
    }

    public void setObject(E obj) {
    	embeddedObj = obj;
    }
    
    public BaseEmbedableWrapper() {
		embeddedObj = createEmbedded();
	}
    
    abstract E createEmbedded();
}

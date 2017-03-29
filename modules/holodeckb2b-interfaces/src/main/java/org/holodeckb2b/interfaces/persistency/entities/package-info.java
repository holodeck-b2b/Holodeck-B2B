/*
 * Copyright (C) 2016 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
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
/**
 * Contains the interface definitions of the "<i>entity objects</i>" used by the Holodeck B2B Core to store information
 * on a message unit to the database. They are an abstraction from, or proxy to, the actual stored object so the
 * persistency provider can easily reload objects from storage without impacting the entity object reference as used by
 * the Core.
 * <p>The entity objects implement the generic message model and also extend the generic message model interfaces. Note
 * that even as they are used to store data there are no <i>setter</i> methods defined in the entity objects. This is
 * because changes to a stored meta-data must be performed through the data access objects so they can manage (and
 * optimize) them.
 * 
 * @since 2.2
 */
package org.holodeckb2b.interfaces.persistency.entities;

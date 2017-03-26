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
 * Contains the interface definitions for the data access objects a Holodeck B2B <i>persistency provider</i> has to
 * offer so the Core can store the meta-data on processed message units. The API defines two interfaces for accessing
 * stored meta-data, one for write operations and one for read operations.
 * <p>The operations of the DAO are based on so called "<i>entity objects</i>" which represent stored message unit meta-
 * data. Using these entity objects allows the persistency provider to easily reload objects from storage without
 * impacting the entity object as used by the Core.
 * <p>Note that the {@link IUpdateManager} DAO doesn't offer a generic update method that allows to save the complete
 * state of an entity object but only supplies methods to change specific fields. This is done to simplify and allow
 * optimizations in the persistency provider implementation.
 * <p>Implementations must ensure that all methods are thread-safe and the necessary locking is implemented both in the
 * Java code as well as in the database operations.
 *
 * @since 2.2
 */
package org.holodeckb2b.interfaces.persistency.dao;

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
 * Contains the interfaces that define a Holodeck B2B <i>Persistency Provider</i> that provides storage services to
 * the Core for the meta-data on the processed message units. The {@link IPersistencyProvider}  interface is the "entry 
 * point" of the provider that follows the factory pattern. It used for initialisation of the provider and provides 
 * access to {@link IQueryManager} and  {@link IUpdateManager} that provide the actual storage functionality. The
 * package {@link org.holodeckb2b.interfaces.persistency.entities} contains definitions for the objects managed by the
 * provider.
 *
 * @since 3.0.0
 * @since 5.0.0  The manager interfaces are now also part of this package. 
 */
package org.holodeckb2b.interfaces.persistency;

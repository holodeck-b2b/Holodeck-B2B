/*
 * Copyright (C) 2024 The Holodeck B2B Team.
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
 * During the message processing Holodeck B2B will need to store the meta-data and payload content. The message meta-
 * data is relatively small and can be stored in a database. For the payload data, which can be large, it may be more 
 * efficient to store it on the file system. Therefore the responsibility for storing the meta-data and payload is 
 * is divided between two components, the <i>Metadata</i> and <i>Payload Storage Providers</i>. 
 * <p>
 * This package defines the interfaces that represent the data model of the information of message units that needs to
 * be stored during processing by the Holodeck B2B Core and a component to query the data. The data model mimics the 
 * general message model defined in {@link org.holodeckb2b.interfaces.messagemodel} and extends is with additional 
 * attributes needed for efficient processing of the message by both the Core and the storage providers.   
 * <p>
 * The <i>entity</i> interfaces represent the meta-data of message units and should therefore be implemented as part of
 * and handled by the <i>Metadata Storage Provider</i>. The {@link org.holodeckb2b.interfaces.storage.IPayloadContent}
 * interface represents the payload content and should be implemented and handled by the <i>Payload Storage Provider</i>.
 * 
 * @since 7.0.0 The interfaces for the entity objects in this packages are moved from the old 
 * 				<code>org.holodeckb2b.interfaces.persistency.entities</code> package.
 */
package org.holodeckb2b.interfaces.storage;

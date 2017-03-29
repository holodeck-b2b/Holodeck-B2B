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
package org.holodeckb2b.interfaces.persistency.entities;

import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * This interface is used to indicate that the <i>User Message</i> message unit meta-data is stored by the persistency
 * layer.
 * <p>Beside the generic meta-data fields that may be <i>lazily loaded</i> persistency implementations MAY also load the
 * information on the <b>sender, receiver, payloads and message properties <i>lazily</i></b>, i.e. before calling
 * getters to access this info the {@link #isLoadedCompletely()} should be executed to check if all information is
 * loaded.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.2
 * @see   IMessageUnitEntity
 */
public interface IUserMessageEntity extends IMessageUnitEntity, IUserMessage {

}

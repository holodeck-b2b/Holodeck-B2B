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

import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;

/**
 * This interface is used to indicate that the <b>selective</b> <i>Pull Request Signal</i> message unit meta-data is
 * stored by the persistency layer.
 * <p>Beside the generic meta-data fields that may be <i>lazily loaded</i> there are no fields specific to the pull
 * request signal that can be <i>lazily loaded</i>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  4.1.0
 * @see   IMessageUnitEntity
 */
public interface ISelectivePullRequestEntity extends IPullRequestEntity, ISelectivePullRequest {

}

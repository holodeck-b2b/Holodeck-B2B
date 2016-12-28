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
package org.holodeckb2b.interfaces.persistency.dao;

import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;

/**
 * Defines the interface for the <i>data access object</i> that is responsible for managing the storage of <i>Pull
 * Request Signal Message</i> meta-data.
 * <p>Implementations must ensure that all methods are thread-safe and the necessary locking is implemented both in the
 * Java code as well as in the database operations.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 * @see IPullRequestEntity
 */
public interface IPullRequestDAO {

    /**
     * Creates a new persistency object to store the meta-data of a <i>Pull Request</i> message unit.
     *
     * @param pullreq   The meta-data on the Pull Request that should be stored in the new persistent object
     * @return          The created persistency object.
     * @throws PersistenceException        If an error occurs when saving the new message unit to the database.
     */
    IPullRequestEntity createPullRequest(final IPullRequest pullreq) throws PersistenceException;

    /**
     * Deletes the meta-data of the given <i>Pull Request</i> message unit from the database.
     *
     * @param msgUnit       The {@link IPullRequestEntity} object to be deleted
     * @throws PersistenceException     When a problem occurs while removing the message unit from the database.
     */
    void deleteUserMessage(final IPullRequestEntity msgUnit) throws PersistenceException;
}

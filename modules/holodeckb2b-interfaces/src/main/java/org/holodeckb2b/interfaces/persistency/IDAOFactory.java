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
package org.holodeckb2b.interfaces.persistency;

import org.holodeckb2b.interfaces.persistency.dao.IErrorMessageDAO;
import org.holodeckb2b.interfaces.persistency.dao.IMessageUnitDAO;
import org.holodeckb2b.interfaces.persistency.dao.IPullRequestDAO;
import org.holodeckb2b.interfaces.persistency.dao.IReceiptDAO;
import org.holodeckb2b.interfaces.persistency.dao.IUserMessageDAO;

/**
 * Defines the interface of the factory object that a <i>persistency implementation</i> has to provide to the Holodeck
 * B2B Core so it can persist the meta-data of the processed message units.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
public interface IDAOFactory {

    /**
     * Gets a data access object to perform generic persistency operations that apply to all types of message units.
     *
     * @return  A {@link IMessageUnitDAO} implementation
     */
    IMessageUnitDAO     getMessageUnitDAOInstance();

    /**
     * Gets a data access object instance to perform persistency operations that apply only to User Message message
     * units.
     *
     * @return  A {@link IUserMessageDAO} implementation
     */
    IUserMessageDAO     getUserMessageDAOInstance();

    /**
     * Gets a data access object instance to perform persistency operations that apply only to Pull Request message
     * units.
     *
     * @return  A {@link IPullRequestDAO} implementation
     */
    IPullRequestDAO     getPullRequestDAOInstance();

    /**
     * Gets a data access object instance to perform persistency operations that apply only to Receipt Signal message
     * units.
     *
     * @return  A {@link IReceiptDAO} implementation
     */
    IReceiptDAO         getReceiptDAOInstance();

    /**
     * Gets a data access object instance to perform persistency operations that apply only to Error Signal message
     * units.
     *
     * @return  A {@link IErrorMessageDAO} implementation
     */
    IErrorMessageDAO    getErrorMessageDAOInstance();
}

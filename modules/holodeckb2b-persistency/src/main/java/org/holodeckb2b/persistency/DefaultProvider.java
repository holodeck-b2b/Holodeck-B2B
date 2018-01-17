/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency;

import org.holodeckb2b.common.constants.ProductId;
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.dao.IDAOFactory;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.persistency.dao.IUpdateManager;
import org.holodeckb2b.persistency.managers.QueryManager;
import org.holodeckb2b.persistency.managers.UpdateManager;
import org.holodeckb2b.persistency.util.EntityManagerUtil;

/**
 * Is the default implementation of a Holodeck B2B <i>Persistency Provider</i>. This provider uses an integrated Derby
 * database for storing all the data. It is suitable for smaller gateway deployments. For larger gateways that have
 * additional requirements on performance and high availability a different provider should be used. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class DefaultProvider implements IPersistencyProvider {

    @Override
    public String getName() {
        return  "HB2B Default Persistency/" + ProductId.MAJOR_VERSION + "." + ProductId.MINOR_VERSION
                                            + "." + ProductId.PATCH_VERSION;
    }


    @Override
    public void init() throws PersistenceException {
        EntityManagerUtil.check();
    }

    /**
     *
     * @return The DAO Factory of the default persistency implementation
     */
    @Override
    public IDAOFactory getDAOFactory() {
        return new DAOFactory();
    }

    /**
     * Is the factory class of the default persistency implementation that enables the Holodeck B2B Core to persist and
     * access the meta-data of the processed message units.
     */
    class DAOFactory implements IDAOFactory {

        /**
         *
         * @return The update manager of the default persistency implementation
         */
        @Override
        public IUpdateManager getUpdateManager() {
            return new UpdateManager();
        }

        /**
         *
         * @return The query manager of the default persistency implementation
         */
        @Override
        public IQueryManager getQueryManager() {
            return new QueryManager();
        }
    }
}

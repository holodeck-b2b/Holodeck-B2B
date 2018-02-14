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

import org.holodeckb2b.interfaces.persistency.dao.IDAOFactory;

/**
 * Defines the interface of a <i>persistency provider</i> that allows the Holodeck B2B Core to persist the meta-data of
 * processed message units. This interface is the "entry point" of the provider used to intialize the provider and to
 * access the storage functions. The actual functionality the provider has to implement is defined in all the interfaces
 * contained in this and sub packages {@link org.holodeckb2b.interfaces.persistency.dao} and {@link
 * org.holodeckb2b.interfaces.persistency.entities}.
 * <p>To allow the Holodeck B2B Core to create an instance of the persistency provider a default non-argument
 * constructor must be provided by the implementation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public interface IPersistencyProvider {

    /**
     * Gets the name of this persistency provider to identify it in logging. This name is only used for logging purposes
     * and SHOULD include a version number of the implementation.
     *
     * @return  The name of the persistency provider.
     */
    String getName();

    /**
     * Initializes the persistency provider. It MUST ensure that all information that is needed to create the DAO
     * factory objects is available and correct.
     *
     * @param hb2bHomeDir               Path to the Holodeck B2B home directory
     * @throws PersistenceException     When the initialization of the provider can not be completed. The exception
     *                                  message SHOULD include a clear indication of what caused the init failure.
     */
    void init(final String hb2bHomeDir) throws PersistenceException;

    /**
     * Gets the provider's {@link IDAOFactory} implementation to create the data access objects.
     *
     * @return The provider's {@link IDAOFactory} implementation
     */
    IDAOFactory getDAOFactory();
}

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

import java.nio.file.Path;

/**
 * Defines the interface of a Holodeck B2B <i>Persistency Provider</i> that allows the Holodeck B2B Core to persist the 
 * meta-data of processed message units. 
 * <p>There can always be just one <i>Persistency Provider</i> active in an Holodeck B2B instance. The implementation to
 * use is loaded using the Java <i>Service Prover Interface</i> mechanism.  
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 * @since  5.0.0 The interface now also includes the methods to get the manager objects. 
 */
public interface IPersistencyProvider {

    /**
     * Gets the name of this persistency provider to identify it in logging. This name is only used for logging purposes 
     * and it is recommended to include a version number of the implementation. If no name is specified by the 
     * implementation the class name will be used. 
     *
     * @return  The name of the persistency provider.
     */
    default String getName() { return this.getClass().getName(); }

    /**
     * Initialises the persistency provider. This method is called once at startup of the Holodeck B2B instance. Since
     * the message processing depends on the correct functioning of the Persistency Provider this method MUST ensure 
     * that all required configuration and data is available . Required configuration parameters must be implemented by 
     * the Certificate Manager. 
     *
     * @param hb2bHomeDir               Path to the Holodeck B2B home directory
     * @throws PersistenceException     When the initialization of the provider can not be completed. The exception
     *                                  message SHOULD include a clear indication of what caused the init failure.
     */
    void init(Path hb2bHomeDir) throws PersistenceException;

    /**
     * Gets the <i>Update Manager</i> to perform write operations on message unit meta-data.
     *
     * @return  A {@link IUpdateManager} implementation
     */
    IUpdateManager  getUpdateManager();

    /**
     * Gets the <i>Query Manager</i> to perform query (read) operations on message unit meta-data.
     *
     * @return  A {@link IQueryManager} implementation
     */
    IQueryManager   getQueryManager();
}

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

import java.nio.file.Path;

import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
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
        return  "HB2B Default Persistency/" + VersionInfo.fullVersion;
    }

    @Override
    public void init(final Path hb2bHomeDir) throws PersistenceException {
        EntityManagerUtil.check();
    }
    
    @Override
    public IUpdateManager getUpdateManager() {
        return new UpdateManager();
    }

    @Override
    public IQueryManager getQueryManager() {
        return new QueryManager();
    }
}

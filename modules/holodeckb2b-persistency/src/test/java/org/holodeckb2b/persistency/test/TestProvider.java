/*
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency.test;

import java.nio.file.Path;

import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.persistency.managers.QueryManager;
import org.holodeckb2b.persistency.managers.UpdateManager;

public class TestProvider implements IPersistencyProvider {

	@Override
	public String getName() {
		return "HB2B Default Test/" + VersionInfo.fullVersion;
	}

	@Override
	public void init(Path hb2bHomeDir) throws PersistenceException {
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

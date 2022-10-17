package org.holodeckb2b.persistency.inmemory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;

/**
 * Is a {@link IPersistencyProvider} implementation for testing that stores the message units in-memory. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class InMemoryProvider implements IPersistencyProvider {

	private Set<IMessageUnitEntity>	 msgUnitStore = Collections.synchronizedSet(new HashSet<IMessageUnitEntity>());	
	
	@Override
	public String getName() {
		return "In Memory Test Provider";
	}
		
	@Override
	public IUpdateManager getUpdateManager() {
		return new UpdateManager(msgUnitStore);
	}

	@Override
	public IQueryManager getQueryManager() {
		return new QueryManager(msgUnitStore);
	}

	@Override
	public void init(IConfiguration config) throws PersistenceException {
	}

	@Override
	public void shutdown() {
	}		
}

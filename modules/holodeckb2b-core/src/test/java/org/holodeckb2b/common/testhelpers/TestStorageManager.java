package org.holodeckb2b.common.testhelpers;

import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;

/**
 * Is a facade to the normal {@link StorageManager} which adds a method to get direct access to the {@link 
 * IUpdateManager) of the persistency provider.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class TestStorageManager extends StorageManager {
	
	public TestStorageManager(IUpdateManager parent) {
		super(parent);		
	}
	
	public IUpdateManager getUpdateManager() {
		return parent;
	}

}

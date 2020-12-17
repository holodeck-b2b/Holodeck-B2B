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
package org.holodeckb2b.common.testhelpers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis2.modules.Module;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.common.pmode.InMemoryPModeSet;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCoreImpl;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.core.validation.DefaultValidationExecutor;
import org.holodeckb2b.core.validation.IValidationExecutor;
import org.holodeckb2b.interfaces.core.IHolodeckB2BCore;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.IMessageDelivererFactory;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;
import org.holodeckb2b.interfaces.general.IVersionInfo;
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.persistency.inmemory.InMemoryProvider;

/**
 * Is utility class for testing that simulates the Holodeck B2B Core.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class HolodeckB2BTestCore extends HolodeckB2BCoreImpl implements IHolodeckB2BCore {

	private InternalConfiguration		configuration;
	private IMessageSubmitter	messageSubmitter;
	private IPModeSet			pmodes;
	private IMessageProcessingEventProcessor eventProcessor;
	private IValidationExecutor validationExec;
	private ICertificateManager certManager;
	private IPersistencyProvider persistencyProvider;
	private List<IMessageProcessingEventConfiguration> eventConfig = new ArrayList<>();
	private Map<String, IMessageDelivererFactory> msgDeliveryMethods = new HashMap<>();
	private Map<String, Module> modules = new HashMap<>();
	
	public HolodeckB2BTestCore() {
		this(".");
	}

	public HolodeckB2BTestCore(final String homeDir) {
		this.configuration = new InternalConfiguration(Paths.get(homeDir));
		this.configuration.setHostName("local.test");
		this.configuration.setTempDirectory(Paths.get(homeDir).resolve("temp_t"));
	}	
	
	public void cleanTemp() {
		Path tmpDir = configuration != null ? configuration.getTempDirectory() : null;
		if (tmpDir != null) 
			deleteDirectory(tmpDir.toFile());
	}

	private static boolean deleteDirectory(File directory) {
	    if(directory.exists()){
	        File[] files = directory.listFiles();
	        if(null!=files){
	            for(int i=0; i<files.length; i++) {
	                if(files[i].isDirectory()) {
	                    deleteDirectory(files[i]);
	                }
	                else {
	                    files[i].delete();
	                }
	            }
	        }
	    }
	    return(directory.delete());
	}	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getMessageDeliverer(org.holodeckb2b.interfaces.delivery.IDeliverySpecification)
	 */
	@Override
	public IMessageDeliverer getMessageDeliverer(IDeliverySpecification deliverySpec) throws MessageDeliveryException {
		IMessageDelivererFactory mdf = msgDeliveryMethods.get(deliverySpec.getId());
		if (mdf == null) {
	        try {
	            final String factoryClassName = deliverySpec.getFactory();
	            mdf = (IMessageDelivererFactory) Class.forName(factoryClassName).newInstance();
	        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
	            // Somehow the factory class failed to load
	            throw new MessageDeliveryException("Factory class not available!", ex);
	        }
	        // Initialize the new factory with the settings from the delivery spec
	        mdf.init(deliverySpec.getSettings());
	        msgDeliveryMethods.put(deliverySpec.getId(), mdf);
		}
        return mdf.createMessageDeliverer();
	}
	
	public void setMessageSubmitter(final IMessageSubmitter submitter) {
		this.messageSubmitter = submitter;
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getMessageSubmitter()
	 */
	@Override
	public IMessageSubmitter getMessageSubmitter() {
		if (messageSubmitter == null)
			synchronized (this) {
				if (messageSubmitter == null)
					messageSubmitter = new Submitter();
			}
		return messageSubmitter;
	}

	public void setPModeSet(final IPModeSet pmodeSet) {
		this.pmodes = pmodeSet;
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getPModeSet()
	 */
	@Override
	public IPModeSet getPModeSet() {
		if (pmodes == null)
			pmodes = new InMemoryPModeSet();
		return pmodes;
	}

	public void setMessageProcessingEventProcessor(final IMessageProcessingEventProcessor processor) {
		this.eventProcessor = processor;
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getEventProcessor()
	 */
	@Override
	public IMessageProcessingEventProcessor getEventProcessor() {
		if (eventProcessor == null)
			eventProcessor = new TestEventProcessor();
		return eventProcessor;
	}

	public void setPersistencyProvider(IPersistencyProvider provider) throws PersistenceException {
		persistencyProvider = provider;
		persistencyProvider.init(configuration.getHolodeckB2BHome());
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getQueryManager()
	 */
	@Override
	public IQueryManager getQueryManager() {
		if (persistencyProvider == null)
			persistencyProvider = new InMemoryProvider();
		
		return persistencyProvider.getQueryManager();
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getQueryManager()
	 */
	@Override
	public StorageManager getStorageManager() {
		if (persistencyProvider == null)
			persistencyProvider = new InMemoryProvider();
		
		return new TestStorageManager(persistencyProvider.getUpdateManager());
	}

	
	public void setCertificateManager(ICertificateManager crtManager) {
		certManager = crtManager;
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getCertificateManager()
	 */
	@Override
	public ICertificateManager getCertificateManager() {
		if (certManager == null)
			try {
				certManager = new InMemoryCertificateManager();
			} catch (SecurityProcessingException e) {				
			}
		return certManager;
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#registerEventHandler(org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration)
	 */
	@Override
	public boolean registerEventHandler(IMessageProcessingEventConfiguration eventConfiguration)
			throws MessageProccesingEventHandlingException {
    	final String id = eventConfiguration.getId();
    	if (Utils.isNullOrEmpty(id))
    		throw new MessageProccesingEventHandlingException("No id specified");
    	
    	int i; boolean exists = false;
    	for(i = 0; i < eventConfig.size() && !exists; i++)
    		exists = eventConfig.get(i).getId().equals(id);
    	if (exists) 
    		eventConfig.set(i, eventConfiguration);
    	else 
    		eventConfig.add(eventConfiguration);
    	return exists;
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#removeEventHandler(java.lang.String)
	 */
	@Override
	public void removeEventHandler(String id) {
    	int i; int exists = -1;
    	for(i = 0; i < eventConfig.size() && exists < 0; i++)
    		exists = eventConfig.get(i).getId().equals(id) ? i : -1;
    	if (exists >= 0)     		
    		eventConfig.remove(exists);
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getEventHandlerConfiguration()
	 */
	@Override
	public List<IMessageProcessingEventConfiguration> getEventHandlerConfiguration() {
		return eventConfig;
	}
	
	@Override
	public IVersionInfo getVersion() {
		return VersionInfo.getInstance();
	}
	
	public void setValidationExecutor(IValidationExecutor exec) {
		this.validationExec = exec;
	}
	
	@Override
    public IValidationExecutor getValidationExecutor() {		
        return validationExec != null ? validationExec : new DefaultValidationExecutor();
    }

	@Override
	public InternalConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public Module getModule(String name) {
		return modules.get(name);
	}
	
	public void setModule(String name, Module module) {
		modules.put(name, module);
	}
}

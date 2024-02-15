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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.modules.Module;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.common.pmode.InMemoryPModeSet;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCoreImpl;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.core.storage.QueryManager;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.core.validation.DefaultValidationExecutor;
import org.holodeckb2b.core.validation.IValidationExecutor;
import org.holodeckb2b.interfaces.core.IHolodeckB2BCore;
import org.holodeckb2b.interfaces.delivery.IDeliveryManager;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;
import org.holodeckb2b.interfaces.general.IVersionInfo;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.storage.IQueryManager;
import org.holodeckb2b.interfaces.storage.providers.IMetadataStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.IPayloadStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.test.storage.InMemoryMDSProvider;
import org.holodeckb2b.test.storage.InMemoryPSProvider;

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
	private IMetadataStorageProvider mdsProvider;
	private IPayloadStorageProvider  psProvider;
	private List<IMessageProcessingEventConfiguration> eventConfig = new ArrayList<>();
	private IDeliveryManager	deliveryManager;
	private Map<String, Module> modules = new HashMap<>();
	
	public HolodeckB2BTestCore() throws AxisFault {
		this(TestUtils.getTestClassBasePath());
	}

	public HolodeckB2BTestCore(final Path homeDir) throws AxisFault {
		this.configuration = new InternalConfiguration(homeDir);
		this.configuration.setHostName("local.test");
		this.configuration.setTempDirectory(homeDir.resolve("temp_t"));
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
	
	public void setDeliveryManager(final IDeliveryManager dm) {
		this.deliveryManager = dm;
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getMessageSubmitter()
	 */
	@Override
	public IDeliveryManager getDeliveryManager() {
		if (deliveryManager == null)
			synchronized (this) {
				if (deliveryManager == null)
					deliveryManager = new TestDeliveryManager();
			}
		return deliveryManager;
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
					messageSubmitter = new TestMessageSubmitter();
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

	public void setMetadataStorageProvider(IMetadataStorageProvider provider) throws StorageException {
		mdsProvider = provider;
		mdsProvider.init(configuration);
	}
	
	public IMetadataStorageProvider getMetadataStorageProvider() {
		if (mdsProvider == null)
			mdsProvider = new InMemoryMDSProvider();
		return mdsProvider;
	}
	
	public void setPayloadStorageProvider(IPayloadStorageProvider provider) throws StorageException {
		psProvider = provider;
		psProvider.init(configuration);
	}
	
	public IPayloadStorageProvider getPayloadStorageProvider() {
		if (psProvider == null)
			psProvider = new InMemoryPSProvider();
		return psProvider;
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getQueryManager()
	 */
	@Override
	public IQueryManager getQueryManager() {
		return new QueryManager(getMetadataStorageProvider(), getPayloadStorageProvider());
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.core.IHolodeckB2BCore#getQueryManager()
	 */
	@Override
	public StorageManager getStorageManager() {
		return new StorageManager(getMetadataStorageProvider(), getPayloadStorageProvider());
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
				certManager = new TestCertificateManager();
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

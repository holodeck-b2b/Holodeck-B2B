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
package org.holodeckb2b.module;

import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.common.testhelpers.TestConfig;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.persistency.dao.IDAOFactory;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.security.ICertificateManager;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.persistency.DefaultProvider;
import org.holodeckb2b.persistency.dao.StorageManager;

/**
 * Is utility class for testing the e-SENS connector that simulates the Holodeck B2B Core.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class HolodeckB2BTestCore extends HolodeckB2BCoreImpl {

    protected org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore     coreImplementation;

    private IDAOFactory daoFactory;
    
    private ISecurityProvider secProvider;
    
    public HolodeckB2BTestCore() throws Exception {
        this(null, null, null);
    }

    public HolodeckB2BTestCore(String homeDir) throws Exception {
        this(homeDir, null, null);
    }

    public HolodeckB2BTestCore(final String homeDir,
                               final String pmodeValidatorClass) throws Exception {
        this(homeDir, pmodeValidatorClass, null);
    }

    public HolodeckB2BTestCore(final String homeDir,
                               final String pmodeValidatorClass,
                               final String pmodeStorageClass) throws Exception {
    	TestConfig tstCfg = new TestConfig(homeDir, pmodeValidatorClass, pmodeStorageClass);
    	coreImplementation = new org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore(tstCfg);
    	
    	DefaultProvider dbProvider = new DefaultProvider();
    	dbProvider.init(homeDir);
    	daoFactory = dbProvider.getDAOFactory();
    }

    public void cleanTemp() {
    	coreImplementation.cleanTemp();
    }
    
    @Override
	public InternalConfiguration getConfiguration() {
        return (InternalConfiguration) coreImplementation.getConfiguration();
    }

    @Override
	public IMessageDeliverer getMessageDeliverer(final IDeliverySpecification deliverySpec)
                                                        throws MessageDeliveryException {
        return coreImplementation.getMessageDeliverer(deliverySpec);
    }

    @Override
	public IMessageSubmitter getMessageSubmitter() {
        return coreImplementation.getMessageSubmitter();
    }

    @Override
	public IPModeSet getPModeSet() {
        return coreImplementation.getPModeSet();
    }

    @Override
	public IMessageProcessingEventProcessor getEventProcessor() {
        return coreImplementation.getEventProcessor();
    }

    @Override
	public void setPullWorkerPoolConfiguration(final IWorkerPoolConfiguration pullConfiguration)
                                                                                    throws TaskConfigurationException {
        coreImplementation.setPullWorkerPoolConfiguration(pullConfiguration);
    }

    @Override
	public IQueryManager getQueryManager() {
        return daoFactory.getQueryManager();
    }
    
    @Override
	public StorageManager getStorageManager() {
    	return new StorageManager(daoFactory.getUpdateManager());
    }
    
    @Override
	public ICertificateManager getCertificateManager() {
        return getSecurityProvider().getCertificateManager();
    }
    
    @Override
	public ISecurityProvider getSecurityProvider() {
    	if (secProvider == null) {
        	secProvider = new org.holodeckb2b.security.DefaultProvider();
        	try {
				secProvider.init(getConfiguration().getHolodeckB2BHome());
			} catch (SecurityProcessingException e) {
				e.printStackTrace();
			}    	
    	}
    	return secProvider;
    }
    
	public void setMessageProcessingEventProcessor(TestEventProcessor eventProcessor) {
		coreImplementation.setMessageProcessingEventProcessor(eventProcessor);
	}    
}

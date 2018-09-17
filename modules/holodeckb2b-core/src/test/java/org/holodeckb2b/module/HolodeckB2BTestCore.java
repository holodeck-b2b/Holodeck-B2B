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

import static org.mockito.Mockito.mock;

import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.common.testhelpers.Config;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.submit.core.MessageSubmitter;
import org.holodeckb2b.events.SyncEventProcessor;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.dao.IDAOFactory;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.security.ICertificateManager;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.InMemoryPModeSet;
import org.holodeckb2b.pmode.PModeManager;
import org.holodeckb2b.security.DefaultProvider;

/**
 * Is utility class for testing the e-SENS connector that simulates the Holodeck B2B Core.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class HolodeckB2BTestCore extends HolodeckB2BCoreImpl {

    private static IMessageSubmitter submitter = new MessageSubmitter();

    private IDAOFactory daoFactory;

    private final Config  config;

    private IPModeSet pmodeSet;

    private IMessageProcessingEventProcessor eventProcessor;

    private ISecurityProvider securityProvider;

    public HolodeckB2BTestCore() {
        this(null, null, null);
    }

    public HolodeckB2BTestCore(String homeDir) {
        this(homeDir, null, null);
    }

    public HolodeckB2BTestCore(final String homeDir,
                               final String pmodeValidatorClass) {
        this(homeDir, pmodeValidatorClass, null);
    }

    public HolodeckB2BTestCore(final String homeDir,
                               final String pmodeValidatorClass,
                               final String pmodeStorageClass) {
        String homePath = homeDir;
        if (Utils.isNullOrEmpty(homePath))
            homePath = HolodeckB2BTestCore.class.getClassLoader().getResource("").getPath();
        config = new Config(homePath, pmodeValidatorClass, pmodeStorageClass);
        pmodeSet = new InMemoryPModeSet();
        eventProcessor = new SyncEventProcessor();
        submitter = testSubmitter;
        initDAOFactory();
        try {
            securityProvider = new DefaultProvider();
            securityProvider.init(homePath);
        } catch (SecurityProcessingException spe) {
            // Ignore, maybe the security provider isn't needed for the current tests
        }
    }

    private void initDAOFactory() {
        IPersistencyProvider persistencyProvider = new org.holodeckb2b.persistency.DefaultProvider();
        try {
            persistencyProvider.init(config.getHolodeckB2BHome());
            daoFactory = persistencyProvider.getDAOFactory();
        } catch (PersistenceException initializationFailure) {
            //throw new AxisFault("Holodeck B2B could not be initialized!");
            System.err.println("Could not initialize the persistency provider " + persistencyProvider.getName()
                    + "! Unable to start Holodeck B2B. \n\tError details: " + initializationFailure.getMessage());
        }
    }

    @Override
    StorageManager getStorageManager() {
        return new StorageManager(daoFactory.getUpdateManager());
    }

    @Override
    ISecurityProvider getSecurityProvider() {
        return securityProvider;
    }

    @Override
    public ICertificateManager getCertificateManager() {
        return securityProvider.getCertifcateManager();
    }

    @Override
    public InternalConfiguration getConfiguration() {
        return config;
    }

    @Override
    public IMessageDeliverer getMessageDeliverer(
            final IDeliverySpecification deliverySpec)
            throws MessageDeliveryException {
        IMessageDeliverer messageDeliverer = mock(IMessageDeliverer.class);
        return messageDeliverer;
    }

    @Override
    public IMessageSubmitter getMessageSubmitter() {
        return submitter;
    }

    @Override
    public IPModeSet getPModeSet() {
        if (pmodeSet == null)
            pmodeSet = new PModeManager(config);

        return pmodeSet;
    }

    public void setEventProcessor(final IMessageProcessingEventProcessor processor) {
        eventProcessor = processor;
    }

    @Override
    public IMessageProcessingEventProcessor getEventProcessor() {
        return eventProcessor;
    }

    @Override
    public void setPullWorkerPoolConfiguration(
            final IWorkerPoolConfiguration pullConfiguration)
            throws TaskConfigurationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IQueryManager getQueryManager() {
        return daoFactory.getQueryManager();
    }
}

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

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.IHolodeckB2BCore;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.events.processing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.security.ICertificateManager;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Is utility class for testing that simulates the Holodeck B2B Core.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class HolodeckB2BTestCore implements IHolodeckB2BCore {

    private final Config  config;

    private IPModeSet pmodeSet;

    private IMessageProcessingEventProcessor eventProcessor;

    public HolodeckB2BTestCore(final String homeDir) {
        this(homeDir, null, null);
    }

    public HolodeckB2BTestCore(final String homeDir,
                               final String pmodeValidatorClass) {
        this(homeDir, pmodeValidatorClass, null);
    }

    public HolodeckB2BTestCore(final String homeDir,
                               final String pmodeValidatorClass,
                               final String pmodeStorageClass) {
        config = new Config(homeDir, pmodeValidatorClass, pmodeStorageClass);
    }

    @Override
    public IConfiguration getConfiguration() {
        return config;
    }

    @Override
    public IMessageDeliverer getMessageDeliverer(
            final IDeliverySpecification deliverySpec)
            throws MessageDeliveryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IMessageSubmitter getMessageSubmitter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IPModeSet getPModeSet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setEventProcessor(
            final IMessageProcessingEventProcessor processor) {
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ICertificateManager getCertificateManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

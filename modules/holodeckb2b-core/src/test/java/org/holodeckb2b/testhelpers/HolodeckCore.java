/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.testhelpers;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.IHolodeckB2BCore;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.pmode.InMemoryPModeSet;

/**
 * Is utility class for testing the e-SENS connector that simulates the Holodeck B2B Core. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class HolodeckCore implements IHolodeckB2BCore {

    private Config  config;
    
    private InMemoryPModeSet pmodeSet;    
    
    private IMessageProcessingEventProcessor eventProcessor;
    
    public HolodeckCore(String homeDir) {
        config = new Config(homeDir);
        pmodeSet = new InMemoryPModeSet();
    }
    
    @Override
    public IConfiguration getConfiguration() {
        return config;
    }

    @Override
    public IMessageDeliverer getMessageDeliverer(IDeliverySpecification deliverySpec) throws MessageDeliveryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IMessageSubmitter getMessageSubmitter() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IPModeSet getPModeSet() {
        return pmodeSet;
    }

    public void setEventProcessor(IMessageProcessingEventProcessor processor) {
        eventProcessor = processor;
    }
    
    @Override
    public IMessageProcessingEventProcessor getEventProcessor() {
        return eventProcessor;
    }

    @Override
    public void setPullWorkerPoolConfiguration(IWorkerPoolConfiguration pullConfiguration) throws TaskConfigurationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

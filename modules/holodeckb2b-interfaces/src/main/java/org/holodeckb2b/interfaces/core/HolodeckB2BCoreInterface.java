/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.core;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;

/**
 * Provides access to the Holodeck B2B Core of a running instance. Note that this is just a <i>facade</i> to the actual
 * Core implementation to provide a clean separation of interface and implementation. Although this class is more  
 * interface than class it needs to be a class to get an object instance on run time.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class HolodeckB2BCoreInterface {
    
    /**
     * The Holodeck B2B Core implementation
     */
    private static IHolodeckB2BCore     coreImplementation;

    /**
     * Returns the current configuration of this Holodeck B2B instance. The configuration parameters can be used
     * by extension to integrate their functionality with the core.
     * 
     * @return  The current configuration as a {@link IConfiguration} object
     */
    public static IConfiguration getConfiguration() {
        assertInitialized();
        return coreImplementation.getConfiguration();
    }
    
    /**
     * Gets a {@link IMessageDeliverer} object configured as specified by the {@link IDeliverySpecification} that can be 
     * used to deliver message units to the <i>Consumer</i> business application.
     * 
     * @param deliverySpec      Specification of the delivery method for which a deliver must be returned.
     * @return                  A {@link IMessageDeliverer} object for the given delivery specification
     * @throws MessageDeliveryException When no delivery specification is given or when the message deliverer can not
     *                                  be created
     */
    public static IMessageDeliverer getMessageDeliverer(IDeliverySpecification deliverySpec) 
                                                        throws MessageDeliveryException {
        assertInitialized();
        return coreImplementation.getMessageDeliverer(deliverySpec);
    }
    
    /**
     * Gets a {@link IMessageSubmitter} object that can be used by the <i>Producer</i> business application for 
     * submitting User Messages to the Holodeck B2B Core. 
     * 
     * @return  A {@link IMessageSubmitter} object to use for submission of User Messages
     */
    public static IMessageSubmitter getMessageSubmitter() {
        assertInitialized();
        return coreImplementation.getMessageSubmitter();
    }
    
    /**
     * Gets the set of currently configured P-Modes.
     * <p>The P-Modes define how Holodeck B2B should process the messages. The set of P-Modes is therefor the most 
     * important configuration item in Holodeck B2B, without P-Modes it will not be possible to send and receive 
     * messages.
     * 
     * @return  The current set of P-Modes as a {@link IPModeSet}
     * @see IPMode
     */
    public static IPModeSet getPModeSet() {
        assertInitialized();
        return coreImplementation.getPModeSet();
    }    
    
    /**
     * Sets the Holodeck B2B Core implementation that is in use. 
     * <p><b>NOTE: </b>This message is for <b>internal use only</b>!
     * 
     * @param impl  The current Holodeck B2B Core implementation
     */
    public static synchronized void setImplementation(IHolodeckB2BCore impl) {
        coreImplementation = impl;
    }
    
    /**
     * Ensures that the Holodeck B2B is loaded and available
     */
    private static void assertInitialized() {
        if (coreImplementation == null)
            throw new IllegalStateException("Holodeck B2B is not [correctly] started yet");        
    }
}

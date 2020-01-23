/*
 * Copyright (C) 2017 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.security;

import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;

/**
 * Defines the interface of a Holodeck B2B <i>security provider</i> which is responsible for handling of the WS-Security
 * header in ebMS3/AS4 messages processed by Holodeck B2B. The security provider has two main components:<ol>
 * <li>The <i>security header processor</i> which is responsible for processing of the WS-Security header in received
 * messages.</li>
 * <li>The <i>security header creator</i> which is responsible for creating the WS-Security header in messages send by
 * Holodeck B2B.</li>
 * </ol>
 * <p>The security provider to use is configured per Holodeck B2B instance, so all ebMS3 messages processed by the 
 * instance will use the same security provider. The security provider will be initialised on startup of the ebMS3 
 * module using the Java SPI mechanism. Implementations that want to support dynamic reconfiguration should handle this 
 * internally.
 *   
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 * @since 5.0.0	The <i>Certificate Manager</i> is removed from the Security Provider and a stand alone component now.
 * 				Loading of the actual provider is done using the SPI mechanism. 
 */
public interface ISecurityProvider {

    /**
     * Gets the name of this security provider.
     * <p>The name of the security provider is only informational and mainly used for logging. However it is recommended
     * to clearly identify the provider.
     *
     * @return The name of this security provider.
     */
    String getName();

    /**
     * Initialises the security provider. This method is called once at the startup of the Holodeck B2B instance (more
     * exactly when the ebMS3 Module is loaded).
     * <p>Since the message processing depends on the correct functioning of the security provider this method MUST 
     * ensure that all its components can be used, i.e. that all required  configuration and data is available. Required 
     * configuration parameters must be implemented by the security provider. It can use the {@link 
     * HolodeckB2BCoreInterface} class to get access to the Core configuration parameters like the HB2B home directory.
     *
     * @throws SecurityProcessingException When the security provider can not be initialised correctly.
     */
    void init() throws SecurityProcessingException;

    /**
     * Gets the {@link ISecurityHeaderProcessor} of this security provider to process the WS-Security header in received
     * messages.
     *
     * @return The security header processor of this security provider
     * @throws SecurityProcessingException 	When the processor cannot be created/returned due to an internal error.  
     */
    ISecurityHeaderProcessor    getSecurityHeaderProcessor() throws SecurityProcessingException;

    /**
     * Gets the {@link ISecurityHeaderCreator} of this security provider to create the WS-Security header in message to
     * send.
     *
     * @return The security header creator of this security provider
     * @throws SecurityProcessingException 	When the creator cannot be created/returned due to an internal error.  
     */
    ISecurityHeaderCreator      getSecurityHeaderCreator() throws SecurityProcessingException;
}

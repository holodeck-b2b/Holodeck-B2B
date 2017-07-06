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

/**
 * Defines the interface of a Holodeck B2B <i>security provider</i> which is responsible for handling of the WS-Security
 * header in messages processed by Holodeck B2B.
 * <p>The security provider has three main components:<ol>
 * <li>The <i>security header processor</i> which is responsible for processing of the WS-Security header in received
 * messages.</li>
 * <li>The <i>security header creator</i> which is responsible for creating the WS-Security header in messages send by
 * Holodeck B2B.</li>
 * <li>The <i>certificate manager</i> which is responsible for storing the keys and certificates needed in the
 * processing of the WS-Security headers.</li>
 * </ol>
 * <p>The security provider to use is configured per Holodeck B2B instance, so all messages processed by the instance
 * will use the same security provider. The security will be initialized on startup of the instance. Implementations
 * that want to support dynamic reconfiguration should handle this internally.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
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
     * Initializes the security provider. This method is called once at the startup of the Holodeck B2B instance. Since
     * the message processing depends on the correct functioning of the security provider this method MUST ensure that
     * all its three components can be used, i.e. that all required configuration and data is available. Required
     * configuration parameters must be implemented by the security provider.
     *
     * @throws SecurityProcessingException When the security provider can not be initialized correctly.
     */
    void init() throws SecurityProcessingException;

    /**
     * Gets the {@link ISecurityHeaderProcessor} of this security provider to process the WS-Security header in received
     * messages.
     *
     * @return The security header processor of this security provider
     */
    ISecurityHeaderProcessor    getSecurityHeaderProcessor();

    /**
     * Gets the {@link ISecurityHeaderCreator} of this security provider to create the WS-Security header in message to
     * send.
     *
     * @return The security header creator of this security provider
     */
    ISecurityHeaderCreator      getSecurityHeaderCreator();

    /**
     * Gets the {@link ICertificateManager} of this security provider which manages storage of keys and certificates.
     *
     * @return The certificate manager of this security provider
     */
    ICertificateManager         getCertifcateManager();
}

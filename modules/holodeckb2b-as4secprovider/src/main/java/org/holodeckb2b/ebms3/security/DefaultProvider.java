/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.security;

import java.security.Provider;
import java.security.Security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.interfaces.security.ISecurityHeaderCreator;
import org.holodeckb2b.interfaces.security.ISecurityHeaderProcessor;
import org.holodeckb2b.interfaces.security.ISecurityProvider;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.security.trust.DefaultCertManager;

/**
 * Is the default implementation of a Holodeck B2B <i>Security Provider</i> as specified by {@link ISecurityProvider}.
 * <p>The provider uses the WSS4J library for the actual processing of the WS-Security headers in the messages. This
 * implementation is tightly coupled to the default certificate manager implementation ({@link DefaultCertManager})
 * which provide some utility function for easy access to the required keys and certificates.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0 A security provider already existed in HB2B version 4.x but in this version the certificate
 * 			manager has been split off.
 */
public class DefaultProvider implements ISecurityProvider {
	private static final Logger log = LogManager.getLogger(DefaultProvider.class);

	/**
	 * The preferred JCE Provider, i.e. Bouncy Castle, that will be used when processing the WS-Security headers
	 */
	private Provider jceProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "HB2B Default Security/" + VersionInfo.fullVersion;
    }

    @Override
    public void init() throws SecurityProcessingException {
    	log.debug("Check that BouncyCaste JCE Provider is available");
    	if ((jceProvider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) == null) {
			log.debug("Install BouncyCaste JCE Provider");
			Security.addProvider(jceProvider = new BouncyCastleProvider());
		} else
			log.trace("BouncyCaste JCE Provider is already installed");
    }

    @Override
    public ISecurityHeaderProcessor getSecurityHeaderProcessor() throws SecurityProcessingException {
        return new SecurityHeaderProcessor(this);
    }

    @Override
    public ISecurityHeaderCreator getSecurityHeaderCreator() throws SecurityProcessingException {
        return new SecurityHeaderCreator(this);
    }

    /**
     * @return	the JCE provider used by the default HB2B Security Provider
     */
    Provider getJceProvider() {
    	return jceProvider;
    }
 }

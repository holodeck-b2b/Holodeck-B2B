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

    private final Logger  log = LogManager.getLogger(DefaultProvider.class);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "HB2B Default Security/" + VersionInfo.fullVersion;
    }

    /**
     * {@inheritDoc}
     * <p>The only initialisation needed here is to ensure that the BouncyCastle security provider is loaded and set as
     * preferred one.
     */
    @Override
    public void init() throws SecurityProcessingException {
		// Make sure that BouncyCastle is the preferred JCE security provider
		final Provider[] providers = Security.getProviders();
		if (providers != null && providers.length > 0)
			Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);		
		log.debug("Registering BouncyCastle as preferred Java security provider");
		Security.insertProviderAt(new BouncyCastleProvider(), 2);		        
        
        // Initialization done
        log.info("Default Security Provider version {} is ready!", VersionInfo.fullVersion);
    }
    
    @Override
    public ISecurityHeaderProcessor getSecurityHeaderProcessor() throws SecurityProcessingException {
        return new SecurityHeaderProcessor();
    }

    @Override
    public ISecurityHeaderCreator getSecurityHeaderCreator() throws SecurityProcessingException {
        return new SecurityHeaderCreator();
    }
    
 }

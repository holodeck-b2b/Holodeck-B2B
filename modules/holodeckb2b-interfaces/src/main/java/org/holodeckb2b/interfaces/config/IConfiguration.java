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
package org.holodeckb2b.interfaces.config;

import org.apache.axis2.context.ConfigurationContext;

/**
 * Defines the interface of the Holodeck B2B configuration.
 * <p>This interface must be used by extension when they need access to the Holodeck B2B settings. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IConfiguration {

    /**
     * Gets the Axis2 configuration context. This context is used for processing messages.
     * 
     * @return The Axis2 configuration context.
     */
    public ConfigurationContext getAxisConfigurationContext();
        
    /**
     * Gets the host name. During the message processing a host name may be needed, 
     * for example for generating a message id. Because the host name of the machine 
     * Holodeck B2B runs on may be for internal use only it is possible to set an 
     * <i>external</i> host name using the <i>ExternalHostName</i> parameter.
     * <p>When no host name is specified in the configuration the first host name bound to
     * a network interface (not being the loopback adapter) will be used. If there
     * is still no host name a random id will be used.
     * 
     * @return  The host name
     */
    public String getHostName();

    /**
     * Gets the Holodeck B2B home directory.
     * 
     * @return  The Holodeck B2B home directory.
     */
    public String getHolodeckB2BHome ();
    
    /**
     * Gets the directory to use for temporarily storing files.
     * <p>By default this the <code>temp</code> directory in the Holodeck B2B
     * installation. The directory to use can also be specified using the
     * <i>TempDir</i> parameter.
     * <p>It is RECOMMENDED to create a subdirectory in this directory when 
     * regularly storing files in the temp directory.
     * 
     * @return  The absolute path to the temp directory. Ends with a directory
     *          separator.
     */
    public String getTempDirectory();
    
    /**
     * Indicates whether bundling of signal message units in a response message is allowed. When enabled Holodeck B2B
     * can add multiple signal message units generated during the processing of the request message to the response. 
     * This however will create ebMS messages that DO NOT conform to the ebMS v3 Core Spec and AS4 profile.
     * <p>The default setting is not to allow this bundling to ensure Core Spec and AS4 compliant ebMS messages. To 
     * enable the feature set the <i>AllowSignalBundling</i> to "on" or "true".
     * 
     * @return Indication whether bundling of signals in a response is allowed
     */    
    public boolean allowSignalBundling();

    /**
     * Gets the default setting whether Errors on Errors should be reported to the sender of the faulty error. This 
     * setting can be overriden in the P-Mode configuration. However the problem that causes an error to be in error is 
     * often an invalid message reference. In such cases the error can not be assigned a P-Mode, so the P-Mode can not 
     * configure the behaviour.
     * 
     * @return <code>true</code> if generated errors on errors should by default be reported to the sender,<br>
     *         <code>false</code> otherwise 
     */
    public boolean shouldReportErrorOnError();

    /**
     * Gets the default setting whether Errors on Receipts should be reported to the sender of the faulty receipt. This 
     * setting can be overriden in the P-Mode configuration. However the problem that causes an error to be in error is 
     * often an invalid message reference. In such cases the receipt can not be assigned a P-Mode, so the P-Mode can not 
     * configure the behaviour.
     * 
     * @return <code>true</code> if generated errors on receipts should by default be reported to the sender,<br>
     *         <code>false</code> otherwise 
     */
    public boolean shouldReportErrorOnReceipt();
    
    /**
     * Indicates if the references in an Error signal message unit should be checked using the strict requirements 
     * defined in the Core Specification (all references equal) or if a bit more relaxed check can be used (signal level
     * reference empty, but individual errors have the same reference).
     * <p>Default the more relaxed check is used. To change this set the value of the <i>StrictErrorReferencesCheck<i>
     * parameter to <i>"true"</i>.
     * 
     * @return <code>true</code> if generated errors on receipts should by default be reported to the sender,<br>
     *         <code>false</code> otherwise 
     */
    public boolean useStrictErrorRefCheck();

    /**
     * Gets the path to the keystore containing the private keys and related certificates that are used for signing
     * and decryption of messages.
     * 
     * @return The path to the <i>"private"</i> keystore.
     */
    public String getPrivateKeyStorePath();
    
    /**
     * Gets the password for the keystore that holds the certificates with the private keys. 
     * 
     * @return  The password for accessing the keystore with the private keys
     */
    public String getPrivateKeyStorePassword();
    
    /**
     * Gets the path to the keystore containing the certificates (i.e. public keys) that are used for encrypting 
     * messages and verification of a signed messages.
     * 
     * @return The path to the <i>"public"</i> keystore.
     */
    public String getPublicKeyStorePath();

    /**
     * Gets the password for the keystore that holds the certificates with the public keys. 
     * 
     * @return  The password for accessing the keystore with the public keys
     */
    public String getPublicKeyStorePassword();

    /**
     * Gets the global setting whether Holodeck B2B should check if a certificate is revoked. As an error that occurs 
     * during the revocation check will result in rejection of the complete ebMS message the default value is <i>false
     * </i>. If required the revocation check can be enable in the P-Mode configuration.
     * <p>To change the default setting for the revocation check change the value of the <i>CertificateRevocationCheck
     * </i> parameter to <i>"true"</i>.
     * 
     * @return <code>true</code> if the revocation of certificates should by default be checked,<br>
     *         <code>false</code> otherwise 
     */
    public boolean shouldCheckCertificateRevocation();
}

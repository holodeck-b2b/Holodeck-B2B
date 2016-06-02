/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.interfaces.pmode.security;

/**
 * Describes the P-Mode parameters related to the security processing of messages. 
 * <p>Security processing is described in section 7 of the ebMS V3 Core Specification and its related P-Mode parameter 
 * group <b>PMode[1].Security</b> in appendix D.3.6. This P-Mode parameter group defines the content of the default 
 * WS-Security header in the message. 
 * <p>This interface also includes the configuration of the username token that can be included in the WS-Security 
 * header targeted at the "ebms" role/actor. In the ebMS specification the P-Mode parameter group <b>Authorization</b> 
 * under <b>PMode.Initiator</b> and <b>PMode.Responder</b> are used for configuration of this username token.<br>
 * The spec defines <b>PMode[1].Security.PModeAuthorize</b> that determines whether the username token in the "ebms" 
 * header should be included. In Holodeck B2B this parameter is derived from the existence of a configuration for a 
 * username token targeted at the "ebms" role/actore, i.e. <b>PMode[1].Security.PModeAuthorize</b> := <code>
 * {@link #getUsernameTokenConfiguration(WSSHeaderTarget.EBMS)} != null</code> 
 * <p><b>NOTE:</b> This interface is also used to define the authorization configuration for pull requests on a 
 * sub-channel MPC. In that case however only the user name token for the "ebms" role/actor and signature configuration 
 * are needed.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @aurhor Bram Bakx <bram at holodeck-b2b.org>
 */
public interface ISecurityConfiguration {
    
    /**
     * Identifiers for the target of the WS-Security headers that can be included in an ebMS message. 
     */
    public enum WSSHeaderTarget {
        DEFAULT,
        EBMS
    }
    
    /**
     * Gets the configuration for the UsernameToken that is/should be included in the WSS header targeted at the 
     * specified role/actor. 
     * <p>The ebMS V3 Core Specification defines that an <code>wsse:UsernameToken</code> can be included twice in a
     * message, once in the default WSS header and once in a WSS header targeted at the "ebms" role/actor. That second
     * header is for authentication of the ebMS message (for example when pulling). 
     * <p><b>NOTE:</b> The value of P-Mode parameter <i>PMode[1].Security.PModeAuthorize</i> can be derived from the 
     * return value of this method when requesting the username token for the "ebms" role: When returning a non null 
     * value the P-Mode parameter value is <i>true</i>, otherwise it is <i>false</i>. 
     * 
     * @param   target      The target of the WSS header in which the username token should be included
     * @return  An {@link IUsernameTokenConfiguration} object containing the information to fill the 
     *          <code>wsse:UsernameToken</code> element in the security header with the specified target,or<br>
     *          <code>null</code> when there should be no <code>wsse:UsernameToken</code> element in the security header 
     *          with the specified target
     */
    public IUsernameTokenConfiguration getUsernameTokenConfiguration(WSSHeaderTarget target);
    
    /**
     * Gets the configuration for adding or validating a Signature in the WS-Security header. 
     * 
     * @return  An {@link ISigningConfiguration} object containing the information to add/validate a Signature to the
     *          WS-Security header, or<br>
     *          <code>null</code> when there should be no signature in the WS-Security header.
     */
    public ISigningConfiguration getSignatureConfiguration();
    
    /**
     * Gets the configuration for adding or validating encryption in the WS-Security header. 
     * 
     * @return  An {@link IEncryptionConfiguration} object containing the information to add/validate Encryption to the
     *          WS-Security header, or<br>
     *          <code>null</code> when there should be no encryption in the WS-Security header.
     */
    public IEncryptionConfiguration getEncryptionConfiguration();
}

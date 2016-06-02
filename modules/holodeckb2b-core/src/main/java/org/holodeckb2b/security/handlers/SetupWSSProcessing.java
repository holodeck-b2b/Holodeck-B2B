/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.security.handlers;

import java.util.Properties;
import org.apache.axis2.context.MessageContext;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.holodeckb2b.ebms.axis2.MessageContextUtils;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.persistency.entities.MessageUnit;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.pmode.security.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.security.ISigningConfiguration;
import org.holodeckb2b.security.callbackhandlers.PasswordCallbackHandler;
import org.holodeckb2b.security.util.SecurityUtils;

/**
 * Is the <i>IN_FLOW</i> handler that sets up the message context for processing the WS-Security headers. This consists
 * of preparing the configuration of the <code>Crypto</code> engine used for decryption and validation of the signature
 * and setting the detection of replay attacks.
 * <p>Part of signature validation is checking if the Certificate used for creating the signature is still valid and not
 * revoked. Not all PKI's might however use CRL. Therefor the revocation check is optional. There is a global default 
 * setting, but if needed this can be overridden by the P-Mode.
 * <p><b>NOTE:</b> Replay attack detection is currently not supported! Both the ebMS V3 Core Specification and the AS4
 * profile do not mention replay attack detection, so this does not influence conformance.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class SetupWSSProcessing extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

        @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {
        
        log.debug("Set up Crypto engine configuration");
        Properties sigProperties = SecurityUtils.createCryptoConfig(SecurityUtils.CertType.pub);
        mc.setProperty(WSHandlerConstants.SIG_PROP_REF_ID, "" + sigProperties.hashCode());
        mc.setProperty("" + sigProperties.hashCode(), sigProperties);
        Properties decProperties = SecurityUtils.createCryptoConfig(SecurityUtils.CertType.priv);
        mc.setProperty(WSHandlerConstants.DEC_PROP_REF_ID, "" + decProperties.hashCode());
        mc.setProperty("" + decProperties.hashCode(), decProperties);
        
        // Set global settings as default and overwrite if necessary with P-Mode parameters from primary MU
        //
        // Disable BSP conformance check. Although AS4 requires conformance with BSP1.1 the check is disable to allow
        // more recent encryption algorithms. 
        mc.setProperty(WSHandlerConstants.IS_BSP_COMPLIANT, "false");
        
        // Revocation check
        log.debug("Set revocation check for certifcates to default setting");
        mc.setProperty(WSHandlerConstants.ENABLE_REVOCATION, 
                     Boolean.toString(HolodeckB2BCoreInterface.getConfiguration().shouldCheckCertificateRevocation()));        
        // Replay detection (disabled in this version)
        mc.setProperty(WSHandlerConstants.ENABLE_NONCE_CACHE, "false");
        mc.setProperty(WSHandlerConstants.ENABLE_TIMESTAMP_CACHE, "false");
        
        log.debug("Get the primary message unit for this message to check specific setting");
        MessageUnit primaryMU = MessageContextUtils.getPrimaryMessageUnit(mc).entity;
        if (primaryMU == null)
            // No primary message => this is probably an empty response
            return InvocationResponse.CONTINUE;

        log.debug("The primary message unit is a " + primaryMU.getClass().getSimpleName() 
                                                                        + " with msg-id=" + primaryMU.getMessageId());
        
        // 2. Get the security settings
        IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(primaryMU.getPMode());
        
        // It is possible that we can not find a PMode when the primary message unit is a signal. In that case there
        // is no special configuration needed
        if (pmode == null) {
            log.warn("No P-Mode found for primary message unit [msgId=" + primaryMU.getMessageId() 
                                                                                    + "]. No specific configuration!");
            return InvocationResponse.CONTINUE;
        }
        
        // For now we only support One-Way so just one leg
        ILeg leg = pmode.getLegs().iterator().next();
        
        // We need to determine whether we are the initiator of the MEP or the responder to get the correct settings
        boolean initiator = false;
        // If the primary message is a pull request it can have a specific security configuration
        ISecurityConfiguration  tpSecConfig = null;
        
        /* If this message unit is an user message Holodeck B2B acts as the initiator when the http message is a
            request, otherwise it is the response to a PullRequest and Holodeck B2B the responder.
           The Error and Receipt signal messages are always response to another message and therefor Holodeck B2B
            will always be the initiator when receiving a error or receipt signals
           NOTE that this is only valid for One-Way MEP's! For Two-Way MEP's the MEP binding has to be considered!
        */ 
        if (primaryMU instanceof UserMessage) {
            log.debug("Primary message unit is user message, detect initiator or responder");
            initiator = isInFlow(INITIATOR);
        } else {
            log.debug("Primary message unit is a error or receipt signal, always responder");
            initiator = true;
        }
        
        // Get security configuration for the other tradingpartner because its
        // securityconfiguration describes how to validate this signature.
        ITradingPartnerConfiguration tradingPartner = initiator ? pmode.getResponder() : pmode.getInitiator(); 
        tpSecConfig = tradingPartner != null ? tradingPartner.getSecurityConfiguration() : null;
        ISigningConfiguration sigConfig = tpSecConfig != null ? tpSecConfig.getSignatureConfiguration() : null;    
        
        // Get security configuration for the tradingpartner because its
        // securityconfiguration describes how decrypt the message.
        tradingPartner = initiator ? pmode.getInitiator() : pmode.getResponder(); 
        tpSecConfig = tradingPartner != null ? tradingPartner.getSecurityConfiguration() : null;
        
        IEncryptionConfiguration encConfig = tpSecConfig != null ? tpSecConfig.getEncryptionConfiguration() : null;
        
        
        log.debug("Security is " + (sigConfig != null || encConfig != null ? "" : "not") + " configured for the primary message unit");
        
        if ( (sigConfig == null) && (encConfig == null) )
            // No security needed, done
            return InvocationResponse.CONTINUE;
        
        // Check if there is specific setting for revocation check
    
        Boolean enableRevocation = sigConfig != null ? sigConfig.enableRevocationCheck() : null;
        if (enableRevocation != null) {
            log.debug("P-Mode " + (enableRevocation ? "enables" : "disables") + " revocation check of certificates");
            mc.setProperty(WSHandlerConstants.ENABLE_REVOCATION, enableRevocation.toString());
        } else 
            log.debug("P-Mode does not specify revocation check of certificates, using default value");
        
        if (encConfig != null) {
            log.debug("Encryption used, provide access to private key.");
            PasswordCallbackHandler pwdCBHandler = new PasswordCallbackHandler();
            mc.setProperty(WSHandlerConstants.PW_CALLBACK_REF, pwdCBHandler);
        
            // The password to access the certificate in the keystore, alias converted to lower case because JKS 
            // aliasses are case insensitive and in lower case
            pwdCBHandler.addUser(encConfig.getKeystoreAlias().toLowerCase(), encConfig.getCertificatePassword());
        } else {
            log.debug("Encryption configuration is NULL! No access to private key.");
        }
        
        return InvocationResponse.CONTINUE;
    }
}

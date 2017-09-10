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
package org.holodeckb2b.pmode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;
import org.holodeckb2b.interfaces.security.ICertificateManager.CertificateUsage;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the default implementation of {@link IPModeValidator} that will check if a P-Mode is correct. Since Holodeck B2B
 * accepts any value as long as processing permits and also allows very generic P-Modes, this implementation only checks
 * basic settings like the values for MEP, MEP binding, MPC for pulling and security settings. If needed because of
 * specific requirements in a certain domain a custom validator can be used.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class BasicPModeValidator implements IPModeValidator {

    private static final Set<String> VALID_MEPS, VALID_MEP_BINDINGS;
    static {
        VALID_MEPS = new HashSet<>();
        VALID_MEPS.add(EbMSConstants.ONE_WAY_MEP);
        VALID_MEPS.add(EbMSConstants.TWO_WAY_MEP);

        VALID_MEP_BINDINGS = new HashSet();
        VALID_MEP_BINDINGS.add(EbMSConstants.ONE_WAY_PULL);
        VALID_MEP_BINDINGS.add(EbMSConstants.ONE_WAY_PUSH);
        VALID_MEP_BINDINGS.add(EbMSConstants.TWO_WAY_PUSH_PUSH);
        VALID_MEP_BINDINGS.add(EbMSConstants.TWO_WAY_PULL_PUSH);
        VALID_MEP_BINDINGS.add(EbMSConstants.TWO_WAY_PULL_PULL);
        VALID_MEP_BINDINGS.add(EbMSConstants.TWO_WAY_PUSH_PULL);
    }

    @Override
    public Collection<PModeValidationError> validatePMode(final IPMode pmode) {
        Collection<PModeValidationError>    errors = new ArrayList<>();

        errors.addAll(checkGeneralParameters(pmode));
        errors.addAll(checkSecurityParameters(pmode));
        errors.addAll(checkPullingMPCs(pmode));

        return errors;
    }

    private Collection<PModeValidationError> checkGeneralParameters(final IPMode pmode) {
        Collection<PModeValidationError>    errors = new ArrayList<>();

        // PMode.MEP must contain a valid MEP url
        String mep = pmode.getMep();
        if (Utils.isNullOrEmpty(mep) || !VALID_MEPS.contains(mep))
            errors.add(new PModeValidationError("PMode.MEP", "PMode.MEP must contain a valid MEP url"));

        // PMode.MEPBinding must contain a valid MEP binding url
        String mepBinding = pmode.getMepBinding();
        if (Utils.isNullOrEmpty(mepBinding) || !VALID_MEP_BINDINGS.contains(mepBinding))
            errors.add(new PModeValidationError("PMode.MEPbinding",
                                    "PMode.MEPbinding must contain an url that identifies the MEP binding to be used"));

        // The number of legs in P-Mode should match to used MEP
        List<? extends ILeg> legs = pmode.getLegs();
        if (Utils.isNullOrEmpty(legs))
            errors.add(new PModeValidationError("PMode.Legs", "The PMode must contain at least one Leg"));
        else if (EbMSConstants.ONE_WAY_MEP.equals(mep) && legs.size() != 1)
            errors.add(new PModeValidationError("PMode.Legs", "A One-Way PMode must contain only one Leg"));
        else if (EbMSConstants.TWO_WAY_MEP.equals(mep) && legs.size() != 2)
            errors.add(new PModeValidationError("PMode.Legs", "A Two-Way PMode must contain two Legs"));

        return errors;
    }

    private Collection<PModeValidationError> checkSecurityParameters(IPMode pmode) {
        Collection<PModeValidationError>    errors = new ArrayList<>();

        // For checking the key references we need to know whether Holodeck acts as intiator or responder
        boolean hb2bIsInitiator = PModeUtils.isHolodeckB2BInitiator(pmode);

        // Any configured Username Token must contain both the username and password and referenced certificate should
        // exist in the keystores and when it's a private key the password should be specified
        //
        // Initiator
        ITradingPartnerConfiguration tpCfg = pmode.getInitiator();
        ISecurityConfiguration secCfg = tpCfg != null ? tpCfg.getSecurityConfiguration() : null;
        errors.addAll(checkUsernameTokenParameters(secCfg, "PMode.Initiator"));
            errors.addAll(checkX509Parameters(secCfg, "PMode.Initiator", hb2bIsInitiator));

        // Responder
        tpCfg = pmode.getResponder();
        secCfg = tpCfg != null ? tpCfg.getSecurityConfiguration() : null;
        errors.addAll(checkUsernameTokenParameters(secCfg, "PMode.Responder"));
        errors.addAll(checkX509Parameters(secCfg, "PMode.Responder", !hb2bIsInitiator));

        // Pull specific configs
        List<? extends ILeg> legs = pmode.getLegs();
        if (!Utils.isNullOrEmpty(legs))
            for (int i = 0; i < legs.size(); i++) {
                Collection<IPullRequestFlow> pullCfgs = legs.get(i).getPullRequestFlows();
                if (!Utils.isNullOrEmpty(pullCfgs)) {
                    String parentParameterName = "PMode[" + i + "]";
                    boolean hb2bSends = PModeUtils.doesHolodeckB2BTrigger(legs.get(i));
                    for (IPullRequestFlow pullCfg : pullCfgs) {
                        parentParameterName += ".Subchannel(" + pullCfg.getMPC() + ")";
                        secCfg = pullCfg.getSecurityConfiguration();
                        errors.addAll(checkUsernameTokenParameters(secCfg, parentParameterName));
                        errors.addAll(checkX509Parameters(secCfg, parentParameterName, hb2bSends));
                    }
                }
        }
        return errors;
    }

    private Collection<PModeValidationError> checkUsernameTokenParameters(final ISecurityConfiguration parentSecurityCfg
                                                                         ,final String parentParameterName) {
        Collection<PModeValidationError>    errors = new ArrayList<>();
        if (parentSecurityCfg != null) {
            IUsernameTokenConfiguration utConfig =
                        parentSecurityCfg.getUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT);
            if (utConfig != null) {
                if (Utils.isNullOrEmpty(utConfig.getUsername()))
                    errors.add(new PModeValidationError(parentParameterName + ".UsernameToken[default].username",
                                                    "The configuration of a Username token must contain a username"));
                if (Utils.isNullOrEmpty(utConfig.getPassword()))
                    errors.add(new PModeValidationError(parentParameterName + ".UsernameToken[default].password",
                                                    "The configuration of a Username token must contain a password"));
            }
            utConfig = parentSecurityCfg.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS);
            if (utConfig != null) {
                if (Utils.isNullOrEmpty(utConfig.getUsername()))
                    errors.add(new PModeValidationError(parentParameterName + ".UsernameToken[ebms].username",
                                                    "The configuration of a Username token must contain a username"));
                if (Utils.isNullOrEmpty(utConfig.getPassword()))
                    errors.add(new PModeValidationError(parentParameterName + ".UsernameToken[ebms].password",
                                                    "The configuration of a Username token must contain a password"));
            }
        }
        return errors;
    }

    private Collection<PModeValidationError> checkX509Parameters(final ISecurityConfiguration parentSecurityCfg,
                                                                 final String parentParameterName,
                                                                 final boolean privateKey) {
        Collection<PModeValidationError>    errors = new ArrayList<>();
        if (parentSecurityCfg != null) {
            ISigningConfiguration sigConfig = parentSecurityCfg.getSignatureConfiguration();
            if (sigConfig != null && privateKey) {
                if (Utils.isNullOrEmpty(sigConfig.getKeystoreAlias()))
                    errors.add(new PModeValidationError(parentParameterName + ".Signature.KeyAlias",
                                                                "A reference to the private key must be specified"));
                else if (Utils.isNullOrEmpty(sigConfig.getCertificatePassword()))
                    errors.add(new PModeValidationError(parentParameterName + ".Signature.KeyPassword",
                                                                "A password for the private key must be specified"));
                else if (!isPrivateKeyAvailable(sigConfig.getKeystoreAlias(),
                                                              sigConfig.getCertificatePassword()))
                    errors.add(new PModeValidationError(parentParameterName + ".Signature",
                                                             "The private key specified for signing is not available"));
            }

            IEncryptionConfiguration encConfig = parentSecurityCfg.getEncryptionConfiguration();
            if (encConfig != null) {
                if (Utils.isNullOrEmpty(encConfig.getKeystoreAlias()))
                    errors.add(new PModeValidationError(parentParameterName + ".Encryption.KeyAlias",
                                                        "A reference to the " + (privateKey ? "private key" :
                                                                                             "certificate")
                                                        + " must be specified"));
                else if (privateKey && Utils.isNullOrEmpty(encConfig.getCertificatePassword()))
                    errors.add(new PModeValidationError(parentParameterName + ".Encryption.KeyPassword",
                                                                "A password for the private key must be specified"));
                else if (privateKey && !isPrivateKeyAvailable(encConfig.getKeystoreAlias(),
                                                              encConfig.getCertificatePassword()))
                    errors.add(new PModeValidationError(parentParameterName + ".Encryption",
                                                          "The private key specified for decryption is not available"));
                else if (!privateKey && !isCertificateAvailable(encConfig.getKeystoreAlias(),
                                                                CertificateUsage.Encryption))
                    errors.add(new PModeValidationError(parentParameterName + ".Encryption",
                                                          "The specified certificate for encryption is not available"));
            }
        }
        return errors;
    }

    private Collection<PModeValidationError> checkPullingMPCs(IPMode pmode) {
        Collection<PModeValidationError>    errors = new ArrayList<>();

        // If a leg contains more than one PullRequestFlow, each one must define a MPC which must be a sub-channel of
        // the MPC defined in the UserMessageFlow of the leg (if defined, otherwise any MPC is okay)
        //
        List<? extends ILeg> legs = pmode.getLegs();
        if (!Utils.isNullOrEmpty(legs)) {
            for (int i = 0; i < legs.size(); i++) {
                ILeg leg = legs.get(i);
                IUserMessageFlow userMsgFlow = leg.getUserMessageFlow();
                String userMsgMpc = userMsgFlow != null && userMsgFlow.getBusinessInfo() != null ?
                                        userMsgFlow.getBusinessInfo().getMpc() : null;

                Collection<IPullRequestFlow> pullCfgs = leg.getPullRequestFlows();
                if (!Utils.isNullOrEmpty(pullCfgs) && pullCfgs.size() > 1) {
                    String parentParameterName = "PMode[" + i + "]";
                    Set<String> pullMPCs = new HashSet<>();
                    for (IPullRequestFlow pullCfg : pullCfgs) {
                        String pullMPC = pullCfg.getMPC();
                        if (Utils.isNullOrEmpty(pullMPC))
                            errors.add(new PModeValidationError(parentParameterName + ".SubChannel.MPC",
                                                                "Each PullRequestFlow must define a (sub-)MPC value"));
                        else {
                            parentParameterName += ".Subchannel(" + pullMPC + ")";
                            if (!Utils.isNullOrEmpty(userMsgMpc) && !pullMPC.startsWith(userMsgMpc))
                                errors.add(new PModeValidationError(parentParameterName,
                                        "The MPC in the PullRequestFlow must be sub-channel of the user message MPC"));
                            if (!pullMPCs.add(pullMPC))
                                errors.add(new PModeValidationError(parentParameterName,
                                                                   "The MPCs in the PullRequestFlows must be unique"));                                ;
                        }
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Checks whether a key pair is available in the installed <i>Certificate Manager</i>.
     *
     * @param keystoreAlias         The alias the key pair should be registered under
     * @param certificatePassword   The password to get access to the keypair
     * @return  <code>true</code> if a keypair exists and is accessible for the given alias and password,<br>
     *          <code>false</code> otherwise
     */
    private boolean isPrivateKeyAvailable(String keystoreAlias, String certificatePassword) {
        try {
            return HolodeckB2BCore.getCertificateManager().getKeyPair(keystoreAlias, certificatePassword) != null;
        } catch (SecurityProcessingException ex) {
            return false;
        }
    }

    /**
     * Checks whether a certificate is available for the given usage in the installed <i>Certificate Manager</i>.
     *
     * @param keystoreAlias         The alias the certificate should be registered under
     * @param certificateUsage      The intended use of the certificate
     * @return  <code>true</code> if a certificate for the given usage and with the given alias exists,<br>
     *          <code>false</code> otherwise
     */
    private boolean isCertificateAvailable(String keystoreAlias, CertificateUsage certificateUsage) {
        try {
            return HolodeckB2BCore.getCertificateManager().getCertificate(certificateUsage, keystoreAlias) != null;
        } catch (SecurityProcessingException ex) {
            return false;
        }
    }
}

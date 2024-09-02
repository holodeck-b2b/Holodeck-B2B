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
package org.holodeckb2b.ebms3.pmode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;

/**
 * Is the default implementation of {@link IPModeValidator} for ebMS3 / AS4 P-Modes. Since Holodeck B2B for most
 * parameters accepts any  value as long as processing permits and also allows very generic P-Modes, this implementation
 * only checks basic settings like the values for MEP, MEP binding, MPC for pulling and security settings. If needed
 * because of specific requirements in a certain domain a custom validator could be added.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class BasicPModeValidator implements IPModeValidator {

    protected static Set<String> VALID_MEPS, VALID_MEP_BINDINGS;
    static {
        VALID_MEPS = new HashSet<>();
        VALID_MEPS.add(EbMSConstants.ONE_WAY_MEP);
        VALID_MEPS.add(EbMSConstants.TWO_WAY_MEP);

        VALID_MEP_BINDINGS = new HashSet<>();
        VALID_MEP_BINDINGS.add(EbMSConstants.ONE_WAY_PULL);
        VALID_MEP_BINDINGS.add(EbMSConstants.ONE_WAY_PUSH);
        VALID_MEP_BINDINGS.add(EbMSConstants.TWO_WAY_PUSH_PUSH);
        VALID_MEP_BINDINGS.add(EbMSConstants.TWO_WAY_PULL_PUSH);
        VALID_MEP_BINDINGS.add(EbMSConstants.TWO_WAY_PULL_PULL);
        VALID_MEP_BINDINGS.add(EbMSConstants.TWO_WAY_PUSH_PULL);
    }

    @Override
    public String getName() {
    	return "HB2B Default ebMS3/AS4";
    }

	@Override
	public boolean doesValidate(String pmodeType) {
		return pmodeType.startsWith("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704");
	}

    @Override
    public Collection<PModeValidationError> validatePMode(final IPMode pmode) {
        Collection<PModeValidationError>    errors = new ArrayList<>();

        errors.addAll(checkMEPParameters(pmode));
        errors.addAll(checkPullingMPCs(pmode));

        return errors;
    }

    protected Collection<PModeValidationError> checkMEPParameters(final IPMode pmode) {
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


    protected Collection<PModeValidationError> checkPullingMPCs(IPMode pmode) {
        Collection<PModeValidationError>    errors = new ArrayList<>();

        // If a leg contains more than one PullRequestFlow, each one must define a MPC which must be a sub-channel of
        // the MPC defined in the UserMessageFlow of the leg (if defined, otherwise use default MPC)
        //
        List<? extends ILeg> legs = pmode.getLegs();
        if (!Utils.isNullOrEmpty(legs)) {
            for (int i = 0; i < legs.size(); i++) {
                ILeg leg = legs.get(i);
                IUserMessageFlow userMsgFlow = leg.getUserMessageFlow();
                String userMsgMpc = userMsgFlow != null && userMsgFlow.getBusinessInfo() != null ?
                                        userMsgFlow.getBusinessInfo().getMpc() : EbMSConstants.DEFAULT_MPC;

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
                            if (!pullMPC.startsWith(userMsgMpc))
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


}

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
package org.holodeckb2b.pmode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import org.holodeckb2b.common.messagemodel.util.CompareUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IAgreement;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IBusinessInfo;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.security.tokens.IAuthenticationInfo;
import org.holodeckb2b.security.tokens.UsernameToken;
import org.holodeckb2b.security.tokens.X509Certificate;
import org.holodeckb2b.security.util.SecurityUtils;

/**
 * Is a helper class for finding the correct processing configuration for a {@see IMessageUnit}. This starts with
 * finding the P-Mode and within the P-Mode the correct leg and channel.
 * <p>The P-Mode specifies how the message unit should be processed and therefore is essential in the processing chain.
 * Because the P-Mode id might not be available during message processing the P-Mode must be found based on the
 * available message meta data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IPMode
 */
public class PModeFinder {

    /**
     *
     */
    protected static enum PARAMETERS {ID, FROM, FROM_ROLE, TO, TO_ROLE, SERVICE, ACTION, MPC, AGREEMENT}

    /**
     *
     */
    protected static Map<PARAMETERS, Integer> MATCH_WEIGHTS; // {37, 7, 2, 7, 2, 5, 5, 1};
    static {
        final Map<PARAMETERS, Integer> aMap = new EnumMap<> (PARAMETERS.class);
        aMap.put(PARAMETERS.ID, 37);
        aMap.put(PARAMETERS.FROM, 7);
        aMap.put(PARAMETERS.FROM_ROLE, 2);
        aMap.put(PARAMETERS.TO, 7);
        aMap.put(PARAMETERS.TO_ROLE, 2);
        aMap.put(PARAMETERS.SERVICE, 5);
        aMap.put(PARAMETERS.ACTION, 5);
        aMap.put(PARAMETERS.MPC, 1);
        aMap.put(PARAMETERS.AGREEMENT, 1);

        MATCH_WEIGHTS = Collections.unmodifiableMap(aMap);
    }

    /**
     * Finds the P-Mode for a received user message message unit.
     * <p>The ebMS specifications do not describe or recommend how the P-Mode for a user message should be determined,
     * see also <a href="https://issues.oasis-open.org/browse/EBXMLMSG-48">issue 48 in the OASIS TC issue tracker</a>.
     * In the issue two suggestions for matching the P-Mode are given.
     * <p>Based on these we compare the meta-data from the message with all P-Modes and return the best matching P-Mode.
     * The following table shows the information that is used for matching and their importance (expressed as a weight).
     * The match of a P-Mode is the sum of the weights for the elements that are equal to the corresponding P-Mode
     * parameter.
     * <p><table border="1">
     * <tr><th>Element</th><th>Weight</th></tr>
     * <tr><td>PMode id</td><td>37</td></tr>
     * <tr><td>From Party Id's</td><td>7</td></tr>
     * <tr><td>From.Role</td><td>2</td></tr>
     * <tr><td>To Party Id's</td><td>7</td></tr>
     * <tr><td>To.Role</td><td>2</td></tr>
     * <tr><td>Service</td><td>5</td></tr>
     * <tr><td>Action</td><td>5</td></tr>
     * <tr><td>Agreement ref</td><td>1</td></tr>
     * <tr><td>MPC</td><td>1</td></tr>
     * </table> </p>
     * <p>If there is a mismatch for one of the elements the P-Mode is considered as a mismatch.</p>
     *
     * @param mu        The user message message unit to find the P-Mode for
     * @return          The P-Mode for the message unit if the message unit can be matched to a P-Mode,
     *                  <code>null</code> if no P-Mode could be found for the user message message unit.
     */
    public static IPMode forReceivedUserMessage(final IUserMessage mu) {
        final IPModeSet pmodes = HolodeckB2BCoreInterface.getPModeSet();
        IPMode    hPMode = null;
        int       hValue = 0;

        if (pmodes == null)
            return null;

        for (final IPMode p : pmodes.getAll()) {
            // Ignore this P-Mode if it is configured for sending
            if (p.getMepBinding().equals(EbMSConstants.ONE_WAY_PUSH)
                && p.getLeg(ILeg.Label.REQUEST).getProtocol() != null
                && !Utils.isNullOrEmpty(p.getLeg(ILeg.Label.REQUEST).getProtocol().getAddress())) {
                continue;
            }

            int cValue = 0;
            // P-Mode id and agreement info are contained in optional element
            final IAgreementReference agreementRef = mu.getCollaborationInfo().getAgreement();

            if (p.includeId() != null && p.includeId()) {
                // The P-Mode id can be used for matching, so check if one is given in message
                if (agreementRef != null) {
                    final String pid = agreementRef.getPModeId();
                    if (!Utils.isNullOrEmpty(pid) && pid.equals(p.getId()))
                        cValue = MATCH_WEIGHTS.get(PARAMETERS.ID);
                }
            }

            // Check agreement info
            final IAgreement agreementPMode = p.getAgreement();
            if (agreementPMode != null) {
                final int i = Utils.compareStrings(agreementRef != null ? agreementRef.getName() : null
                                                  , agreementPMode.getName());
                switch (i) {
                    case -2 :
                    case 2 :
                        // mismatch on agreement name, either because different or one defined in P-Mode but not in msg
                        continue;
                    case 0 :
                        // names equal, but for match also types must be equal
                        final int j = Utils.compareStrings(agreementRef.getType(), agreementPMode.getType());
                        if (j == -1 || j == 0)
                            cValue += MATCH_WEIGHTS.get(PARAMETERS.AGREEMENT);
                        else
                            continue; // mis-match on agreement type
                    case -1 :
                        // both P-Mode and message agreement ref are empty, ignore
                    case 1 :
                        // the message contains agreement ref, but P-Mode does not, ignore
                }
            }

            // Check trading partner info
            final ITradingPartner from = mu.getSender(), to = mu.getReceiver();
            ITradingPartner fromPMode = null, toPMode = null;
            if (p.getMepBinding().equals(EbMSConstants.ONE_WAY_PUSH)) {
                fromPMode = p.getInitiator(); toPMode = p.getResponder();
            } else {
                fromPMode = p.getResponder(); toPMode = p.getInitiator();
            }

            // Check To info
            if (toPMode != null) {
                final int c = Utils.compareStrings(to.getRole(), toPMode.getRole());
                if ( c == -1 || c == 0)
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.TO_ROLE);
                else if (c != 1)
                    continue; // mis-match on To party role
                Collection<IPartyId> pmodeToIds = toPMode.getPartyIds();
                if (!Utils.isNullOrEmpty(pmodeToIds))
                    if (CompareUtils.areEqual(to.getPartyIds(), pmodeToIds))
                        cValue += MATCH_WEIGHTS.get(PARAMETERS.TO);
                    else
                        continue; // mis-match on To party id('s)
            }

            // Check From info
            if (fromPMode != null) {
                final int c = Utils.compareStrings(from.getRole(), fromPMode.getRole());
                if ( c == -1 || c == 0)
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.FROM_ROLE);
                else if (c != 1)
                    continue; // mis-match on From party role
                Collection<IPartyId> pmodeFromIds = fromPMode.getPartyIds();
                if (!Utils.isNullOrEmpty(pmodeFromIds))
                    if (CompareUtils.areEqual(from.getPartyIds(), pmodeFromIds))
                        cValue += MATCH_WEIGHTS.get(PARAMETERS.FROM);
                    else
                        continue;  // mis-match on From party id('s)
            }

            // Next info items are defined per Leg basis, for now we only have one-way MEP, so only one leg to check
            // Within the leg all relevant information is contained in the user message flow.
            final IUserMessageFlow  flow = p.getLeg(ILeg.Label.REQUEST).getUserMessageFlow();
            final IBusinessInfo     pmBI = flow != null ? flow.getBusinessInfo() : null;
            if (pmBI != null) {
                // Check Service
                final IService svcPMode = pmBI.getService();
                if (svcPMode != null) {
                    final IService svc = mu.getCollaborationInfo().getService();
                    if (svc.getName().equals(svcPMode.getName())) {
                        final int i = Utils.compareStrings(svc.getType(), svcPMode.getType());
                        if (i == -1 || i == 0)
                            cValue += MATCH_WEIGHTS.get(PARAMETERS.SERVICE);
                        else
                            continue; // mis-match on service type
                    } else
                        continue; // mis-match on service name
                }
                // Check Action
                final int i = Utils.compareStrings(mu.getCollaborationInfo().getAction(), pmBI.getAction());
                if (i == 0)
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.ACTION);
                else if (i == -2)
                    continue; // mis-match on action
            }

            // Check MPC, first check the MPC defined in the User Message flow, and if there is none there, check
            // if there is maybe on in Pull Request flow
            String mpc = mu.getMPC();
            if (Utils.isNullOrEmpty(mpc))
                mpc = EbMSConstants.DEFAULT_MPC;
            String mpcPMode = pmBI != null ? pmBI.getMpc() : null;
            // If no MPC is provided in User Message flow, check if this P-Mode is for pulling messages and if it is
            // use the MPC defined in PR flow
            if (Utils.isNullOrEmpty(mpcPMode) && p.getMepBinding().equals(EbMSConstants.ONE_WAY_PULL)
                && p.getLeg(ILeg.Label.REQUEST).getProtocol() != null
                && !Utils.isNullOrEmpty(p.getLeg(ILeg.Label.REQUEST).getProtocol().getAddress()))
            {
                try {
                    mpcPMode = p.getLeg(ILeg.Label.REQUEST).getPullRequestFlows().iterator().next().getMPC();
                } catch (NullPointerException npe) {
                    mpcPMode = null;
                }
                if (Utils.isNullOrEmpty(mpcPMode))
                    mpcPMode = EbMSConstants.DEFAULT_MPC;
                // Now compare MPC, but take into account that MPC from P-Mode can be a sub MPC, so a message that
                // contains parent MPC does match
                if (mpcPMode.startsWith(mpc))
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.MPC);
                else
                    continue; // mis-match on MPC
            } else {
                // If no MPC is given in P-Mode, it uses the default
                if (Utils.isNullOrEmpty(mpcPMode))
                    mpcPMode = EbMSConstants.DEFAULT_MPC;
                // Now compare the MPC values
                if (mpc.equalsIgnoreCase(mpcPMode))
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.MPC);
                else
                    continue; // mis-match on MPC
            }

            // Does this P-Mode better match to the message meta data than the current highest match?
            if (cValue > hValue) {
                // Yes, it does, set it as new best match
                hValue = cValue;
                hPMode = p;
            }
        }

        return hPMode;
    }

    /**
     * Gets the list of P-Modes for which Holodeck B2B is the responder in a pull operation for the given MPC and
     * authentication info.
     *
     *
     * @param mpc   The <i>MPC</i> that the message are exchanged on
     * @return      A collection of {@link IPMode} objects for the P-Modes for which Holodeck B2B is the responder in
     *              a pull operation for the given MPC
     */
    public static Collection<IPMode> findForPulling(final Map<String, IAuthenticationInfo> authInfo, final String mpc) {
        final ArrayList<IPMode> pmodesForPulling = new ArrayList<>();

        for(final IPMode p : HolodeckB2BCoreInterface.getPModeSet().getAll()) {
            // Check if this P-Mode uses pulling with Holodeck B2B being the responder
            final ILeg leg = p.getLegs().iterator().next();
            if (EbMSConstants.ONE_WAY_PULL.equalsIgnoreCase(p.getMepBinding())
               && ( leg.getProtocol() == null || leg.getProtocol().getAddress() == null )
               ) {
                // Leg uses pulling and Holodeck B2B is responder, check if given MPC matches P-Mode MPC defined for UM
                if (!checkMainMPC(leg, mpc))
                    // MPC does not match to one defined in P-Mode
                    continue;

                // Check if authentication info matches to one of the pull request flows
                boolean authorized = false;
                final ISecurityConfiguration initiatorSecCfg = p.getInitiator() == null ? null :
                                                                            p.getInitiator().getSecurityConfiguration();
                final Collection<IPullRequestFlow> flows = leg.getPullRequestFlows();
                if (flows == null || flows.isEmpty()) {
                    // There is no specific configuration for pulling, so use security settings from initiator
                    authorized = verifyPullRequestAuthorization(null, initiatorSecCfg, authInfo);
                } else {
                    for (final Iterator<IPullRequestFlow> it = flows.iterator(); it.hasNext() && !authorized;) {
                        final IPullRequestFlow flow = it.next();
                        if (checkSubMPC(flow, mpc)) {
                            // Check if message satisfies to security config
                            authorized = verifyPullRequestAuthorization(flow.getSecurityConfiguration(),
                                                                        initiatorSecCfg,
                                                                        authInfo);
                        }
                    }
                }
                // If the info from the message is succesfully verified this P-Mode can be pulled
                if (authorized)
                    pmodesForPulling.add(p);
            }
        }

        return pmodesForPulling;
    }

    /**
     * Checks if the given MPC is equal to or is a sub channel of the MPC on which user messages are exchanged for the
     * given Leg.
     *
     * @param leg   The Leg
     * @param mpc   The mpc that must be checked
     * @return      <code>true</code> if<br>
     *                  an MPC is defined in the user message flow of the Leg and it is a prefix of the given MPC,<br>
     *                  or when no MPC is defined for the user message flow,<br>
     *              <code>false</code> otherwise
     */
    private static boolean checkMainMPC(final ILeg leg, final String mpc) {
        String pModeMPC = null;
        try {
            pModeMPC = leg.getUserMessageFlow().getBusinessInfo().getMpc();
        } catch (final NullPointerException npe) {
            pModeMPC = null;
        }

        if (pModeMPC != null && !pModeMPC.isEmpty())
            return mpc.toLowerCase().startsWith(pModeMPC.toLowerCase());
        else
            return true;
    }

    /**
     * Checks if the given MPC is equal to the sub channel MPC defined in the given [pull request] flow.
     *
     * @param flow  The pull request flow to check
     * @param mpc   The mpc that must be checked
     * @return      <code>true</code> if
     *                  an MPC is defined in the pull request flow and it matches the given MPC,<br>
     *                  or when no MPC is defined for the pull request flow,<br>
     *              <code>false</code> otherwise
     */
    private static boolean checkSubMPC(final IPullRequestFlow flow, final String mpc) {
        String pModeMPC = null;
        try {
            pModeMPC = flow.getMPC();
        } catch (final NullPointerException npe) {
            pModeMPC = null;
        }

        if (pModeMPC != null && !pModeMPC.isEmpty())
            return mpc.equalsIgnoreCase(pModeMPC);
        else
            return true;
    }

    /**
     * Helper method to verify that the required authentication defined for the pull request is correctly satisfied.
     * <p>As described in the ebMS V3 Core Specification there are four option to include the authentication information
     * for a pull request, that is using a:<ol>
     * <li>Digital signature in the default WSS Header,</li>
     * <li>Username token in the default WSS header,</li>
     * <li>Username token in the WSS header addressed to the "ebms" actor/role,</li>
     * <li>Transfer-protocol-level identity-authentication mechanism (e.g. TLS)</li></ol>
     * Holodeck B2B supports the first three options, either on their own or as a combination. By default these settings
     * are defined on the trading partner level. But to support authentication for multiple sub-channels or only for
     * the pull request it is possible to define the settings on the <i>pull request flow</i>. When settings are
     * provided both at the trading partner and pull request flow the latter take precedence and will be used for the
     * verification of the supplied authentication info.
     * <p>NOTE: The pull request flow specific configuration only allows for authentication options 1 (signature in
     * default header) and 3 (username token in "ebms" header).
     *
     * @param pullSecCfg    The {@link ISecurityConfiguration} specified on the pull request flow that applies to this
     *                      <i>PullRequest</i>
     * @param tpSecCfg      The {@link ISecurityConfiguration} specified for the trading partner that is the sender of
     *                      the <i>PullRequest</i>
     * @param authInfo      A map containing key value pairs representing all authentication info provided in the
     *                      received message. The keys identify the role defined in the WSS header in which the
     *                      authentication info was found.
     * @return              <code>true</code> if the received message satisfies the authentication requirements defined
     *                      in the flow, <br>
     *                      <code>false</code> otherwise.
     */
    private static boolean verifyPullRequestAuthorization(final ISecurityConfiguration pullSecCfg,
                                                          final ISecurityConfiguration tpSecCfg,
                                                          final Map<String, IAuthenticationInfo> authInfo) {
        boolean verified = true;

        // If there are no security parameters specified there is authentication expected, so there should be no
        // authentication info in the message
        if (pullSecCfg == null && tpSecCfg == null)
            return (authInfo == null || authInfo.isEmpty());
        else if (authInfo == null || authInfo.isEmpty())
            return false;

        // Verify username token in ebms header, first check if pull request flow contains config for this UT
        IUsernameTokenConfiguration expectedUT = pullSecCfg == null ? null : pullSecCfg.getUsernameTokenConfiguration(
                                                                        ISecurityConfiguration.WSSHeaderTarget.EBMS);
        if (expectedUT == null) {
            // if not fall back to trading partner config
            expectedUT = tpSecCfg == null ? null :
                                    tpSecCfg.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.EBMS);
        }

        verified = SecurityUtils.verifyUsernameToken(expectedUT,
                                                    (UsernameToken) authInfo.get(SecurityConstants.EBMS_USERNAMETOKEN));

        // Verify user name token in default header
        expectedUT = tpSecCfg == null ? null :
                                tpSecCfg.getUsernameTokenConfiguration(ISecurityConfiguration.WSSHeaderTarget.DEFAULT);
        verified &= SecurityUtils.verifyUsernameToken(expectedUT,
                                                (UsernameToken) authInfo.get(SecurityConstants.DEFAULT_USERNAMETOKEN));

        // Verify that the expected certificate was used for creating the signature, again start with configuration from
        // PR-flow and fall back to TP
        ISigningConfiguration expectedSig = pullSecCfg == null ? null : pullSecCfg.getSignatureConfiguration();
        if (expectedSig == null)
            expectedSig = tpSecCfg == null ? null : tpSecCfg.getSignatureConfiguration();

        verified &= SecurityUtils.verifySignature(expectedSig,
                                                        (X509Certificate) authInfo.get(SecurityConstants.SIGNATURE));

        return verified;
    }

    /**
     * Retrieves all P-Modes in the current P-Mode set which specify the given URL as the destination of <i>Error</i>
     * signals, i.e. <code>PMode[1].ErrorHandling.ReceiverErrorsTo</code> = <i>«specified URL»</i> or when no specific
     * URL is specified for errors <code>PMode[1].Protocol.Address</code> = <i>«specified URL»</i>
     *
     * @param url   The destination URL
     * @return      Collection of {@link IPMode}s for which errors must be sent to the given URL. When no such P-Mode
     *              exists <code>null</code> is returned
     */
    public static Collection<IPMode> getPModesWithErrorsTo(final String url) {
        final Collection<IPMode>  result = new ArrayList<>();

        for(final IPMode p : HolodeckB2BCoreInterface.getPModeSet().getAll()) {
            // Get all relevent P-Mode info
            final ILeg leg = p.getLegs().iterator().next();
            final IProtocol protocolInfo = leg.getProtocol();
            final IUserMessageFlow flow = leg.getUserMessageFlow();
            final IErrorHandling errorHandling = flow != null ? flow.getErrorHandlingConfiguration() : null;
            // First check if error has specific URL defined or if generic address should be used
            if (errorHandling != null && url.equalsIgnoreCase(errorHandling.getReceiverErrorsTo()))
                result.add(p);
            else if (protocolInfo != null && url.equalsIgnoreCase(protocolInfo.getAddress()))
                result.add(p);
        }

        return result;
    }

    /**
     * Retrieves all P-Modes in the current P-Mode set which specify the given URL as the destination of <i>Receipt</i>
     * signals, i.e. <code>PMode[1].Security.SendReceipt.ReplyTo</code> = <i>«specified URL»</i> or when no specific
     * URL is specified for errors <code>PMode[1].Protocol.Address</code> = <i>«specified URL»</i>
     * <p>NOTE: This P-Mode parameter is not defined in the ebMS V3 Core Specification but defined in Part 2 (see issue
     * https://tools.oasis-open.org/issues/browse/EBXMLMSG-33?jql=project%20%3D%20EBXMLMSG).
     *
     * @param url   The destination URL
     * @return      Collection of {@link IPMode}s for which receipts must be sent to the given URL. When no such P-Mode
     *              exists <code>null</code> is returned
     */
    public static Collection<IPMode> getPModesWithReceiptsTo(final String url) {
        final Collection<IPMode>  result = new ArrayList<>();

        for(final IPMode p : HolodeckB2BCoreInterface.getPModeSet().getAll()) {
            // Get all relevent P-Mode info
            final ILeg leg = p.getLegs().iterator().next();
            final IProtocol protocolInfo = leg.getProtocol();
            final IReceiptConfiguration rcptConfig = leg.getReceiptConfiguration();
            if (rcptConfig != null && url.equalsIgnoreCase(rcptConfig.getTo()))
                result.add(p);
            else if (protocolInfo != null && url.equalsIgnoreCase(protocolInfo.getAddress()))
                result.add(p);
        }

        return result;
    }

}

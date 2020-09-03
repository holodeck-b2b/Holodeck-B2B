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
package org.holodeckb2b.ebms3.pmode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.holodeckb2b.common.util.CompareUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.pmode.PModeUtils;
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
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.IUsernameTokenProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.security.util.VerificationUtils;

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
     * Identifiers for the meta-data that is being used in the matching
     */
    protected static enum PARAMETERS {ID, FROM, FROM_ROLE, TO, TO_ROLE, SERVICE, ACTION, MPC, AGREEMENT}

    /**
     * The weight for each of the parameters
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
     * Finds the P-Mode for a received <i>User Message</i> message unit.
     * <p>The ebMS specifications do not describe or recommend how the P-Mode for a user message should be determined,
     * see also <a href="https://issues.oasis-open.org/browse/EBXMLMSG-48">issue 48 in the OASIS TC issue tracker</a>.
     * In the issue two suggestions for matching the P-Mode are given. Based on these we compare the meta-data from the 
     * message with all P-Modes and return the best matching P-Mode.
     * <p>The following table shows the information that is used for matching and their importance (expressed as a 
     * weight). The match of a P-Mode is the sum of the weights for the elements that are equal to the corresponding 
     * P-Mode parameter. If there is a mismatch on any of the elements the P-Mode is considered as a mismatch, but if
     * no value if specified in the P-Mode the element is not considered and not scored.
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
     * <p>Because the 2-Way MEP can be governed by two 1-Way P-Modes this method will just check all P-Modes that govern
     * message receiving. It is up to the handlers to decide whether the result is acceptable or not. This method will 
     * only find one matching P-Mode. This means that when multiple P-Modes with the highest match score are found none 
     * is returned. 
     *
     * @param mu        The user message message unit to find the P-Mode for
     * @return          The P-Mode for the message unit if the message unit can be matched to a <b>single</b> P-Mode,
     *                  <code>null</code> if no P-Mode could be found for the user message message unit.
     */
    public static IPMode forReceivedUserMessage(final IUserMessage mu) {
        final IPModeSet pmodes = HolodeckB2BCoreInterface.getPModeSet();
        if (pmodes == null)
            return null;

        IPMode    hPMode = null;
        int       hValue = 0;
        boolean   multiple = false;
        
        
        for (final IPMode p : pmodes.getAll()) {
        	// If the P-Mode MEP binding does not start with the ebMS3 namespace URI it does not apply to ebMS3/AS4 and
        	// therefore should be ignored
        	if (!p.getMepBinding().startsWith(EbMSConstants.EBMS3_NS_URI))
        		continue;
        	
        	/*
        	 * First step is to determine if the P-Mode should be evaluated, i.e. if it governs message receiving. For
        	 * a 2-Way P-Mode this is always true. But for 1-Way P-Modes this is only the case when it is not triggering
        	 * a Push or responding to a Pull.  
        	 */
        	final boolean initiator = PModeUtils.isHolodeckB2BInitiator(p);
        	final String  mepBinding = p.getMepBinding();
        	if ((initiator && !mepBinding.equals(EbMSConstants.ONE_WAY_PULL)) // sending using Push
    		|| (!initiator && mepBinding.equals(EbMSConstants.ONE_WAY_PULL))) // sending using Pull
                continue;            

        	/*
        	 * Now first check the generic meta-data elements like P-Mode identifier, agreement reference and trading
        	 * partners.
        	 */        	
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
            /*
             * If HB2B is the initiator of the MEP it will either send the first User Message or Pull Request which 
             * implies that it will receive the User Message from the Responder. If it isn't the initiator the first
             * User Message is either pushed to HB2B by the other MSH or send by HB2B as a response to a Pull Request
             * meaning that the sender is always the Initiator of the MEP.
             */
            if (initiator) {
            	fromPMode = p.getResponder(); toPMode = p.getInitiator();
            } else {
            	fromPMode = p.getInitiator(); toPMode = p.getResponder(); 
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

            /*
             * Remaining meta-data to be matched are defined per Leg basis. All relevant information is contained in the 
             * user message flow, except for the MPC which can also be specified in a pull request flow.
             */ 
            final ILeg leg = PModeUtils.getReceiveLeg(p);
            final IUserMessageFlow  flow = leg.getUserMessageFlow();
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

            /*
             * Check MPC, first check the MPC defined in the User Message flow, and if there is none there, check
             * if there is maybe on in Pull Request flow. When no MPC is provided the default MPC is used (applies to
             * both message and P-Mode)
             */            
            String mpc = mu.getMPC();
            if (Utils.isNullOrEmpty(mpc))
                mpc = EbMSConstants.DEFAULT_MPC;
            String mpcPMode = pmBI != null ? pmBI.getMpc() : null;
            
            if (Utils.isNullOrEmpty(mpcPMode) && !Utils.isNullOrEmpty(leg.getPullRequestFlows())) {
            	mpcPMode = leg.getPullRequestFlows().iterator().next().getMPC();        
                if (Utils.isNullOrEmpty(mpcPMode))
                    mpcPMode = EbMSConstants.DEFAULT_MPC;
                // Now compare MPC, but take into account that MPC in a PullRequestFlow can be a sub MPC, so the one
                // from the message can be a parent MPC
                if (mpcPMode.startsWith(mpc))
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.MPC);
                else
                    continue; // mis-match on MPC
            } else {
                // If no MPC is given in P-Mode, it uses the default
                if (Utils.isNullOrEmpty(mpcPMode))
                    mpcPMode = EbMSConstants.DEFAULT_MPC;
                // Now compare the MPC values
                if (mpc.equals(mpcPMode))
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.MPC);
                else
                    continue; // mis-match on MPC
            }

            // Does this P-Mode better match to the message meta data than the current highest match?
            if (cValue > hValue) {
                // Yes, it does, set it as new best match
                hValue = cValue;
                hPMode = p;
                multiple = false;
            } else if (cValue == hValue)
                // It has the same match as the current highest scoring one
                multiple = true;
        }

        // Only return a single P-Mode
        return !multiple ? hPMode : null;
    }

    /**
     * Gets the list of P-Modes for which Holodeck B2B is the responder in a pull operation for the given MPC and
     * authentication info which can consist of the signature and the username tokens in the security header targeted to
     * the <i>default</i> and <i>ebms</i> role/actor.
     *
     * @param authInfo  The authentication info included in the message.
     * @param mpc       The <i>MPC</i> that the message are exchanged on
     * @return          Collection of P-Modes for which Holodeck B2B is the responder in a pull operation for the given
     *                  MPC and authentication info
     * @throws SecurityProcessingException
     */
    public static Collection<IPMode> findForPulling(final Collection<ISecurityProcessingResult> authInfo,
                                                    final String mpc) throws SecurityProcessingException {
        final ArrayList<IPMode> pmodesForPulling = new ArrayList<>();

        for(final IPMode p : HolodeckB2BCoreInterface.getPModeSet().getAll()) {
            // Check if this P-Mode uses pulling with Holodeck B2B being the responder
            final ILeg leg = PModeUtils.getInPullRequestLeg(p);
            if (leg != null) {
                boolean authorized = false;
                // Get the security configuration of the trading partner
                ISecurityConfiguration tpSecCfg = null;
                if (PModeUtils.isHolodeckB2BInitiator(p) && p.getResponder() != null)
                    tpSecCfg = p.getResponder().getSecurityConfiguration();
                else if (!PModeUtils.isHolodeckB2BInitiator(p) && p.getInitiator() != null)
                    tpSecCfg = p.getInitiator().getSecurityConfiguration();

                // Security config can also be defined per sub-channel in a PullRequestFlow, so these must be checked
                // as well
                final Collection<IPullRequestFlow> flows = leg.getPullRequestFlows();
                if (Utils.isNullOrEmpty(flows)) {
                    // There is no specific configuration for pulling, so use trading partner security settings only
                    // also means we need to check the MPC on leg level
                    authorized = checkMainMPC(leg, mpc) && verifyPullRequestAuthorization(null, tpSecCfg, authInfo);
                } else {
                    for (final Iterator<IPullRequestFlow> it = flows.iterator(); it.hasNext() && !authorized;) {
                        final IPullRequestFlow flow = it.next();
                        // Check if mpc matches to this specific PR-flow
                        authorized = checkSubMPC(flow, mpc)
                                     && verifyPullRequestAuthorization(flow.getSecurityConfiguration(),
                                                                        tpSecCfg,
                                                                        authInfo);
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
     * Checks if the given MPC is equal to or a sub-channel of the MPC on which user messages are exchanged for the
     * given Leg.
     *
     * @param leg   The Leg
     * @param mpc   The mpc that must be checked
     * @return      <code>true</code> if the given MPC starts with or is equal to the one defined in the Leg, taking
     *              into account that a <code>null</code> value is equal to the default MPC, or if no MPC is specified
     *              on the leg,<br>
     *              <code>false</code> otherwise
     */
    private static boolean checkMainMPC(final ILeg leg, final String mpc) {
        String pModeMPC = null;
        try {
            pModeMPC = leg.getUserMessageFlow().getBusinessInfo().getMpc();
        } catch (final NullPointerException npe) {
            pModeMPC = null;
        }

        return ((Utils.isNullOrEmpty(pModeMPC) || EbMSConstants.DEFAULT_MPC.equalsIgnoreCase(pModeMPC))
                && ((Utils.isNullOrEmpty(mpc)) || EbMSConstants.DEFAULT_MPC.equalsIgnoreCase(mpc))
               )
               || (!Utils.isNullOrEmpty(pModeMPC) && !Utils.isNullOrEmpty(mpc)
                     && mpc.toLowerCase().startsWith(pModeMPC.toLowerCase()));
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

        return ((Utils.isNullOrEmpty(pModeMPC) || EbMSConstants.DEFAULT_MPC.equalsIgnoreCase(pModeMPC))
                && ((Utils.isNullOrEmpty(mpc)) || EbMSConstants.DEFAULT_MPC.equalsIgnoreCase(mpc))
               )
               || (!Utils.isNullOrEmpty(mpc) && mpc.equalsIgnoreCase(pModeMPC));
    }

    /**
     * Helper method to verify that the required authorization defined for the Pull Request is correctly satisfied.
     * <p>As described in the ebMS V3 Core Specification there are four option to include the authentication information
     * for a Pull Request, that is using a:<ol>
     * <li>Digital signature in the default WSS Header,</li>
     * <li>Username token in the default WSS header,</li>
     * <li>Username token in the WSS header addressed to the "ebms" actor/role,</li>
     * <li>Transfer-protocol-level identity-authentication mechanism (e.g. TLS)</li></ol>
     * Holodeck B2B supports the first three options, either on their own or as a combination. By default these settings
     * are defined on the trading partner level. But to support authentication for multiple sub-channels or only for
     * the Pull Request it is possible to define the settings on the <i>pull request flow</i>. When settings are
     * provided both at the trading partner and pull request flow the latter take precedence and will be used for the
     * verification of the supplied authentication info.
     * <p>NOTE: The pull request flow specific configuration only allows for authentication options 1 (signature in
     * default header) and 3 (username token in "ebms" header).
     *
     * @param pullSecCfg    The {@link ISecurityConfiguration} specified on the pull request flow that applies to this
     *                      <i>PullRequest</i>
     * @param tpSecCfg      The {@link ISecurityConfiguration} specified for the trading partner that is the sender of
     *                      the <i>PullRequest</i>
     * @param authInfo      All authentication info provided in the received message. 
     * @return              <code>true</code> if the received message satisfies the authentication requirements defined
     *                      in the flow, <br>
     *                      <code>false</code> otherwise.
     */
    private static boolean verifyPullRequestAuthorization(final ISecurityConfiguration pullSecCfg,
                                                          final ISecurityConfiguration tpSecCfg,
                                                          final Collection<ISecurityProcessingResult> authInfo)
                                                                                    throws SecurityProcessingException {
        boolean verified = true;

        // If there are no security parameters specified there is no authentication expected, so there should be no
        // authentication info in the message
        if (pullSecCfg == null && tpSecCfg == null)
            return Utils.isNullOrEmpty(authInfo);
        else if (Utils.isNullOrEmpty(authInfo))
            return false;

        // Verify username token in ebms header, first check if pull request flow contains config for this UT
        IUsernameTokenConfiguration expectedUT = pullSecCfg == null ? null : pullSecCfg.getUsernameTokenConfiguration(
                                                                                             SecurityHeaderTarget.EBMS);
        if (expectedUT == null)
            // if not fall back to trading partner config
            expectedUT = tpSecCfg == null ? null : tpSecCfg.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS);

        Optional<ISecurityProcessingResult> secToken = authInfo.parallelStream()
        												  .filter(ai -> ai instanceof IUsernameTokenProcessingResult 
        													   	 && ai.getTargetedRole() == SecurityHeaderTarget.EBMS)
        												  .findFirst();
        
        verified = VerificationUtils.verifyUsernameToken(expectedUT, 
        								 secToken.isPresent() ? (IUsernameTokenProcessingResult) secToken.get() : null);

        // Verify user name token in default header
        expectedUT = tpSecCfg == null ? null :
                                tpSecCfg.getUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT);
        secToken = authInfo.parallelStream().filter(ai -> ai instanceof IUsernameTokenProcessingResult 
				   	 									&& ai.getTargetedRole() == SecurityHeaderTarget.DEFAULT)
				  						    .findFirst();
        verified &= VerificationUtils.verifyUsernameToken(expectedUT,
        								 secToken.isPresent() ? (IUsernameTokenProcessingResult) secToken.get() : null);
        
        // Verify that the expected certificate was used for creating the signature, again start with configuration from
        // PR-flow and fall back to TP
        ISigningConfiguration expectedSig = pullSecCfg == null ? null : pullSecCfg.getSignatureConfiguration();
        if (expectedSig == null)
            expectedSig = tpSecCfg == null ? null : tpSecCfg.getSignatureConfiguration();

        secToken = authInfo.parallelStream().filter(ai -> ai instanceof ISignatureProcessingResult 
													   && ai.getTargetedRole() == SecurityHeaderTarget.DEFAULT)
        									.findFirst();
        verified &= VerificationUtils.verifySigningCertificate(expectedSig,
        									secToken.isPresent() ? (ISignatureProcessingResult) secToken.get() : null);

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

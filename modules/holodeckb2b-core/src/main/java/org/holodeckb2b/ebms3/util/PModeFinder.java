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
package org.holodeckb2b.ebms3.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.general.IAgreement;
import org.holodeckb2b.common.general.IService;
import org.holodeckb2b.common.general.ITradingPartner;
import org.holodeckb2b.common.messagemodel.IAgreementReference;
import org.holodeckb2b.common.messagemodel.ICollaborationInfo;
import org.holodeckb2b.common.messagemodel.IUserMessage;
import org.holodeckb2b.common.messagemodel.util.compare;
import org.holodeckb2b.common.pmode.IBusinessInfo;
import org.holodeckb2b.common.pmode.IErrorHandling;
import org.holodeckb2b.common.pmode.ILeg;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.common.pmode.IPModeSet;
import org.holodeckb2b.common.pmode.IProtocol;
import org.holodeckb2b.common.pmode.IPullRequestFlow;
import org.holodeckb2b.common.pmode.IReceiptConfiguration;
import org.holodeckb2b.common.pmode.IUserMessageFlow;
import org.holodeckb2b.common.security.ISecurityConfiguration;
import org.holodeckb2b.common.security.ISigningConfiguration;
import org.holodeckb2b.common.security.IUsernameTokenConfiguration;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.module.HolodeckB2BCore;
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
 * @author Sander Fieten <sander at holodeck-b2b.org>
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
        Map<PARAMETERS, Integer> aMap = new EnumMap<> (PARAMETERS.class);
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
     * see <a href="https://issues.oasis-open.org/browse/EBXMLMSG-48?jql=project%20%3D%20EBXMLMSG">issue 48 in the TC 
     * issue tracker</a>. In the issue two suggestion for matching the P-Mode are given.
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
     * </table>
     * <p>If there is a mismatch for one of the elements the P-Mode is considered as a mismatch.
     * 
     * @param mu        The user message message unit to find the P-Mode for
     * @return          The P-Mode for the message unit if the message unit can be matched to a P-Mode,
     *                  <code>null</code> if no P-Mode could be found for the user message message unit.
     */
    public static IPMode forReceivedUserMessage(IUserMessage mu) {
        IPModeSet pmodes = HolodeckB2BCore.getPModeSet();
        IPMode    hPMode = null;
        int       hValue = 0;
        
        if (pmodes == null)
            return null;
        
        for (IPMode p : pmodes.getAll()) {
            int cValue = 0;
            // P-Mode id and agreement info are contained in optional element
            IAgreementReference agreementRef = mu.getCollaborationInfo().getAgreement();
            
            if (agreementRef != null) {
                // Check P-Mode id, do only when id is expected to be included
                String pid = agreementRef.getPModeId();
                if (pid != null && !pid.isEmpty() && (p.includeId() != null && p.includeId())) {
                    if (pid.equals(p.getId()))
                        cValue = MATCH_WEIGHTS.get(PARAMETERS.ID);
                    else
                        continue; // mis-match on P-Mode id
                }
                
                // Check agreement info
                IAgreement agreementPMode = p.getAgreement();
                if (agreementPMode != null) {
                    if (Utils.compareStrings(agreementRef.getName(), agreementPMode.getName()) == 0) {
                        // names equal, but for match also types must be equal
                        int i = Utils.compareStrings(agreementRef.getType(), agreementPMode.getType());
                        if (i == -1 || i == 0)
                            cValue += MATCH_WEIGHTS.get(PARAMETERS.AGREEMENT);
                        else 
                            continue; // mis-match on agreement type
                    } else
                        continue; // mis-match on agreement name
                }
            }
            
            // Check trading partner info
            ITradingPartner from = mu.getSender(), to = mu.getReceiver(); 
            ITradingPartner fromPMode = null, toPMode = null;
            if (p.getMepBinding().equals(Constants.ONE_WAY_PUSH)) {
                fromPMode = p.getInitiator(); toPMode = p.getResponder(); 
            } else {
                fromPMode = p.getResponder(); toPMode = p.getInitiator();
            }
            
            // Check To info
            if (toPMode != null) {
                int c = Utils.compareStrings(to.getRole(), toPMode.getRole());
                if ( -1 <= c && c <= 1) 
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.TO_ROLE);
                else
                    continue; // mis-match on To party role
                if (compare.PartyIds(to.getPartyIds(), toPMode.getPartyIds()))
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.TO);
                else
                    continue; // mis-match on To party id('s)
            }
            
            // Check From info
            if (fromPMode != null) {
                int c = Utils.compareStrings(from.getRole(), fromPMode.getRole());
                if ( -1 <= c && c <= 1) 
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.FROM_ROLE);
                else
                    continue; // mis-match on From party role
                if (compare.PartyIds(from.getPartyIds(), fromPMode.getPartyIds()))
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.FROM);
                else
                    continue;  // mis-match on From party id('s)
            }
            
            // Next info items are defined per Leg basis, for now we only have one-way MEP, so only one leg to check
            // Within the leg all relevant information is contained in the user message flow.
            IUserMessageFlow    flow = p.getLegs().iterator().next().getUserMessageFlow();
            
            if (flow != null) {
                IBusinessInfo pmBI = flow.getBusinessInfo();
                if (pmBI != null) {
                    // Check Service
                    IService svcPMode = pmBI.getService();
                    if (svcPMode != null) {
                        IService svc = mu.getCollaborationInfo().getService();
                        if (svc.getName().equals(svcPMode.getName())) {
                            int i = Utils.compareStrings(svc.getType(), svcPMode.getType());
                            if (i == -1 || i == 0)
                                cValue += MATCH_WEIGHTS.get(PARAMETERS.SERVICE);
                            else 
                                continue; // mis-match on service type
                        } else
                            continue; // mis-match on service name
                    }
                    // Check Action
                    int i = Utils.compareStrings(mu.getCollaborationInfo().getAction(), pmBI.getAction());
                    if (i == 0)
                        cValue += MATCH_WEIGHTS.get(PARAMETERS.ACTION);
                    else if (i == -2)
                        continue; // mis-match on action
                    // Check MPC, should handle default MPC when none is given
                    String mpc = mu.getMPC(); String mpcPMode = pmBI.getMpc();
                    if (mpc == null || mpc.isEmpty())
                        mpc = Constants.DEFAULT_MPC;
                    if (mpcPMode == null || mpcPMode.isEmpty())
                        mpcPMode = Constants.DEFAULT_MPC;
                    if (mpc.equalsIgnoreCase(mpcPMode))
                        cValue += MATCH_WEIGHTS.get(PARAMETERS.MPC);
                    else 
                        continue; // mis-match on MPC
                }                
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
     * Finds the P-Mode for an user message message unit submitted to Holodeck B2B for sending.
     * <p>Currently the matching is solely based on the P-Mode id so this must be supplied when submitting the message
     * to the Holodeck B2B core.
     * 
     * @param mu        The user message message unit to find the P-Mode for
     * @return          The P-Mode for the message unit if the message unit can be matched to a P-Mode,
     *                  <code>null</code> if no P-Mode could be found for the user message message unit.
     */    
    public static IPMode forSubmitted(IUserMessage mu) {
        // For now the only method to find the P-Mode is by specifying it directly
        try {
            return HolodeckB2BCore.getPModeSet().get(mu.getCollaborationInfo().getAgreement().getPModeId());
        } catch (NullPointerException npe) {
            return null;
        }
    }

    /**
     * Finds the P-Mode for a submitted user message message unit.
     * <p>The submit operation is defined as abstract in the ebMS specifications so there is not standard way for 
     * determination of the P-Mode for a submitted user message. In this method a matching algorithm similar to the one
     * used to find the P-Mode for received user message is used. 
     * <p>It uses the same meta-data elements and weights to find the P-Mode. The difference with the algorithm for
     * received messages is that a mismatch caused by a <code>null</code> value for either the meta-data or P-Mode 
     * element does not cause a complete mismatch between meta-data and P-Mode. This is because it is common that only 
     * one provides a value for the element.
     * 
     * @param mu        The user message message unit to find the P-Mode for
     * @return          The P-Mode for the message unit if the message unit can be matched to a P-Mode,
     *                  <code>null</code> if no P-Mode could be found for the user message message unit.
     * @see #forReceivedUserMessage(org.holodeckb2b.common.messagemodel.IUserMessage) 
     */
    public static IPMode forSubmittedUserMessage(IUserMessage mu) {
        IPModeSet pmodes = HolodeckB2BCore.getPModeSet();
        IPMode    hPMode = null;
        int       hValue = 0;
        
        if (pmodes == null)
            return null;
        
        for (IPMode p : pmodes.getAll()) {
            int cValue = 0;
            
            // First check if the meta-data contains a P-Mode id
            ICollaborationInfo  collaborationInfo = mu.getCollaborationInfo();
            IAgreementReference agreementRef = collaborationInfo != null ? collaborationInfo.getAgreement() : null;
            
            if (agreementRef != null) {
                String pid = agreementRef.getPModeId();
                // If a P-Mode id is specified in submission meta-data it must match against P-Mode, otherwise no match
                if (!Utils.isNullOrEmpty(pid)) {
                    if (pid.equals(p.getId()))
                        cValue = MATCH_WEIGHTS.get(PARAMETERS.ID);
                    else
                        continue; // mis-match on P-Mode id
                }                    
                // Check agreement info
                IAgreement agreementPMode = p.getAgreement();
                if (agreementPMode != null) {
                    int i = Utils.compareStrings(agreementRef.getName(), agreementPMode.getName());                    
                    if (i == 0) {
                        // names equal, but for match also types must be equal
                        Utils.compareStrings(agreementRef.getType(), agreementPMode.getType());
                        if (i == -1 || i == 0)
                            cValue += MATCH_WEIGHTS.get(PARAMETERS.AGREEMENT);
                        else if (i == -2)
                            continue; // mis-match on agreement type
                    } else if (i == -2)
                        continue; // mis-match on agreement name
                }
            }
            
            // Check trading partner info
            ITradingPartner from = mu.getSender(), to = mu.getReceiver(); 
            ITradingPartner fromPMode = null, toPMode = null;            
            if (p.getMepBinding().startsWith(Constants.ONE_WAY_PUSH)) {
                if (p.getMep().equals(Constants.ONE_WAY_MEP) || Utils.isNullOrEmpty(mu.getRefToMessageId())) {
                    // One-Way P-Mode or message on the first leg of two-way
                    fromPMode = p.getInitiator(); toPMode = p.getResponder(); 
                } else {
                    // Submitted message is reply in a two-Way P-Mode that started with a Push
                    fromPMode = p.getResponder(); toPMode = p.getInitiator();
                }
            } else {
                if (p.getMep().equals(Constants.ONE_WAY_MEP) || Utils.isNullOrEmpty(mu.getRefToMessageId())) {
                    // Message is pulled on One-Way P-Mode or on first leg of Two-Way P-Mode 
                    fromPMode = p.getResponder(); toPMode = p.getInitiator();
                } else { 
                    // Submitted message is reply in a two-Way P-Mode that started with a Pull
                    fromPMode = p.getInitiator(); toPMode = p.getResponder();
                }
            }
            
            // Check To info
            if (to != null && toPMode != null) {
                int c = Utils.compareStrings(to.getRole(), toPMode.getRole());
                if (-1 == c || c == 0) 
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.TO_ROLE);
                else if (c == -2)
                    continue; // mis-match on To party role
                if (!Utils.isNullOrEmpty(to.getPartyIds()) && !Utils.isNullOrEmpty(toPMode.getPartyIds())) {                    
                    if (compare.PartyIds(to.getPartyIds(), toPMode.getPartyIds()))
                        cValue += MATCH_WEIGHTS.get(PARAMETERS.TO);
                    else
                        continue; // mis-match on To party id('s)
                }
            }            
            // Check From info
            if (from != null && fromPMode != null) {
                int c = Utils.compareStrings(from.getRole(), fromPMode.getRole());
                if (-1 == c || c == 0) 
                    cValue += MATCH_WEIGHTS.get(PARAMETERS.FROM_ROLE);
                else if (c == -2)
                    continue; // mis-match on To party role
                if (!Utils.isNullOrEmpty(from.getPartyIds()) && !Utils.isNullOrEmpty(fromPMode.getPartyIds())) {                    
                    if (compare.PartyIds(from.getPartyIds(), fromPMode.getPartyIds()))
                        cValue += MATCH_WEIGHTS.get(PARAMETERS.FROM);
                    else
                        continue; // mis-match on From party id('s)
                }
            }
            
            // Next info items are defined per Leg basis, so check in which Leg this message is sent.
            ILeg leg = null;
            if (p.getMep().equals(Constants.ONE_WAY_MEP) || Utils.isNullOrEmpty(mu.getRefToMessageId()))
                leg = p.getLeg(ILeg.Label.REQUEST);
            else 
                leg = p.getLeg(ILeg.Label.REPLY);
            
            // The meta-data on a User Message message unit is contained in the UserMessage Flow of the leg
            IUserMessageFlow    flow = leg.getUserMessageFlow();            
            if (flow != null) {
                IBusinessInfo pmBI = flow.getBusinessInfo();
                if (pmBI != null) {
                    // Check Service and Action (if given in submitted meta-data
                    if (collaborationInfo != null) {
                        IService svc = collaborationInfo.getService();
                        IService svcPMode = pmBI.getService();
                        if (svc != null && svcPMode != null) {
                            int c = Utils.compareStrings(svc.getName(), svcPMode.getName());
                            if (-1 == c || c == 0) {
                                // The Service name matches, but for complete match also the type must match
                                c = Utils.compareStrings(svc.getType(), svcPMode.getType());
                                if (-1 == c || c == 0)
                                    cValue += MATCH_WEIGHTS.get(PARAMETERS.SERVICE);
                                else 
                                    continue; // mis-match on service type
                            } else if (c == -2)
                                continue; // mis-match on service name
                        }                    
                        // Check Action
                        int c = Utils.compareStrings(collaborationInfo.getAction(), pmBI.getAction());
                        if (-1 == c || c == 0)
                            cValue += MATCH_WEIGHTS.get(PARAMETERS.ACTION);
                        else if (c == -2)
                            continue; // mis-match on action
                    }
                    
                    // Check MPC, should handle default MPC when none is given
                    String mpc = mu.getMPC(); String pmodeMPC = pmBI.getMpc();
                    if (Utils.isNullOrEmpty(mpc))
                        mpc = Constants.DEFAULT_MPC;
                    if (Utils.isNullOrEmpty(pmodeMPC))
                        pmodeMPC = Constants.DEFAULT_MPC;
                    if (mpc.equalsIgnoreCase(pmodeMPC))
                        cValue += MATCH_WEIGHTS.get(PARAMETERS.MPC);
                    else 
                        continue; // mis-match on MPC
                }                
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
    public static Collection<IPMode> findForPulling(Map<String, IAuthenticationInfo> authInfo, String mpc) {
        ArrayList<IPMode> pmodesForPulling = new ArrayList<IPMode>();
        
        for(IPMode p : HolodeckB2BCore.getPModeSet().getAll()) {
            // Check if this P-Mode uses pulling with Holodeck B2B being the responder
            ILeg leg = p.getLegs().iterator().next();
            if (Constants.ONE_WAY_PULL.equalsIgnoreCase(p.getMepBinding())
               && ( leg.getProtocol() == null || leg.getProtocol().getAddress() == null )
               ) { 
                // Leg uses pulling and Holodeck B2B is responder, check if given MPC matches P-Mode MPC defined for UM
                if (!checkMainMPC(leg, mpc))
                    // MPC does not match to one defined in P-Mode
                    continue;
                
                // Check if authentication info matches to one of the pull request flows
                boolean authorized = false; 
                ISecurityConfiguration initiatorSecCfg = p.getInitiator() == null ? null :
                                                                            p.getInitiator().getSecurityConfiguration();
                Collection<IPullRequestFlow> flows = leg.getPullRequestFlows();
                if (flows == null || flows.isEmpty()) {
                    // There is no specific configuration for pulling, so use security settings from initiator
                    authorized = verifyPullRequestAuthorization(null, initiatorSecCfg, authInfo);
                } else {                
                    for (Iterator<IPullRequestFlow> it = flows.iterator(); it.hasNext() && !authorized;) {
                        IPullRequestFlow flow = it.next();
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
    private static boolean checkMainMPC(ILeg leg, String mpc) {
        String pModeMPC = null;
        try {
            pModeMPC = leg.getUserMessageFlow().getBusinessInfo().getMpc();
        } catch (NullPointerException npe) {
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
    private static boolean checkSubMPC(IPullRequestFlow flow, String mpc) {
        String pModeMPC = null;
        try {
            pModeMPC = flow.getMPC();
        } catch (NullPointerException npe) {
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
    private static boolean verifyPullRequestAuthorization(ISecurityConfiguration pullSecCfg, 
                                                          ISecurityConfiguration tpSecCfg,
                                                          Map<String, IAuthenticationInfo> authInfo) {
        boolean verified = true;
        
        // If there are no security parameters specified there is authentication expected, so there should be no
        // authentication info in the message
        if (pullSecCfg == null && tpSecCfg == null)
            return (authInfo == null || authInfo.isEmpty());
        else if (authInfo == null || authInfo.isEmpty())
            return false;
        
        // Verify username token in ebms header, first check if pull request flow contains config for this UT
        IUsernameTokenConfiguration expectedUT = pullSecCfg.getUsernameTokenConfiguration(
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
        ISigningConfiguration expectedSig = pullSecCfg.getSignatureConfiguration();
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
    public static Collection<IPMode> getPModesWithErrorsTo(String url) {
        Collection<IPMode>  result = new ArrayList<IPMode>();
        
        for(IPMode p : HolodeckB2BCore.getPModeSet().getAll()) {
            // Get all relevent P-Mode info
            ILeg leg = p.getLegs().iterator().next();
            IProtocol protocolInfo = leg.getProtocol();
            IUserMessageFlow flow = leg.getUserMessageFlow();
            IErrorHandling errorHandling = flow != null ? flow.getErrorHandlingConfiguration() : null;
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
    public static Collection<IPMode> getPModesWithReceiptsTo(String url) {
        Collection<IPMode>  result = new ArrayList<IPMode>();
        
        for(IPMode p : HolodeckB2BCore.getPModeSet().getAll()) {
            // Get all relevent P-Mode info
            ILeg leg = p.getLegs().iterator().next();
            IProtocol protocolInfo = leg.getProtocol();
            IReceiptConfiguration rcptConfig = leg.getReceiptConfiguration();
            if (rcptConfig != null && url.equalsIgnoreCase(rcptConfig.getTo()))
                result.add(p);
            else if (protocolInfo != null && url.equalsIgnoreCase(protocolInfo.getAddress()))
                result.add(p);
        }
        
        return result;
    }    
    
}

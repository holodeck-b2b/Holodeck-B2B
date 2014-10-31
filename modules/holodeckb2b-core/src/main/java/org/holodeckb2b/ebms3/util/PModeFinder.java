/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
import java.util.Iterator;
import java.util.List;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.messagemodel.IUserMessage;
import org.holodeckb2b.common.pmode.IErrorHandling;
import org.holodeckb2b.common.pmode.IFlow;
import org.holodeckb2b.common.pmode.ILeg;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.common.pmode.IProtocol;
import org.holodeckb2b.common.pmode.IReceiptConfiguration;
import org.holodeckb2b.module.HolodeckB2BCore;

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
     * Finds the P-Mode for a user message message unit.
     * 
     * @param mu        The user message message unit to find the P-Mode for
     * @return          The P-Mode for the message unit if the message unit
     *                  can be matched to a P-Mode,
     *                  <code>null</code> if no P-Mode could be found for the
     *                  message unit.
     */
    public static IPMode find(IUserMessage mu) {
        // For now the only method to find the P-Mode is by specifying it directly
        try {
            return HolodeckB2BCore.getPModeSet().get(mu.getCollaborationInfo().getAgreement().getPModeId());
        } catch (NullPointerException npe) {
            return null;
        }
    }
    
    /**
     * Gets a list of P-Modes for which Holodeck B2B is the responder in a pull operation for the given MPC.
     * 
     * @param mpc   The <i>MPC</i> that the message are exchanged on
     * @return      A collection of {@link IPMode} objects for the P-Modes for which Holodeck B2B is the responder in
     *              a pull operation for the given MPC
     */
    public static Collection<IPMode> findForPulling(String mpc) {
        ArrayList<IPMode> pmodesForPulling = new ArrayList<IPMode>();
        ArrayList<IPMode> noMpcPModes = new ArrayList<IPMode>();
                
        for(IPMode p : HolodeckB2BCore.getPModeSet().getAll()) {
            // Check if this P-Mode uses pulling 
            ILeg leg = p.getLegs().iterator().next();
            if (Constants.ONE_WAY_PULL.equalsIgnoreCase(p.getMepBinding())
               && ( leg.getProtocol() == null || leg.getProtocol().getAddress() == null )
               ) { 
                // Leg uses pulling and Holodeck B2B is responder, but for right MPC?
                // First check if the given MPC is a sub channel of MPC defined for user messages
                IFlow flow = leg.getUserMessageFlow();
                String pmodeMPC = flow != null && flow.getBusinessInfo() != null 
                                           ? flow.getBusinessInfo().getMpc() : null;
                
                if (pmodeMPC != null && !pmodeMPC.isEmpty()) {
                    if (mpc.startsWith(pmodeMPC)) 
                        // The requested MPC is a sub channel
                        pmodesForPulling.add(p);
                } else {
                    // There is no user message MPC defined in the P-Mode, so check if one of the pull request flows
                    // contains the given MPC
                    List<IFlow> flows = leg.getPullRequestFlows();
                    if (flows != null && !flows.isEmpty()) {
                        boolean sameMPC = false;                 
                        for (Iterator<IFlow> it = flows.iterator(); it.hasNext() && !sameMPC;) {
                            flow = it.next();
                            pmodeMPC = flow != null && flow.getBusinessInfo() != null 
                                                              ? flow.getBusinessInfo().getMpc() : null;
                            sameMPC = mpc.equalsIgnoreCase(pmodeMPC);
                        }
                        if (sameMPC) 
                            // There is at least one PR-flow that has a matching MPC
                            pmodesForPulling.add(p);
                    } else 
                        // P-Mode does not define MPC, maybe there are messages in database for this P-Mode with the 
                        // given MPC (assigned when submitted) so include this P-Mode as candidate
                        pmodesForPulling.add(p);
                }
            }
        }        
        
        return pmodesForPulling;
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
            IFlow flow = leg.getUserMessageFlow();
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

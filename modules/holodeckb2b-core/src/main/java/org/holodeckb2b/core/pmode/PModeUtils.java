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
package org.holodeckb2b.core.pmode;

import java.util.Collection;
import java.util.List;

import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;

/**
 * Contains some common operations on ebMS V3 / AS4 P-Modes to determine the message exchange pattern they support and 
 * get pulling configurations. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.1.0
 */
public class PModeUtils {

    /**
     * Determines whether Holodeck B2B is the <i>Initiator</i> of this P-Mode, i.e. sends the first ebMS message unit.
     *
     * @param pmode     The P-Mode to check
     * @return          <code>true</code> if Holodeck B2B is the <i>Initiator</i> of the P-Mode, <br>
     *                  <code>false</code> if Holodeck B2B is the <i>Respondor</i> of the P-Mode
     */
    public static boolean isHolodeckB2BInitiator(final IPMode pmode) {
        // Checking is actually very easy as Holodeck is initiator when it triggers the Request leg
        return doesHolodeckB2BTrigger(pmode.getLeg(Label.REQUEST));
    }

    /**
     * Determines whether the given P-Mode contains a leg in which Holodeck B2B sends the Pull Request. 
     *  
     * @param pmode		The P-Mode to check
     * @return			<code>true</code> if the P-Mode supports sending Pull Requests,<br>
     * 					<code>false</code> otherwise
     * @since 4.1.0
     */
    public static boolean doesHolodeckB2BPull(final IPMode pmode) {
    	return getOutPullLeg(pmode) != null;
    }
    
    /**
     * Gets the configuration of the Leg on which the given message unit is exchanged.
     * 
     * @param m		Message unit to determine leg configuration for 
     * @return		Configuration of the leg the message unit belongs to,<br>
     * 				<code>null</code> if the message unit is not assigned to a P-Mode 
     * @throws IllegalStateException	When the current P-Mode set does not contain the P-Mode that is registered in 
     * 									the meta-data of the message unit 
     * @since 5.0.0
     */
    public static ILeg getLeg(final IMessageUnit m) {
    	final String pmodeId = m.getPModeId();
    	if (Utils.isNullOrEmpty(pmodeId))
    		return null;
    	
    	final IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(pmodeId);
    	if (pmode == null)
    		throw new IllegalStateException("P-Mode [" + pmodeId + "] not available");
    	
    	if (m instanceof IUserMessage)
    		if (m.getDirection() == Direction.OUT)
    			return getSendLeg(pmode);
    		else
    			return getReceiveLeg(pmode);
    	else if (m instanceof IErrorMessage) {
    		/* When the P-Mode is Two-Way the Error can be on both legs and we need to use the referenced message unit
    		 * to find the correct leg. As no message processing context is available at this point we can only use the 
    		 * reference included in the Error Message. This implies that for Error Messages without a reference we
    		 * won't be able to determine their leg. 
    		 */
    		if (EbMSConstants.TWO_WAY_MEP.equals(pmode.getMep())) {
    			final String refToMsgInError = MessageUnitUtils.getRefToMessageId(m);
    			if (Utils.isNullOrEmpty(refToMsgInError))
    				return null; // no reference available, nothing we can do
    			else {
    				try {
						final Collection<IMessageUnitEntity> rfdMsgs = HolodeckB2BCore.getQueryManager()
																			.getMessageUnitsWithId(refToMsgInError, 
																					m.getDirection() == Direction.IN ?
																						 Direction.OUT : Direction.IN);					
						if (rfdMsgs == null || rfdMsgs.size() != 1) 
							return null; // No or multiple message units found, so unclear to which the error applies
						
						// Use the leg of the referenced message unit
						return getLeg(rfdMsgs.iterator().next());						
    				} catch (PersistenceException e) {
						// If we cannot get the information about the referenced message unit, just return null
    					return null;
					}
    			}
    		} else 
    			// For one way it's easy, as it must be the single leg :-)
    			return pmode.getLeg(Label.REQUEST);    		
    	} else // m instanceof IReceipt | IPullRequest
    		if (m.getDirection() == Direction.OUT)
    			return getReceiveLeg(pmode);
    		else
    			return getSendLeg(pmode);
    }
    
    /**
     * Gets the configuration of the <i>Leg</i> that governs the sending by Holodeck B2B of <i>User Message</i> message 
     * units. Note that there can be no such leg if the given P-Mode is managing a One-Way MEP for receiving.
     * 
     * @param pmode		The P-Mode
     * @return			Configuration of the Leg that manages sending of User Messages by HB2B,<br>
     * 					<code>null</code> if this P-Mode has no such Leg
     * @since 5.0.0
     */
    public static ILeg getSendLeg(final IPMode pmode) {
        ILeg   outLeg = null;
        switch (pmode.getMepBinding()) {
            case EbMSConstants.ONE_WAY_PUSH :
            case EbMSConstants.TWO_WAY_PUSH_PULL:
            case EbMSConstants.TWO_WAY_PUSH_PUSH :
            case EbMSConstants.TWO_WAY_SYNC :
            	outLeg = doesHolodeckB2BTrigger(pmode.getLeg(Label.REQUEST)) ? pmode.getLeg(Label.REQUEST): 
            														EbMSConstants.TWO_WAY_MEP.equals(pmode.getMep()) ? 
            																   pmode.getLeg(Label.REPLY) 
            																 : null; 
            	break;
            case EbMSConstants.ONE_WAY_PULL :
            case EbMSConstants.TWO_WAY_PULL_PUSH :
            case EbMSConstants.TWO_WAY_PULL_PULL :
            	outLeg = !doesHolodeckB2BTrigger(pmode.getLeg(Label.REQUEST)) ? pmode.getLeg(Label.REQUEST): 
																	EbMSConstants.TWO_WAY_MEP.equals(pmode.getMep()) ? 
																			   pmode.getLeg(Label.REPLY) 
																			 : null;
				break;
            default:
            	outLeg = pmode.getLeg(Label.REQUEST);																				   
        }        
        return outLeg;    	
    }
    
    /**
     * Gets the configuration of the <i>Leg</i> that governs receiving by Holodeck B2B of <i>User Message</i> message 
     * units. Note that there can be no such leg if the given P-Mode is managing a One-Way MEP for sending.
     * 
     * @param pmode		The P-Mode
     * @return			Configuration of the Leg that manages receiving of User Messages by HB2B,<br>
     * 					<code>null</code> if this P-Mode has no such Leg
     * @since 5.0.0
     */
    public static ILeg getReceiveLeg(final IPMode pmode) {
    	ILeg   inLeg = null;
    	switch (pmode.getMepBinding()) {
    	case EbMSConstants.ONE_WAY_PUSH :
    	case EbMSConstants.TWO_WAY_PUSH_PULL:
    	case EbMSConstants.TWO_WAY_PUSH_PUSH :
    	case EbMSConstants.TWO_WAY_SYNC :
    		inLeg = !doesHolodeckB2BTrigger(pmode.getLeg(Label.REQUEST)) ? pmode.getLeg(Label.REQUEST): 
													    			EbMSConstants.TWO_WAY_MEP.equals(pmode.getMep()) ? 
													    					pmode.getLeg(Label.REPLY) 
													    					: null; 
													    					break;
    	case EbMSConstants.ONE_WAY_PULL :
    	case EbMSConstants.TWO_WAY_PULL_PUSH :
    	case EbMSConstants.TWO_WAY_PULL_PULL :
    		inLeg = doesHolodeckB2BTrigger(pmode.getLeg(Label.REQUEST)) ? pmode.getLeg(Label.REQUEST): 
													    			EbMSConstants.TWO_WAY_MEP.equals(pmode.getMep()) ? 
													    					pmode.getLeg(Label.REPLY) 
													    					: null;
													    					break;
        default:
        	inLeg = pmode.getLeg(Label.REQUEST);													    					
    	}        
    	return inLeg;    	
    }
    
    /**
     * Checks if a P-Mode contains a specific pulling configuration in case Holodeck B2B is the sender of the Pull
     * Request.  
     *
     * @param pmode     The P-Mode to check
     * @return          The {@link IPullRequestFlow} containing the pull specific configuration, or<br>
     *                  <code>null</code> if no specific configuration is given
     */
    public static IPullRequestFlow getOutPullRequestFlow(final IPMode pmode) {
    	final ILeg pullLeg = getOutPullLeg(pmode);
        // And then check if that leg contains specific PullRequestFlow
    	return getOutPullRequestFlow(pullLeg);
    }
    
    /**
     * Checks if the Leg contains a specific pulling configuration for sending of Pull Requests. Note that a leg that 
     * specifies the pulling by Holodeck B2B <b>shall contain at most one specific pulling configuration</b>. If the 
     * leg contains more than one only the first one will be used! 
     *
     * @param pmode     The P-Mode to check
     * @return          The {@link IPullRequestFlow} containing the pull specific configuration, or<br>
     *                  <code>null</code> if no specific configuration is given
     */
    public static IPullRequestFlow getOutPullRequestFlow(final ILeg leg) {    
    	if (leg != null && doesHolodeckB2BTrigger(leg) && !Utils.isNullOrEmpty(leg.getPullRequestFlows()))
            return leg.getPullRequestFlows().iterator().next();
        else
            return null;
    }

    /**
     * Determines whether Holodeck B2B is the <i>"trigger"</i> of this leg, i.e. sends the first ebMS message unit.
     * When Push is used this will be the User Message and Holodeck B2B is also acting as the <i>Sending MSH</i> as
     * defined in the ebMS Specification, otherwise Holodeck B2B sends the Pull Request and is the <i>Receiver</i> of
     * the User Message.
     *
     * @param leg       The leg configuration
     * @return          <code>true</code> when Holodeck B2B is the trigger of this leg,<br><code>false</code> if not
     */
    public static boolean doesHolodeckB2BTrigger(final ILeg leg) {
        return leg.getProtocol() != null && !Utils.isNullOrEmpty(leg.getProtocol().getAddress());
    }

    /**
     * Checks if the P-Mode has a leg that uses pulling to send a <i>User Message</i> from Holodeck B2B to another MSH,
     * i.e. where Holodeck B2B sends the <i>User Message</i> as response to a received <i>PullRequest</i>.
     *
     * @param pmode     The P-Mode to check
     * @return          The {@link ILeg} that uses pulling to send the message, or<br>
     *                  <code>null</code> if there is no such leg
     * @since 4.0.0
     */
    public static ILeg getInPullRequestLeg(IPMode pmode) {
        ILeg   inPullLeg = null;
        final String mepBinding = pmode.getMepBinding();
        switch (mepBinding) {
            case EbMSConstants.ONE_WAY_PULL :
            case EbMSConstants.TWO_WAY_PULL_PUSH :
                inPullLeg = !doesHolodeckB2BTrigger(pmode.getLeg(Label.REQUEST)) ? pmode.getLeg(Label.REQUEST) 
                																 : null; 
                break;
            case EbMSConstants.TWO_WAY_PUSH_PULL :
            	inPullLeg = !doesHolodeckB2BTrigger(pmode.getLeg(Label.REPLY)) ? pmode.getLeg(Label.REPLY): null; 
            	break;
            case EbMSConstants.TWO_WAY_PULL_PULL :
                // If both legs use pulling the one that has no target URL defined is the leg where Holodeck B2B
                // receives the Pull Request
                final List<? extends ILeg> legs = pmode.getLegs();
                for (int i = 0; i < 2 && inPullLeg == null; i++)
                    inPullLeg = !doesHolodeckB2BTrigger(legs.get(i)) ? legs.get(i) : null;
        }
        return inPullLeg;
    }

    /**
     * Checks if the P-Mode has a leg that uses pulling to receive a <i>User Message</i> by Holodeck B2B from another 
     * MSH, i.e. where Holodeck B2B sends the <i>PullRequest</i> and gets the <i>User Message</i> as response. 
     *
     * @param pmode     The P-Mode to check
     * @return          The {@link ILeg} that uses pulling to receive the message, or<br>
     *                  <code>null</code> if there is no such leg
     * @since 4.1.0
     * @since 5.0.0 Changed visibility to public 
     */
    public static ILeg getOutPullLeg(final IPMode pmode) {
	    // First we need to get the leg that configures the sending of the Pull Request
	    final List<? extends ILeg> legs = pmode.getLegs();
	    ILeg   pullLeg = null;
	    final String mepBinding = pmode.getMepBinding();
	    switch (mepBinding) {
	        case EbMSConstants.ONE_WAY_PULL :
	        case EbMSConstants.TWO_WAY_PULL_PUSH :
	            pullLeg = doesHolodeckB2BTrigger(pmode.getLeg(Label.REQUEST)) ? pmode.getLeg(Label.REQUEST) : null;
	            break;
	        case EbMSConstants.TWO_WAY_PUSH_PULL :
	            pullLeg = doesHolodeckB2BTrigger(pmode.getLeg(Label.REPLY)) ? pmode.getLeg(Label.REPLY) : null; 
	            break;
	        case EbMSConstants.TWO_WAY_PULL_PULL :
	            // If both legs use pulling the one that has a target URL defined is the leg where Holodeck B2B sends
	            // the Pull Request
	            for (int i = 0; i < 2 && pullLeg == null; i++)
	                pullLeg = doesHolodeckB2BTrigger(legs.get(i)) ? legs.get(i) : null;
	    }	    
        return pullLeg;
    }    
}

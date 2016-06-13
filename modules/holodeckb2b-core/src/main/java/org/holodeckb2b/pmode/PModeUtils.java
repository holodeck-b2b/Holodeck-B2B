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

import java.util.List;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;

/**
 * Contains some common operations on P-Modes. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
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
        // Checking is actually very easy as Holodeck is initiator when it triggers the first leg
        return doesHolodeckB2BTrigger(pmode.getLegs().get(0));        
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
        // First we need to get the leg that configures the sending of the Pull Request
        List<? extends ILeg> legs = pmode.getLegs();
        ILeg   pullLeg = null;
        String mepBinding = pmode.getMepBinding();        
        switch (mepBinding) {
            case EbMSConstants.ONE_WAY_PULL :
            case EbMSConstants.TWO_WAY_PULL_PUSH : 
                pullLeg = doesHolodeckB2BTrigger(legs.get(0)) ? legs.get(0) : null; break;
            case EbMSConstants.TWO_WAY_PUSH_PULL : 
                pullLeg = doesHolodeckB2BTrigger(legs.get(1)) ? legs.get(1) : null; break;
            case EbMSConstants.TWO_WAY_PULL_PULL :
                // If both legs use pulling the one that has a target URL defined is the leg where Holodeck B2B sends
                // the Pull Request
                for (int i = 0; i < 2 && pullLeg == null; i++) 
                    pullLeg = doesHolodeckB2BTrigger(legs.get(i)) ? legs.get(i) : null;
        }

        // And then check if that leg contains specific PullRequestFlow 
        if (pullLeg != null && !Utils.isNullOrEmpty(pullLeg.getPullRequestFlows())) 
            return pullLeg.getPullRequestFlows().iterator().next();
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
    private static boolean doesHolodeckB2BTrigger(ILeg leg) {
        return leg.getProtocol() != null && leg.getProtocol().getAddress() != null;
    }
}

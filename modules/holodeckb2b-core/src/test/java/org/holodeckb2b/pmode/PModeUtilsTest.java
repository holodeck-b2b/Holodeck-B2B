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

import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.pmode.helpers.*;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Created at 14:02 10.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PModeUtilsTest {

    /**
     * Tests the minimal configuration of P-Mode needed to pass the check
     * in case of One-Way/Pull MEP
     */
    @Test
    public void testGetOutPullRequestFlow() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PULL);

        PartnerConfig initiator = new PartnerConfig();
        validPMode.setInitiator(initiator);

        Leg leg = new Leg();

        Protocol protocolConfig = new Protocol();
        protocolConfig.setAddress("address");
        leg.setProtocol(protocolConfig);

        PullRequestFlow prFlow = new PullRequestFlow();
        leg.addPullRequestFlow(prFlow);

        validPMode.addLeg(leg);

        IPullRequestFlow requestFlow =
                PModeUtils.getOutPullRequestFlow(validPMode);

        assertNotNull(requestFlow);
    }

    /**
     * Tests the minimal configuration of P-Mode needed to pass the check
     * in case of Two-Way/Pull-and-Push MEP
     */
    @Test
    public void testGetOutPullPushRequestFlow() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.TWO_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.TWO_WAY_PULL_PUSH);

        PartnerConfig initiator = new PartnerConfig();
        validPMode.setInitiator(initiator);

        Leg leg = new Leg();

        Protocol protocolConfig = new Protocol();
        protocolConfig.setAddress("address");
        leg.setProtocol(protocolConfig);

        PullRequestFlow prFlow = new PullRequestFlow();
        leg.addPullRequestFlow(prFlow);

        validPMode.addLeg(leg);

        IPullRequestFlow requestFlow =
                PModeUtils.getOutPullRequestFlow(validPMode);

        assertNotNull(requestFlow);
    }

    /**
     * Tests the minimal configuration of P-Mode needed to pass the check
     * in case of Two-Way/Push-and-Pull MEP
     */
    @Test
    public void testGetOutPushPullRequestFlow() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.TWO_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.TWO_WAY_PUSH_PULL);

        PartnerConfig responder = new PartnerConfig();
        validPMode.setResponder(responder);

        validPMode.addLeg(new Leg());

        PartnerConfig initiator = new PartnerConfig();
        validPMode.setInitiator(initiator);
        Leg leg = new Leg();
        Protocol protocolConfig = new Protocol();
        protocolConfig.setAddress("address");
        leg.setProtocol(protocolConfig);

        PullRequestFlow prFlow = new PullRequestFlow();
        leg.addPullRequestFlow(prFlow);

        validPMode.addLeg(leg);

        IPullRequestFlow requestFlow =
                PModeUtils.getOutPullRequestFlow(validPMode);

        assertNotNull(requestFlow);
    }

    /**
     * Tests the minimal configuration of P-Mode needed to pass the check
     * in case of Two-Way/Pull-and-Pull MEP
     */
    @Test
    public void testGetOutPullPullRequestFlow() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.TWO_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.TWO_WAY_PULL_PULL);

        PartnerConfig responder = new PartnerConfig();
        validPMode.setResponder(responder);

        validPMode.addLeg(new Leg());

        PartnerConfig initiator = new PartnerConfig();
        validPMode.setInitiator(initiator);
        Leg leg = new Leg();
        Protocol protocolConfig = new Protocol();
        protocolConfig.setAddress("address");
        leg.setProtocol(protocolConfig);

        PullRequestFlow prFlow = new PullRequestFlow();
        leg.addPullRequestFlow(prFlow);

        validPMode.addLeg(leg);

        IPullRequestFlow requestFlow =
                PModeUtils.getOutPullRequestFlow(validPMode);

        assertNotNull(requestFlow);
    }
}
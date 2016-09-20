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
        prFlow.setMPC("MPC");
        leg.addPullRequestFlow(prFlow);

        validPMode.addLeg(leg);

        IPullRequestFlow requestFlow = PModeUtils.getOutPullRequestFlow(validPMode);

        assertNotNull(requestFlow);
    }
}
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.holodeckb2b.common.pmode.BusinessInfo;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.PullRequestFlow;
import org.holodeckb2b.common.pmode.UserMessageFlow;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Tests the {@link BasicPModeValidator}
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class BasicPModeValidatorTest {

    private static BasicPModeValidator validator = new BasicPModeValidator();

    /**
     * Tests a valid simple P-Mode which does not contain security settings
     */
    @Test
    public void testMinimalValidPMode() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        validPMode.addLeg(new Leg());

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(validPMode)));
    }

    /**
     * Tests valid P-Modes which contain multiple PullRequestFlows
     */
    @Test
    public void testValidMultiplePRFlows() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PULL);

        Leg leg = new Leg();
        PullRequestFlow prFlow = new PullRequestFlow();
        prFlow.setMPC(EbMSConstants.DEFAULT_MPC + "/sub-MPC-1");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC(EbMSConstants.DEFAULT_MPC + "/sub-MPC-2");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC(EbMSConstants.DEFAULT_MPC + "/sub-MPC-3");
        leg.addPullRequestFlow(prFlow);
        validPMode.addLeg(leg);

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(validPMode)));

        validPMode = new PMode();
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PULL);

        leg = new Leg();
        UserMessageFlow umFlow = new UserMessageFlow();
        BusinessInfo busInfo = new BusinessInfo();
        busInfo.setMpc("MPC");
        umFlow.setBusinnessInfo(busInfo);
        leg.setUserMessageFlow(umFlow);

        prFlow = new PullRequestFlow();
        prFlow.setMPC("MPC-1");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC("MPC-2");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC("MPC-3");
        leg.addPullRequestFlow(prFlow);
        validPMode.addLeg(leg);

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(validPMode)));
    }

    @Test
    public void testInvalidGeneralParameters() {
    	PMode invalidPMode = new PMode();

        invalidPMode.setMep(EbMSConstants.ONE_WAY_MEP+"/smth"); // wrong mep
        invalidPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        invalidPMode.addLeg(new Leg());

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
        invalidPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        invalidPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH + "/smth"); // wrong mep binding

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
        invalidPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        invalidPMode.addLeg(new Leg()); // adding second leg

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        invalidPMode.setLegs(null);
        invalidPMode.setMep(EbMSConstants.TWO_WAY_MEP);
        invalidPMode.addLeg(new Leg()); // adding only one leg into two way mep

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
    }
}

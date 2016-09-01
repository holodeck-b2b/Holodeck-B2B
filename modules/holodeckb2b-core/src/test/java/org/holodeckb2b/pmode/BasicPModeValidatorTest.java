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

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.pmode.helpers.BusinessInfo;
import org.holodeckb2b.pmode.helpers.EncryptionConfig;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.PartnerConfig;
import org.holodeckb2b.pmode.helpers.PullRequestFlow;
import org.holodeckb2b.pmode.helpers.SecurityConfig;
import org.holodeckb2b.pmode.helpers.SigningConfig;
import org.holodeckb2b.pmode.helpers.UserMessageFlow;
import org.holodeckb2b.testhelpers.HolodeckCore;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link BasicPModeValidator}
 *
 * @author Sander Fieten <sander at chasquis-services.com>
 */
public class BasicPModeValidatorTest {

    private static String baseDir;

    private static BasicPModeValidator validator = new BasicPModeValidator();

    public BasicPModeValidatorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        baseDir = BasicPModeValidatorTest.class.getClassLoader().getResource("pmode_validation").getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckCore(baseDir));
    }

    /**
     * Tests a valid simple P-Mode which does not contain security settings
     */
    @Test
    public void testMinimalValidPMode() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        validPMode.addLeg(new Leg());

        assertTrue(Utils.isNullOrEmpty(validator.isPModeValid(validPMode)));
    }

    /**
     * Tests a valid P-Mode which contains security settings
     */
    @Test
    public void testValidPModeWithSecurity() {
        PMode validPMode = new PMode();
        validPMode.setMep(EbMSConstants.TWO_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.TWO_WAY_PUSH_PULL);

        PartnerConfig initiator = new PartnerConfig();
        SecurityConfig secConfig = new SecurityConfig();
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        secConfig.setSignatureConfiguration(sigConfig);
        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("partya");
        secConfig.setEncryptionConfiguration(encConfig);
        initiator.setSecurityConfiguration(secConfig);
        validPMode.setInitiator(initiator);

        PartnerConfig responder = new PartnerConfig();
        secConfig = new SecurityConfig();
        sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("partyb");
        sigConfig.setCertificatePassword("ExampleB");
        secConfig.setSignatureConfiguration(sigConfig);
        encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("partyb");
        encConfig.setCertificatePassword("ExampleB");
        secConfig.setEncryptionConfiguration(encConfig);
        responder.setSecurityConfiguration(secConfig);
        validPMode.setResponder(responder);

        validPMode.addLeg(new Leg());

        Leg leg = new Leg();
        PullRequestFlow prFlow = new PullRequestFlow();
        prFlow.setMPC("sub-MPC");
        secConfig = new SecurityConfig();
        sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("partyc");
        secConfig.setSignatureConfiguration(sigConfig);
        prFlow.setSecurityConfiguration(secConfig);
        leg.addPullRequestFlow(prFlow);
        validPMode.addLeg(leg);

        assertTrue(Utils.isNullOrEmpty(validator.isPModeValid(validPMode)));
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
        prFlow.setMPC("sub-MPC-1");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC("sub-MPC-2");
        leg.addPullRequestFlow(prFlow);
        prFlow = new PullRequestFlow();
        prFlow.setMPC("sub-MPC-3");
        leg.addPullRequestFlow(prFlow);
        validPMode.addLeg(leg);

        assertTrue(Utils.isNullOrEmpty(validator.isPModeValid(validPMode)));

        validPMode = new PMode();
        validPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        validPMode.setMepBinding(EbMSConstants.ONE_WAY_PULL);

        leg = new Leg();
        UserMessageFlow umFlow = new UserMessageFlow();
        BusinessInfo busInfo = new BusinessInfo();
        busInfo.setMpc("MPC");
        umFlow.setBusinnessInfo(busInfo);

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

        assertTrue(Utils.isNullOrEmpty(validator.isPModeValid(validPMode)));
    }
}

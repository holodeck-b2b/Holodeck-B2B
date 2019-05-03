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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.holodeckb2b.common.pmode.BusinessInfo;
import org.holodeckb2b.common.pmode.EncryptionConfig;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.PartnerConfig;
import org.holodeckb2b.common.pmode.Protocol;
import org.holodeckb2b.common.pmode.PullRequestFlow;
import org.holodeckb2b.common.pmode.SecurityConfig;
import org.holodeckb2b.common.pmode.SigningConfig;
import org.holodeckb2b.common.pmode.UserMessageFlow;
import org.holodeckb2b.common.pmode.UsernameTokenConfig;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link BasicPModeValidator}
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class BasicPModeValidatorTest {

    private static String baseDir;

    private static BasicPModeValidator validator = new BasicPModeValidator();

    private PMode invalidPMode;

    public BasicPModeValidatorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = BasicPModeValidatorTest.class.getClassLoader().getResource("pmode_validation").getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(baseDir));
    }

    @Before
    public void setUp() {
        invalidPMode = new PMode();
    }

    @After
    public void tearDown() {
        invalidPMode = null;
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

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(validPMode)));
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

        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

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

    @Test
    public void testInvalidUsernameTokenParameters() {
        invalidPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        invalidPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        invalidPMode.addLeg(new Leg());

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        PartnerConfig initiator = new PartnerConfig();
        SecurityConfig secConfig = new SecurityConfig();
        SigningConfig sigConfig = new SigningConfig();
        sigConfig.setKeystoreAlias("exampleca");
        secConfig.setSignatureConfiguration(sigConfig);
        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("partya");
        secConfig.setEncryptionConfiguration(encConfig);
        initiator.setSecurityConfiguration(secConfig);
        invalidPMode.setInitiator(initiator);

        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();

        assertTrue(Utils.isNullOrEmpty(tokenConfig.getUsername()));

        // non null empty token with default header
        secConfig.setUsernameTokenConfiguration(
                SecurityHeaderTarget.DEFAULT, tokenConfig);

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        tokenConfig = new UsernameTokenConfig();

        // non null empty token with EBMS header
        secConfig.setUsernameTokenConfiguration(
                SecurityHeaderTarget.EBMS, tokenConfig);

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
    }

    @Test
    public void testInvalidX509Parameters() {
        invalidPMode.setMep(EbMSConstants.ONE_WAY_MEP);
        invalidPMode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

        Leg leg = new Leg();
        Protocol protocolConfig = new Protocol();
        leg.setProtocol(protocolConfig);
        invalidPMode.addLeg(leg);

        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        PartnerConfig initiator = new PartnerConfig();
        SecurityConfig secConfig = new SecurityConfig();
        SigningConfig sigConfig = new SigningConfig();
        secConfig.setSignatureConfiguration(sigConfig);
        EncryptionConfig encConfig = new EncryptionConfig();
        secConfig.setEncryptionConfiguration(encConfig);
        initiator.setSecurityConfiguration(secConfig);
        invalidPMode.setInitiator(initiator);

        assertFalse(PModeUtils.isHolodeckB2BInitiator(invalidPMode));
        protocolConfig.setAddress("address");
        assertTrue(PModeUtils.isHolodeckB2BInitiator(invalidPMode));
        assertTrue(Utils.isNullOrEmpty(sigConfig.getKeystoreAlias()));

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        encConfig.setKeystoreAlias("partya");

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        encConfig.setCertificatePassword("ExampleA");

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        sigConfig.setKeystoreAlias("partyb");

        assertNotNull(sigConfig.getKeystoreAlias());

        assertFalse(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));

        sigConfig.setCertificatePassword("ExampleB");
        assertTrue(Utils.isNullOrEmpty(validator.validatePMode(invalidPMode)));
    }
}

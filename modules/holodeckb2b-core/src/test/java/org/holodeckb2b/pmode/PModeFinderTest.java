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
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.pmode.helpers.*;
import org.holodeckb2b.security.tokens.IAuthenticationInfo;
import org.holodeckb2b.security.tokens.X509Certificate;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created at 16:56 02.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PModeFinderTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = PModeFinderTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Test
    public void testFindForPulling() throws Exception {
        PMode p = new PMode();
        p.setMep(EbMSConstants.ONE_WAY_MEP);
        p.setMepBinding(EbMSConstants.ONE_WAY_PULL);

        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        PartnerConfig initiator = new PartnerConfig();
        SecurityConfig secConfig = new SecurityConfig();
        X509Certificate sigConfig = new X509Certificate(null);
        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("exampleca");
        secConfig.setEncryptionConfiguration(encConfig);
        secConfig.setUsernameTokenConfiguration(
                ISecurityConfiguration.WSSHeaderTarget.EBMS, tokenConfig);
        initiator.setSecurityConfiguration(secConfig);
        p.setInitiator(initiator);

        Leg leg = new Leg();
        PullRequestFlow prFlow = new PullRequestFlow();
        prFlow.setSecurityConfiguration(secConfig);
        leg.addPullRequestFlow(prFlow);

        p.addLeg(leg);

        core.getPModeSet().add(p);

        final Map<String, IAuthenticationInfo> authInfo = new HashMap<>();
        authInfo.put(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);
        authInfo.put(SecurityConstants.SIGNATURE, sigConfig);

        Collection<IPMode> pmodes = PModeFinder.findForPulling(authInfo, null);
        assertFalse(Utils.isNullOrEmpty(pmodes));
    }

    @Test
    public void testVerifyPullRequestAuthorization() throws Exception {
        PMode p = new PMode();
        p.setMep(EbMSConstants.ONE_WAY_MEP);
        p.setMepBinding(EbMSConstants.ONE_WAY_PULL);

        UsernameTokenConfig tokenConfig = new UsernameTokenConfig();
        tokenConfig.setUsername("username");
        tokenConfig.setPassword("secret");

        PartnerConfig initiator = new PartnerConfig();
        SecurityConfig secConfig = new SecurityConfig();
        X509Certificate sigConfig = new X509Certificate(null);

        EncryptionConfig encConfig = new EncryptionConfig();
        encConfig.setKeystoreAlias("exampleca");
        secConfig.setEncryptionConfiguration(encConfig);
        secConfig.setUsernameTokenConfiguration(
                ISecurityConfiguration.WSSHeaderTarget.EBMS, tokenConfig);
        initiator.setSecurityConfiguration(secConfig);
        p.setInitiator(initiator);

        PullRequestFlow prFlow = new PullRequestFlow();
        prFlow.setSecurityConfiguration(secConfig);

        final Map<String, IAuthenticationInfo> authInfo = new HashMap<>();
        authInfo.put(SecurityConstants.EBMS_USERNAMETOKEN, tokenConfig);
        authInfo.put(SecurityConstants.SIGNATURE, sigConfig);

        Method method = PModeFinder.class
                .getDeclaredMethod("verifyPullRequestAuthorization",
                        ISecurityConfiguration.class,
                        ISecurityConfiguration.class, Map.class);
        method.setAccessible(true);
        boolean result = (Boolean) method.invoke(null, prFlow.getSecurityConfiguration(),
                p.getInitiator().getSecurityConfiguration(), authInfo);
        assertTrue(result);
    }
}
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workers.PModeWatcher;
import org.holodeckb2b.ebms3.pmode.PModeFinder;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.IUsernameTokenProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 16:56 02.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PModeFinderTest {

    private static HolodeckB2BTestCore core;

    @BeforeClass
    public static void setUpClass() throws Exception {
        core = new HolodeckB2BTestCore();
        HolodeckB2BCoreInterface.setImplementation(core);

        // Read the set of test P-Modes
        Map<String, Object> param = new HashMap<>();
        param.put("watchPath", PModeFinderTest.class.getClassLoader().getResource("pmodefinding").getPath());
        PModeWatcher    pmodeReader = new PModeWatcher();
        pmodeReader.setParameters(param);

        pmodeReader.run();
    }

    @Test
    public void testPartnerDefinedEbmsUTOnly() throws Exception {

        final Collection<ISecurityProcessingResult> authInfo = new ArrayList<>();
        IUsernameTokenProcessingResult utResultMock = mock(IUsernameTokenProcessingResult.class);
        when(utResultMock.getUsername()).thenReturn("johndoe");
        when(utResultMock.getPassword()).thenReturn("secret");
        when(utResultMock.getPasswordType()).thenReturn(UTPasswordType.TEXT);
        when(utResultMock.getTargetedRole()).thenReturn(SecurityHeaderTarget.EBMS);

        authInfo.add(utResultMock);

        Collection<IPMode> pmodes = PModeFinder.findForPulling(authInfo, null);
        assertFalse(Utils.isNullOrEmpty(pmodes));
        assertEquals("tp_ebms_ut_only", pmodes.iterator().next().getId());
    }

    @Test
    public void testFlowDefinedEbmsUTOnly() throws Exception {

    	final Collection<ISecurityProcessingResult> authInfo = new ArrayList<>();
        IUsernameTokenProcessingResult utResultMock = mock(IUsernameTokenProcessingResult.class);
        when(utResultMock.getUsername()).thenReturn("johndoe");
        when(utResultMock.getPassword()).thenReturn("secret");
        when(utResultMock.getPasswordType()).thenReturn(UTPasswordType.TEXT);
        when(utResultMock.getTargetedRole()).thenReturn(SecurityHeaderTarget.EBMS);

        authInfo.add(utResultMock);

        Collection<IPMode> pmodes = PModeFinder.findForPulling(authInfo,
                                                                "http://test.holodeck-b2b.org/sampleMPC/subchannel1");
        assertFalse(Utils.isNullOrEmpty(pmodes));
        assertEquals("flow_ebms_ut_only", pmodes.iterator().next().getId());
    }

    @Test
    public void testFlowOverrideEbmsUTOnly() throws Exception {

    	final Collection<ISecurityProcessingResult> authInfo = new ArrayList<>();
        IUsernameTokenProcessingResult utResultMock = mock(IUsernameTokenProcessingResult.class);
        when(utResultMock.getUsername()).thenReturn("janedoe");
        when(utResultMock.getPassword()).thenReturn("secret");
        when(utResultMock.getPasswordType()).thenReturn(UTPasswordType.TEXT);
        when(utResultMock.getTargetedRole()).thenReturn(SecurityHeaderTarget.EBMS);

        authInfo.add(utResultMock);

        Collection<IPMode> pmodes = PModeFinder.findForPulling(authInfo, null);

        assertFalse(Utils.isNullOrEmpty(pmodes));
        assertEquals("flow_override_ebms_ut_only", pmodes.iterator().next().getId());
    }

    @Test
    public void testNoneFound() throws Exception {
    	final Collection<ISecurityProcessingResult> authInfo = new ArrayList<>();
        IUsernameTokenProcessingResult utResultMock = mock(IUsernameTokenProcessingResult.class);
        when(utResultMock.getUsername()).thenReturn("nothere");
        when(utResultMock.getPassword()).thenReturn("notsosecret");
        when(utResultMock.getPasswordType()).thenReturn(UTPasswordType.TEXT);
        when(utResultMock.getTargetedRole()).thenReturn(SecurityHeaderTarget.EBMS);

        authInfo.add(utResultMock);

        Collection<IPMode> pmodes = PModeFinder.findForPulling(authInfo, null);

        assertTrue(Utils.isNullOrEmpty(pmodes));            	
    }
}
/*
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.holodeckb2b.common.pmode.Agreement;
import org.holodeckb2b.common.pmode.InMemoryPModeSet;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.SimplePModeSet;
import org.holodeckb2b.common.testhelpers.TestConfig;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 17:28 09.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PModeManagerTest {
	
	private static TestConfig config;
	
    @BeforeClass
    public static void setUpClass() throws Exception {
    	config = new TestConfig();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
    }
    
    @Test
    public void testDefaultConfig() {
    	try {
    		config.pmodeStorageClass = null;
    		config.pmodeValidatorClass = null;
    		
    		PModeManager manager = new PModeManager(config);
    		
    		Field pmodeStorage = PModeManager.class.getDeclaredField("deployedPModes");
    		pmodeStorage.setAccessible(true);
    		assertTrue(pmodeStorage.get(manager) instanceof InMemoryPModeSet);

    		Field pmodeValidator = PModeManager.class.getDeclaredField("validator");
    		pmodeValidator.setAccessible(true);
    		assertNull(pmodeValidator.get(manager));
    	} catch (Throwable t) {
    		t.printStackTrace();
    		fail();
    	}
    }
    
    @Test
    public void testCustomConfig() {
    	try {
    		config.pmodeStorageClass = SimplePModeSet.class.getName();
    		config.pmodeValidatorClass = TestValidator.class.getName();
    		PModeManager manager = new PModeManager(config);
    		
    		Field pmodeStorage = PModeManager.class.getDeclaredField("deployedPModes");
    		pmodeStorage.setAccessible(true);
    		assertTrue(pmodeStorage.get(manager) instanceof SimplePModeSet);
    		
    		Field pmodeValidator = PModeManager.class.getDeclaredField("validator");
    		pmodeValidator.setAccessible(true);
    		assertTrue(pmodeValidator.get(manager) instanceof TestValidator);
    	} catch (Throwable t) {
    		t.printStackTrace();
    		fail();
    	}
    }

    @Test
    public void testPModeAddReplace() {
		config.pmodeValidatorClass = TestValidator.class.getName();
		PModeManager manager = new PModeManager(config);
    	
    	PMode pmode = new PMode();
        pmode.setId("valid-pmode-1");
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        pmode.addLeg(new Leg());

        try {
            manager.add(pmode);
        } catch (PModeSetException ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        assertTrue(manager.containsId(pmode.getId()));
        
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PULL);
        try {
            manager.replace(pmode);
        } catch (PModeSetException ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }        
        assertTrue(manager.containsId(pmode.getId()));
        assertEquals(EbMSConstants.ONE_WAY_PULL, pmode.getMepBinding());
    }
    
    @Test
    public void testPModeRejectInvalid() {
		config.pmodeValidatorClass = TestValidator.class.getName();
		PModeManager manager = new PModeManager(config);
    	
    	PMode pmode = new PMode();
        pmode.setId("invalid-pmode");
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        pmode.setAgreement(new Agreement());
        pmode.addLeg(new Leg());

        try {
            manager.add(pmode);
            fail();
        } catch (PModeSetException ex) {
        }
        assertFalse(manager.containsId(pmode.getId()));
        
        pmode.setAgreement(null);
        try {
            manager.add(pmode);
        } catch (PModeSetException ex) {
        	fail();
        }
        assertTrue(manager.containsId(pmode.getId()));
        
        pmode.setAgreement(new Agreement());
        try {
            manager.replace(pmode);
            fail();
        } catch (PModeSetException ex) {
        }
    }
    
    @Test
    public void testPModeRemove() {
		config.pmodeValidatorClass = TestValidator.class.getName();
		PModeManager manager = new PModeManager(config);
    	
    	PMode pmode = new PMode();
        pmode.setId("valid-pmode-1");
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        pmode.addLeg(new Leg());

        try {
            manager.add(pmode);
        } catch (PModeSetException ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }
        assertTrue(manager.containsId(pmode.getId()));
        
        try {
            manager.remove(pmode.getId());
        } catch (PModeSetException ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            fail();
        }        
        assertFalse(manager.containsId(pmode.getId()));
    }
}
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.holodeckb2b.common.pmode.Agreement;
import org.holodeckb2b.common.pmode.InMemoryPModeSet;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestConfig;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.junit.Before;
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
    
    @Before
    public void setupTest() {
    	config.acceptNonValidablePMode = true;
    	TestNOpPModeSet.failOnInit = false; 
    }
    
    @Test
    public void testDefaultConfig() {
    	try {
    		PModeManager manager = new PModeManager(config);
    		
    		Field pmodeStorage = PModeManager.class.getDeclaredField("deployedPModes");
    		pmodeStorage.setAccessible(true);
    		assertTrue(pmodeStorage.get(manager) instanceof InMemoryPModeSet);

    	} catch (Throwable t) {
    		t.printStackTrace();
    		fail();
    	}
    }

    @Test
    public void testStorageLoading() {
    	
    	ClassLoader ccl = this.getClass().getClassLoader();
    	URLClassLoader ecl = new URLClassLoader(new URL[] { ccl.getResource("pmodeManager/") }, ccl);    	
    	Thread.currentThread().setContextClassLoader(ecl);
    	
    	PModeManager manager = null; 
    	try {
    		manager = new PModeManager(config);
    	} catch (PModeSetException pse) {
    		fail();
    	}
    	
    	try {
			Field pmodeStorage = PModeManager.class.getDeclaredField("deployedPModes");
			pmodeStorage.setAccessible(true);
			IPModeSet storageComp = (IPModeSet) pmodeStorage.get(manager);
			
			assertNotNull(storageComp);
			assertEquals(TestNOpPModeSet.class, storageComp.getClass());			
    	} catch (Throwable t) {
    		t.printStackTrace();
    		fail();
    	}

    	TestNOpPModeSet.failOnInit = true; 
    	try {
    		manager = new PModeManager(config);
    		fail();
    	} catch (PModeSetException pse) {
    		// Correct
    	}

    	Thread.currentThread().setContextClassLoader(ccl);
    }
    
    @Test
    public void testValidatorLoading() {
    	
    	ClassLoader ccl = this.getClass().getClassLoader();
    	URLClassLoader ecl = new URLClassLoader(new URL[] { ccl.getResource("pmodeManager/") }, ccl);    	
    	Thread.currentThread().setContextClassLoader(ecl);
    	
    	PModeManager manager = null; 
    	try {
    		manager = new PModeManager(config);
    	} catch (PModeSetException pse) {
    		fail();
    	}
    	
    	try {
			Field pmodeValidators = PModeManager.class.getDeclaredField("validators");
			pmodeValidators.setAccessible(true);
			List<IPModeValidator> validators = (List<IPModeValidator>) pmodeValidators.get(manager);
			
			assertNotNull(validators);
			assertEquals(2, validators.size());
			assertTrue(  validators.parallelStream().anyMatch(v -> v.getName().equals(new TestValidatorAllWrong().getName()))
					  && validators.parallelStream().anyMatch(v -> v.getName().equals(new TestValidatorAllGood().getName())));
    	} catch (Throwable t) {
    		t.printStackTrace();
    		fail();
    	}

    	Thread.currentThread().setContextClassLoader(ccl);
    }
    
    @Test
    public void testInvalidConfig() {
    	try {
    		config.acceptNonValidablePMode = false;
    		PModeManager manager = new PModeManager(config);
    		fail();
    	} catch (PModeSetException correct) {
    	}
    }    

    @Test
    public void testPModeAddReplace() throws PModeSetException {
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
    public void testPModeValidation() throws PModeSetException {
    	PModeManager manager = new PModeManager(config);
    	final TestValidatorAllWrong vAllWrong = new TestValidatorAllWrong();
    	final TestValidatorAllGood vAllGood = new TestValidatorAllGood();
    	try {
    		Field pmodeValidators = PModeManager.class.getDeclaredField("validators");
    		pmodeValidators.setAccessible(true);
    		List<IPModeValidator> validators = (List<IPModeValidator>) pmodeValidators.get(manager);
    		validators.add(vAllWrong);
    		validators.add(vAllGood);
    	} catch (Throwable t) {
    		fail();
    	}
    	PMode pmode = new PMode();
    	pmode.setId("invalid-pmode");    	
    	try {
    		manager.add(pmode);
    	} catch (PModeSetException ex) {
    	}
    	
    	assertTrue(vAllGood.isExecuted());
    	assertTrue(vAllWrong.isExecuted());
    }
    
    @Test
    public void testPModeRejectInvalid() throws PModeSetException {
		PModeManager manager = new PModeManager(config);

		try {
			Field pmodeValidators = PModeManager.class.getDeclaredField("validators");
			pmodeValidators.setAccessible(true);
			List<IPModeValidator> validators = (List<IPModeValidator>) pmodeValidators.get(manager);
			validators.add(new TestValidatorAllWrong());
		} catch (Throwable t) {
			fail();
		}
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
    }
    
    @Test
    public void testPModeRemove() throws PModeSetException {
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
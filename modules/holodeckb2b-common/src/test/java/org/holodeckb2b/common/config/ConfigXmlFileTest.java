/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.config;

import org.holodeckb2b.common.util.Utils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Tests the reading of the Holodeck B2B configuration file.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ConfigXmlFileTest {
    
    
    public ConfigXmlFile readConfigFile(String path) throws Exception {
        String absPath = this.getClass().getClassLoader().getResource("cfgxml/" + path).getPath();        
        return ConfigXmlFile.loadFromFile(absPath);
    }
    
    @Test
    public void testWrongPath() {        
        try {
            readConfigFile("no_such_file.xml");
            fail("Non existing file should have triggered exception!");
        } catch (Exception e) {
            // This is okay
        }
    }

    @Test
    public void testEmptyFile() {
        try {
            readConfigFile("emptyfile.xml");
            fail("Empty file should have triggered exception!");
        } catch (Exception e) {
            // This is okay
        }
    }

    @Test
    public void testNoNameParameter() {
        try {
            readConfigFile("nonameparameter.xml");
            fail("File with errors should have triggered exception!");
        } catch (Exception e) {
            // This is okay
        }
    }
    
    @Test
    public void testNoParameters() {
        try {
            ConfigXmlFile config = readConfigFile("noparameters.xml");
            fail("A config file with no parameters should trigger exception!");
        } catch (Exception e) {
            // Okay, config file must have at least one parameter
        }        
    }

    @Test
    public void testOneParameter() {
        try {
            ConfigXmlFile config = readConfigFile("oneparameter.xml");
            assertFalse(Utils.isNullOrEmpty(config.getParameters()));
            assertEquals("test value 1", config.getParameter("justone"));
        } catch (Exception e) {
            fail("A config file with one parameter should not trigger exception!");
        }        
    }
    
    @Test
    public void testMultipleParameters() {
        try {
            ConfigXmlFile config = readConfigFile("fullconfig.xml");
            assertEquals(9 , config.getParameters().size());

            // Test an empty parameter
            assertTrue(Utils.isNullOrEmpty(config.getParameter("ExternalHostName")));
            
            // Test some parameters with special characters
            assertEquals("conf/workers.xml", config.getParameter("WorkerConfig"));
            assertEquals("keypwd2$%'s;:@#$:!", config.getParameter("PrivateKeyStorePassword"));
            assertEquals(">pwd#$%!091", config.getParameter("PublicKeyStorePassword"));            
        } catch (Exception e) {
            fail("The full config file should not trigger exception!");
        }            
    }   
}

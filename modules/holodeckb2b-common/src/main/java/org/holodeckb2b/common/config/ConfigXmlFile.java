/**
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
package org.holodeckb2b.common.config;

import java.io.File;
import java.util.Map;
import org.holodeckb2b.common.util.Utils;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.core.Validate;

/**
 * Is responsible for reading the Holodeck B2B configuration parameters from an XML file. The structure of the config
 * file is defined by the XML Schema Definition <code>http://holodeck-b2b.org/schemas/2015/10/config</code>
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Root(name = "holodeckb2b-org")
@Namespace(reference = "http://holodeck-b2b.org/schemas/2015/10/config")
public class ConfigXmlFile {
    
    @ElementMap(entry = "parameter", key = "name", attribute = true, inline = true)
    private Map<String, String>     parameters;
       
    /**
     * Reads the configuration parameters from the XML document at the specified path. 
     * <p>The specified path can either be a relative or absolute path. In case of a relative path the base directory
     * is the Holodeck B2B deployment directory. To prevent issues with loading the configuration it is RECOMMENDED to
     * provide an absolute path.
     * 
     * @param path      The path to the XML document containing the configuration parameters.
     * @return  A {@link ConfigXmlFile} object containing the parameters read from the configuration file
     * @throws Exception When the configuration can not be read from the file at the specified path, either because it
     *                   can not be found or errors in the file contents.
     */
    public static ConfigXmlFile loadFromFile(final String path) throws Exception {
        ConfigXmlFile    configuration = null;
        
        File f = new File(path);        
        if (f.exists() && f.canRead()) {
            Serializer serializer = new Persister();
            try {
                configuration = serializer.read(ConfigXmlFile.class, f);                
            } catch (Exception ex) {
                throw new Exception("Could not load configuration parameters from " + path + "! " +
                                    "Details: " + ex.getMessage());
            }
         } else
                throw new Exception("Unable to read file " + path + ", can not load configuration!");
        
        return configuration;        
    }
    
    /**
     * Validates the read configuration. At the moment this validation only checks that each parameter is named.
     * 
     * @throws PersistenceException When a parameter included in the XML document has no name.
     */
    @Validate
    public void validate() throws PersistenceException {
        if (!Utils.isNullOrEmpty(parameters)) {
            for (String name : parameters.keySet())
                if (Utils.isNullOrEmpty(name))
                    throw new PersistenceException("Each parameter in the configuration must be named!");
        }
    }
    
    /**
     * Gets all configuration parameters read from the configuration file.
     * 
     * @return A <code>Map&lt;String, String&gt;</code> containing all configuration parameters. NOTE that <code>null
     *         </code>will be returned if the configuration file is not read.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }
    
    /**
     * Get the value for the configuration parameter with the specified name.
     * 
     * @param name  String containing the name of the parameter to get the value of.
     * @return      The value of the specified parameter as <code>String</code>, or<br>
     *              <code>null</code> if the parameter has no value or does not exist.
     */
    public String getParameter(final String name) {
        if (Utils.isNullOrEmpty(parameters))
            return null;
        else
            return parameters.get(name);
    }
}

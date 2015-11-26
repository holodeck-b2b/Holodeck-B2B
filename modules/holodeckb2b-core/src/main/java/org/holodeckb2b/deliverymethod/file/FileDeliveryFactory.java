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
package org.holodeckb2b.deliverymethod.file;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.holodeckb2b.common.delivery.IMessageDeliverer;
import org.holodeckb2b.common.delivery.IMessageDelivererFactory;
import org.holodeckb2b.common.delivery.MessageDeliveryException;

/**
 * Is an {@link IMessageDelivererFactory} implementation that creates {@link IMessageDeliverer} implementations that
 * deliver the message unit by writing the message units info to one or more file. Depending on the requested format 
 * an implementation is created. Currently there are three formats that can be chosen:<dl>
 *      <dt><i>1 - "mmd"</i></dt>
 *                  <dd>writes the message meta as included in the ebMS header and payloads into separate files. For
 *                      the message meta the MMD format described by schema 
 *                      <code>http://holodeck-b2b.org/schemas/2014/06/mmd</code> is used.<br> 
 *             <b>NOTE that this format can only deliver user message message units!</b></dd>
 *      <dt><i>2 - "ebms"</i></dt>
 *                  <dd>also writes the message meta data and payloads to separate files but uses a copy of 
 *                      ebMS header element as format for the meta data file;</dd>
 *      <dt><i>2 - "single_xml"</i></dt>
 *                  <dd>writes the complete message unit, including the payloads, to one XML file. Because the payloads
 *                      can be binary data they will be included <i>base64</i> encoded.</dd>
 * </dl>
 * <p>Which format is requested must be specified when creating the factory using the "<i>format</i>" setting. If not 
 * specified the <i>"ebms"</i> format will be used as default.<br>
 * Furthermore the directory where to write the files MUST be specified using the "<i>deliveryDirectoy</i>" setting.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see MMDDeliverer
 * @see SimpleFileDeliverer
 * @see SingleXMLDeliverer
 * 
 */
public class FileDeliveryFactory implements IMessageDelivererFactory {

    /**
     * The name of the parameter for the delivery directory
     */
    public static final String DELIVERY_DIR_PARAM = "deliveryDirectory";
 
    /**
     * The name of the parameter for the format
     */
    public static final String FORMAT_PARAM = "format";
    
    /**
     * Enumeration for the possible formats
     */
    enum FileFormat { MMD, EBMS, SINGLE_XML }
    
    /**
     * The delivery directory path
     */
    protected String deliveryDir = null;
    
    /**
     * The XML format to use for the message info
     */
    protected FileFormat miFormat = FileFormat.EBMS;
    
    /**
     * Initializes the factory, ensures that a valid delivery directory is specified.
     * 
     * @param settings  The settings to use for the factory. MUST contain at least on entry with key 
     *                  <code>deliveryDirectoy</code> holding the path to the delivery directory. 
     * @throws MessageDeliveryException When there is no directory specified or the specified directory does not exist
     *                                  or is not writable
     */
    @Override
    public void init(Map<String, ?> settings) throws MessageDeliveryException {
        if (settings != null) {
            try {
               deliveryDir = (String) settings.get(DELIVERY_DIR_PARAM);                
            } catch (ClassCastException ex) {
                throw new MessageDeliveryException("Configuration error! No directory specified!");
            }
        }        
        if (!checkDirectory())
            throw new MessageDeliveryException("Configuration error! Specified directory [" + deliveryDir
                                                                        + " does not exits or is not writable!");
        // Ensure directory path ends with separator
        deliveryDir = (deliveryDir.endsWith(FileSystems.getDefault().getSeparator()) ? deliveryDir 
                              : deliveryDir + FileSystems.getDefault().getSeparator());
        
        // Check if XML format is specified
        String xmlFormat = null;
        try {
             xmlFormat = (String) settings.get(FORMAT_PARAM);
        } catch (ClassCastException ex) {
            // Ignore error, use default
        }
        
        if ("mmd".equalsIgnoreCase(xmlFormat))
            miFormat = FileFormat.MMD;
        else if ("single_xml".equalsIgnoreCase(xmlFormat))
            miFormat = FileFormat.SINGLE_XML;
        else
            miFormat = FileFormat.EBMS;
    }

    /**
     * Create a new deliverer for message delivery. 
     * 
     * @return  The new deliverer, the type depends on the selected XML format
     * @throws MessageDeliveryException When the specified directory has become invalid
     */
    @Override
    public IMessageDeliverer createMessageDeliverer() throws MessageDeliveryException {
        if (checkDirectory())
            switch (miFormat) {
                case SINGLE_XML :
                    return new SingleXMLDeliverer(deliveryDir);
                case MMD :
                    return new MMDDeliverer(deliveryDir);
                default:
                    return new SimpleFileDeliverer(deliveryDir);
            }        
        else
            // Directory is not valid anymore
            throw new MessageDeliveryException("Specified directory [" + deliveryDir
                                                                        + " does not exits or is not writable!");            
    }
    
    /**
     * Checks if the directory is still valid, i.e. exists and is writable.
     * 
     * @return <code>true</code> when directory is valid,<br><code>false</code> if not
     */
    private boolean checkDirectory() {
        try {
            Path path = Paths.get(deliveryDir);                        

            // Test if given path exists and is a directory
            if (path == null || !Files.isDirectory(path) || !Files.isWritable(path))
                // Not a writable directory!
                return false;
        } catch (Exception ex) {
            return false;
        }
        
        return true;
    }
}

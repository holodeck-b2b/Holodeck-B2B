/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.pmode.xml;

import org.holodeckb2b.common.pmode.IProtocol;
import org.simpleframework.xml.Element;

/**
 *
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class Protocol implements IProtocol {
    
    @Element (name = "Address", required = false)
    private String address = "";
    
    @Element (name = "UseChunking" , required = false)
    private Boolean chunked = true;
    
    @Element (name = "SoapVersion", required = false)
    private String soapVersion = "1.2";

    @Element (name = "UseHTTPCompression", required = false)
    private Boolean useHTTPCompression = false;
    
     /**
     * Gets the protocol address
     * 
     * @return The protocol address
     */
    @Override
    public String getAddress() {
        return this.address;
    }
    
    /**
     * Get the value for the chunking parameter.
     * @return True if the HTTP message is chunked, otherwise false.
     */
    @Override
    public boolean useChunking() {
        return this.chunked;
    }
    
    /**
     * Gets the soap version
     * 
     * @return The soap version
     */
    @Override
    public String getSOAPVersion() {
        return this.soapVersion;
    }
   
    /**
     * Check if HTTP compression is used.
     * @return  Boolean value indicating if HTTP compression is used or not.
     */
    @Override
    public boolean useHTTPCompression() {
        return this.useHTTPCompression;
    }
 
}

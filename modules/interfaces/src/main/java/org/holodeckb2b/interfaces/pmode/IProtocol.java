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
package org.holodeckb2b.interfaces.pmode;

/*
 * #%L
 * Holodeck B2B - Interfaces
 * %%
 * Copyright (C) 2015 The Holodeck B2B Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

/**
 * Defines the settings for the SOAP and transport protocol used for exchanging the messages. Currently limited to the
 * URL where the other MSH can be found, the SOAP version to use and whether HTTP compression and/or chunking must be
 * used.
 * <p>
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IProtocol {
    
    /**
     * Gets the URL where the message should be sent to.
     * 
     * @return The destination address
     */
    public String getAddress();
    
    /**
     * Gets the SOAP version to use for packaging the ebMS message.
     * 
     * @return  "1.1" when SOAP 1.1 should be used, "1.2" for SOAP 1.2
     * @todo: Consider using enum?
     */
    public String getSOAPVersion();
    
    /**
     * Indicates whether the HTTP <i>"chunked"</i> transfer encoding should be used. See section 3.6 of the HTTP/1.1 
     * protocol [RFC2616] for more details.
     * <p>Using the gzip content encoding requires the use of the chunked transfer encoding. Therefor Holodeck B2B will
     * first check whether compression content encoding must be used an ignore the "chunked" setting. 
     * 
     * @return  <code>true</code> when the <i>"chunked"</i> encoding should be used,<br>
     *          <code>false</code> otherwise
     * @see #useHTTPCompression() 
     */
    public boolean useChunking();
    
    /**
     * Indicates whether the HTTP <i>"gzip"</i> compression content encoding should be used. See section 3.5 of the 
     * HTTP/1.1 protocol [RFC2616] for more details.
     * 
     * @return  <code>true</code> when the <i>"gzip"</i> content encoding should be used,<br>
     *          <code>false</code> otherwise
     */
    public boolean useHTTPCompression();
    
}

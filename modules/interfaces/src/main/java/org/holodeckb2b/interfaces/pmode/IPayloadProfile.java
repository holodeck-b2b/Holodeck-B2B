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


/**
 * Represents the P-Mode parameters that define what and how payloads should be included in the message. 
 * <p>The <b>PayloadProfile</b> parameter is described in appendix D of the ebMS V3 Core Specification.
 * <p><b>NOTE: </b>Currently the payload profile is not used by Holodeck B2B to restrict the payloads that can be 
 * included in a user message. Therefore this interface currently does not define any methods.
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public interface IPayloadProfile {
 
    /*
     *  
     * public int getMaxSize();
    */
    
}

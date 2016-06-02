/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.pmode;



/**
 * Represents the P-Mode parameters for the exchange of a user message message unit and related error signal message 
 * units. 
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IUserMessageFlow {
 
    /**
     * Gets the business information that must included in the message. 
     * <p><b>NOTE: </b>A P-Mode does not need to include this information. If not specified the information to include
     * must be supplied during message submit.
     * 
     * @return The business information to include in the message as an {@link IBusinessInfo} object if specified by the
     *         P-Mode, or<br>
     *         <code>null</code> when not specified.
     */
    public IBusinessInfo getBusinessInfo();
    
    /**
     * Gets the payload profile which defines what and how payloads are to be included in the message. 
     * <p>The profile is about the payload meta-data like the maximum size of payloads (individual and total), the 
     * maximum number of payloads, whether they must compressed, etc.
     * <p><b>NOTE: </b>Except AS4 compression profiling the payloads is currently NOT supported.
     * 
     * @return An {@link IPayloadProfile} object containing the payload profile, or<br>
     *         <code>null</code> when not specified. 
     */
    public IPayloadProfile getPayloadProfile();

    /**
     * Gets the configuration for handling errors which are caused by user message exchanged in this flow. 
     * <p>Providing configuration for error handling is optional, but it is RECOMMENDED to do so. If no error handling
     * configuration is provided errors will only be logged but not otherwise reported.
     * 
     * @return An {@link IErrorHandling} object representing the error handling configuration, or<br>
     *         <code>null</code> when not specified 
     */
    public IErrorHandling getErrorHandlingConfiguration();    

}

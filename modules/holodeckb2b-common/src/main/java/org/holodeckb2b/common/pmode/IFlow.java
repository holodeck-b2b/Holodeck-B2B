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
package org.holodeckb2b.common.pmode;

/**
 * Contains the P-Mode parameters for the exchange of a pull request or user message and its related messages (error and
 * or receipt). As a pull request does not contain any business information other than the MPC the requested user 
 * message should be pulled from the configuration is limited to security and error handling.
 * 
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IFlow {
    
 
    /**
     * Gets the business information that must included in the message. Most of this information only applies to user
     * messages as pull request do not contain business information. Only the MPC name which is contained in the 
     * business info is contained in the pull request.
     * <p><b>NOTE: </b>A P-Mode does not need to include this information. If not specified the information to include
     * must be supplied during message submit.
     * 
     * @return The business information to include in the message as an {@link IBusinessInfo} object if specified by the
     *         P-Mode, or<br>
     *         <code>null</code> when not specified.
     */
    public IBusinessInfo getBusinessInfo();
    
    /**
     * Gets the payload profile which defines how payloads are to be included in the message. Only applies to user 
     * messages. As the payloads contain the actual business information, the profile is about the payload meta-data 
     * like the maximum size of payloads, the number of payloads, whether they must compressed, etc.
     * <p><b>NOTE: </b>Profiling the payloads is optional.  
     * 
     * @return An {@link IPayloadProfile} object containing the payload profile, or<br>
     *         <code>null</code> when not specified. 
     */
    public IPayloadProfile getPayloadProfile();
    
    /**
     * Gets the configuration for handling errors which are caused by the pull request or user message exchanged in
     * this flow. This configuration settings (largely) correspond with the P-Mode parameter group ErrorHandling.
     * <p>For the pull request the error handling configuration is (currently) limited to indication whether errors
     * should be reported to the business application as the other settings are not useful in case of a pull request. 
     * 
     * @return An {@link IErrorHandling} object representing the error handling configuration, or<br>
     *         <code>null</code> when not specified
     */
    public IErrorHandling getErrorHandlingConfiguration();
}

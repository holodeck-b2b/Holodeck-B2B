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
 * Is the base class defining the P-Mode parameters shared for the exchange of a pull request, user messages and 
 * related error messages.
 * <p>The P-Mode model as described in appendix D of the ebMS specification also suggests that the P-Mode parameters
 * for security are defined per message (see figure 15) and therefor would be part of <code>Flow</code>. Within Holodeck
 * B2B we however assume that trading partners involved in a message exchange will use the same settings for all 
 * messages they sent. Therefor security settings are included with the trading partners configuration.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IFlow {
        
    /**
     * Gets the configuration for handling errors which are caused by the pull request or user message exchanged in
     * this flow. These configuration settings (largely) correspond with the P-Mode parameter group ErrorHandling.
     * <p>For the pull request the error handling configuration is (currently) limited to indication whether errors
     * should be reported to the business application as the other settings are not useful in case of a pull request. 
     * 
     * @return An {@link IErrorHandling} object representing the error handling configuration, or<br>
     *         <code>null</code> when not specified
     */
    public IErrorHandling getErrorHandlingConfiguration();    
}

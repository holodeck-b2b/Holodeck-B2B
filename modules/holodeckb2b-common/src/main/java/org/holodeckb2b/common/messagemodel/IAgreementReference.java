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
package org.holodeckb2b.common.messagemodel;

/**
 * Defines an interface to access information on the agreement the business partners use to exchange messages. This 
 * interface does not provide methods to access the agreement itself, but only for getting the meta-data on the 
 * agreement. Corresponds to the information contained in the <code>eb:AgreementRef</code> element of the ebMS header.
 * See also section 5.2.2.7 of the ebMS Core Specification for more info.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IAgreementReference {
    
   /**
    * Gets the agreement name
    * 
    * @return The agreement name
    */ 
    public String getName();
    
    /**
     * Gets the agreement type
     * 
     * @return The agreement type
     */
    public String getType();
    
    /**
     * Gets the id of the P-Mode that governs the current message exchange. It is possible that one agreement will 
     * "oversee" more than one message exchange and therefore reference more than one P-Mode. Therefore this method is
     * only useful in the context of an actual message exchange.
     * 
     * @return  The P-Mode id
     */
    public String getPModeId();
}

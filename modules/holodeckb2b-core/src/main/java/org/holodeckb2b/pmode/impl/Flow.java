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
package org.holodeckb2b.pmode.impl;

import org.holodeckb2b.common.pmode.IFlow;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 *
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Root (name = "Flow", strict = false)
public class Flow implements IFlow {
    
    @Element (name = "BusinessInfo", required = false)
    private BusinessInfo businessInfo;

    @Element (name = "ErrorHandling", required = false)
    private ErrorHandling errorHandling;
    
    @Element (name = "PayloadProfile", required = false)
    private PayloadProfile payloadProfile;

    /**
     * Gets the business information
     * 
     * @return The business information
     */
    @Override
    public BusinessInfo getBusinessInfo() {
        return this.businessInfo;
    }
    
    @Override
    public ErrorHandling getErrorHandlingConfiguration() {
        return errorHandling;
    }
    
    /**
     * Gets the payload profile
     * 
     * @return The payload profile
     */
    @Override
    public PayloadProfile getPayloadProfile() {
        return this.payloadProfile;
    }
      
}

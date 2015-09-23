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

import java.util.ArrayList;
import java.util.Collection;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.pmode.IBusinessInfo;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Implements a BusinessInfo class which describes a trading partner.
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class BusinessInfo implements IBusinessInfo {
    
    
    @Element (name = "Action", required = false)
    private String action;
    
    @Element (name = "Mpc", required = false)
    private String mpc;
    
    @Element (name = "Service", required = false)
    private Service service;
    
    @ElementList (entry = "Property", type = Property.class , required = false, inline = true)
    private ArrayList<IProperty> properties;
    
    /**
     * Gets the action
     * 
     * @return The action
     */
    
    @Override
    public String getAction() {
        return this.action;
    }
    
    
    /**
     * Gets the Mpc
     * 
     * @return The Mpc
     */
    @Override
    public String getMpc() {
        return this.mpc;
    }
    
    /**
     * Gets the service
     * 
     * @return The service
     */
    
    @Override
    public Service getService() {
        return this.service;
    }
    
    /**
     * Gets the property
     * 
     * @return The property
     */
    @Override
    public Collection<IProperty> getProperties() {
        return this.properties;
    }
    
    /*
     * Constructor
     */
    public BusinessInfo() {}
    
}

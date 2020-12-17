/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.pmode.IBusinessInfo;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Contains the parameters related to a business information meta-data like Service and Action.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class BusinessInfo implements IBusinessInfo, Serializable {
	private static final long serialVersionUID = 1656766261293059907L;

    @Element (name = "Action", required = false)
    private String action;

    @Element (name = "Mpc", required = false)
    private String mpc;

    @Element (name = "Service", required = false)
    private Service service;

    @ElementList (entry = "Property", type = Property.class , required = false, inline = true)
    private ArrayList<IProperty> properties;

    /**
     * Default constructor creates a new and empty <code>BusinessInfo</code> instance.
     */
    public BusinessInfo() {}

    /**
     * Creates a new <code>BusinessInfo</code> instance using the parameters from the provided {@link IBusinessInfo}
     * object.
     *
     * @param source The source object to copy the parameters from
     */
    public BusinessInfo(final IBusinessInfo source) {
        this.action = source.getAction();
        this.mpc = source.getMpc();
        this.service = new Service(source.getService());
        if (!Utils.isNullOrEmpty(source.getProperties())) {
        	this.properties = new ArrayList<IProperty>(source.getProperties().size());
            source.getProperties().forEach(p -> this.properties.add(new Property(p)));
        }
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    @Override
    public String getMpc() {
        return mpc;
    }

    public void setMpc(final String mpc) {
        this.mpc = mpc;
    }

    @Override
    public IService getService() {
        return service;
    }

    public void setService(final Service service) {
        this.service = service;
    }

    @Override
    public Collection<IProperty> getProperties() {
        return properties;
    }

    public void setProperties(final Collection<Property> props) {
        if (this.properties == null || props == null)
            this.properties = new ArrayList<>();

        if (props != null)
            props.forEach((p) -> this.properties.add(p));
    }

    public void addProperty(final Property prop) {
        if (this.properties == null)
            this.properties = new ArrayList<>();

        this.properties.add(prop);
    }
}

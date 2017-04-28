/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.pmode.helpers;

import java.util.ArrayList;
import java.util.Collection;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.pmode.IBusinessInfo;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class BusinessInfo implements IBusinessInfo {

    private String                  action;
    private String                  mpc;
    private Service                 service;
    private Collection<IProperty>   properties;

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

    public void addProperty(final Property prop) {
        if (this.properties == null)
            this.properties = new ArrayList<>();

        this.properties.add(prop);
    }
}

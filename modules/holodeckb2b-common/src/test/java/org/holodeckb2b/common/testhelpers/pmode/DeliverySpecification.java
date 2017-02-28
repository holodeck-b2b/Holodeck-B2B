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
package org.holodeckb2b.common.testhelpers.pmode;

import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class DeliverySpecification implements IDeliverySpecification {

    private String               id;
    private String               factory;
    private Map<String, Object>  parameters;

    @Override
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String getFactory() {
        return factory;
    }

    public void setFactory(final String factory) {
        this.factory = factory;
    }

    @Override
    public Map<String, ?> getSettings() {
        return parameters;
    }

    public void setSettings(final Map<String, ?> parameters) {
        if (parameters != null)
            this.parameters = new HashMap<String, Object>(parameters);
        else
            this.parameters = null;
    }

}

/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Represents the <code>ValidatorConfiguration</code> XML complex type defined in the P-Mode XSD which is used to
 * provide the configuration of one validator that is part of the custom validation of message units.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
class ValidatorConfiguration implements IMessageValidatorConfiguration {

    @Element(name = "id", required = true)
    private String  id;

    @Element(name = "ValidatorFactoryClass", required = true)
    private String  validatorFactoryClass;

    @ElementList(entry = "Parameter", inline = true, required = false)
    private Collection<Property>    parameters;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getFactory() {
        return validatorFactoryClass;
    }

    @Override
    public Map<String, ?> getSettings() {
        if (!Utils.isNullOrEmpty(parameters)) {
            final HashMap<String, String>  settings = new HashMap<>();
            for (final Property p : parameters)
                settings.put(p.getName(), p.getValue());

            return settings;
        } else
            return null;
    }

}

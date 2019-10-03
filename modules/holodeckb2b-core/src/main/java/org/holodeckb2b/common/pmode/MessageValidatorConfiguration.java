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
import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Contains the parameters for one validator that is part of a custom validation that should be applied to <i>User
 * Message</i> message units exchanged using this P-Mode.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class MessageValidatorConfiguration implements IMessageValidatorConfiguration, Serializable {
	private static final long serialVersionUID = 1046031536481661223L;
	
    @Element(name = "id", required = true)
    private String  id;

    @Element(name = "ValidatorFactoryClass", required = true)
    private String  validatorFactoryClass;

    @ElementList(entry = "Parameter", inline = true, required = false)
    private Collection<Parameter>    parameters;

    /**
     * Default constructor creates a new and empty <code>MessageValidatorConfiguration</code> instance.
     */
    public MessageValidatorConfiguration() {}

    /**
     * Creates a new <code>MessageValidatorConfiguration</code> instance using the parameters from the provided {@link
     * IMessageValidatorConfiguration}  object.
     *
     * @param source The source object to copy the parameters from
     */
    @SuppressWarnings("unchecked")
	public MessageValidatorConfiguration(final IMessageValidatorConfiguration source) {
        this.id = source.getId();
        this.validatorFactoryClass = source.getFactory();
        setSettings((Map<String, Object>) source.getSettings());
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String getFactory() {
        return validatorFactoryClass;
    }

    public void setFactory(final String factoryClass) {
        this.validatorFactoryClass = factoryClass;
    }

    @Override
    public Map<String, ?> getSettings() {
    	if (!Utils.isNullOrEmpty(parameters)) {
	        HashMap<String, String> settings = new HashMap<>(parameters.size());
	        parameters.forEach(p -> settings.put(p.getName(), p.getValue()));
	        return settings;
    	} else
    		return null;
    }
    
    public void setSettings(final Map<String, Object> sourceSettings) {
        if (!Utils.isNullOrEmpty(sourceSettings)) {
            this.parameters = new ArrayList<>(sourceSettings.size());
            sourceSettings.forEach((n, v) -> this.parameters.add(new Parameter(n, v.toString())));
        }    
    }

    public void addSetting(final String name, final Object value) {
        if (this.parameters == null)
            this.parameters = new ArrayList<>();
        this.parameters.add(new Parameter(name, value.toString()));
    }
}

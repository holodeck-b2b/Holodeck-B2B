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

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.delivery.IDeliveryMethod;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;

/**
 * Contains the parameters related to a delivery method.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class DeliveryConfiguration implements IDeliverySpecification, Serializable {
	private static final long serialVersionUID = -4375532353411434169L;

    @Element(name = "DeliveryMethod", required = true)
    private String  deliveryMethodClassName;

    @Element(name = "PerformAsyncDelivery", required = false)
    private boolean performAsync = false;

    @Transient
    private Class<? extends IDeliveryMethod> deliveryMethodClass;

    @ElementList(entry = "Parameter", inline = true, required = false)
    private Collection<Parameter>    parameters;

    // The id of the delivery specification is not from the XML, but set based on the P-Mode id and type of delivery
    @Transient
    private String id;

    /**
     * Default constructor creates a new and empty <code>DeliverySpecification</code> instance.
     */
    public DeliveryConfiguration() {}

    /**
     * Creates a new <code>DeliverySpecification</code> instance using the parameters from the provided {@link
     * IDeliverySpecification}  object.
     *
     * @param source The source object to copy the parameters from
     */
    public DeliveryConfiguration(final IDeliverySpecification source) {
        this.id = source.getId();
        this.deliveryMethodClass = source.getDeliveryMethod();
        this.deliveryMethodClassName = source.getDeliveryMethod() == null ? null : source.getDeliveryMethod().getName();
        this.performAsync = source.performAsyncDelivery();
        setSettings(source.getSettings());
    }

    /**
     * Validates and converts the string provided in the <code>DeliveryMethod</code> element into the class object as
     * required by the {@link IDeliverySpecification} interface.
     *
     * @throws PersistenceException When the string provided in the XML can not be converted to a class.
     */
	@SuppressWarnings("unchecked")
	@Validate
    public void convertToClass() throws PersistenceException {
		if (Utils.isNullOrEmpty(deliveryMethodClassName))
			throw new PersistenceException("Missing required class name of Delivery Method");
		try {
			Class<?> c = Class.forName(deliveryMethodClassName);
            if (!IDeliveryMethod.class.isAssignableFrom(c))
                throw new PersistenceException("%s is not a IDeliveryMethod!", deliveryMethodClassName);
            deliveryMethodClass = (Class<? extends IDeliveryMethod>) c;
        } catch (final ClassNotFoundException ex) {
            throw new PersistenceException("Class %s could not be loaded!", deliveryMethodClassName);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public Class<? extends IDeliveryMethod> getDeliveryMethod() {
    	return deliveryMethodClass;
    }

    public void setDeliveryMethod(final Class<? extends IDeliveryMethod> deliveryMethodClass) {
    	this.deliveryMethodClass = deliveryMethodClass;
        this.deliveryMethodClassName = deliveryMethodClass.getName();
    }

    /**
     * @since 6.0.0
     */
    @Override
    public boolean performAsyncDelivery() {
    	return performAsync;
    }

    /**
     * Sets whether the delivery of message units should be performed asynchronous.
     * <p>
     * Note that the configured Delivery Method must support asynchronous delivery when asynchronous delivery is
     * requested.
     *
     * @param useAsync	<code>true</code> when message units should be delivered asynchronously,
     * 					<code>false</code> if not
     * @since 6.0.0
     */
    public void setAsyncDelivery(boolean useAsync) {
    	performAsync = useAsync;
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

    public void setSettings(final Map<String, ?> sourceSettings) {
        if (!Utils.isNullOrEmpty(sourceSettings)) {
            this.parameters = new ArrayList<>(sourceSettings.size());
            sourceSettings.forEach((n, v) -> this.parameters.add(new Parameter(n, v != null ? v.toString() : null)));
        }
    }

    public void addSetting(final String name, final Object value) {
        if (this.parameters == null)
            this.parameters = new ArrayList<>();
        this.parameters.add(new Parameter(name, value != null ? value.toString() : null));
    }
}

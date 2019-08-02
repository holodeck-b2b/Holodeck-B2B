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
import java.util.List;
import java.util.Map;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventHandlerFactory;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;

/**
 * Contains the parameters related to the handling of {@link IMessageProcessingEvent}s.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class EventHandlerConfig implements IMessageProcessingEventConfiguration, Serializable {
	private static final long serialVersionUID = 7648905261512216386L;

    /**
     * Maps the names from the XSD defined enumeration to the message unit interfaces
     */
    private static final Map<String, Class<? extends IMessageUnit>> XSD_MU_NAMES_2_CLASS;
    static {
        XSD_MU_NAMES_2_CLASS = new HashMap<>();
        XSD_MU_NAMES_2_CLASS.put("UserMessage", IUserMessage.class);
        XSD_MU_NAMES_2_CLASS.put("SignalMessage", ISignalMessage.class);
        XSD_MU_NAMES_2_CLASS.put("Receipt", IReceipt.class);
        XSD_MU_NAMES_2_CLASS.put("Error", IErrorMessage.class);
        XSD_MU_NAMES_2_CLASS.put("PullRequest", IPullRequest.class);
    }

    @Element(name = "HandlerFactoryClass", required = true)
    private String  handlerFactoryClass = null;

    @ElementList(entry = "HandledEvent", required = false, inline = true)
    private List<String>    handledEventNames = null;

    @ElementList(entry = "ForMessageUnit", required = false, inline = true)
    private List<String>    messageUnitNames = null;

    @ElementList(entry = "Parameter", inline = true, required = false)
    private Collection<Parameter>    parameters;

    @Transient
    private List<Class<? extends IMessageProcessingEvent>> handledEvents = null;

    @Transient
    private List<Class<? extends IMessageUnit>> forMessageUnits = null;

    /**
     * Validates and converts the strings provided in the <code>HandlerClass</code>, <code>HandledEvent</code> and
     * <code>ForMessageUnit</code> elements into lists of class / interfaces as required by the
     * {@link IMessageProcessingEventConfiguration} interface.
     *
     * @throws PersistenceException When the strings provided in the XML can not be converted to classes.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Validate
    public void convertToClasses() throws PersistenceException {
        // Check the provided handlerClass name
        try {
            if (!IMessageProcessingEventHandlerFactory.class.isAssignableFrom(Class.forName(handlerFactoryClass)))
                throw new PersistenceException("%s is not a IMessageProcessingEventHandlerFactory!",
                                                handlerFactoryClass);
        } catch (final ClassNotFoundException ex) {
            throw new PersistenceException("Class %s could not be loaded!", handlerFactoryClass);
        }
        // Convert the class names of the handled event to Class object
        if (!Utils.isNullOrEmpty(handledEventNames)) {
            handledEvents = new ArrayList<>();
            for (final String e : handledEventNames) {
                try {
                    final Class c = Class.forName(e);
                    if (!IMessageProcessingEvent.class.isAssignableFrom(c))
                        throw new ClassCastException();
                    handledEvents.add(c);
                } catch (ClassNotFoundException | ClassCastException classError) {
                    // The given String could not be converted into a class that is a message processing event
                    throw new PersistenceException("Class %s could not be loaded or is not a IMessageProcessingEvent!",
                                                    e);
                }
            }
        }
        // And convert the names in the ForMessageUnit element to the message unit interfaces
        if (!Utils.isNullOrEmpty(messageUnitNames)) {
            forMessageUnits = new ArrayList<>();
            for(final String m : messageUnitNames) {
                final Class<? extends IMessageUnit> c = XSD_MU_NAMES_2_CLASS.get(m);
                if (c == null)
                    throw new PersistenceException("%s is not a valid value for ForMessageUnit!", m);
                forMessageUnits.add(c);
            }
        }
    }

    /**
     * Default constructor creates a new and empty <code>EventHandlerConfig</code> instance.
     */
    public EventHandlerConfig() {}

    /**
     * Creates a new <code>EventHandlerConfig</code> instance using the parameters from the provided {@link
     * IMessageProcessingEventConfiguration}  object.
     *
     * @param source The source object to copy the parameters from
     */
    public EventHandlerConfig(final IMessageProcessingEventConfiguration source) {
    	this.handlerFactoryClass = source.getFactoryClass();
        setHandledEvents(source.getHandledEvents());
        setAppliesTo(source.appliesTo());
        setHandlerSettings((Map<String, Object>) source.getHandlerSettings());
    }

    @Override
    public String getId() {
        return null;
    }

    public void setHandledEvents(final List<Class<? extends IMessageProcessingEvent>> events) {
    	if (Utils.isNullOrEmpty(events)) {
    		this.handledEventNames = null;
    		this.handledEvents = null;
    	} else {
    		this.handledEvents = new ArrayList<>(events);
    		this.handledEventNames = new ArrayList<>(events.size());
    		events.forEach(e -> handledEventNames.add(e.getClass().getName()));
    	}
    }

    @Override
    public List<Class<? extends IMessageProcessingEvent>> getHandledEvents() {
        return handledEvents;
    }

    public void setAppliesTo(final List<Class<? extends IMessageUnit>> msgUnits) {
    	if (Utils.isNullOrEmpty(msgUnits)) {
    		this.forMessageUnits = null;
    		this.messageUnitNames = null;
    	} else {
    		this.forMessageUnits = new ArrayList<>(msgUnits);
    		this.messageUnitNames = new ArrayList<>(msgUnits.size());
    		msgUnits.forEach(mu -> messageUnitNames.add(mu.getClass().getName()));
    	}
    }
    
    @Override
    public List<Class<? extends IMessageUnit>> appliesTo() {
        return forMessageUnits;
    }

    @Override
    public String getFactoryClass() {
        return handlerFactoryClass;
    }

    public void setFactoryClass(final String className) {
    	this.handlerFactoryClass = className;
    }
    
    @Override
    public Map<String, ?> getHandlerSettings() {
        HashMap<String, String> settings = new HashMap<>(parameters.size());
        parameters.forEach(p -> settings.put(p.getName(), p.getValue()));
        return settings;
    }
    
    public void setHandlerSettings(final Map<String, Object> sourceSettings) {
        if (!Utils.isNullOrEmpty(sourceSettings)) {
            this.parameters = new ArrayList<>(sourceSettings.size());
            sourceSettings.forEach((n, v) -> this.parameters.add(new Parameter(n, v.toString())));
        }    
    }

    public void addHandlerSetting(final String name, final Object value) {
        if (this.parameters == null)
            this.parameters = new ArrayList<>();
        this.parameters.add(new Parameter(name, value.toString()));
    }
}

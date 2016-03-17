/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventHandlerFactory;
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
 * Represents the <code>EventHandler</code> element containing the configuration of a <i>message processing event 
 * handler</i>. These <i>events</i> are used to provide additional information to the business application about the 
 * processing of a message unit in addition to the formally specified <i>Submit</i>, <i>Deliver</i> and <i>Notify</i>
 * operations. An example of an event is that a message unit has been (re)sent.
 * <p>The element has four child elements:<ul>
 * <li>The first and REQUIRED child is <code>HandlerFactoryClass</code> which contains the class name of the factory 
 * class that creates the actual handlers.</li>
 * <li>The second child element configures which events are handled by this handler by listing the class / interface 
 * names of event types that are handled. This element is optional and when not provided all events will be passed to
 * the handler.</li>
 * <li>The third child configures for which message units event must be handled. The allowed values are: 
 * <i>"UserMessage"</i>, <i>"SignalMessage"</i>, <i>"Receipt"</i>, <i>"Error"</i> and <i>"PullRequest"</i>. Also this
 * element is optional and when not provided events will be passed to the handler for all message unit types.</li>
 * <li>The fourth element is the <code>Parameter</code> element that can occur zero or more times and contains the 
 * settings to initialize the handler.</li></ul>
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */

public class EventHandlerConfig implements IMessageProcessingEventConfiguration {
    
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
    
    @Element(name = "HandlerClass", required = true)
    private String  handlerFactoryClass = null;
    
    @ElementList(entry = "HandledEvent", required = false, inline = true)
    private List<String>    handledEventNames = null;
    
    @ElementList(entry = "ForMessageUnit", required = false, inline = true)
    private List<String>    messageUnitNames = null;
    
    @ElementList(entry = "Parameter", inline = true, required = false)
    private Collection<Property>    parameters;
    
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
    @Validate
    public void convertToClasses() throws PersistenceException {
        // Check the privided handlerClass name
        try {
            if (!IMessageProcessingEventHandlerFactory.class.isAssignableFrom(Class.forName(handlerFactoryClass)))
                throw new PersistenceException("%s is not a IMessageProcessingEventHandlerFactory!", 
                                                handlerFactoryClass);            
        } catch (ClassNotFoundException ex) {
            throw new PersistenceException("Class %s could not be loaded!", handlerFactoryClass);
        }
        // Convert the class names of the handled event to Class object
        if (!Utils.isNullOrEmpty(handledEventNames)) {
            handledEvents = new ArrayList<>();
            for (String e : handledEventNames) {
                try {
                    Class c = Class.forName(e);
                    if (!IMessageProcessingEvent.class.isAssignableFrom(c))
                        throw new ClassCastException();                    
                    handledEvents.add((Class<? extends IMessageProcessingEvent>) c);
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
            for(String m : messageUnitNames) {
                Class<? extends IMessageUnit> c = XSD_MU_NAMES_2_CLASS.get(m);
                if (c == null)
                    throw new PersistenceException("%s is not a valid value for ForMessageUnit!", m);                
                forMessageUnits.add(c);
            }
        }       
    }
    
    @Override
    public String getId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Class<? extends IMessageProcessingEvent>> getHandledEvents() {
        return handledEvents;
    }

    @Override
    public List<Class<? extends IMessageUnit>> appliesTo() {
        return forMessageUnits;
    }

    @Override
    public String getFactoryClass() {
        return handlerFactoryClass;
    }

    @Override
    public Map<String, ?> getHandlerSettings() {
        if (!Utils.isNullOrEmpty(parameters)) {
            HashMap<String, String>  settings = new HashMap<>();
            for (Property p : parameters)
                settings.put(p.getName(), p.getValue());

            return settings;
        } else
            return null;                    
    }       
}

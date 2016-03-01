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
package org.holodeckb2b.common.events;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventHandler;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventHandlerFactory;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Contains some utility methods for processing {@link IMessageProcessingEvent}s.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */
public final class EventUtils {
    
    /**
     * Determines whether a event handler should handle a given message processing event based on the given event 
     * handler configuration.
     * 
     * @param handlerCfg    The event handlers configuration as a {@link IMessageProcessingEventConfiguration}
     * @param event         The {@link IMessageProcessingEvent} to be handled 
     * @return  <code>true</code> when the event should be handled according to the given configuration,<br>
     *          <code>false</code> otherwise
     */
    public static boolean shouldHandleEvent(IMessageProcessingEventConfiguration handlerCfg, 
                                            IMessageProcessingEvent event) {
        boolean shouldHandle = true;
        IMessageUnit subject = event.getSubject();
        List<Class<? extends IMessageProcessingEvent>> handledEvents = handlerCfg.getHandledEvents();
        List<Class<? extends IMessageUnit>> forMsgUnits = handlerCfg.appliesTo();
        // Does handler apply to this event type? Need to check the type hierarchy to decide
        if (!Utils.isNullOrEmpty(handledEvents)) {
            boolean appliesTo = false;
            for (Class cls : handledEvents)
                appliesTo |= cls.isAssignableFrom(subject.getClass());                    
            shouldHandle &= appliesTo;            
        }
        // And to the message unit type? Here we can't just check if the refd message unit class is contained
        // in the config, but have to check on (super)interfaces
        if (!Utils.isNullOrEmpty(forMsgUnits)) {
            boolean appliesTo = false;
            for (Class cls : forMsgUnits)
                appliesTo |= cls.isAssignableFrom(subject.getClass());                    
            shouldHandle &= appliesTo;
        } // else no restriction specified on message type, so automatically applies to this handler
                        
        return shouldHandle;
    }
    
    /**
     * Gets the handler implementation class that is configured in the given handler configuration. 
     * <p>This class is retrieved by checking the <i>actual type</i> used in the implementation of the factory class for
     * the generic {@link IMessageProcessingEventHandlerFactory} interface.
     * 
     * @param handlerCfg    The event handler configuration to check
     * @return  A {@link Class} object that represent the {@link IMessageProcessingEventHandler} implementation that 
     *          will handle events configured by the given configuration,<br>
     *          or <code>null</code> if the handler class could not be determined.
     */
    public static Class<? extends IMessageProcessingEventHandler> getConfiguredHandler(
                                                                    IMessageProcessingEventConfiguration handlerCfg) {
        String handlerClassname = handlerCfg.getFactoryClass();
        try {
            Class factoryClass = Class.forName(handlerClassname);
            // Check that it indeed is handler factory
            if (IMessageProcessingEventHandlerFactory.class.isAssignableFrom(factoryClass)) {
                // Get all the generic interface implemented by the factory class
                Type[]  interfaces = factoryClass.getGenericInterfaces();            
                for (Type intf : interfaces) {
                    if (intf.getTypeName().startsWith(IMessageProcessingEventHandlerFactory.class.getTypeName())) {
                        // This the IMessageProcessingEventHandlerFactory interface, check the actual handler type 
                        return (Class<? extends IMessageProcessingEventHandler>) 
                                                                ((ParameterizedType) intf).getActualTypeArguments()[0];
                    }
                }
            }                        
        } catch (ClassNotFoundException noSuchClass) {            
        }
        
        return null;
    }
}

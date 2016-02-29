/*
 * Copyright (C) 2016 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published 
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.events;

import java.util.List;
import java.util.Map;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * Defines the interface for the configuration of an event handler instance. Event handlers are configured in the P-Mode
 * as events need only be processed for specific message exchanges. The configuration is used by the {@link 
 * IMessageProcessingEventProcessor} to decide if a handler needs to be created and called to handle an event.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IPMode
 * @see IMessageProcessingEventProcessor
 * @since 2.1.0
 */
public interface IMessageProcessingEventConfiguration {
    
    /**
     * Gets the unique identifier of this configuration. 
     * 
     * @return A {@link String} containing the unique identifier of this configuration 
     */
    public String getId();
    
    /**
     * Gets the list of events that this configuration applies to, i.e. which event types can and should be handled by 
     * the configured {@link IMessageProcessingEventHandler}.  
     * 
     * @return The {@link List} of classes / interfaces that implement the {@link IMessageProcessingEvent} interface and 
     *         that will be handled by this configuration. If no classes are given (value is <code>null</code> or empty
     *         list) all events will be passed to the configured handler.
     */
    public List<Class<? extends IMessageProcessingEvent>> getHandledEvents();
    
    /**
     * Gets the list of message unit types for which the specified event should be handled. Using this list the 
     * processing of events can be limited to a specific message unit type, for example only for User Messages.
     * 
     * @return The {@link List} of {@link IMessageUnit} classes / interfaces for which the given events should be 
     *         handled. If no classes are given (value is <code>null</code> or empty list) events will be passed to the 
     *         configured handler for all message unit types.
     */
    public List<Class<? extends IMessageUnit>> appliesTo();
    
    /**
     * Gets the name of factory class that is responsible to create the actual event handler.
     * 
     * @return A {@link String} containing the class name of the {@link IMessageProcessingEventHandlerFactory} that 
     *         should be used to create the {@link IMessageProcessingEventHandler}
     */
    public String getFactoryClass();
    
    /**
     * Gets the settings that must be used to initialize the event handler [factory]. The settings for handling the 
     * events are passed to the factory so it can decide what is the best way to create the handler objects.
     * 
     * @return The settings as a {@link Map}<code>&lt;String, ?&gt;</code> where the keys are the parameter names.
     */
    public Map<String, ?> getHandlerSettings();
}

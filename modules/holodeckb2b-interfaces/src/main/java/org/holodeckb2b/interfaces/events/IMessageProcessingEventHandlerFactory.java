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

import java.util.Map;

/**
 * Defines the interface for the factory classes that are responsible for creating and configuring the {@link
 * IMessageProcessingEventHandler}s.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @param <T>   The type of event handler the implementing factory class can create
 * @since 2.1.0
 */
public interface IMessageProcessingEventHandlerFactory<T extends IMessageProcessingEventHandler> {

    /**
     * Initializes the factory with the settings provided by the {@link IMessageProcessingEventConfiguration}. The
     * provided settings must be used to create the event handlers.
     *
     * @param settings  A {@link Map}<code>&lt;String, ?&gt;</code> with the settings to create the
     *                  {@link IMessageProcessingEventHandler}s
     * @throws MessageProccesingEventHandlingException When the factory is unable to successfully initialize itself and therefor can
     *                                  not create event handlers.
     */
    public void init(Map<String, ?> settings) throws MessageProccesingEventHandlingException;

    /**
     * Gets a {@link IMessageProcessingEventHandler} object for handling an event. It is up to the factory
     * implementation to decide whether a new object must be created or that an existing handler object can be reused.
     *
     * @return A instance of class <code>T</code> ready for handling an event.
     * @throws MessageProccesingEventHandlingException When the factory is unable to to create event handlers.
     */
    public T createHandler() throws MessageProccesingEventHandlingException;
}

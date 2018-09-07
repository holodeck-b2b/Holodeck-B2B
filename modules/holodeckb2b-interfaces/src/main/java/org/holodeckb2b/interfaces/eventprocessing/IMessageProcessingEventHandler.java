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
package org.holodeckb2b.interfaces.eventprocessing;

/**
 * Defines the interface of the component responsible for handling a {@link IMessageProcessingEvent}, i.e. an event that
 * occurred during the processing of a message unit.
 * <p>The interface just defines a method for handling an event, the handlers will be created by their accompanying
 * factory class.  Which event a handler will handle is determined in the handler configuration.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IMessageProcessingEventHandlerFactory
 * @see IMessageProcessingEventConfiguration
 * @since 2.1.0
 */
public interface IMessageProcessingEventHandler {

    /**
     * Handles a {@link IMessageProcessingEvent}.
     *
     * @param event     The event to be handled
     * @throws MessageProccesingEventHandlingException  When the event cannot be handled. This can be caused by either 
     * 													errors in processing or incorrect configuration causing a 
     * 													unknown type to be given to the handler.
     */
    public void handleEvent(IMessageProcessingEvent event) throws MessageProccesingEventHandlingException;
}

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

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * Defines the interface of the Holodeck B2B Core component that is responsible for the processing of {@link
 * IMessageProcessingEvent}s. When a component wants to inform others about an event that occurred in the processing of
 * a message unit it MUST <i>"raise"</i> the event with the event processor by calling the {@link
 * HolodeckB2BCoreInterface#getEventProcessor()#raiseEvent(IMessageProcessingEvent)} method.
 * <p>The event processor will then ensure that the event is handled as configured in either the P-Mode governing the
 * processing of the message unit to which the event applies or the global configuration (as registered with the Core).
 * The event processor always checks the most specific configuration first, i.e. first the configuration of the Leg
 * level is checked, then the generic P-Mode configuration and finally the global configuration.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.1.0
 * @since 4.1.0 Requirement to take the global event configuration into account when processing the event.
 * @since 5.3.0 Option to have multiple handlers process the same event
 * @see HolodeckB2BCoreInterface#getEventProcessor()
 * @see ILeg#getMessageProcessingEventConfiguration()
 * @see IPMode#getMessageProcessingEventConfiguration()
 * @see HolodeckB2BCoreInterface#getMessageProcessingEventConfiguration()
 */
public interface IMessageProcessingEventProcessor {

    /**
     * Gets the name of this event processor to identify it in logging. This name is only used for logging purposes
     * and it is recommended to include a version number of the implementation. If no name is specified by the
     * implementation the class name will be used.
     *
     * @return  The name of the event processor to use in logging
     * @since 5.0.0
     */
    default String getName() { return this.getClass().getName(); }

	/**
     * Initializes the event processor. This method is called once at startup of the Holodeck B2B instance.
     * <p><b>NOTE:</b> When the event processor cannot be successfully initialised the Holodeck B2B Core uses the
     * default event processor as fall back. If running the configured event processor is required for correct
     * functioning of the gateway this fall back can be disabled in the configuration, in which case the startup will
     * be aborted if loading the event processor fails.
     *
     * @param config 	the Holodeck B2B configuration
	 * @throws MessageProccesingEventHandlingException when the event processor cannot be initialised correctly.
	 * @since 6.0.0
	 */
	void init(final IConfiguration config) throws MessageProccesingEventHandlingException;

	/**
	 * Shuts down the event processor.
	 * <p>This method is called by the Holodeck B2B Core when the instance is shut down. Implementations should use it
	 * to release resources held needed for the event processing.
	 *
	 * @since 6.0.0
	 */
	void shutdown();

    /**
     * Raises an event for processing by the configured event handler.
     * <p>Because the event is only to inform about the message processing but not part of it the implementation MUST
     * ensure that message processing is not affected, i.e. not change any information of the referenced message unit
     * and not throw any exception.
     * <p>NOTE: Event handlers are executed in the order that they are configured and execution continues until all
     * handlers have been executed or when a handler's configuration specifies execution should stop when it has run.
     *
     * @param event         The event that occurred while processing the message unit and that should be processed
     */
    void raiseEvent(final IMessageProcessingEvent event);
}

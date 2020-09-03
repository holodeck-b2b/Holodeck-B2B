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

import java.nio.file.Path;
import java.util.List;

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventHandlerFactory;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.pmode.ILeg;

/**
 * Is the default implementation of {@link IMessageProcessingEventProcessor} for handling <i>message processing events
 * </i>that occur during the processing of a message unit and about which the business application may need to be
 * informed.
 * <p>Whether an event must be reported is configured in the P-Mode that governs the processing of the referenced
 * message unit or in the global event configuration registered in the Holodeck B2B Core. As the P-Mode configuration
 * takes precedence over the global one it is searched first for a event handler.  
 * <p>This implementation processes the events directly when raised to the processor, i.e. processing of the event is
 * done as part of the message processing. This processor only passes raised events to the handlers, there is no
 * archiving.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IMessageProcessingEvent
 * @see IMessageProcessingEventConfiguration
 * @since 2.1.0
 */
public class SyncEventProcessor implements IMessageProcessingEventProcessor {
    /**
     * Logging
     */
    private static final Log log = LogFactory.getLog(SyncEventProcessor.class);

    @Override
    public String getName() {
    	return "HB2B Sync Event Processor";
    }
    
    /**
     * Raises an event for processing.
     * <p>The P-Mode of the referenced message unit is checked for configured event handlers and each handler that can
     * handle the event will be called. If no P-Mode is known the events will be ignored.
     *
     * @param event  The event that occurred while processing the message unit and that should be processed
     * @param mc     The Axis2 {@link MessageContext} of the message unit the event applies to, if available. Currently
     *               not used.
     */
    @Override
    public void raiseEvent(final IMessageProcessingEvent event) {
        final String eventType = event.getClass().getSimpleName();
        if (event.getSubject() == null) {
            log.warn("A " + eventType + " was raised, but without reference to a message unit!");
            return;
        }
        final String msgUnitType = MessageUnitUtils.getMessageUnitName(event.getSubject());
        final String messageId = event.getSubject().getMessageId();
        log.trace("A " + eventType + " event [" + event.getId() + "] was raised for " + msgUnitType 
        			+ " with msgId=" + messageId);
        final IMessageUnit subject = event.getSubject();
        boolean isEventHandled = false;
        // Get the event handler configuration from the correct leg of the P-Mode.         	
        try {
        	final ILeg leg = PModeUtils.getLeg(subject);
        	final List<IMessageProcessingEventConfiguration> eventHandlers = leg == null ? null :
        															leg.getMessageProcessingEventConfiguration();
        	isEventHandled = handleEvent(eventHandlers, event);
        } catch (IllegalStateException pmodeNotAvailable) {
        	// The P-Mode is not available anymore (should not happen as the message unit is current in process)
            log.error("The P-Mode for the message unit [" + subject.getMessageId() + "] is not available!");
        }        
        if (!isEventHandled) {
        	log.trace(eventType + " not handled by P-Mode configured handlers => check global configuration");
        	if (!handleEvent(HolodeckB2BCoreInterface.getMessageProcessingEventConfiguration(), event))
        		log.debug("No handler defined for " + eventType + ", event [" + event.getId() + "] ignored!");
        }            
    }

	/**
	 * Helper method to check if and handle the given event is handled by any of the given event handlers.
	 *  
	 * @param eventHandlers		The set of configured event handlers 
	 * @param event				The event to be handled
	 */
	private boolean handleEvent(List<IMessageProcessingEventConfiguration> eventHandlers, IMessageProcessingEvent event)
	{
		if (Utils.isNullOrEmpty(eventHandlers))
			return false;
		
        final String eventType = event.getClass().getSimpleName();
        final String msgUnitType = MessageUnitUtils.getMessageUnitName(event.getSubject());
		// Check each configured if it needs to handle this event
		for (final IMessageProcessingEventConfiguration c : eventHandlers) {
			final boolean shouldHandle = EventUtils.shouldHandleEvent(c, event);
			final String handlerClassname = c.getFactoryClass();
			log.trace(handlerClassname + (shouldHandle ? " should" : " does not") + " handle " + eventType + " for "
					+ msgUnitType);
			if (shouldHandle) {
				// Create the factory class
				IMessageProcessingEventHandlerFactory factory = null;
				try {
					factory = (IMessageProcessingEventHandlerFactory) Class.forName(handlerClassname).newInstance();
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
					log.error("Could not create factory instance (specified class name=" + c.getFactoryClass() 
								+ ") due to a " + ex.getClass().getSimpleName());
					continue;
				}
				// Catch exceptions while the event is processed by the handler to prevent that error in one handler
				// will stop processing in others as well
				try {
					log.trace("Initialize the handler factory");
					factory.init(c.getHandlerSettings());
					log.trace("Pass event to handler for further processing");
					factory.createHandler().handleEvent(event);
					log.debug(eventType + "[id= " + event.getId() + "] for " + msgUnitType + "] handled by " 
								+ handlerClassname);
				} catch (final Throwable t) {
					log.warn("An exception occurred when " + eventType + " [id= " + event.getId() + " was processed by "
							+ handlerClassname + "\n\tException details: " + t.getMessage());
				}
				// Even if the handler failed to execute correctly we consider the event as handled because this was
				// the first configured handler able to handle the event
				return true;
			}
		}		
		return false;
	}

	@Override
	public void init(Path hb2bHome) throws MessageProccesingEventHandlingException {
	}
}

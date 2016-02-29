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
package org.holodeckb2b.events;

import java.util.List;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.events.EventUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.persistency.entities.MessageUnit;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventHandlerFactory;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * Is the default implementation of {@link IMessageProcessingEventProcessor} for handling <i>message processing events 
 * </i>that occur during the processing of a message unit and about which the business application may need to be 
 * informed.
 * <p>Whether an event must be reported is configured in the P-Mode that governs the processing of the referenced
 * message unit. The P-Mode is however not part of the information set of {@link IMessageUnit} and the processor 
 * therefore should find it. While the message units are processed by the Holodeck B2B Core the P-Mode is part of the
 * entity object implementation of {@link IMessageUnit}. This processor will therefore try to convert the referenced
 * message unit in the event to the {@link MessageUnit} entity class to get the P-Mode. As a result <b>this processor
 * can only handle events raised by Holodeck B2B Core components that include the entity object in the event!</b>
 * <p>This implementation processes the events directly when raised to the processor, i.e. processing of the event is
 * done as part of the message processing. This processor only passes raised events to the handlers, there is no 
 * archiving. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IMessageProcessingEvent
 * @see IMessageProcessingEventConfiguration
 * @since 2.1.0
 */
public class SyncEventProcessor implements IMessageProcessingEventProcessor {

    /**
     * Logging
     */
    private static final Log log = LogFactory.getLog(SyncEventProcessor.class);
    
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
    public void raiseEvent(IMessageProcessingEvent event, MessageContext mc) {
        String eventType = event.getClass().getSimpleName();
        if (event.getSubject() == null) {
            log.warn("A " + eventType + " was raised, but without reference to a message unit!");
            return;
        }
        String msgUnitType = event.getSubject().getClass().getSimpleName();
        String messageId = event.getSubject().getMessageId();
        try {
            log.info("A " + eventType + " event [" + event.getId() + "] was raised for " + msgUnitType + " with msgId=" 
                     + messageId);
            // Check that the referenced event is an entity object
            if (!(event.getSubject() instanceof MessageUnit)) {
                log.error("This processor can only handle event that inlude an entity object reference!");
                return;
            }
            MessageUnit subject = (MessageUnit) event.getSubject();
            String pmodeId = subject.getPMode();
            if (Utils.isNullOrEmpty(pmodeId)) {
                // No P-Mode available for this message unit => no handler config
                log.warn(msgUnitType + " with msgId=[" + messageId + "] has no P-Mode assigned. Can not handle event!");
                return;
            }
            // Get the event handler configuration from the correct leg of the P-Mode. Because the message unit may not
            // refer to a leg, we use the REQUEST leg as default
            IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(pmodeId);
            if (pmode == null) {
                // The P-Mode is not available anymore (should not happen as the message unit is current in process)
                log.error("The P-Mode for the message unit [" + pmodeId + "] is not available!");
                return;
            }
            ILeg leg = pmode.getLeg(subject.getLeg()!= null ? subject.getLeg() : ILeg.Label.REQUEST);  
            List<IMessageProcessingEventConfiguration> eventHandlers = leg.getMessageProcessingEventConfiguration();
            if (Utils.isNullOrEmpty(eventHandlers)) {
                log.debug(leg.getLabel() != null ? leg.getLabel().toString() : "REQUEST" + " leg of P-Mode [" + pmodeId 
                         + "] for event [" + event.getId() + "] has no event handlers configured => event is ignored");
                return;
            } 
            log.debug(leg.getLabel() != null ? leg.getLabel().toString() : "REQUEST" + " leg of P-Mode [" + pmodeId 
                         + "] for event [" + event.getId() + "] has " + eventHandlers.size() 
                         + " event handlers configured.");
            // Check each configured if it needs to handle this event
            for (IMessageProcessingEventConfiguration c : eventHandlers) {
                boolean shouldHandle = EventUtils.shouldHandleEvent(c, event);
                String handlerClassname = EventUtils.getConfiguredHandler(c).getSimpleName();
                log.debug(handlerClassname + (shouldHandle ? " should" : "does not") + " handle " + eventType + " for " 
                          + msgUnitType + " with msgId=[" + messageId + "]");
                if (shouldHandle) {
                    // Create the factory class
                    IMessageProcessingEventHandlerFactory factory = null;
                    try {
                        factory = (IMessageProcessingEventHandlerFactory) 
                                                                    Class.forName(c.getFactoryClass()).newInstance();
                    }   catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                        log.error("Could not create factory instance (specified class name=" + c.getFactoryClass() 
                                  + ") due to a " + ex.getClass().getSimpleName());   
                        return;
                    }
                    log.debug("Initialize the handler factory");
                    factory.init(c.getHandlerSettings());
                    // Catch exceptions while the event is processed by the handler to prevent that error in one handler
                    // will stop processing in others as well
                    try {
                        log.debug("Pass event to handler for further processing");                    
                        factory.createHandler().handleEvent(event);
                        log.info(eventType + "[id= " + event.getId() + "] for " + msgUnitType + " with msgId=[" 
                                + messageId + "] handled by " + handlerClassname);
                    } catch (Exception ex) {
                        log.warn("An exception occurred when " + eventType + "[id= " + event.getId() 
                                + " was processed by " + handlerClassname 
                                + "\n\tException details: " + ex.getMessage());                        
                    }
                }
            }
        } catch (Throwable t) {
            // Ensure that any problem with handling the event does not affect the normal message processing
            log.error("An " + t.getClass().getSimpleName() + " error occurred while processing a "
                        + eventType + " event was raised for " + msgUnitType + " with msgId=" + messageId);
        }
    }    
}

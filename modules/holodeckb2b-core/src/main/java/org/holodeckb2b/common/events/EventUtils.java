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

import java.util.List;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Contains some utility methods for processing {@link IMessageProcessingEvent}s.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
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
    public static boolean shouldHandleEvent(final IMessageProcessingEventConfiguration handlerCfg,
                                            final IMessageProcessingEvent event) {
        boolean shouldHandle = true;
        final IMessageUnit subject = event.getSubject();
        final List<Class<? extends IMessageProcessingEvent>> handledEvents = handlerCfg.getHandledEvents();
        final List<Class<? extends IMessageUnit>> forMsgUnits = handlerCfg.appliesTo();
        // Does handler apply to this event type? Need to check the type hierarchy to decide
        if (!Utils.isNullOrEmpty(handledEvents)) {
            boolean appliesTo = false;
            for (final Class<?> cls : handledEvents)
                appliesTo |= cls.isAssignableFrom(event.getClass());
            shouldHandle &= appliesTo;
        }
        // And to the message unit type? Here we can't just check if the refd message unit class is contained
        // in the config, but have to check on (super)interfaces
        if (!Utils.isNullOrEmpty(forMsgUnits)) {
            boolean appliesTo = false;
            for (final Class<?> cls : forMsgUnits)
                appliesTo |= cls.isAssignableFrom(subject.getClass());
            shouldHandle &= appliesTo;
        } // else no restriction specified on message type, so automatically applies to this handler

        return shouldHandle;
    }
}

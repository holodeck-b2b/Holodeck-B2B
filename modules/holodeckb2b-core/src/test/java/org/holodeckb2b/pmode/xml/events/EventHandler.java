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
package org.holodeckb2b.pmode.xml.events;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventHandler;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class EventHandler implements IMessageProcessingEventHandler {

    private Log log = LogFactory.getLog(EventHandler.class);
    
    @Override
    public void handleEvent(IMessageProcessingEvent event) throws IllegalArgumentException {
        log.info("Handling a " + event.getClass().getSimpleName() + " with id=" + event.getId());
    }
    
}

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
package org.holodeckb2b.common.testhelpers;

import java.nio.file.Path;
import java.util.ArrayList;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;

/**
 * Is a {@link IMessageProcessingEventProcessor} implementation for testing that just collects all raised events.
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class TestEventProcessor implements IMessageProcessingEventProcessor {

    public ArrayList<IMessageProcessingEvent> events = new ArrayList<>();

    @Override
    public void raiseEvent(final IMessageProcessingEvent event) {
    	synchronized (events) {
    		events.add(event);
		}
    }
    
    public void reset() {
    	synchronized (events) {
    		events.clear();	
		}   	
    }

	@Override
	public void init(Path hb2bHome) throws MessageProccesingEventHandlingException {		
	}
}

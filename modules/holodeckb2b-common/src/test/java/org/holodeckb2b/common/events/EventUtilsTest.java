/*
 * Copyright (C) 2015 The Holodeck B2B Team
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

import org.holodeckb2b.common.testhelpers.pmode.EventHandlerConfig;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventHandler;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventHandlerFactory;
import org.holodeckb2b.interfaces.events.MessageProccesingEventHandlingException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created at 14:58 14.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class EventUtilsTest {

    @Test
    public void testShouldHandleEvent() throws Exception {
        EventForTest event = new EventForTest(null);
        AnotherEventForTest unhandledEvent = new AnotherEventForTest(null);

        EventHandlerConfig config = new EventHandlerConfig();
        List<Class<? extends  IMessageProcessingEvent>> list = new ArrayList<>();
        list.add(event.getClass());
        config.setHandledEvents(list);

        assertTrue(EventUtils.shouldHandleEvent(config, event));
        assertFalse(EventUtils.shouldHandleEvent(config, unhandledEvent));
    }

    @Test
    public void testGetConfiguredHandler() throws Exception {
        EventForTest event = new EventForTest(null);

        EventHandlerConfig config = new EventHandlerConfig();
        List<Class<? extends  IMessageProcessingEvent>> list = new ArrayList<>();
        list.add(event.getClass());
        config.setHandledEvents(list);
        config.setFactoryClass(EventHandlerFactory.class.getName());

        assertEquals(
                (new EventHandlerFactory()).createHandler().getClass(),
                EventUtils.getConfiguredHandler(config));
    }

    class EventForTest extends AbstractMessageProcessingEvent {

        public EventForTest(IMessageUnit subject) {
            super(subject);
        }
    }

    class EventForTestHandler implements IMessageProcessingEventHandler {

        @Override
        public void handleEvent(IMessageProcessingEvent event) throws IllegalArgumentException {

        }
    }

    class AnotherEventForTest extends AbstractMessageProcessingEvent {

        public AnotherEventForTest(IMessageUnit subject) {
            super(subject);
        }
    }

    class EventHandlerFactory implements IMessageProcessingEventHandlerFactory<EventForTestHandler> {

        @Override
        public void init(Map settings) throws MessageProccesingEventHandlingException {

        }

        @Override
        public EventForTestHandler createHandler() throws MessageProccesingEventHandlingException {
            return new EventForTestHandler();
        }
    }
}
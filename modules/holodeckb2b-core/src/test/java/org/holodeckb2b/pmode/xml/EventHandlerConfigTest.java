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
package org.holodeckb2b.pmode.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Test the <code>EventHanlder</code> from P-Mode
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class EventHandlerConfigTest {

    public EventHandlerConfig createFromFile(final String fName)  {

        try {
            // retrieve the resource from the pmodetest directory.
            final File f = new File(this.getClass().getClassLoader().getResource("pmodetest/events/" + fName).getPath());

            final Serializer  serializer = new Persister();
            return serializer.read(EventHandlerConfig.class, f);
        }
        catch (final Exception ex) {
            System.out.println("Exception '" + ex.getLocalizedMessage() + "'");
            return null;
        }
    }

    @Test
    public void testInvalidHandlerFactoryClass() {
        EventHandlerConfig conf = createFromFile("noHandlerClass.xml");
        assertNull(conf);
        conf = createFromFile("invalidHandler.xml");
        assertNull(conf);
    }

    @Test
    public void testMinimalConfig() {
        final EventHandlerConfig conf = createFromFile("minimal.xml");

        assertNotNull(conf);
        assertNull(conf.getHandledEvents());
        assertNull(conf.appliesTo());
        assertNull(conf.getHandlerSettings());
    }

    @Test
    public void testInvalidHandledEvent() {
        EventHandlerConfig conf = createFromFile("unknownEvent.xml");
        assertNull(conf);
        conf = createFromFile("invalidEvent.xml");
        assertNull(conf);
    }

    @Test
    public void testInvalidMsgUnitType() {
        final EventHandlerConfig conf = createFromFile("invalidMsgType.xml");
        assertNull(conf);
    }

    @Test
    public void testAllMsgUnitType() {
        final EventHandlerConfig conf = createFromFile("allMsgTypes.xml");
        assertNotNull(conf);
        assertEquals(conf.appliesTo().size(), 5);
    }

    @Test
    public void testFullConfig() {
        final EventHandlerConfig conf = createFromFile("fullConfig.xml");
        assertNotNull(conf);
        assertEquals(2, conf.getHandledEvents().size());
        assertEquals("TestEvent1", conf.getHandledEvents().get(0).getSimpleName());
        assertEquals("TestEvent2", conf.getHandledEvents().get(1).getSimpleName());
        assertEquals(2, conf.appliesTo().size());
        assertEquals("IUserMessage", conf.appliesTo().get(0).getSimpleName());
        assertEquals("ISignalMessage", conf.appliesTo().get(1).getSimpleName());
        assertEquals(2, conf.getHandlerSettings().size());
        assertEquals("value1", conf.getHandlerSettings().get("p1"));
        assertEquals("value2", conf.getHandlerSettings().get("p2"));
    }

}

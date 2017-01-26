package org.holodeckb2b.common.events;

import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.common.testhelpers.pmode.EventHandlerConfig;
import org.holodeckb2b.common.testhelpers.pmode.Leg;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created at 14:58 14.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class EventUtilsTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private static String hostname;

    @BeforeClass
    public static void setUpClass() {
        baseDir = EventUtilsTest.class
                .getClassLoader().getResource("utils").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
        hostname =
                HolodeckB2BCoreInterface.getConfiguration().getHostName();
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testShouldHandleEvent() throws Exception {
        SyncEventForTest event =
                new SyncEventForTest("test_event", "test event", null);
        Leg leg = new Leg();
        EventHandlerConfig config = new EventHandlerConfig();
        leg.addMessageProcessingEventConfiguration(config);
        List<IMessageProcessingEventConfiguration> ec =
                leg.getMessageProcessingEventConfiguration();
        assertTrue(ec.size() > 0);
        assertTrue(EventUtils.shouldHandleEvent(ec.get(0), event));
    }

    @Test
    public void testGetConfiguredHandler() throws Exception {
        fail("Not implemented yet!");
    }

    class SyncEventForTest implements IMessageProcessingEvent {

        private String id;
        private String message;
        private IMessageUnit subject;

        public SyncEventForTest(String id, String message, IMessageUnit subject) {
            this.id = id;
            this.message = message;
            this.subject = subject;
        }

        /**
         * Gets the <b>unique</b> identifier of this event.
         * It is RECOMMENDED that this identifier is a valid XML ID so it
         * can be easily included in an XML representation of the event.
         *
         * @return A {@link String} containing the unique identifier of this event
         */
        @Override
        public String getId() {
            return id;
        }

        /**
         * Gets the timestamp when the event occurred.
         *
         * @return A {@link Date} representing the date and time the event occurred
         */
        @Override
        public Date getTimestamp() {
            return new Date();
        }

        /**
         * Gets an <b>optional</b> short description of what happened.
         * It is RECOMMENDED to limit the length of the
         * description to 100 characters.
         *
         * @return A {@link String} with a short description of the event
         * if available, <code>null</code> otherwise
         */
        @Override
        public String getMessage() {
            return message;
        }

        /**
         * Gets the message unit that the event applies to.
         * <p>NOTE: An event can only relate to one message unit.
         * If an event occurs that applies to multiple message units
         * the <i>event source component</i> must create multiple
         * <code>IMessageProcessingEvent</code> objects for each
         * message unit.
         *
         * @return The {@link IMessageUnit} this event applies to.
         */
        @Override
        public IMessageUnit getSubject() {
            return subject;
        }
    }
}
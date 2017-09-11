package org.holodeckb2b.common.testhelpers.events;

import org.holodeckb2b.interfaces.events.processing.IMessageProcessingEventHandlerFactory;
import org.holodeckb2b.interfaces.events.processing.MessageProccesingEventHandlingException;

import java.util.Map;

/**
 * Created at 16:36 15.07.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class TestEventHandlerFactory
        implements IMessageProcessingEventHandlerFactory<TestEventHandler> {
    @Override
    public void init(Map settings)
            throws MessageProccesingEventHandlingException {

    }

    @Override
    public TestEventHandler createHandler()
            throws MessageProccesingEventHandlingException {
        return new TestEventHandler();
    }
}

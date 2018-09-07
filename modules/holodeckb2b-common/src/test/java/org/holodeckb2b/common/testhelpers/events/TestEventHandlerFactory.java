package org.holodeckb2b.common.testhelpers.events;

import java.util.Map;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventHandlerFactory;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;

/**
 * Created at 16:36 15.07.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class TestEventHandlerFactory
        implements IMessageProcessingEventHandlerFactory {
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

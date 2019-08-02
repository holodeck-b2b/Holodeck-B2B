package org.holodeckb2b.common.events;

import java.util.ArrayList;
import java.util.Map;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventHandler;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventHandlerFactory;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;

/**
 * Is a test implementation of message processing event handler that will just collect all events passed to it or throws
 * the given exception as an error.  
 * 
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class NullEventHandler implements IMessageProcessingEventHandlerFactory {

	private ArrayList<IMessageProcessingEvent>	events = null;
	private Throwable	exceptionToThrow = null;
	
    @Override
    public void init(Map settings)
            throws MessageProccesingEventHandlingException {
		Object p = settings.values().iterator().next();
		
		if (p instanceof Throwable)
			exceptionToThrow = (Throwable) p;
		else if (p instanceof ArrayList)
			events = (ArrayList<IMessageProcessingEvent>) p;
    }

    @Override
    public IMessageProcessingEventHandler createHandler()
            throws MessageProccesingEventHandlingException {
        return new IMessageProcessingEventHandler() {
			
			@Override
			public void handleEvent(IMessageProcessingEvent event) throws MessageProccesingEventHandlingException {
				// TODO Auto-generated method stub
				if (exceptionToThrow != null)
					if (exceptionToThrow instanceof MessageProccesingEventHandlingException) 
						throw (MessageProccesingEventHandlingException) exceptionToThrow;
					else
						throw new IllegalStateException(exceptionToThrow);
				else
					events.add(event);
			}
		};
    }
}

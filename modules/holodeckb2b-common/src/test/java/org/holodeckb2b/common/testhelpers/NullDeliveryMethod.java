/**
 * 
 */
package org.holodeckb2b.common.testhelpers;

import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.IMessageDelivererFactory;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is a {@link IMessageDelivererFactory} implementation for testing that just collects all messages delivered and 
 * provides methods to check the delivered messages.
 * 
 * @author Sander Fieten (sander at chasquis-consulting.com)
 */
public class NullDeliveryMethod implements IMessageDelivererFactory {

	private Map<String, ?>	settings;
	
	private Map<String, IMessageUnit>	deliveredMessages = new HashMap<>();

	private IMessageDeliverer deliverer;
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.delivery.IMessageDelivererFactory#init(java.util.Map)
	 */
	@Override
	public void init(Map<String, ?> settings) throws MessageDeliveryException {
		if (settings != null)
			this.settings = new HashMap<>(settings);			
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.delivery.IMessageDelivererFactory#createMessageDeliverer()
	 */
	@Override
	public IMessageDeliverer createMessageDeliverer() throws MessageDeliveryException {
		if (deliverer == null)
			deliverer = new NullDeliverer();
		return deliverer;
	}
	
	public boolean wasDelivered(final String msgId) {
		return deliveredMessages.containsKey(msgId);
	}
	
	public IMessageUnit getMessageUnit(final String msgId) {
		return deliveredMessages.get(msgId);
	}
	
	public class NullDeliverer implements IMessageDeliverer {
		/* (non-Javadoc)
		 * @see org.holodeckb2b.interfaces.delivery.IMessageDeliverer#deliver(org.holodeckb2b.interfaces.messagemodel.IMessageUnit)
		 */
		@Override
		public void deliver(IMessageUnit rcvdMsgUnit) throws MessageDeliveryException {
			deliveredMessages.put(rcvdMsgUnit.getMessageId(), rcvdMsgUnit);			
		}		
	}	
		
}

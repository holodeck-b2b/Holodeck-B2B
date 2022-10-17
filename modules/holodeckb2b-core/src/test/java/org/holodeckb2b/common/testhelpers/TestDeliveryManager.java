package org.holodeckb2b.common.testhelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.holodeckb2b.interfaces.delivery.IDeliveryManager;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * A mock for {@link IDeliveryManager} that will simply add the message unit to be delivered to a list. Does not use
 * an actual delivery method and therefore has also no support for registration of delivery specifications.
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class TestDeliveryManager implements IDeliveryManager {

	public List<IMessageUnitEntity> 	deliveredMessages = new ArrayList<>();
	
	public MessageDeliveryException		rejection = null;
	
	@Override
	public void deliver(IMessageUnitEntity m) throws IllegalArgumentException, MessageDeliveryException {
		if (!m.getProcessingStates().stream().anyMatch(s -> s.getState() == ProcessingState.READY_FOR_DELIVERY)) 
			throw new IllegalArgumentException("Message unit not ready for delivery");
		
		if (rejection != null)
			throw rejection;
		
		deliveredMessages.add(m);
	}

	public boolean isDelivered(String msgId) {
		return deliveredMessages.parallelStream().anyMatch(m -> msgId.equals(m.getMessageId()));
	}
	
	@Override
	public void registerDeliverySpec(IDeliverySpecification spec) {
	}

	@Override
	public boolean isSpecIdUsed(String id) {
		return false;
	}

	@Override
	public IDeliverySpecification getDeliverySpecification(String id) {
		return null;
	}

	@Override
	public Collection<IDeliverySpecification> getAllRegisteredSpecs() {
		return null;
	}

	@Override
	public void removeDeliverySpec(String id) {
	}

}

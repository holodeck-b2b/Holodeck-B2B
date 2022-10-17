package org.holodeckb2b.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.holodeckb2b.interfaces.delivery.IDeliveryCallback;
import org.holodeckb2b.interfaces.delivery.IDeliveryMethod;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

class TestDeliveryMethod implements IDeliveryMethod {

	public static List<TestDeliveryMethod> instances = new ArrayList<>();
	
	public Map<String, ?> settings;
	public MessageDeliveryException rejection;
	public List<IMessageUnit> deliveredMsgUnits = new ArrayList<>();
	public int sync = 0;
	public int async = 0;
	
	@Override
	public void init(Map<String, ?> settings) throws MessageDeliveryException {
		instances.add(this);
		this.settings = settings;
		String reject = settings != null ? (String) settings.get("reject") : null;
		if ("warning".equals(reject))
			rejection = new MessageDeliveryException();
		else if ("failure".equals(reject))
			rejection = new MessageDeliveryException("For ever", true);
	}	
	
	@Override
	public void shutdown() {
		instances.remove(this);
	}
	
	@Override
	public boolean supportsAsyncDelivery() {
		return true;
	}

	@Override
	public void deliver(IMessageUnit rcvdMsgUnit) throws MessageDeliveryException {
		sync++;
		if (rejection != null)
			throw rejection;		
		deliveredMsgUnits.add(rcvdMsgUnit);
	}

	@Override
	public void deliver(IMessageUnit rcvdMsgUnit, IDeliveryCallback callback) throws MessageDeliveryException {
		async++;
		if (rejection != null)
			callback.failed(rejection);
		else {
			deliveredMsgUnits.add(rcvdMsgUnit);
			callback.success();
		}
	}
}

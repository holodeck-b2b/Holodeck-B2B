/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.holodeckb2b.interfaces.delivery.IDeliveryCallback;
import org.holodeckb2b.interfaces.delivery.IDeliveryMethod;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

public class TestDeliveryMethod implements IDeliveryMethod {

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
	public void deliver(IMessageUnit rcvdMsgUnit, IDeliveryCallback callback)  {
		async++;
		if (rejection != null)
			callback.failed(rejection);
		else {
			deliveredMsgUnits.add(rcvdMsgUnit);
			callback.success();
		}
	}
}

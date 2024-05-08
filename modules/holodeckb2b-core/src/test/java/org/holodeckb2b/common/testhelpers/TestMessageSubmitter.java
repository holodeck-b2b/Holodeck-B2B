/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.submit.MessageSubmitException;


/**
 * Is a {@link IMessageSubmitter} implementation for testing that just collects all messages submitted and provides
 * methods to check the submitted messages.
 * 
 * @author Sander Fieten (sander at chasquis-consulting.com)
 */
public class TestMessageSubmitter implements IMessageSubmitter {

	private Map<String, IMessageUnit>	submittedMessages = new HashMap<>();
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.submit.IMessageSubmitter#submitMessage(org.holodeckb2b.interfaces.messagemodel.IUserMessage, boolean)
	 */
	@Override
	public String submitMessage(IUserMessage um) throws MessageSubmitException {
		return submitMessageUnit(um);
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.submit.IMessageSubmitter#submitMessage(org.holodeckb2b.interfaces.messagemodel.IPullRequest)
	 */
	@Override
	public String submitMessage(IPullRequest pr) throws MessageSubmitException {
		return submitMessageUnit(pr);
	}

	public boolean wasSubmitted(final String msgId) {
		return submittedMessages.containsKey(msgId);
	}
	
	public IMessageUnit getMessageUnit(final String msgId) {
		return submittedMessages.get(msgId);
	}
	
	public Collection<IMessageUnit> getAllSubmitted() {
		return submittedMessages.values();
	}
	
	public void clear() {
		synchronized (submittedMessages) {			
			submittedMessages.clear();	
		}		
	}
	
	private  String submitMessageUnit(IMessageUnit mu) throws MessageSubmitException {
		String msgId = mu.getMessageId();
		if (Utils.isNullOrEmpty(msgId))
			msgId = MessageIdUtils.createMessageId();
		synchronized (submittedMessages) {			
			submittedMessages.put(msgId, mu);	
		}
		return msgId;
	}
}

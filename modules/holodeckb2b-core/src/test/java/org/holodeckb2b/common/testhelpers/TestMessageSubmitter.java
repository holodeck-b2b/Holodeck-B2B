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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.submit.MessageSubmitException;


/**
 * Is a {@link IMessageSubmitter} implementation for testing that just collects all messages and payloads submitted and
 * provides methods to check the submitted messages.
 *
 * @author Sander Fieten (sander at chasquis-consulting.com)
 */
public class TestMessageSubmitter implements IMessageSubmitter {

	private Map<String, IMessageUnit>	submittedMessages = new HashMap<>();
	private Map<String, IPayloadEntity>	submittedPayloads = new HashMap<>();

	private boolean rejectNext = false;

	public void rejectNext() {
		this.rejectNext = true;
	}

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

	@Override
	public IPayloadEntity submitPayload(IPayload payload, String pmodeId) throws MessageSubmitException {
		if (rejectNext) {
			rejectNext = false;
			throw new MessageSubmitException("Rejected on request");
		}

		PayloadEntry stored = new PayloadEntry(payload);
		submittedPayloads.put(stored.getPayloadId(), stored);
		return stored;
	}

	@Override
	public void cancelPayloadSubmission(IPayloadEntity submittedPayload) throws MessageSubmitException {
		if (submittedPayloads.remove(submittedPayload.getPayloadId()) == null)
			throw new MessageSubmitException("Payload bound to User Message");
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

	public IPayloadEntity getPayload(final String payloadId) {
		return submittedPayloads.get(payloadId);
	}

	public Collection<IPayloadEntity> getAllSubmittedPayloads() {
		return submittedPayloads.values();
	}

	public void clear() {
		synchronized (submittedMessages) {
			submittedMessages.clear();
			submittedPayloads.clear();
		}
	}

	private  String submitMessageUnit(IMessageUnit mu) throws MessageSubmitException {
		if (rejectNext) {
			rejectNext = false;
			throw new MessageSubmitException("Rejected on request");
		}

		String msgId = mu.getMessageId();
		if (Utils.isNullOrEmpty(msgId))
			msgId = MessageIdUtils.createMessageId();
		if (mu instanceof IUserMessage)
			mu = new UserMessageEntry((IUserMessage) mu);
		synchronized (submittedMessages) {
			submittedMessages.put(msgId, mu);
		}
		return msgId;
	}

	class UserMessageEntry extends UserMessage {
		private Collection<IPayload> payloads = new ArrayList<>();

		UserMessageEntry(IUserMessage src) throws MessageSubmitException {
			super(src);
			if (!Utils.isNullOrEmpty(src.getPayloads()))
				for(IPayload pl : src.getPayloads())
					payloads.add(new PayloadEntry(pl));
		}

		@Override
		public Collection<IPayload> getPayloads() {
			return payloads;
		}
	}

	class PayloadEntry extends Payload implements IPayloadEntity {
		private String payloadId;
		private byte[] content;

		PayloadEntry(IPayload src) throws MessageSubmitException {
			super(src);
			this.payloadId = UUID.randomUUID().toString();
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				InputStream is = src.getContent();
				if (is != null) {
					Utils.copyStream(is, baos);
					content = baos.toByteArray();
				}
			} catch (IOException e) {
				throw new MessageSubmitException("Could not read payload data", e);
			}
		}

		@Override
		public InputStream getContent() throws IOException {
			return content != null ? new ByteArrayInputStream(content) : null;
		}

		@Override
		public String getPayloadId() {
			return payloadId;
		}

		@Override
		public String getParentCoreId() {
			return null;
		}

		@Override
		public String getPModeId() {
			return null;
		}

		@Override
		public Direction getDirection() {
			return null;
		}

		@Override
		public void removeProperty(IProperty p) {
			throw new UnsupportedOperationException();
		}
	}
}

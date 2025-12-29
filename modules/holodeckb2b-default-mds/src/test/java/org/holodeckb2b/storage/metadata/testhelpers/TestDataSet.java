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
package org.holodeckb2b.storage.metadata.testhelpers;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.holodeckb2b.common.messagemodel.MessageProcessingState;
import org.holodeckb2b.common.messagemodel.PartyId;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IPayload.Containment;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IPullRequestEntity;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.storage.metadata.ErrorMessageEntity;
import org.holodeckb2b.storage.metadata.PayloadEntity;
import org.holodeckb2b.storage.metadata.PullRequestEntity;
import org.holodeckb2b.storage.metadata.ReceiptEntity;
import org.holodeckb2b.storage.metadata.UserMessageEntity;
import org.holodeckb2b.storage.metadata.jpa.ErrorMessage;
import org.holodeckb2b.storage.metadata.jpa.PayloadInfo;
import org.holodeckb2b.storage.metadata.jpa.Property;
import org.holodeckb2b.storage.metadata.jpa.PullRequest;
import org.holodeckb2b.storage.metadata.jpa.Receipt;
import org.holodeckb2b.storage.metadata.jpa.UserMessage;

public class TestDataSet {

	public static final String			T_PMODE_1 = UUID.randomUUID().toString();
	public static final String			T_PMODE_2 = UUID.randomUUID().toString();
	public static final String			T_PMODE_3 = UUID.randomUUID().toString();

	public static final String			T_DUP_MESSAGEID = UUID.randomUUID().toString();

	public static IUserMessageEntity	T_USERMESSAGE_1;
	public static IUserMessageEntity	T_USERMESSAGE_2;
	public static IUserMessageEntity	T_USERMESSAGE_3;
	public static IUserMessageEntity	T_USERMESSAGE_4;
	public static IUserMessageEntity	T_USERMESSAGE_5;
	public static IUserMessageEntity	T_USERMESSAGE_6;
	public static IUserMessageEntity	T_USERMESSAGE_7;

	public static IPullRequestEntity	T_PULLREQ_1;

	public static IReceiptEntity 		T_RECEIPT_1;
	public static IReceiptEntity 		T_RECEIPT_2;

	public static IErrorMessageEntity 	T_ERROR_1;

	public static IPayloadEntity		T_PAYLOAD_1;

	private static boolean created = false;

	public static void createTestSet() {
		if (created)
			return;

		org.holodeckb2b.common.messagemodel.UserMessage base = new org.holodeckb2b.common.messagemodel.UserMessage();
		base.setMessageId(UUID.randomUUID().toString());
		base.setTimestamp(new Date(System.currentTimeMillis() - 1000));
		base.setDirection(Direction.OUT);
		base.setPModeId(T_PMODE_1);
		TradingPartner tp = new TradingPartner();
		tp.setPartyIds(Collections.singleton(new PartyId("senderId", null)));
		tp.setRole("Sender");
		base.setSender(tp);
		tp = new TradingPartner();
		tp.setPartyIds(Collections.singleton(new PartyId("receiverId", null)));
		tp.setRole("Receiver");
		base.setReceiver(tp);
		base.addMessageProperty(new Property("mp-document-type", "test"));
		base.addMessageProperty(new Property("mp-dossierno", "A01"));

		UserMessage um = new UserMessage(base);
		um.setCoreId(UUID.randomUUID().toString());
		um.setProcessingState(createState(ProcessingState.SUBMITTED, 2));
		um.setProcessingState(createState(ProcessingState.READY_TO_PUSH, 2));
		um.setProcessingState(createState(ProcessingState.SENDING, 2));
		um.setProcessingState(createState(ProcessingState.TRANSPORT_FAILURE, 2));
		um.setProcessingState(createState(ProcessingState.SENDING, 1));
		um.setProcessingState(createState(ProcessingState.TRANSPORT_FAILURE, 1));
		um.setProcessingState(createState(ProcessingState.SENDING, 0));
		um.setProcessingState(createState(ProcessingState.TRANSPORT_FAILURE, 0));

		PayloadInfo pl = new PayloadInfo();
		pl.setPayloadId(UUID.randomUUID().toString());
		pl.setParent(um);
		um.addPayload(pl);

		EntityManagerUtil.save(um);
		T_USERMESSAGE_1 = new UserMessageEntity(um);

		um = new UserMessage();
		um.setCoreId(UUID.randomUUID().toString());
		um.setTimestamp(new Date(System.currentTimeMillis() - 900));
		um.setMessageId(UUID.randomUUID().toString());
		um.setDirection(Direction.OUT);
		um.setPModeId(T_PMODE_1);
		um.setProcessingState(createState(ProcessingState.SUBMITTED, 0));
		um.setProcessingState(createState(ProcessingState.READY_TO_PUSH, 0));
		EntityManagerUtil.save(um);
		T_USERMESSAGE_2 = new UserMessageEntity(um);

		um = new UserMessage();
		um.setCoreId(UUID.randomUUID().toString());
		um.setTimestamp(new Date(System.currentTimeMillis() - 800));
		um.setMessageId(UUID.randomUUID().toString());
		um.setDirection(Direction.OUT);
		um.setPModeId(T_PMODE_3);
		um.setProcessingState(createState(ProcessingState.SUBMITTED, 5));
		um.setProcessingState(createState(ProcessingState.READY_TO_PUSH, 5));
		EntityManagerUtil.save(um);
		T_USERMESSAGE_3 = new UserMessageEntity(um);

		um = new UserMessage();
		um.setCoreId(UUID.randomUUID().toString());
		um.setTimestamp(new Date(System.currentTimeMillis() - 700));
		um.setMessageId(T_DUP_MESSAGEID);
		um.setDirection(Direction.OUT);
		um.setPModeId(T_PMODE_3);
		um.setProcessingState(createState(ProcessingState.SUBMITTED, 8));
		um.setProcessingState(createState(ProcessingState.READY_TO_PUSH, 8));
		EntityManagerUtil.save(um);
		T_USERMESSAGE_4 = new UserMessageEntity(um);

		um = new UserMessage();
		um.setCoreId(UUID.randomUUID().toString());
		um.setTimestamp(new Date(System.currentTimeMillis() - 600));
		um.setMessageId(T_DUP_MESSAGEID);
		um.setDirection(Direction.IN);
		um.setPModeId(T_PMODE_2);
		um.setProcessingState(createState(ProcessingState.RECEIVED, 5));
		um.setProcessingState(createState(ProcessingState.READY_FOR_DELIVERY, 5));
		EntityManagerUtil.save(um);
		T_USERMESSAGE_5 = new UserMessageEntity(um);

		um = new UserMessage();
		um.setCoreId(UUID.randomUUID().toString());
		um.setTimestamp(new Date(System.currentTimeMillis() - 500));
		um.setMessageId(UUID.randomUUID().toString());
		um.setDirection(Direction.IN);
		um.setPModeId(T_PMODE_3);
		um.setProcessingState(createState(ProcessingState.RECEIVED, 5));
		um.setProcessingState(createState(ProcessingState.READY_FOR_DELIVERY, 5));
		um.setProcessingState(createState(ProcessingState.DELIVERED, 5));
		EntityManagerUtil.save(um);
		T_USERMESSAGE_6 = new UserMessageEntity(um);

		um = new UserMessage();
		um.setTimestamp(new Date(System.currentTimeMillis() - 400));
		um.setCoreId(UUID.randomUUID().toString());
		um.setMessageId(UUID.randomUUID().toString());
		um.setDirection(Direction.IN);
		um.setPModeId(T_PMODE_3);
		um.setProcessingState(createState(ProcessingState.RECEIVED, 5));
		um.setProcessingState(createState(ProcessingState.READY_FOR_DELIVERY, 5));
		um.setProcessingState(createState(ProcessingState.FAILURE, 5));
		EntityManagerUtil.save(um);
		T_USERMESSAGE_7 = new UserMessageEntity(um);

		PullRequest pr = new PullRequest();
		pr.setCoreId(UUID.randomUUID().toString());
		pr.setTimestamp(new Date(System.currentTimeMillis() - 300));
		pr.setMessageId(UUID.randomUUID().toString());
		pr.setDirection(Direction.IN);
		pr.setPModeId(T_PMODE_3);
		pr.setProcessingState(createState(ProcessingState.RECEIVED, 7));
		pr.setProcessingState(createState(ProcessingState.DONE, 7));
		EntityManagerUtil.save(pr);
		T_PULLREQ_1 = new PullRequestEntity(pr);

		Receipt	rcpt = new Receipt();
		rcpt.setCoreId(UUID.randomUUID().toString());
		rcpt.setTimestamp(new Date(System.currentTimeMillis() - 200));
		rcpt.setMessageId(UUID.randomUUID().toString());
		rcpt.setDirection(Direction.OUT);
		rcpt.setPModeId(T_PMODE_2);
		rcpt.setProcessingState(createState(ProcessingState.CREATED, 1));
		rcpt.setProcessingState(createState(ProcessingState.READY_TO_PUSH, 1));
		EntityManagerUtil.save(rcpt);
		T_RECEIPT_1 = new ReceiptEntity(rcpt);

		rcpt = new Receipt();
		rcpt.setCoreId(UUID.randomUUID().toString());
		rcpt.setTimestamp(new Date(System.currentTimeMillis() - 100));
		rcpt.setMessageId(UUID.randomUUID().toString());
		rcpt.setDirection(Direction.IN);
		rcpt.setPModeId(T_PMODE_2);
		rcpt.setProcessingState(createState(ProcessingState.CREATED, 1));
		rcpt.setProcessingState(createState(ProcessingState.READY_FOR_DELIVERY, 1));
		EntityManagerUtil.save(rcpt);
		T_RECEIPT_2 = new ReceiptEntity(rcpt);

		ErrorMessage err = new ErrorMessage();
		err.setCoreId(UUID.randomUUID().toString());
		err.setTimestamp(new Date());
		err.setMessageId(UUID.randomUUID().toString());
		err.setDirection(Direction.OUT);
		err.setPModeId(T_PMODE_3);
		err.setProcessingState(createState(ProcessingState.CREATED, 1));
		err.setProcessingState(createState(ProcessingState.READY_TO_PUSH, 1));
		EntityManagerUtil.save(err);
		T_ERROR_1 = new ErrorMessageEntity(err);

		pl = new PayloadInfo();
		pl.setPayloadId(UUID.randomUUID().toString());
		pl.setPModeId(T_PMODE_3);
		pl.setDirection(Direction.OUT);
		pl.setContainment(Containment.ATTACHMENT);
		EntityManagerUtil.save(pl);
		T_PAYLOAD_1 = new PayloadEntity(pl);

		created = true;
	}

	private static MessageProcessingState createState(ProcessingState state, int daysBack) {
		MessageProcessingState procstate = new MessageProcessingState(state);
		Calendar stateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		stateTime.add(Calendar.DAY_OF_YEAR, -daysBack);
		procstate.setStartTime(stateTime.getTime());
		return procstate;
	}
}

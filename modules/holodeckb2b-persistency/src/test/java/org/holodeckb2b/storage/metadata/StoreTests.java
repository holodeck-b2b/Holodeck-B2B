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
package org.holodeckb2b.storage.metadata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import org.holodeckb2b.common.errors.OtherContentError;
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.Description;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.PartyId;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Property;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.SchemaReference;
import org.holodeckb2b.common.messagemodel.SelectivePullRequest;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.CompareUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.receptionawareness.MissingReceipt;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IPayload.Containment;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;
import org.holodeckb2b.storage.metadata.jpa.PayloadInfo;
import org.holodeckb2b.storage.metadata.jpa.ReceiptTest;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.junit.jupiter.api.Test;

public class StoreTests extends BaseProviderTest {

	@Test
	void testGenericInfo() {
		PullRequest pr = new PullRequest();
		pr.setPModeId("pm-generic");
		pr.setMessageId(UUID.randomUUID().toString());
		pr.setRefToMessageId(UUID.randomUUID().toString());
		pr.setTimestamp(new Date());
		pr.setProcessingState(ProcessingState.PROCESSING);

		PullRequestEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(pr));

		assertNotNull(stored.getCoreId());
		assertEquals(pr.getPModeId(), stored.getPModeId());
		assertEquals(pr.getMessageId(), stored.getMessageId());
		assertEquals(pr.getRefToMessageId(), stored.getRefToMessageId());
		assertEquals(pr.getTimestamp(), stored.getTimestamp());

		assertEquals(1, stored.getProcessingStates().size());

		IMessageUnitProcessingState orgState = pr.getCurrentProcessingState();
		IMessageUnitProcessingState storedState = stored.getCurrentProcessingState();

		assertEquals(orgState.getState(), storedState.getState());
		assertEquals(orgState.getStartTime(), storedState.getStartTime());
		assertEquals(orgState.getDescription(), storedState.getDescription());
	}

	@Test
	void testSetInitialState() {
		PullRequest pr = new PullRequest();
		pr.setMessageId(UUID.randomUUID().toString());

		PullRequestEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(pr));

		assertNotNull(stored.getCurrentProcessingState());
	}

	@Test
	void testRejectDuplicateMsgId() {
		org.holodeckb2b.storage.metadata.jpa.UserMessage um = new org.holodeckb2b.storage.metadata.jpa.UserMessage();
		um.setDirection(Direction.OUT);
		um.setMessageId(UUID.randomUUID().toString());
		EntityManagerUtil.save(um);

		UserMessage dup = new UserMessage();
		dup.setDirection(Direction.OUT);
		dup.setMessageId(um.getMessageId());

		assertThrows(DuplicateMessageIdException.class, () -> provider.storeMessageUnit(dup));
	}

	@Test
	void testUserMessageNewPayload() {
		UserMessage um = new UserMessage();
		CollaborationInfo ci = new CollaborationInfo();
		ci.setAction("StoreInDB");
		ci.setConversationId(UUID.randomUUID().toString());
		ci.setAgreement(new AgreementReference("MDSTesting", null, null));
		ci.setService(new Service("MDSProvider"));
		um.setCollaborationInfo(ci);
		um.addMessageProperty(new Property("p1", "v1"));
		Payload pl = new Payload();
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		um.addPayload(pl);
		um.setMPC(EbMSConstants.DEFAULT_MPC);
		TradingPartner rcvr = new TradingPartner();
		rcvr.addPartyId(new PartyId("Receiver", "urn:org:holodeckb2b:pid"));
		um.setReceiver(rcvr);
		TradingPartner sender = new TradingPartner();
		sender.addPartyId(new PartyId("Sender", "urn:org:holodeckb2b:pid"));
		um.setSender(sender);

		UserMessageEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(um));

		assertNotNull(stored.getCoreId());
		assertEquals(um.getPModeId(), stored.getPModeId());
		assertEquals(um.getMPC(), stored.getMPC());
		assertEquals(um.getCollaborationInfo().getAction(), stored.getCollaborationInfo().getAction());
		assertEquals(um.getCollaborationInfo().getConversationId(), stored.getCollaborationInfo().getConversationId());
		assertTrue(CompareUtils.areEqual(um.getCollaborationInfo().getAgreement(),
										stored.getCollaborationInfo().getAgreement()));
		assertTrue(CompareUtils.areEqual(um.getCollaborationInfo().getService(),
										stored.getCollaborationInfo().getService()));
		assertTrue(CompareUtils.areEqual(um.getReceiver(), stored.getReceiver()));
		assertTrue(CompareUtils.areEqual(um.getSender(), stored.getSender()));

		Collection<PayloadEntity> storedPlInfo = stored.getPayloads();
		assertEquals(1, storedPlInfo.size());
		PayloadEntity storedPl = storedPlInfo.iterator().next();
		assertNotNull(storedPl.getPayloadId());
		assertEquals(stored.getCoreId(), storedPl.getParentCoreId());
		assertEquals(pl.getContainment(), storedPl.getContainment());
		assertEquals(pl.getPayloadURI(), storedPl.getPayloadURI());

		assertExistsInDb(stored);
	}

	@Test
	void testUserMessageExistingPayload() {
		UserMessage um = new UserMessage();
		PayloadInfo pl = new PayloadInfo();
		pl.setPayloadId(UUID.randomUUID().toString());
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		EntityManagerUtil.save(pl);
		assertNotNull(pl.getOID());
		um.addPayload(new PayloadEntity(pl));

		UserMessageEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(um));

		Collection<PayloadEntity> storedPlInfo = stored.getPayloads();

		assertEquals(1, storedPlInfo.size());
		PayloadEntity storedPl = storedPlInfo.iterator().next();

		assertEquals(pl.getOID(), storedPl.getOID());
	}

	@Test
	void testUserMessageRejectNotFoundPayloadId() {
		UserMessage um = new UserMessage();
		PayloadInfo pl = new PayloadInfo();
		pl.setPayloadId(UUID.randomUUID().toString());
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		um.addPayload(new PayloadEntity(pl));

		StorageException exception = assertThrows(StorageException.class, () -> provider.storeMessageUnit(um));

		assertTrue(exception.getMessage().contains("Unknown"));
	}

	@Test
	void testUserMessageRejectAlreadyLinkedPayloadId() {
		UserMessage um = new UserMessage();
		PayloadInfo pl = new PayloadInfo();
		pl.setPayloadId(UUID.randomUUID().toString());
		pl.setParentCoreId(UUID.randomUUID().toString());
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		EntityManagerUtil.save(pl);
		um.addPayload(new PayloadEntity(pl));

		StorageException exception = assertThrows(StorageException.class, () -> provider.storeMessageUnit(um));

		assertTrue(exception.getMessage().contains(pl.getParentCoreId()));
	}

	@Test
	void testPullRequest() {
		PullRequest pr = new PullRequest("pm-pulling", EbMSConstants.DEFAULT_MPC);

		PullRequestEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(pr));

		assertEquals(pr.getPModeId(), stored.getPModeId());
		assertEquals(pr.getMPC(), stored.getMPC());

		assertExistsInDb(stored);
	}

	@Test
	void testSelectivePullRequest() {
		SelectivePullRequest pr = new SelectivePullRequest();
		pr.setPModeId("pm-selective=pulling");
		pr.setMPC("test-channel");
		pr.setAction("PullMe");
		pr.setConversationId(UUID.randomUUID().toString());
		pr.setService(new Service("Pulling"));
		pr.setAgreementRef(new AgreementReference("test-select-pulling-store", null, null));

		SelectivePullRequestEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(pr));

		assertEquals(pr.getPModeId(), stored.getPModeId());
		assertEquals(pr.getMPC(), stored.getMPC());
		assertEquals(pr.getAction(), stored.getAction());
		assertEquals(pr.getConversationId(), stored.getConversationId());
		assertTrue(CompareUtils.areEqual(pr.getService(), stored.getService()));
		assertTrue(CompareUtils.areEqual(pr.getAgreementRef(), stored.getAgreementRef()));

		// Cannot use the assertExistsInDb method because the SelectivePullRequestEntity extends PullRequestEntity
		// and therefore has no type parameter
		assertNotNull(assertDoesNotThrow(() -> EntityManagerUtil.getEntityManager()
							.find(org.holodeckb2b.storage.metadata.jpa.SelectivePullRequest.class, stored.getOID())));
	}

	@Test
	void testReceipt() {
		Receipt r = new Receipt();
		r.setContent(ReceiptTest.generateRcptContent());

		ReceiptEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(r));

		ReceiptTest.assertSameXML(r.getContent(), stored.getContent());

		assertExistsInDb(stored);
	}

	@Test
	void testErrorMessage() {
		Collection<IEbmsError> errs = new ArrayList<>();
		errs.add(new OtherContentError("An error for testing"));
		EbmsError me = new MissingReceipt("Second error");
		me.setDescription(new Description("Seom additional text why the Receipt is missing"));
		errs.add(me);
		ErrorMessage em = new ErrorMessage(errs);

		ErrorMessageEntity stored = assertDoesNotThrow(() -> provider.storeMessageUnit(em));

		assertEquals(2, stored.getErrors().size());
		assertTrue(errs.parallelStream()
						.allMatch(e -> stored.getErrors().stream()
											.anyMatch(se -> Utils.nullSafeEqual(e.getCategory(), se.getCategory())
													&& Utils.nullSafeEqual(e.getErrorCode(), se.getErrorCode())
													&& Utils.nullSafeEqual(e.getSeverity(), se.getSeverity())
													&& Utils.nullSafeEqual(e.getErrorDetail(), se.getErrorDetail())
													&& (e.getDescription() == se.getDescription()
														|| e.getDescription().getText().equals(se.getDescription().getText()))
													)
								));
	}

	@Test
	void testPayload() {
		Payload pl = new Payload();
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		pl.addProperty(new Property("msgprop1", "value1"));
		pl.addProperty(new Property("msgprop2", "value2"));
		pl.setMimeType("test/payload");
		pl.setDescription(new Description("This description should not be used"));
		pl.setSchemaReference(new SchemaReference("https://holodeck-b2b.org/some/test/schema"));

		IPayloadEntity stored = assertDoesNotThrow(() -> provider.storePayloadMetadata(pl));

		assertNotNull(stored.getPayloadId());
		assertNull(stored.getParentCoreId());
		assertEquals(pl.getContainment(), stored.getContainment());
		assertEquals(pl.getPayloadURI(), stored.getPayloadURI());
		assertEquals(pl.getMimeType(), stored.getMimeType());
		assertEquals(pl.getDescription().getText(), stored.getDescription().getText());
		assertEquals(pl.getSchemaReference().getNamespace(), stored.getSchemaReference().getNamespace());

		assertNotNull(EntityManagerUtil.getEntityManager().find(PayloadInfo.class, ((PayloadEntity) stored).getOID()));
	}

}

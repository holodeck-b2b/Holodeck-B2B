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
package org.holodeckb2b.core.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import org.apache.axis2.AxisFault;
import org.holodeckb2b.common.errors.OtherContentError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.PartyId;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.SelectivePullRequest;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPayload.Containment;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IPullRequestEntity;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;
import org.holodeckb2b.interfaces.storage.ISelectivePullRequestEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.test.storage.InMemoryMDSProvider;
import org.holodeckb2b.test.storage.InMemoryPSProvider;
import org.holodeckb2b.test.storage.PayloadEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorageManagerTest {

	private static InMemoryMDSProvider	mdsProvider;
	private static InMemoryPSProvider	psProvider;

	@BeforeAll
	static void setupTest() throws AxisFault {
		HolodeckB2BTestCore testCore = new HolodeckB2BTestCore();
		mdsProvider = (InMemoryMDSProvider) testCore.getMetadataStorageProvider();
		psProvider = (InMemoryPSProvider) testCore.getPayloadStorageProvider();
		HolodeckB2BCoreInterface.setImplementation(testCore);
	}

	@BeforeEach
	void cleanup() {
		mdsProvider.clear();
		psProvider.clear();
	}

	@Test
	void testStoreReceivedUserMessage() {
		UserMessage um = new UserMessage();
		um.setMessageId(UUID.randomUUID().toString());
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

		IUserMessageEntity entity = assertDoesNotThrow(() ->
													HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(um));
		assertNotNull(entity);
		assertTrue(entity instanceof UserMessageEntityProxy);
		assertTrue(mdsProvider.existsMessageId(um.getMessageId()));
		assertEquals(1, entity.getPayloads().size());
		assertEquals(1, mdsProvider.getNumberOfStoredPayloads());
		assertEquals(0, psProvider.getPayloadCount());
	}

	@Test
	void testStoreReceivedSelectivePullRequest() {
		SelectivePullRequest pr = new SelectivePullRequest();
		pr.setMessageId(UUID.randomUUID().toString());

		ISelectivePullRequestEntity entity = assertDoesNotThrow(() ->
													HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(pr));
		assertNotNull(entity);
		assertTrue(mdsProvider.existsMessageId(pr.getMessageId()));
	}

	@Test
	void testStoreReceivedReceipt() {
		Receipt receipt = new Receipt();
		receipt.setMessageId(UUID.randomUUID().toString());

		IReceiptEntity entity = assertDoesNotThrow(() ->
												HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(receipt));
		assertNotNull(entity);
		assertTrue(mdsProvider.existsMessageId(receipt.getMessageId()));
	}

	@Test
	void testStoreReceivedError() {
		ErrorMessage err = new ErrorMessage();
		err.setMessageId(UUID.randomUUID().toString());

		IErrorMessageEntity entity = assertDoesNotThrow(() ->
													HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(err));
		assertNotNull(entity);
		assertTrue(mdsProvider.existsMessageId(err.getMessageId()));
	}

	@Test
	void testStoreOutgoingUserMessageNewPayloads() throws FileNotFoundException {
		UserMessage um = new UserMessage();
		um.setMessageId(UUID.randomUUID().toString());
		Payload pl = new Payload();
		pl.setContainment(Containment.ATTACHMENT);
		pl.setContentStream(new FileInputStream(TestUtils.getTestResource("flower.jpg").toFile()));
		um.addPayload(pl);
		Payload pl2 = new Payload();
		pl2.setContainment(Containment.ATTACHMENT);
		pl2.setContentStream(new FileInputStream(TestUtils.getTestResource("kitten.jpg").toFile()));
		um.addPayload(pl2);

		IUserMessageEntity entity = assertDoesNotThrow(() ->
													HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(um));
		assertNotNull(entity);
		assertTrue(entity instanceof UserMessageEntityProxy);
		assertTrue(mdsProvider.existsMessageId(um.getMessageId()));
		assertEquals(2, entity.getPayloads().size());

		for(IPayloadEntity pe : entity.getPayloads())
			assertNotNull(pe.getPayloadURI());

		assertEquals(2, mdsProvider.getNumberOfStoredPayloads());
		assertEquals(2, psProvider.getPayloadCount());

		assertDoesNotThrow(() -> psProvider.getPayloadContent(entity.getPayloads().iterator().next()).getContent());

		assertTrue(entity.getPayloads().stream()
						 .filter(p -> equalContent(p, TestUtils.getTestResource("flower.jpg"))).count() == 1);
		assertTrue(entity.getPayloads().stream()
						.filter(p -> equalContent(p, TestUtils.getTestResource("kitten.jpg"))).count() == 1);
	}

	@Test
	void testStoreOutgoingUserMessageExistingPayload() throws FileNotFoundException {
		Payload pl = new Payload();
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		pl.setContentStream(new FileInputStream(TestUtils.getTestResource("flower.jpg").toFile()));
		IPayloadEntity plEntity = assertDoesNotThrow(() -> mdsProvider.storePayloadMetadata(pl, null));
		assertDoesNotThrow(() -> psProvider.createNewPayloadStorage(plEntity));

		UserMessage um = new UserMessage();
		um.setMessageId(UUID.randomUUID().toString());
		um.addPayload(plEntity);

		IUserMessageEntity entity = assertDoesNotThrow(() ->
													HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(um));
		assertNotNull(entity);
		assertTrue(entity instanceof UserMessageEntityProxy);
		assertTrue(mdsProvider.existsMessageId(um.getMessageId()));
		assertEquals(1, entity.getPayloads().size());
		assertEquals(plEntity.getPayloadId(), entity.getPayloads().iterator().next().getPayloadId());
		assertEquals(plEntity.getPayloadURI(), entity.getPayloads().iterator().next().getPayloadURI());
		assertEquals(1, mdsProvider.getNumberOfStoredPayloads());
		assertEquals(1, psProvider.getPayloadCount());
	}

	@Test
	void testStoreOutgoingPullRequest() {
		PullRequest pr = new PullRequest();
		pr.setMessageId(UUID.randomUUID().toString());

		IPullRequestEntity entity = assertDoesNotThrow(() ->
													HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(pr));
		assertNotNull(entity);
		assertTrue(mdsProvider.existsMessageId(pr.getMessageId()));
	}

	@Test
	void testStoreOutgoingReceipt() {
		Receipt receipt = new Receipt();
		receipt.setMessageId(UUID.randomUUID().toString());

		IReceiptEntity entity = assertDoesNotThrow(() ->
												HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(receipt));
		assertNotNull(entity);
		assertTrue(mdsProvider.existsMessageId(receipt.getMessageId()));
	}

	@Test
	void testStoreOutgingError() {
		ErrorMessage err = new ErrorMessage();
		err.setMessageId(UUID.randomUUID().toString());

		IErrorMessageEntity entity = assertDoesNotThrow(() ->
													HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(err));
		assertNotNull(entity);
		assertTrue(mdsProvider.existsMessageId(err.getMessageId()));
	}

	@Test
	void testCreateErrorMsgFor() throws PModeSetException {
		IPMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		HolodeckB2BCoreInterface.getPModeSet().add(pmode);

		UserMessage um = new UserMessage();
		um.setMessageId(UUID.randomUUID().toString());
		um.setDirection(Direction.OUT);
		um.setPModeId(pmode.getId());

		IUserMessageEntity umEntity = assertDoesNotThrow(() -> mdsProvider.storeMessageUnit(um));

		IErrorMessageEntity error =  assertDoesNotThrow(() ->
					HolodeckB2BCore.getStorageManager().createErrorMsgFor(Set.of(new OtherContentError()), umEntity));

		assertEquals(umEntity.getMessageId(), error.getRefToMessageId());
		assertEquals(2, mdsProvider.getNumberOfStoredMessageUnits());
		assertEquals(Label.REQUEST, error.getLeg());
	}

	@Test
	void testStoreSubmittedPayload() throws FileNotFoundException {
		Payload pl = new Payload();
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		pl.setContentStream(new FileInputStream(TestUtils.getTestResource("flower.jpg").toFile()));

		final PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		IPayloadEntity plEntity = assertDoesNotThrow(() ->
												HolodeckB2BCore.getStorageManager().storeSubmittedPayload(pl, pmode));

		assertNotNull(plEntity);
		assertTrue(plEntity instanceof PayloadEntityProxy);
		assertEquals(1, mdsProvider.getNumberOfStoredPayloads());
		assertEquals(1, psProvider.getPayloadCount());

		assertTrue(equalContent(plEntity, TestUtils.getTestResource("flower.jpg")));
	}

	@Test
	void testCreateStorageReceivedPayload() throws FileNotFoundException {
		Payload pl = new Payload();
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		pl.setContentStream(new FileInputStream(TestUtils.getTestResource("flower.jpg").toFile()));

		assertNotNull(assertDoesNotThrow(() ->
							HolodeckB2BCore.getStorageManager().createStorageReceivedPayload(new PayloadEntity(pl))));
		assertEquals(1, psProvider.getPayloadCount());
	}

	@Test
	void testSetProcessingStateOnly() {
		IMessageUnitEntity mu = assertDoesNotThrow(() ->
										HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(new Receipt()));

		assertDoesNotThrow(() -> HolodeckB2BCore.getStorageManager().setProcessingState(mu, ProcessingState.PROCESSING));

		assertEquals(ProcessingState.PROCESSING, mu.getCurrentProcessingState().getState());
		assertNull(mu.getCurrentProcessingState().getDescription());
	}

	@Test
	void testSetProcessingStateWDescription() {
		IMessageUnitEntity mu = assertDoesNotThrow(() ->
									HolodeckB2BCore.getStorageManager().storeOutGoingMessageUnit(new PullRequest()));

		final String description = "Sending PR to responder";
		assertDoesNotThrow(() ->
					HolodeckB2BCore.getStorageManager().setProcessingState(mu, ProcessingState.SENDING, description));

		assertEquals(ProcessingState.SENDING, mu.getCurrentProcessingState().getState());
		assertEquals(description, mu.getCurrentProcessingState().getDescription());
	}

	@Test
	void setPModeId() {
		IMessageUnitEntity mu = assertDoesNotThrow(() ->
									HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(new UserMessage()));

		final String pmodeId = "1234";
		assertDoesNotThrow(() -> HolodeckB2BCore.getStorageManager().setPModeId(mu, pmodeId));

		assertEquals(pmodeId, mu.getPModeId());
	}

	@Test
	void testSetPModeAndLeg() {
		IErrorMessageEntity err = assertDoesNotThrow(() ->
								HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(new ErrorMessage()));

		final PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		final Pair<IPMode, Leg.Label> pmodeAndLeg = new Pair<>(pmode, Leg.Label.REPLY);

		assertDoesNotThrow(() -> HolodeckB2BCore.getStorageManager().setPModeAndLeg(err, pmodeAndLeg));

		assertEquals(pmodeAndLeg.value1().getId(), err.getPModeId());
		assertEquals(pmodeAndLeg.value2(), err.getLeg());
	}

	@Test
	void testSetMultiHop() {
		IMessageUnitEntity mu = assertDoesNotThrow(() ->
									HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(new UserMessage()));

		assertDoesNotThrow(() -> HolodeckB2BCore.getStorageManager().setMultiHop(mu, true));

		assertTrue(mu.usesMultiHop());
	}

	@Test
	void testSetAddSOAPFault() {
		IErrorMessageEntity err = assertDoesNotThrow(() ->
									HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(new ErrorMessage()));

		assertDoesNotThrow(() -> HolodeckB2BCore.getStorageManager().setAddSOAPFault(err, true));

		assertTrue(err.shouldHaveSOAPFault());
	}

	@Test
	void testUpdatePayloadInfo() throws FileNotFoundException {
		Payload pl = new Payload();
		pl.setContainment(Containment.ATTACHMENT);
		pl.setPayloadURI("cid:attachment");
		pl.setContentStream(new FileInputStream(TestUtils.getTestResource("flower.jpg").toFile()));

		PayloadEntityProxy entity = new PayloadEntityProxy(
												assertDoesNotThrow(() -> mdsProvider.storePayloadMetadata(pl, null)));

		final String mimeType = "image/jpeg";
		entity.setMimeType(mimeType);

		assertDoesNotThrow(() -> HolodeckB2BCore.getStorageManager().updatePayloadInformation(entity));

		assertEquals(mimeType, assertDoesNotThrow(() ->
								mdsProvider.getPayloadMetadata(entity.getPayloadId())).getMimeType());
	}

	private boolean equalContent(IPayload p, Path expected) {
		try (InputStream is1 = p.getContent(); FileInputStream is2 = new FileInputStream(expected.toFile());) {
			int b1, b2;
			do {
				b1 = is1.read(); b2 = is2.read();
			} while (b1 == b2 && b1 != -1 && b2 != -1);
			return b1 == b2;
		} catch (IOException e) {
			fail(e);
			return false;
		}
	}
}

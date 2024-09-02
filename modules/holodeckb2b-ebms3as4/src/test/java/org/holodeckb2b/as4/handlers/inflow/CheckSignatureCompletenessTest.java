/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.as4.handlers.inflow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.ReceiptConfiguration;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Created at 17:01 10.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@ExtendWith(MockitoExtension.class)
public class CheckSignatureCompletenessTest {

	private static PMode pmode;

	private MessageProcessingContext 	procCtx;

    @BeforeAll
    public static void setUpClass() throws Exception {
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());

        pmode = HB2BTestUtils.create1WayReceivePMode();
        Leg leg = pmode.getLeg(Label.REQUEST);
        ReceiptConfiguration receiptConfig = new ReceiptConfiguration();
        receiptConfig.setPattern(ReplyPattern.RESPONSE);
        leg.setReceiptConfiguration(receiptConfig);
        HolodeckB2BCore.getPModeSet().add(pmode);
    }

    @BeforeEach
    public void prepareContext() throws StorageException {
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setServerSide(true);

        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        userMessage.setPModeId(pmode.getId());

        Payload pl1 = new Payload();
        pl1.setContainment(IPayload.Containment.ATTACHMENT);
        pl1.setPayloadURI("first-payload-ref");
        userMessage.addPayload(pl1);
        Payload pl2 = new Payload();
        pl2.setContainment(IPayload.Containment.ATTACHMENT);
        pl2.setPayloadURI("first-payload-ref");
        userMessage.addPayload(pl2);
        Payload pl3 = new Payload();
        pl1.setContainment(IPayload.Containment.BODY);
        userMessage.addPayload(pl3);

		procCtx = MessageProcessingContext.getFromMessageContext(mc);
        procCtx.setUserMessage(HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(userMessage));
    }

    @Test
    public void testUnSigned() throws Exception {
    	assertDoesNotThrow(() -> new CheckSignatureCompleteness().invoke(procCtx.getParentContext()));

        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }

    @Test
    public void testSignedCompletely() throws Exception {
        // Create complete set of digests
        ISignedPartMetadata headerData = mock(ISignedPartMetadata.class);

    	Map<IPayloadEntity, ISignedPartMetadata>  digestData = new HashMap<>();
        procCtx.getReceivedUserMessage().getPayloads().forEach((p) -> digestData.put(p, null));

        ISignatureProcessingResult  signatureResult = mock(ISignatureProcessingResult.class);
        when(signatureResult.getTargetedRole()).thenReturn(SecurityHeaderTarget.DEFAULT);
        when(signatureResult.getHeaderDigest()).thenReturn(headerData);
        when(signatureResult.getPayloadDigests()).thenReturn(digestData);

        procCtx.addSecurityProcessingResult(signatureResult);

        assertDoesNotThrow(() -> new CheckSignatureCompleteness().invoke(procCtx.getParentContext()));

        assertTrue(Utils.isNullOrEmpty(procCtx.getGeneratedErrors()));
    }

    @Test
    public void testUnsignedebMSHeader() throws Exception {
    	final IUserMessageEntity userMessage = procCtx.getReceivedUserMessage();

    	// Create complete set of digests
    	Map<IPayloadEntity, ISignedPartMetadata>  digestData = new HashMap<>();

    	userMessage.getPayloads().forEach((p) -> digestData.put(p, null));

    	ISignatureProcessingResult  signatureResult = mock(ISignatureProcessingResult.class);
    	when(signatureResult.getTargetedRole()).thenReturn(SecurityHeaderTarget.DEFAULT);
    	when(signatureResult.getHeaderDigest()).thenReturn(null);
    	when(signatureResult.getPayloadDigests()).thenReturn(digestData);
    	procCtx.addSecurityProcessingResult(signatureResult);

    	assertDoesNotThrow(() -> new CheckSignatureCompleteness().invoke(procCtx.getParentContext()));

    	assertFalse(Utils.isNullOrEmpty(procCtx.getGeneratedErrors().get(userMessage.getMessageId())));
    	assertEquals("EBMS:0103",
    			procCtx.getGeneratedErrors().get(userMessage.getMessageId()).iterator().next().getErrorCode());
    }

    @Test
    public void testUnsignedPayload() throws Exception {
    	final IUserMessageEntity userMessage = procCtx.getReceivedUserMessage();

    	// Create complete set of digests
    	ISignedPartMetadata headerData = mock(ISignedPartMetadata.class);

    	Map<IPayloadEntity, ISignedPartMetadata>  digestData = new HashMap<>();
        Iterator<? extends IPayloadEntity> iterator = userMessage.getPayloads().iterator();
        for (int i = 0; i < userMessage.getPayloads().size() - 1; i++) {
            digestData.put(iterator.next(), null);
        }

        ISignatureProcessingResult  signatureResult = mock(ISignatureProcessingResult.class);
        when(signatureResult.getTargetedRole()).thenReturn(SecurityHeaderTarget.DEFAULT);
        when(signatureResult.getHeaderDigest()).thenReturn(headerData);
        when(signatureResult.getPayloadDigests()).thenReturn(digestData);
        procCtx.addSecurityProcessingResult(signatureResult);

        assertDoesNotThrow(() -> new CheckSignatureCompleteness().invoke(procCtx.getParentContext()));

        assertFalse(Utils.isNullOrEmpty(procCtx.getGeneratedErrors().get(userMessage.getMessageId())));
        assertEquals("EBMS:0103",
        				procCtx.getGeneratedErrors().get(userMessage.getMessageId()).iterator().next().getErrorCode());
    }
}
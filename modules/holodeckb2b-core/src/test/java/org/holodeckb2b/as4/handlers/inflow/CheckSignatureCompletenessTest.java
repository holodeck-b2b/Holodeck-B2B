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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.pmode.xml.PMode;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 17:01 10.03.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckSignatureCompletenessTest {

    private static PMode                pmode;
    private static UserMessage          userMessage;
    private static ArrayList<IPayload>  payloads;

    private MessageContext  mc;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String baseDir = CheckSignatureCompletenessTest.class.getClassLoader()
                    .getResource(CheckSignatureCompletenessTest.class.getName().replace('.', '/')).getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(baseDir));

        // Create the basic test data
        pmode = PMode.createFromFile(new File(baseDir + "/pmode.xml"));
        HolodeckB2BCore.getPModeSet().add(pmode);

        userMessage = new UserMessage();
        userMessage.setPModeId(pmode.getId());

        payloads = new ArrayList<>();
        Payload pl1 = new Payload();
        pl1.setContainment(IPayload.Containment.ATTACHMENT);
        pl1.setPayloadURI("first-payload-ref");
        payloads.add(pl1);
        Payload pl2 = new Payload();
        pl2.setContainment(IPayload.Containment.ATTACHMENT);
        pl2.setPayloadURI("first-payload-ref");
        payloads.add(pl2);
        Payload pl3 = new Payload();
        pl1.setContainment(IPayload.Containment.BODY);
        payloads.add(pl3);
        userMessage.setPayloads(payloads);
    }

    @Before
    public void setupMessageContext() throws PersistenceException {
        userMessage.setMessageId(MessageIdUtils.createMessageId());
        IUserMessage umEntity = HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);

        mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, umEntity);
    }

    @Test
    public void testNotSigned() throws Exception {
        try {
            new CheckSignatureCompleteness().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }

        assertNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
    }

    @Test
    public void testSignedCompletely() throws Exception {

        // Create complete set of digests
        Map<IPayload, ISignedPartMetadata>  digestData = new HashMap<>();
        payloads.forEach((p) -> digestData.put(p, null));

        ISignatureProcessingResult  signatureResult = mock(ISignatureProcessingResult.class);
        when(signatureResult.getPayloadDigests()).thenReturn(digestData);
        mc.setProperty(MessageContextProperties.SIG_VERIFICATION_RESULT, signatureResult);

        try {
            new CheckSignatureCompleteness().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }
        assertNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
    }

    @Test
    public void testUnsignedPayload() throws Exception {
        // Create complete set of digests
        Map<IPayload, ISignedPartMetadata>  digestData = new HashMap<>();

        for (int i = 0; i < payloads.size() - 1; i++) {
            digestData.put(payloads.get(i), null);
        }
        ISignatureProcessingResult  signatureResult = mock(ISignatureProcessingResult.class);
        when(signatureResult.getPayloadDigests()).thenReturn(digestData);
        mc.setProperty(MessageContextProperties.SIG_VERIFICATION_RESULT, signatureResult);

        try {
            new CheckSignatureCompleteness().invoke(mc);
        } catch (AxisFault e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + "/" + e.getMessage());
        }
        assertNotNull(mc.getProperty(MessageContextProperties.GENERATED_ERRORS));
    }
}
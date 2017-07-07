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
package org.holodeckb2b.as4.compression;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.activation.DataHandler;
import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.PayloadProfile;
import org.holodeckb2b.pmode.helpers.UserMessageFlow;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 13:57 21.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class CompressionHandlerTest {

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private CompressionHandler handler;

    @BeforeClass
    public static void setUpClass() {
        baseDir = CompressionHandlerTest.class.getClassLoader()
                .getResource("compression").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new CompressionHandler();
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("compression/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        PMode pmode = new PMode();

        Leg leg = new Leg();

        UserMessageFlow umFlow = new UserMessageFlow();

        PayloadProfile plProfile = new PayloadProfile();
        plProfile.setCompressionType(CompressionFeature.COMPRESSED_CONTENT_TYPE);
        umFlow.setPayloadProfile(plProfile);

        leg.setUserMessageFlow(umFlow);

        pmode.addLeg(leg);

        UserMessage userMessage = UserMessageElement.readElement(umElement);

        // We need to add payload with containment type "attachment" to user message,
        // because PartInfoElement does not read the "containment" attribute
        // value of the PartInfo tag from file now (23-Feb-2017 T.S.)
        Payload payload = new Payload();
        payload.setContainment(IPayload.Containment.ATTACHMENT);
        String payloadPath = "file://./uncompressed.jpg";
        payload.setPayloadURI(payloadPath);
        userMessage.addPayload(payload);

        Attachments attachments = new Attachments();

        DataHandler attDataHandler = new DataHandler(new URL(payload.getPayloadURI()));

        attachments.addDataHandler(payloadPath, attDataHandler);
        mc.setAttachmentMap(attachments);

        String pmodeId =
                userMessage.getCollaborationInfo().getAgreement().getPModeId();
        // Copy pmodeId of the agreement to the user message
        userMessage.setPModeId(pmodeId);
        pmode.setId(pmodeId);

        core.getPModeSet().add(pmode);

        // Setting input message property
        IUserMessageEntity userMessageEntity =
                HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.OUT_USER_MESSAGE,
                userMessageEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertEquals(Handler.InvocationResponse.CONTINUE, invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Check that attachment payload contains compression enabling properties
        for (final IPayload p : userMessageEntity.getPayloads()) {
            // Only payloads contained in attachment can use compression
            if (p.getContainment() == IPayload.Containment.ATTACHMENT) {
                Collection<IProperty> properties = p.getProperties();
                assertNotNull(properties);
                Iterator<IProperty> it = properties.iterator();
                IProperty property = it.next();
                assertEquals(CompressionFeature.FEATURE_PROPERTY_NAME,
                        property.getName());
                assertEquals(CompressionFeature.COMPRESSED_CONTENT_TYPE,
                        property.getValue());
                property = it.next();
                assertEquals(CompressionFeature.MIME_TYPE_PROPERTY_NAME,
                        property.getName());
                assertEquals(attDataHandler.getContentType(),
                        property.getValue());
            }
        }

        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String expLogMsg = "Enabled compression for all attached payloads";
        boolean containsExpLogMsg = false;
        for(LoggingEvent e : events) {
            if(e.getLevel().equals(Level.DEBUG)) {
                if(e.getRenderedMessage().equals(expLogMsg)) {
                    containsExpLogMsg = true;
                }
            }
        }
        assertTrue(containsExpLogMsg);
    }
}
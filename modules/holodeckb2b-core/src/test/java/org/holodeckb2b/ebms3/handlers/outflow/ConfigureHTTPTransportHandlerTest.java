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
package org.holodeckb2b.ebms3.handlers.outflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;

import javax.activation.DataHandler;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.transport.http.HTTPConstants;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.Protocol;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created at 15:48 27.02.17
 *
 * Checked for cases coverage (11.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigureHTTPTransportHandlerTest {
    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private ConfigureHTTPTransportHandler handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = ConfigureHTTPTransportHandlerTest.class.getClassLoader()
                .getResource("security").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new ConfigureHTTPTransportHandler();
    }

    @After
    public void tearDown() throws Exception {
        core.getPModeSet().removeAll();
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testDoProcessing() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("handlers/full_mmd.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        PMode pmode = new PMode();

        Leg leg = new Leg();
        // Setting all protocol configurations checked by the tested handler
        Protocol protocolConfig = new Protocol();
        String destUrl = "http://example.com";
        protocolConfig.setAddress(destUrl);
        protocolConfig.setHTTPCompression(true);
        protocolConfig.setChunking(true);
        leg.setProtocol(protocolConfig);
        pmode.addLeg(leg);

        UserMessage userMessage = UserMessageElement.readElement(umElement);

        // Setting attachments
        Attachments attachments = new Attachments();
        Payload payload = new Payload();
        payload.setContainment(IPayload.Containment.ATTACHMENT);
        String payloadPath = "file://./flower.jpg";
        payload.setPayloadURI(payloadPath);
        payload.setContentLocation(baseDir + "/flower.jpg");
        userMessage.addPayload(payload);
        attachments.addDataHandler(payloadPath,
                new DataHandler(new URL(payload.getPayloadURI())));

        mc.setAttachmentMap(attachments);

        String pmodeId =
                userMessage.getCollaborationInfo().getAgreement().getPModeId();
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

        assertEquals(destUrl, mc.getProperty(Constants.Configuration.TRANSPORT_URL));

        Options options = mc.getOptions();
        assertNotNull(options);
        assertTrue((Boolean) options.getProperty(HTTPConstants.MC_GZIP_REQUEST));
        assertTrue((Boolean) options.getProperty(HTTPConstants.CHUNKED));
        assertNull(options.getProperty(Constants.Configuration.ENABLE_SWA));
    }
}
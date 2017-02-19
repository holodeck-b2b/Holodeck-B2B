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
package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.TradingPartner;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.persistency.dao.UpdateManager;
import org.holodeckb2b.pmode.helpers.Agreement;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.PartnerConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created at 23:22 29.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class FindPModesTest {

    // Appender to control logging events
    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private FindPModes handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = FindPModesTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new FindPModes();
        // Adding appender to the FindPModes logger
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
        MessageMetaData mmd = getMMD("handlers/full_mmd.xml");
        // Creating SOAP envelope
        SOAPEnvelope env = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);
        // Adding UserMessage from mmd
        OMElement umElement = UserMessageElement.createElement(headerBlock, mmd);

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.IN_FLOW);

        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        PMode pmode = new PMode();
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

        PartnerConfig initiator = new PartnerConfig();
        pmode.setInitiator(initiator);

        PartnerConfig responder = new PartnerConfig();
        pmode.setResponder(responder);

        Leg leg = new Leg();
        pmode.addLeg(leg);

        UserMessage userMessage
                = UserMessageElement.readElement(umElement);

        String msgId = userMessage.getMessageId();

        TradingPartner sender = userMessage.getSender();
        initiator.setRole(sender.getRole());
        initiator.setPartyIds(sender.getPartyIds());

        TradingPartner receiver = userMessage.getReceiver();
        responder.setRole(receiver.getRole());
        responder.setPartyIds(receiver.getPartyIds());

        AgreementReference agreementReference =
                userMessage.getCollaborationInfo().getAgreement();
        String pmodeId = agreementReference.getPModeId();
        String agreementRefName = agreementReference.getName();
        String agreementRefType = agreementReference.getType();

        pmode.setId(pmodeId);

        Agreement agreement = new Agreement();
        agreement.setName(agreementRefName);
        agreement.setType(agreementRefType);
        pmode.setAgreement(agreement);

        // todo It seems strange that we need to set the PMode id value separately
        // todo when it is contained within the agreement
        // todo but if we don't set it the value returned by userMessage.getPModeId() is null now
        userMessage.setPModeId(pmodeId);

        core.getPModeSet().add(pmode);

        // Setting input message property
        UpdateManager updateManager = core.getUpdateManager();
        System.out.println("um: " + updateManager.getClass());
        IUserMessageEntity userMessageEntity =
                updateManager.storeIncomingMessageUnit(userMessage);
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE,
                userMessageEntity);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Checking log messages to make sure handler found pmode
        verify(mockAppender, atLeastOnce())
                .doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String expLogMsg = "Found P-Mode [" + pmode.getId()
                + "] for User Message [" + msgId + "]";
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

    /**
     * Get filled mmd document for testing
     * @return
     */
    private MessageMetaData getMMD(String resource) {
        final String mmdPath =
                this.getClass().getClassLoader().getResource(resource).getPath();
        final File f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        return mmd;
    }
}
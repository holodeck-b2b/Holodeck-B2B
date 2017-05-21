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
package org.holodeckb2b.as4.receptionawareness;

import org.apache.axis2.context.MessageContext;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.Interval;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.jpa.MessageUnit;
import org.holodeckb2b.persistency.jpa.MessageUnitProcessingState;
import org.holodeckb2b.persistency.util.EntityManagerUtil;
import org.holodeckb2b.pmode.helpers.Leg;
import org.holodeckb2b.pmode.helpers.PMode;
import org.holodeckb2b.pmode.helpers.Protocol;
import org.holodeckb2b.pmode.helpers.ReceptionAwarenessConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Created at 17:54 30.04.17
 *
 * todo [] complete the test
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class RetransmissionWorkerTest {

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private RetransmissionWorker worker;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = RetransmissionWorkerTest.class.getClassLoader()
                .getResource("security").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        prepareTestMessages();
        worker = new RetransmissionWorker();
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(mockAppender);
        logger.setLevel(Level.ALL);
    }

    @After
    public void tearDown() throws Exception {
        core.getPModeSet().removeAll();
        LogManager.getRootLogger().removeAppender(mockAppender);
    }

    @Test
     public void testDoProcessing() throws Exception {
        String pmodeId = T_PMODEID_2;

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        PMode pmode = new PMode();
        pmode.setId(pmodeId);
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        Leg leg = new Leg();
        ReceptionAwarenessConfig rac = new ReceptionAwarenessConfig();
        rac.setDuplicateDetection(true);
        rac.setRetryInterval(new Interval(5, TimeUnit.SECONDS));
        leg.setReceptionAwareness(rac);
        pmode.addLeg(leg);

        //Adding PMode to the managed PMode set.
        core.getPModeSet().add(pmode);

        worker.run();

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg0 = "1 messages may be waiting for a Receipt";
        String msg1 = "Get retry configuration from P-Mode [PMODE-02]";
        String msg2 = "Message must be pulled by receiver again";
        String msg3 = "Message unit is ready for retransmission";
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg0));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg1));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg2));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg3));
    }

    @Test
    public void testDoProcessingWithProtocol() throws Exception {
        String pmodeId = T_PMODEID_2;

        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);

        PMode pmode = new PMode();
        pmode.setId(pmodeId);
        pmode.setMep(EbMSConstants.ONE_WAY_MEP);
        pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);
        Leg leg = new Leg();
        ReceptionAwarenessConfig rac = new ReceptionAwarenessConfig();
        rac.setDuplicateDetection(true);
        rac.setRetryInterval(new Interval(5, TimeUnit.SECONDS));
        leg.setReceptionAwareness(rac);

        Protocol protocolConfig = new Protocol();
        String destUrl = "http://example.com";
        protocolConfig.setAddress(destUrl);
        leg.setProtocol(protocolConfig);
        leg.setProtocol(protocolConfig);

        pmode.addLeg(leg);

        //Adding PMode to the managed PMode set.
        core.getPModeSet().add(pmode);

        worker.run();

        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        String msg0 = "1 messages may be waiting for a Receipt";
        String msg1 = "Get retry configuration from P-Mode [PMODE-02]";
        String msg2 = "Message must be pushed to receiver again";
        String msg3 = "Message unit is ready for retransmission";
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg0));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg1));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg2));
        assertTrue(TestUtils.eventContainsMsg(events, Level.DEBUG, msg3));
    }

    private static final String T_MSG_ID  = "0003-msgid@test.holodeck-b2b.org";
    private static final String T_PMODEID_2 = "PMODE-02";
    private static final String T_MSG_ID_1  = "0001-msgid@test.holodeck-b2b.org";

    private void prepareTestMessages() throws Exception {
        UserMessage userMsg = new UserMessage();
        userMsg.setMessageId(T_MSG_ID);
        userMsg.setTimestamp(new Date());
        userMsg.setPModeId(T_PMODEID_2);
        userMsg.setDirection(IMessageUnit.Direction.OUT);
        userMsg.setRefToMessageId(T_MSG_ID_1);
        userMsg.setProcessingState(ProcessingState.AWAITING_RECEIPT);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        EntityManager em = null;
        try {
            // First clean the database
            em = EntityManagerUtil.getEntityManager();
            em.getTransaction().begin();
            final Collection<MessageUnit> allMU =
                    em.createQuery("from MessageUnit", MessageUnit.class).getResultList();
            for(final MessageUnit mu : allMU)
                em.remove(mu);
            em.getTransaction().commit();

            // Then add the test data
            em.getTransaction().begin();
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.UserMessage(userMsg), 5));
            em.getTransaction().commit();
        } finally {
            if (em != null) em.close();
        }
    }

    /**
     * Helper to change the start time of the last processing state of the message unit.
     *
     * @param jpaObject     The message unit to change
     * @param days          The number of days to set the start time back
     * @return              The modified JPA message unit object
     */
    private static MessageUnit modifyLastStateChange(MessageUnit jpaObject, int days) {
        MessageUnitProcessingState currentState = (MessageUnitProcessingState) jpaObject.getCurrentProcessingState();
        Calendar stateTime = Calendar.getInstance();
        stateTime.setTime(currentState.getStartTime());
        stateTime.add(Calendar.DAY_OF_YEAR, -days);
        currentState.setStartTime(stateTime.getTime());

        return jpaObject;
    }
}
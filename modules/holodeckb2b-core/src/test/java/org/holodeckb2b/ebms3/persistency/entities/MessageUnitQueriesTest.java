/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.persistency.entities;


import java.io.StringReader;
import java.util.Collection;
import javax.persistence.EntityManager;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.JPAUtil;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.testhelpers.HolodeckCore;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MessageUnitQueriesTest {

    private static final String T_MSG_ID_1 = "63768761876278234678624@msg-id.org";
    private static final String T_MSG_ID_2 = "987654321098ddsa6543210@msg-id.org";
    private static final String T_MSG_ID_3 = "64554576187627846a122qa@msg-id.org";
    private static final String T_MSG_ID_4 = "98765445545409876543210@msg-id.org";
    private static final String T_MSG_ID_5 = "fffw2345545409876543220@msg-id.org";
    private static final String T_MSG_ID_6 = "sadk2345545409876543210@msg-id.org";
    private static final String T_MSG_ID_7 = "djdkck110309kap[6543220@msg-id.org";
    private static final String T_MSG_ID_8 = "i2duncdke23owd912kw6310@msg-id.org";
    private static final String T_MSG_ID_9 = "adi29190k3210sksk200212@msg-id.org";

    private static final String T_PROCSTATE_1 = "firststate";
    private static final String T_PROCSTATE_2 = "secondstate";

    private static final String T_PMODE1 = "PMODE_1";
    private static final String T_PMODE2 = "PMODE_2";
    private static final String T_PMODE3 = "PMODE_3";

    private static final String T_CONTENT_1 =   "<content>" +
                                            "<confirmation>\n" +
                                            "    <from>Party_X</from>\n" +
                                            "    <message>Success</message>\n" +
                                            "</confirmation>\n" +
                                            "</content>";
    EntityManager   em;

    public MessageUnitQueriesTest() {
    }

    @BeforeClass
    public static void setUpClass() throws DatabaseException {

        HolodeckB2BCoreInterface.setImplementation(new HolodeckCore(null));

        final EntityManager em = JPAUtil.getEntityManager();

        em.getTransaction().begin();

        final UserMessage mu1 = new UserMessage();
        mu1.setMessageId(T_MSG_ID_1);
        final ProcessingState s1 = new ProcessingState(T_PROCSTATE_1);
        em.persist(s1);
        mu1.setProcessingState(s1);
        mu1.setPMode(T_PMODE1);
        em.persist(mu1);

        final UserMessage mu2 = new UserMessage();
        mu2.setMessageId(T_MSG_ID_2);
        final ProcessingState s2 = new ProcessingState(T_PROCSTATE_1);
        em.persist(s2);
        mu2.setProcessingState(s2);
        mu2.setPMode(T_PMODE2);
        em.persist(mu2);

        final PullRequest mu3 = new PullRequest();
        mu3.setMessageId(T_MSG_ID_3);
        final ProcessingState s3 = new ProcessingState(T_PROCSTATE_2);
        em.persist(s3);
        mu3.setProcessingState(s3);
        mu3.setPMode(T_PMODE2);
        em.persist(mu3);

        final Receipt mu4 = new Receipt();
        mu4.setMessageId(T_MSG_ID_4);
        mu4.setRefToMessageId(T_MSG_ID_2);
        final OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(new StringReader(T_CONTENT_1));
        // Parse document and get root element
        final OMElement contentElement = builder.getDocumentElement();

        mu4.setContent(contentElement.getChildElements());
        final ProcessingState s4 = new ProcessingState(T_PROCSTATE_2);
        em.persist(s4);
        mu4.setProcessingState(s4);
        mu4.setPMode(T_PMODE3);
        em.persist(mu4);

        final UserMessage mu5 = new UserMessage();
        mu5.setMessageId(T_MSG_ID_5);
        mu5.setRefToMessageId(T_MSG_ID_3);
        final ProcessingState s5 = new ProcessingState(ProcessingStates.DELIVERED);
        em.persist(s5);
        mu5.setProcessingState(s5);
        mu5.setDirection(MessageUnit.Direction.IN);
        mu5.setPMode(T_PMODE3);
        em.persist(mu5);

        final UserMessage mu6 = new UserMessage();
        mu6.setMessageId(T_MSG_ID_5);
        mu6.setRefToMessageId(T_MSG_ID_1);
        final ProcessingState s6 = new ProcessingState(ProcessingStates.AWAITING_RECEIPT);
        em.persist(s6);
        mu6.setProcessingState(s6);
        mu6.setDirection(MessageUnit.Direction.IN);
        mu6.setPMode(T_PMODE2);
        em.persist(mu6);

        final UserMessage mu7 = new UserMessage();
        mu7.setMessageId(T_MSG_ID_6);
        mu7.setRefToMessageId(T_MSG_ID_1);
        final ProcessingState s7 = new ProcessingState(ProcessingStates.DELIVERED);
        em.persist(s7);
        mu7.setProcessingState(s7);
        mu7.setDirection(MessageUnit.Direction.IN);
        mu7.setPMode(T_PMODE3);
        em.persist(mu7);

        final ErrorMessage mu8 = new ErrorMessage();
        mu8.setMessageId(T_MSG_ID_7);
        mu8.setRefToMessageId(T_MSG_ID_5);
        em.persist(mu8);

        final ErrorMessage mu9 = new ErrorMessage();
        mu9.setMessageId(T_MSG_ID_5);
        mu9.setRefToMessageId(T_MSG_ID_7);
        mu9.setDirection(MessageUnit.Direction.IN);
        em.persist(mu9);

        em.getTransaction().commit();
        em.close();
    }

    @AfterClass
    public static void cleanup() throws DatabaseException {
        final EntityManager em = JPAUtil.getEntityManager();

        em.getTransaction().begin();
        final Collection<MessageUnit> tps = em.createQuery("from MessageUnit", MessageUnit.class).getResultList();

        for(final MessageUnit mu : tps)
            em.remove(mu);

        em.getTransaction().commit();
    }

    @Before
    public void setUp() throws DatabaseException {
        em = JPAUtil.getEntityManager();
    }

    @After
    public void tearDown() {
        em.close();
    }


    @Test
    public void test01_findWithMessageIdQuery() {
        em.getTransaction().begin();

        final Collection<MessageUnit> result = em.createNamedQuery("MessageUnit.findWithMessageIdInDirection",
                                            MessageUnit.class)
                                            .setParameter("msgId", T_MSG_ID_5)
                                            .setParameter("direction", MessageUnit.Direction.IN)
                                            .getResultList();
        assertEquals(3, result.size());

        em.getTransaction().commit();
    }

    @Test
    public void test02_getResponsesTo() {
        em.getTransaction().begin();

        Collection<MessageUnit> result = em.createNamedQuery("Receipt.findResponsesTo",
                                            MessageUnit.class)
                                            .setParameter("refToMsgId", T_MSG_ID_2)
                                            .getResultList();
        assertEquals(1, result.size());

        result = null;
        result = em.createNamedQuery("ErrorMessage.findResponsesTo",
                                            MessageUnit.class)
                                            .setParameter("refToMsgId", T_MSG_ID_5)
                                            .getResultList();
        assertEquals(1, result.size());

        result = null;
        result = em.createNamedQuery("UserMessage.findResponsesTo",
                                            MessageUnit.class)
                                            .setParameter("refToMsgId", T_MSG_ID_1)
                                            .getResultList();
        assertEquals(2, result.size());

        em.getTransaction().commit();
    }

}

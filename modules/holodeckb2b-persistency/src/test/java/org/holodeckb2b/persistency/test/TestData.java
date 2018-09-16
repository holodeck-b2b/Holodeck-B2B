/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency.test;

//import com.sun.xml.internal.ws.api.SOAPVersion;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPFactory;
import org.holodeckb2b.common.messagemodel.*;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.jpa.MessageUnit;
import org.holodeckb2b.persistency.jpa.MessageUnitProcessingState;
import org.holodeckb2b.persistency.util.EntityManagerUtil;

import javax.persistence.EntityManager;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * Helper class to create a set of message unit meta-data for testing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class TestData {

    private static final String T_MSG_ID_1  = "0001-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_2  = "0002-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_3  = "0003-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_4  = "0004-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_5  = "0005-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_6  = "0006-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_7  = "0007-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_8  = "0008-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_9  = "0009-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_10 = "0010-msgid@test.holodeck-b2b.org";
    private static final String T_MSG_ID_11 = "0011-msgid@test.holodeck-b2b.org";

    private static final String T_PMODEID_1 = "PMODE-01";
    private static final String T_PMODEID_2 = "PMODE-02";
    private static final String T_PMODEID_3 = "PMODE-03";

    private static final String T_SENDER_1_PID  = "http://test.holodeck-b2b.org/partyids/Sender/001";
    private static final String T_SENDER_1_ROLE = EbMSConstants.DEFAULT_ROLE;

    private static final String T_RECEIVER_1_PID  = "http://test.holodeck-b2b.org/partyids/Receiver/001";
    private static final String T_RECEIVER_1_ROLE = EbMSConstants.DEFAULT_ROLE;

    private static final String T_SERVICE_1   = "http://test.holodeck-b2b.org/tests/ensureloaded";
    private static final String T_ACTION_1    = "LoadTest";
    private static final String T_AGREEMENT_1 = "AgreementReference-1";

    private static final IPayload.Containment T_PL_CONTAINMENT_1 = IPayload.Containment.BODY;
    private static final IPayload.Containment T_PL_CONTAINMENT_2 = IPayload.Containment.ATTACHMENT;
    private static final IPayload.Containment T_PL_CONTAINMENT_3 = IPayload.Containment.EXTERNAL;

    private static final String T_PL_CONTENT_LOC_1 = "/root/holodeckb2b/test/payload/file01.xml";
    private static final String T_PL_CONTENT_LOC_2 = "/root/holodeckb2b/test/payload/file02.pdf";
    private static final String T_PL_CONTENT_LOC_3 = "/root/holodeckb2b/test/payload/file03.mp4";

    private static final String T_PL_PROP_1_NAME  = "MimeType";
    private static final String T_PL_PROP_1_VALUE = "image/jpeg";
    private static final String T_PL_DESCR_TEXT   = "This is a fictious payload";
    private static final String T_PL_DESCR_LANG   = "en";
    private static final String T_PL_SCHEMA_NS    = "http://test.holodeck-b2b.org/payload/001/schema";

    private static final String T_MSG_PROP_1_NAME  = "isTestDocument";
    private static final String T_MSG_PROP_1_VALUE = "true";

    private static final String T_ERR_CODE      = "EBMS:0001";
    private static final String T_ERR_DETAIL    = "This is just a test error";

    public static final UserMessage   userMsg1;
    public static final Receipt       receipt1;

    public static final UserMessage   userMsg2;
    public static final Receipt       receipt2;

    public static final UserMessage   userMsg3;
    public static final ErrorMessage  error3;

    public static final PullRequest   pull4;
    public static final ErrorMessage  error4;

    public static final PullRequest   pull5;
    public static final UserMessage   userMsg5;

    public static final UserMessage   userMsg6;
    public static final Receipt       receipt6;

    public static final Payload       payload1;
    public static final Payload       payload2;
    public static final Payload       payload3;

    // Initialize the test set
    static {
        payload1 = new Payload();
        payload1.setContainment(T_PL_CONTAINMENT_1);
        payload1.setContentLocation(T_PL_CONTENT_LOC_1);
        payload1.addProperty(new Property(T_PL_PROP_1_NAME, T_PL_PROP_1_VALUE));
        payload1.setDescription(new Description(T_PL_DESCR_TEXT, T_PL_DESCR_LANG));
        payload1.setSchemaReference(new SchemaReference(T_PL_SCHEMA_NS));

        payload2 = new Payload();
        payload2.setContainment(T_PL_CONTAINMENT_2);
        payload2.setContentLocation(T_PL_CONTENT_LOC_2);

        payload3 = new Payload();
        payload3.setContainment(T_PL_CONTAINMENT_3);
        payload3.setContentLocation(T_PL_CONTENT_LOC_3);

        userMsg1 = new UserMessage();
        userMsg1.setMessageId(T_MSG_ID_1);
        userMsg1.setTimestamp(new Date());
        userMsg1.setPModeId(T_PMODEID_1);
        userMsg1.setDirection(Direction.OUT);
        userMsg1.setProcessingState(ProcessingState.SUBMITTED);
        TradingPartner sender = new TradingPartner();
        sender.addPartyId(new PartyId(T_SENDER_1_PID, null));
        sender.setRole(T_SENDER_1_ROLE);
        userMsg1.setSender(sender);
        TradingPartner receiver = new TradingPartner();
        receiver.addPartyId(new PartyId(T_RECEIVER_1_PID, null));
        receiver.setRole(T_RECEIVER_1_ROLE);
        userMsg1.setReceiver(receiver);
        CollaborationInfo collabInfo = new CollaborationInfo();
        collabInfo.setService(new Service(T_SERVICE_1));
        collabInfo.setAction(T_ACTION_1);
        collabInfo.setAgreement(new AgreementReference(T_AGREEMENT_1, null, null));
        userMsg1.setCollaborationInfo(collabInfo);
        userMsg1.addPayload(payload1);
        userMsg1.addMessageProperty(new Property(T_MSG_PROP_1_NAME, T_MSG_PROP_1_VALUE));
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        receipt1 = new Receipt();
        receipt1.setMessageId(T_MSG_ID_2);
        receipt1.setTimestamp(new Date());
        receipt1.setPModeId(T_PMODEID_1);
        receipt1.setDirection(Direction.IN);
        receipt1.setRefToMessageId(T_MSG_ID_1);
        receipt1.setProcessingState(ProcessingState.PROCESSING);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        userMsg2 = new UserMessage();
        userMsg2.setMessageId(T_MSG_ID_3);
        userMsg2.setTimestamp(new Date());
        userMsg2.setPModeId(T_PMODEID_1);
        userMsg2.setDirection(Direction.OUT);
        userMsg2.setProcessingState(ProcessingState.SUBMITTED);
        userMsg2.setProcessingState(ProcessingState.PROCESSING);
        userMsg2.setProcessingState(ProcessingState.SENDING);
        userMsg2.setProcessingState(ProcessingState.AWAITING_RECEIPT);
        userMsg2.setProcessingState(ProcessingState.SENDING);
        userMsg2.setProcessingState(ProcessingState.AWAITING_RECEIPT);
        userMsg2.setProcessingState(ProcessingState.DELIVERED);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        receipt2 = new Receipt();
        receipt2.setMessageId(T_MSG_ID_4);
        receipt2.setTimestamp(new Date());
        receipt2.setPModeId(T_PMODEID_1);
        receipt2.setDirection(Direction.IN);
        receipt2.setRefToMessageId(T_MSG_ID_2);
        receipt2.setProcessingState(ProcessingState.DONE);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        userMsg3 = new UserMessage();
        userMsg3.setMessageId(T_MSG_ID_5);
        userMsg3.setTimestamp(new Date());
        userMsg3.setPModeId(T_PMODEID_2);
        userMsg3.setDirection(Direction.IN);
        userMsg3.setRefToMessageId(T_MSG_ID_1);
        userMsg3.setProcessingState(ProcessingState.RECEIVED);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        error3 = new ErrorMessage();
        error3.setMessageId(T_MSG_ID_6);
        error3.setTimestamp(new Date());
        error3.setPModeId(T_PMODEID_2);
        error3.setDirection(Direction.OUT);
        error3.setRefToMessageId(T_MSG_ID_5);
        error3.setProcessingState(ProcessingState.CREATED);
        EbmsError error = new EbmsError();
        error.setErrorCode(T_ERR_CODE);
        error.setErrorDetail(T_ERR_DETAIL);
        error3.addError(error);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        pull4 = new PullRequest();
        pull4.setMessageId(T_MSG_ID_7);
        pull4.setTimestamp(new Date());
        pull4.setPModeId(T_PMODEID_3);
        pull4.setDirection(Direction.OUT);
        pull4.setMPC(EbMSConstants.DEFAULT_MPC);
        pull4.setProcessingState(ProcessingState.DELIVERED);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        error4 = new ErrorMessage();
        error4.setMessageId(T_MSG_ID_5);
        error4.setTimestamp(new Date());
        error4.setPModeId(T_PMODEID_3);
        error4.setDirection(Direction.IN);
        error4.setRefToMessageId(T_MSG_ID_7);
        error4.setProcessingState(ProcessingState.DONE);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        pull5 = new PullRequest();
        pull5.setMessageId(T_MSG_ID_9);
        pull5.setTimestamp(new Date());
        pull5.setPModeId(T_PMODEID_3);
        pull5.setDirection(Direction.OUT);
        pull5.setMPC(EbMSConstants.DEFAULT_MPC);
        pull5.setProcessingState(ProcessingState.DELIVERED);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        userMsg5 = new UserMessage();
        userMsg5.setMessageId(T_MSG_ID_10);
        userMsg5.setTimestamp(new Date());
        userMsg5.setPModeId(T_PMODEID_3);
        userMsg5.setDirection(Direction.IN);
        userMsg5.setProcessingState(ProcessingState.DELIVERED);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        userMsg6 = new UserMessage();
        userMsg6.setMessageId(T_MSG_ID_11);
        userMsg6.setTimestamp(new Date());
        userMsg6.setPModeId(T_PMODEID_1);
        userMsg6.setDirection(Direction.OUT);
        userMsg6.setProcessingState(ProcessingState.SUBMITTED);
        userMsg6.setProcessingState(ProcessingState.PROCESSING);
        userMsg6.setProcessingState(ProcessingState.SENDING);
        userMsg6.setProcessingState(ProcessingState.AWAITING_RECEIPT);
        try { Thread.sleep(200); } catch (InterruptedException ex) {}

        receipt6 = new Receipt();
        receipt6.setMessageId(T_MSG_ID_4);
        receipt6.setTimestamp(new Date());
        receipt6.setPModeId(T_PMODEID_1);
        receipt6.setDirection(Direction.OUT);
        receipt6.setRefToMessageId(T_MSG_ID_11);
        receipt6.setProcessingState(ProcessingState.DELIVERY_FAILED);
        // Adding content data
        final QName RECEIPT_CHILD_ELEMENT_NAME = new QName("ReceiptChild");
        org.apache.axiom.soap.SOAPEnvelope env = createEnvelope(SOAPVersion.SOAP_12);
        OMElement receiptChildElement =
                env.getOMFactory().createOMElement(RECEIPT_CHILD_ELEMENT_NAME);
        receiptChildElement.setText("eb3:UserMessage");
        ArrayList<OMElement> receiptContent = new ArrayList<>();
        receiptContent.add(receiptChildElement);
        receipt6.setContent(receiptContent);
    }

    enum SOAPVersion { SOAP_11, SOAP_12 }

    private static org.apache.axiom.soap.SOAPEnvelope createEnvelope(final SOAPVersion v) {
        SOAPFactory omFactory = null;
        // Check which SOAP version to use
        if (v == SOAPVersion.SOAP_11) {
            omFactory = OMAbstractFactory.getSOAP11Factory();
        } else {
            omFactory = OMAbstractFactory.getSOAP12Factory();
        }
        final org.apache.axiom.soap.SOAPEnvelope envelope = omFactory.getDefaultEnvelope();
        declareNamespaces(envelope);
        return envelope;
    }

    private static void declareNamespaces(final org.apache.axiom.soap.SOAPEnvelope envelope) {
        // Declare all namespaces that are needed by default
        envelope.declareNamespace("http://www.w3.org/1999/XMLSchema-instance/", "xsi");
        envelope.declareNamespace("http://www.w3.org/1999/XMLSchema", "xsd");
        envelope.declareNamespace(EbMSConstants.EBMS3_NS_URI, EbMSConstants.EBMS3_NS_PREFIX);
    }

    /**
     * Creates a new test set of several message units
     */
    public static void createTestSet() throws PersistenceException {
        EntityManager em = null;
        try {
            // First clean the database
            em = EntityManagerUtil.getEntityManager();
            em.getTransaction().begin();
            final Collection<MessageUnit> allMU = em.createQuery("from MessageUnit", MessageUnit.class).getResultList();
            for(final MessageUnit mu : allMU)
                em.remove(mu);
            em.getTransaction().commit();

            // Then add the test data
            em.getTransaction().begin();
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.UserMessage(userMsg1), 11));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.Receipt(receipt1), 9));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.UserMessage(userMsg2), 8));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.Receipt(receipt2), 8));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.Receipt(receipt2), 8));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.UserMessage(userMsg3), 5));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.ErrorMessage(error3), 5));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.PullRequest(pull4), 4));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.ErrorMessage(error4), 4));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.PullRequest(pull5), 3));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.UserMessage(userMsg5), 3));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.UserMessage(userMsg6), 0));
            em.persist(modifyLastStateChange(new org.holodeckb2b.persistency.jpa.Receipt(receipt6), 0));
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

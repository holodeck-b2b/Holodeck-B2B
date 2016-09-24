package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.SOAPEnv;
import org.holodeckb2b.ebms3.packaging.UserMessage;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.testhelpers.HolodeckCore;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created at 23:28 21.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ReadUserMessageTest {

    private static String baseDir;

    private static final String ENVELOPE
            = "<?xml version='1.0' encoding='utf-8'?>"
            + "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\""
            + " xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\""
            + " xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\""
            + " xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance/\">"
            + "<soapenv:Body/>"
            + "</soapenv:Envelope>";

    private static final String ENVELOPE_WITH_HEADER_AND_MESSAGING
            = "<?xml version='1.0' encoding='utf-8'?>"
            + "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\""
            + " xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\""
            + " xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\""
            + " xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance/\">"
            + "<soapenv:Header>"
            + "<eb3:Messaging soapenv:mustUnderstand=\"true\"/>"
            + "</soapenv:Header>"
            + "<soapenv:Body/>"
            + "</soapenv:Envelope>";

    private static final String UM_TAG = "<eb3:UserMessage>";

    private static final String UM_TAG_WITH_XMLNS =
            "<eb3:UserMessage xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">";

    private static final String USER_MESSAGE_NO_PREFIX
            = "<eb3:MessageInfo>"
            + "<eb3:Timestamp>2014-04-18T11:50:32.000Z</eb3:Timestamp>"
            + "<eb3:MessageId>n-soaDLzuliyRmzSlBe7</eb3:MessageId>"
            + "</eb3:MessageInfo>"
            + "<eb3:PartyInfo>"
            + "<eb3:From>"
            + "<eb3:PartyId>IYYUUHdhdh73773299HHHhdn</eb3:PartyId>"
            + "<eb3:Role>mODnY0XJN-</eb3:Role>"
            + "</eb3:From>"
            + "<eb3:To>"
            + "<eb3:PartyId type=\"pGksuoWh6B_Bhh4efISydLBgaaD316\">TUojxGOtP6vcbUr</eb3:PartyId>"
            + "<eb3:PartyId type=\"IDfGubOn2Mmvr4_lUWBVefSSkfto3t\">pjtrp</eb3:PartyId>"
            + "<eb3:Role>WdVd89s9fDz6T</eb3:Role>"
            + "</eb3:To>"
            + "</eb3:PartyInfo>"
            + "<eb3:CollaborationInfo>"
            + "<eb3:AgreementRef type=\"sdbLV\" pmode=\"QtzizhtL.QZg3UXFvby7tXDE2FL\">yklQbULTiTmY-b6pXztLqtbU9H2uUW</eb3:AgreementRef>"
            + "<eb3:Service type=\"Ii6\">yvuA3im</eb3:Service>"
            + "<eb3:Action>:HkhBfdK</eb3:Action>"
            + "<eb3:ConversationId>BHa-xmy_</eb3:ConversationId>"
            + "</eb3:CollaborationInfo>"
            + "<eb3:MessageProperties>"
            + "<eb3:Property name=\"TPlbNKkRtP4rbcdZeY\">y1</eb3:Property>"
            + "<eb3:Property name=\"LuJUQ0J1-\">sWkOqek8-iNy_kNLcpS_jBiM.Q_</eb3:Property>"
            + "</eb3:MessageProperties>"
            + "<eb3:PayloadInfo>"
            + "<eb3:PartInfo/>"
            + "<eb3:PartInfo href=\"http://pcVJBuTT/\">"
            + "<eb3:Schema location=\"http://KFfZaFTi/\" version=\"uC\" namespace=\"E9eUYc92\"/>"
            + "<eb3:Description xml:lang=\"en-CA\">XDbQu5r2xVbSEW57D32O5lw</eb3:Description>"
            + "<eb3:PartProperties>"
            + "<eb3:Property name=\"ozJjzK1OZJEF\">iKE_IOXWIDdk._sk3S</eb3:Property>"
            + "<eb3:Property name=\"VCHWFAqaEiadKr2F-\">rIBv7u3T05CVNDyov8e-</eb3:Property>"
            + "</eb3:PartProperties>"
            + "</eb3:PartInfo>"
            + "</eb3:PayloadInfo>"
            + "</eb3:UserMessage>";

    private static final String USER_MESSAGE = UM_TAG + USER_MESSAGE_NO_PREFIX;

    private static final String USER_MESSAGE_WITH_XMLNS = UM_TAG_WITH_XMLNS + USER_MESSAGE_NO_PREFIX;

    private static final String ENVELOPE_WITH_HEADER_MESSAGING_AND_USER_MESSAGE
            = "<?xml version='1.0' encoding='utf-8'?>"
            + "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\""
            + " xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\""
            + " xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\""
            + " xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance/\">"
            + "<soapenv:Header>"
            + "<eb3:Messaging soapenv:mustUnderstand=\"true\">"
            + USER_MESSAGE
            + "</eb3:Messaging>"
            + "</soapenv:Header>"
            + "<soapenv:Body/>"
            + "</soapenv:Envelope>";

    private ReadUserMessage handler;

    @BeforeClass
    public static void setUpClass() {
        baseDir = ReadUserMessageTest.class.getClassLoader().getResource("multihop").getPath();
        HolodeckB2BCoreInterface.setImplementation(new HolodeckCore(baseDir));
    }

    @Before
    public void setUp() throws Exception {
        handler = new ReadUserMessage();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testProcessing() {
        final String mmdPath =
                this.getClass().getClassLoader()
                        .getResource("multihop/icloud/full_mmd.xml").getPath();
        final File f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }

        // Creating SOAP envelope
        SOAPEnvelope env =
                SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        assertEquals(ENVELOPE, env.toString());
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(env);

        assertEquals(ENVELOPE_WITH_HEADER_AND_MESSAGING, env.toString());
        // Adding UserMessage from mmd
        OMElement userMessage = UserMessage.createElement(headerBlock, mmd);
        assertEquals(USER_MESSAGE_WITH_XMLNS, userMessage.toString());

        assertEquals(ENVELOPE_WITH_HEADER_MESSAGING_AND_USER_MESSAGE, env.toString());

        MessageContext mc = new MessageContext();

        // Setting input message property
        mc.setProperty(MessageContextProperties.IN_USER_MESSAGE, userMessage);
        try {
            mc.setEnvelope(env);
        } catch (AxisFault axisFault) {
            fail(axisFault.getMessage());
        }

        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        System.out.println("messaging: " + messaging);
        assertNotNull(messaging);

        final Iterator<?> it = UserMessage.getElements(messaging);

        assertNotNull(it);
        assertTrue(it.hasNext());

        final OMElement umElement = (OMElement) it.next();

        assertEquals(USER_MESSAGE_WITH_XMLNS, umElement.toString());

        try {
            assertNotNull(messaging);
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
            Handler.InvocationResponse doProcessingResp = handler.doProcessing(mc);
            assertNotNull(doProcessingResp);
        } catch (Exception e) {
            System.out.println("e class: " + e.getClass());
            fail(e.getMessage());
        }
    }
}

package org.holodeckb2b.testhelpers;

import org.holodeckb2b.interfaces.general.EbMSConstants;

/**
 * Helper interface which contains messages parts.
 *
 * Created at 13:31 24.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public interface MP {

        String ENVELOPE
                = "<?xml version='1.0' encoding='utf-8'?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\""
                + " xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\""
                + " xmlns:eb3=\"" + EbMSConstants.EBMS3_NS_URI + "\""
                + " xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance/\">"
                + "<soapenv:Body/>"
                + "</soapenv:Envelope>";

        String ENVELOPE_WITH_HEADER_AND_MESSAGING
                = "<?xml version='1.0' encoding='utf-8'?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\""
                + " xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\""
                + " xmlns:eb3=\"" + EbMSConstants.EBMS3_NS_URI + "\""
                + " xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance/\">"
                + "<soapenv:Header>"
                + "<eb3:Messaging soapenv:mustUnderstand=\"true\"/>"
                + "</soapenv:Header>"
                + "<soapenv:Body/>"
                + "</soapenv:Envelope>";

        String UM_TAG = "<eb3:UserMessage>";

        String UM_TAG_WITH_XMLNS =
                "<eb3:UserMessage xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">";

        String USER_MESSAGE_NO_PREFIX
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

        String USER_MESSAGE = UM_TAG + USER_MESSAGE_NO_PREFIX;

        String USER_MESSAGE_WITH_XMLNS = UM_TAG_WITH_XMLNS + USER_MESSAGE_NO_PREFIX;

        String ENVELOPE_WITH_HEADER_MESSAGING_AND_USER_MESSAGE
                = "<?xml version='1.0' encoding='utf-8'?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\""
                + " xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\""
                + " xmlns:eb3=\"" + EbMSConstants.EBMS3_NS_URI + "\""
                + " xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance/\">"
                + "<soapenv:Header>"
                + "<eb3:Messaging soapenv:mustUnderstand=\"true\">"
                + USER_MESSAGE
                + "</eb3:Messaging>"
                + "</soapenv:Header>"
                + "<soapenv:Body/>"
                + "</soapenv:Envelope>";
}

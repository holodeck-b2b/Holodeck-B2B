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
package org.holodeckb2b.ebms3.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Created at 23:17 29.01.17
 *
 * Checked for cases coverage (25.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CollaborationInfoElementTest extends AbstractPackagingTest {

    private static final QName COLLABORATION_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "CollaborationInfo");
    private static final QName AGREEMENT_REF_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "AgreementRef");
    private static final QName SERVICE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Service");
    private static final QName ACTION_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Action");
    private static final QName CONVERSATIONID_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "ConversationId");


    @Test
    public void testCreateElement() throws Exception {
        CollaborationInfo ciData = new CollaborationInfo();
        String aName = "agreement_name";
        String aType = "agreement_type";
        String aPmode = "some_pmode_id";
        ciData.setAgreement(new AgreementReference(aName, aType, aPmode));
        ciData.setService(new Service("PackagingTest"));
        ciData.setAction("some action");
        ciData.setConversationId("conv id");
        OMElement ciElement = CollaborationInfoElement.createElement(createParent(), ciData);
        assertNotNull(ciElement);
        assertEquals(COLLABORATION_INFO_ELEMENT_NAME, ciElement.getQName());
        OMElement arElement = AgreementRefElement.getElement(ciElement);
        assertEquals(AGREEMENT_REF_INFO_ELEMENT_NAME, arElement.getQName());
        assertEquals(aName, arElement.getText());
        assertEquals(aType,
                arElement.getAttribute(new QName("type")).getAttributeValue());
        assertEquals(aPmode,
                arElement.getAttribute(new QName("pmode")).getAttributeValue());
        OMElement sElement = ServiceElement.getElement(ciElement);
        assertEquals(SERVICE_ELEMENT_NAME, sElement.getQName());

        assertNotNull(ciElement.getChildrenWithName(ACTION_ELEMENT_NAME).next());
        assertNotNull(ciElement.getChildrenWithName(
                CONVERSATIONID_ELEMENT_NAME).next());
    }

    @Test
    public void testGetElement() throws Exception {
    	OMElement ciElement = CollaborationInfoElement.getElement(createXML(
        		"<parent>" +
				"    <eb3:CollaborationInfo" +
				"        xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">\n" + 
				"        <eb3:AgreementRef>http://agreements.holodeckb2b.org/examples/agreement0</eb3:AgreementRef>\n" + 
				"        <eb3:Service type=\"org:holodeckb2b:services\">PackagingTest</eb3:Service>\n" + 
				"        <eb3:Action>GetElement</eb3:Action>\n" + 
				"        <eb3:ConversationId>org:holodeckb2b:test:conversation</eb3:ConversationId>\n" + 
				"    </eb3:CollaborationInfo>"
				+ "</parent>"));
        assertNotNull(ciElement);
        assertEquals(COLLABORATION_INFO_ELEMENT_NAME, ciElement.getQName());    	
    }

    @Test
    public void testReadElement() throws Exception {
    	String action = "ReadElement";
    	String convID = UUID.randomUUID().toString();
    	
        CollaborationInfo collaborationInfo = CollaborationInfoElement.readElement(createXML(
				"    <eb3:CollaborationInfo" +
				"        xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">\n" + 
				"        <eb3:AgreementRef>http://agreements.holodeckb2b.org/examples/agreement0</eb3:AgreementRef>\n" + 
				"        <eb3:Service type=\"org:holodeckb2b:services\">PackagingTest</eb3:Service>\n" + 
				"        <eb3:Action>" + action + "</eb3:Action>\n" + 
				"        <eb3:ConversationId>" + convID + "</eb3:ConversationId>\n" + 
				"    </eb3:CollaborationInfo>"       		
        		));
        
        assertNotNull(collaborationInfo);
        assertNotNull(collaborationInfo.getService());
        assertEquals(action, collaborationInfo.getAction());
        assertEquals(convID, collaborationInfo.getConversationId());
    }
 }
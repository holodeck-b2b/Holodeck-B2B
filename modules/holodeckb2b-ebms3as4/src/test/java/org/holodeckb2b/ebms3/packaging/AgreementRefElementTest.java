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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Test;

/**
 * Created at 23:18 29.01.17
 *
 * Checked for cases coverage (25.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class AgreementRefElementTest extends AbstractPackagingTest {

    private static final QName AGREEMENT_REF_INFO_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "AgreementRef");

    @Test
    public void testCreateElement() throws Exception {
        String name = "agreement_name";
        String type = "agreement_type";
        String pmodeId = "some pmode id string";
        AgreementReference agreementReference = new AgreementReference(name, type, pmodeId);

        OMElement agreementRefElement = AgreementRefElement.createElement(createParent(), agreementReference);
        
        assertEquals(AGREEMENT_REF_INFO_ELEMENT_NAME, agreementRefElement.getQName());
        assertEquals(name, agreementRefElement.getText());
        assertEquals(type, agreementRefElement.getAttributeValue(new QName("type")));
        assertEquals(pmodeId, agreementRefElement.getAttributeValue(new QName("pmode")));
    }

    @Test
    public void testGetElement() throws Exception {    	
	    OMElement agreementRefElement = AgreementRefElement.getElement(createXML(
        		"<parent>"
        		+ "<eb3:AgreementRef xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\">"
        				+ "http://agreements.holodeckb2b.org/examples/agreement0</eb3:AgreementRef>"
				+ "</parent>"));
        assertNotNull(agreementRefElement);
        assertEquals(AGREEMENT_REF_INFO_ELEMENT_NAME, agreementRefElement.getQName());
    }

    @Test
    public void testReadElement() throws Exception {
    	String name = "agreement_name";
        String type = "agreement_type";
        String pmodeId = "some pmode id string";
        
        AgreementReference agreementReference = AgreementRefElement.readElement(createXML(
        		"<eb3:AgreementRef xmlns:eb3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\""
        		+ " type=\"" + type + "\" pmode=\"" + pmodeId + "\">"
        		+ name + "</eb3:AgreementRef>"));

        assertNotNull(agreementReference);
        assertEquals(name, agreementReference.getName());
        assertEquals(type, agreementReference.getType());
        assertEquals(pmodeId, agreementReference.getPModeId());
    }
}
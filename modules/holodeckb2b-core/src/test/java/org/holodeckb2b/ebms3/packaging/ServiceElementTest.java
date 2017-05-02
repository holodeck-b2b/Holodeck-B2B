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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.core.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;

import static org.junit.Assert.assertEquals;

/**
 * Created at 15:18 19.02.17
 *
 * Checked for cases coverage (30.04.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ServiceElementTest {

    private static final QName SERVICE_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Service");

    private OMElement umElement;

    @Before
    public void setUp() throws Exception {
        MessageMetaData mmd = TestUtils.getMMD("packagetest/mmd_pcktest.xml", this);
        // Creating SOAP envelope
        SOAPEnvelope soapEnvelope = SOAPEnv.createEnvelope(SOAPEnv.SOAPVersion.SOAP_12);
        // Adding header
        SOAPHeaderBlock headerBlock = Messaging.createElement(soapEnvelope);
        // Adding UserMessage from mmd
        umElement = UserMessageElement.createElement(headerBlock, mmd);
    }

    @Test
    public void testCreateElement() throws Exception {
        CollaborationInfo ciData = new CollaborationInfo();
        ciData.setService(new Service());
        OMElement ciElement =
                CollaborationInfoElement.createElement(umElement, ciData);
        Service service = new Service("some_name", "some_type");

        OMElement sElement = ServiceElement.createElement(ciElement, service);

        assertEquals(SERVICE_ELEMENT_NAME, sElement.getQName());
        assertEquals("some_type", sElement.getAttributeValue(new QName("type")));
        assertEquals("some_name", sElement.getText());
    }

    @Test
    public void testGetElement() throws Exception {
        CollaborationInfo ciData = new CollaborationInfo();
        ciData.setService(new Service("some_name", "some_type"));
        OMElement ciElement =
                CollaborationInfoElement.createElement(umElement, ciData);

        OMElement sElement = ServiceElement.getElement(ciElement);

        assertEquals(SERVICE_ELEMENT_NAME, sElement.getQName());
        assertEquals("some_type", sElement.getAttributeValue(new QName("type")));
        assertEquals("some_name", sElement.getText());
    }

    @Test
    public void testReadElement() throws Exception {
        CollaborationInfo ciData = new CollaborationInfo();
        ciData.setService(new Service());
        OMElement ciElement =
                CollaborationInfoElement.createElement(umElement, ciData);
        Service service = new Service("some_name", "some_type");

        OMElement sElement = ServiceElement.createElement(ciElement, service);

        service = ServiceElement.readElement(sElement);

        assertEquals("some_name", service.getName());
        assertEquals("some_type", service.getType());
    }
}
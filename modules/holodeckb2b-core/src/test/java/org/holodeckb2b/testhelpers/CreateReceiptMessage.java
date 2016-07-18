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
package org.holodeckb2b.testhelpers;

import java.io.StringReader;
import java.util.Date;
import javax.persistence.EntityManager;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistency.entities.Receipt;
import org.holodeckb2b.ebms3.persistency.entities.ProcessingState;
import org.holodeckb2b.ebms3.persistent.dao.JPAUtil;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CreateReceiptMessage {
    
    public static void main(String args[]) {
        EntityManager   em = JPAUtil.getEntityManagerToAlpha();
        
        Receipt    newRcptMsg = new Receipt();
        newRcptMsg.setMessageId("this-is-not-a-real-msg-id@just.for.test.holodeck");
        newRcptMsg.setRefToMessageId("this-is-a-fake-refto-msg-id@just.for.test.holodeck");
        newRcptMsg.setTimestamp(new Date());
        newRcptMsg.setPMode("PMODE-JUST-FOR-TEST-RECEIPT-BUNDLING");
        
        String content = "<content>" +
                            "<confirmation>\n" +
                            "    <from>Party_X</from>\n" +
                            "    <message>Success</message>\n" +
                            "</confirmation>\n" +
                         "</content>";        
        OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(new StringReader(content));
        // Parse document and get root element
        OMElement contentElement = builder.getDocumentElement();

        newRcptMsg.setContent(contentElement.getChildElements());
            
        ProcessingState state = new ProcessingState(ProcessingStates.CREATED);
        newRcptMsg.setProcessingState(state);
        
        em.getTransaction().begin();
        em.persist(newRcptMsg);
        em.getTransaction().commit();
        
        System.out.println("Added receipt message to database!");
    }
}

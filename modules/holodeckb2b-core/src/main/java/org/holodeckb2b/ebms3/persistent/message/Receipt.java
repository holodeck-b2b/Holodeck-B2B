/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.persistent.message;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.holodeckb2b.common.messagemodel.IReceipt;

/**
 * Is a persistency class representing an ebMS Receipt message unit that is processed 
 * by Holodeck B2B. 
 * <p>NOTE: Storage of the receipt content, i.e. the XML child elements, is currently 
 * done by storing this information as a large String object by serializing the XML. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Entity
@Table(name="RECEIPT")
@DiscriminatorValue("RECEIPT")
@NamedQueries({
        @NamedQuery(name="Receipt.findForPModesInState",
            query = "SELECT r " +
                    "FROM Receipt r JOIN r.states s1 " +
                    "WHERE r.PMODE_ID IN :pmodes " +
                    "AND s1.START = (SELECT MAX(s2.START) FROM r.states s2) " +
                    "AND s1.NAME = :state " +
                    "ORDER BY s1.START"  
            ),
        @NamedQuery(name="Receipt.findResponsesTo",
            query = "SELECT r " +
                    "FROM Receipt r " +
                    "WHERE r.REF_TO_MSG_ID = :refToMsgId " 
            )
        }
)
public class Receipt extends SignalMessage implements IReceipt {

    /**
     * The XML content from the receipt is wrapped in a single XML element for
     * easy serialization and deserialization. This constant defines the used
     * element name.
     */
    private static final QName XML_CONTENT_QNAME = new QName("receipt_content");
    
    /*
     * Getters and setters
     */
    
    public ArrayList<OMElement> getContent() {
        if (xmlContent == null || (CONTENT != null && !CONTENT.isEmpty() && xmlContent.isEmpty())) {
            // Data has not been deserialized yet
            // Create parser to get XML from database string
            OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(new StringReader(CONTENT));
            // Parse document and get root element
            OMElement contentElement = builder.getDocumentElement();
            // Now the children of this element are the actual content of the Receipt
            xmlContent = new ArrayList<OMElement>();
            Iterator<?> it = contentElement.getChildElements();
            while (it.hasNext())
                xmlContent.add((OMElement) it.next());
        }
        
        return xmlContent;
    }
    
    public void setContent(Iterator<OMElement> elements) {
        // Serialize to database object
        // Create a "placeholder" element for all children of the Receipt element
        OMElement c = OMAbstractFactory.getOMFactory().createOMElement(XML_CONTENT_QNAME);
        // add all elements to it
        while (elements.hasNext())
            c.addChild(elements.next().cloneOMElement());
        
        try {
            // Now serialize the placeholder element to database
            CONTENT = c.toStringWithConsume();
        } catch (XMLStreamException ex) {
            CONTENT = null; 
        }
    }
    
    /*
     * Fields
     * 
     * NOTES: 
     * 1) The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     * 2) The primary key field is inherited from super class
     */
        
    /*
     * The authentication info is saved by serializing the <code>IAuthenticationInfo</code>
     * object, therefor this column is a binary object, for easy access also 
     * a <i>transient</i> object is defined to store the object temporarely as
     * an object
     */
    @Column(name = "CONTENT", length = 10000, nullable = false)
    private String          CONTENT;
    
    @Transient
    private ArrayList<OMElement>    xmlContent;
}


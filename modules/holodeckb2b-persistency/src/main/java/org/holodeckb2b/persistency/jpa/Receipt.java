/**
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
package org.holodeckb2b.persistency.jpa;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;

/**
 * Is the JPA entity class for storing the meta-data of an ebMS <b>Receipt Signal</b> message unit as described by the
 * {@link IReceiptEntity} interface in the Holodeck B2B persistency model. The class however does not
 * implement this interface as it is not the actual entity provided to the Core.
 * <p>As the actual XML elements that form the Receipt's content are unknown (because not spec'd) all are wrapped in a
 * container element and converted to a String. The maximum length of the String is set to 65535 characters.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
@Entity
@Table(name="RECEIPT")
@DiscriminatorValue("RECEIPT")
public class Receipt extends MessageUnit implements IReceipt {
    private static final long serialVersionUID = -1475865816627014255L;

	/**
     * The XML content from the receipt is wrapped in a single XML element for easy serialization and deserialization.
     * This constant defines the used container element name.
     */
    private static final QName XML_CONTENT_QNAME = new QName("receipt_content");

    /*
     * Getters and setters
     */

    public ArrayList<OMElement> getContent() {
        if (Utils.isNullOrEmpty(xmlContent) && !Utils.isNullOrEmpty(CONTENT)) {
            // Data has not been deserialized yet
            // Create parser to get XML from database string
            final OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(new StringReader(CONTENT));
            // Parse document and get root element
            final OMElement contentElement = builder.getDocumentElement();
            // Now the children of this element are the actual content of the Receipt
            xmlContent = new ArrayList<>();
            final Iterator<?> it = contentElement.getChildElements();
            while (it.hasNext())
                xmlContent.add((OMElement) it.next());
        }

        return xmlContent;
    }

    public void setContent(final List<OMElement> content) {
        if (Utils.isNullOrEmpty(content)) {
            xmlContent = null;
            CONTENT = null;
        } else {
            xmlContent = new ArrayList<>();
            // Copy XML elements to instance variable and add them to container element for serialization to database
            // Create a "placeholder" element for all children of the Receipt element
            final OMElement c = OMAbstractFactory.getOMFactory().createOMElement(XML_CONTENT_QNAME);
            // add all elements to it
            for (OMElement e : content) {
                OMElement clone = e.cloneOMElement();
                xmlContent.add(clone);
                c.addChild(clone);
            }
            try {
                // Now serialize the placeholder element to database
                CONTENT = c.toStringWithConsume();
            } catch (final XMLStreamException ex) {
                CONTENT = null;
            }
        }
    }

    /*
     * Constructors
     */
    /**
     * Default constructor creates a new empty <code>Receipt</code> object
     */
    public Receipt() {}

    /**
     * Creates a new <code>Receipt</code> object using the data provided in the given source object
     *
     * @param source    The source object to get the data from
     */
    public Receipt(final IReceipt source) {
        super(source);

        if (source == null)
            return;
        else
            setContent(source.getContent());
    }

    /*
     * The authentication info is saved by serializing the <code>IAuthenticationInfo</code>
     * object, therefor this column is a binary object, for easy access also
     * a <i>transient</i> object is defined to store the object temporarely as
     * an object
     */
    @Lob
    @Column(length = 65535)
    private String          CONTENT;

    @Transient
    private ArrayList<OMElement>    xmlContent;
}


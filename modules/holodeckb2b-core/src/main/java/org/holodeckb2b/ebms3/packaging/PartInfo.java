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
package org.holodeckb2b.ebms3.packaging;

import java.util.Collection;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.ebms3.persistent.message.Payload;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.messagemodel.IPayload;

/**
 * Is a helper class for handling the ebMS PartInfo element in the ebMS SOAP 
 * header.
 * <p>This element is specified in section 5.2.2.13 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PartInfo {
    
    /**
     * The fully qualified name of the element as an {@see QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "PartInfo");
    
    /**
     * The local name of the href attribute
     */
    private static final String HREF_ATTR = "href";
    
    /**
     * Creates a <code>PartInfo</code> element and adds it to the given <code>PayloadInfo</code> element. 
     * 
     * @param plElement     The <code>PayloadInfo</code> element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(OMElement plElement, IPayload data) {
        OMFactory f = plElement.getOMFactory();
        
        // Create the element
        OMElement piElement = f.createOMElement(Q_ELEMENT_NAME, plElement);
        
        // Fill it based on the given data
        
        // href attribute
        String href = data.getPayloadURI();
        if (href != null && !href.isEmpty()) {
            // Add prefixes for different reference
            if (IPayload.Containment.ATTACHMENT == data.getContainment())
                href = "cid:" + href;
            else if (IPayload.Containment.EXTERNAL != data.getContainment())
                href = "#" + href;
        
            piElement.addAttribute(HREF_ATTR, href, null);
        }
        
        // Create the Schema element (if schema reference is provided)
        ISchemaReference schemaRef = data.getSchemaReference();
        if (schemaRef != null)
            Schema.createElement(piElement, schemaRef);
        // Create the Description element (if a description is provided)
        IDescription descr = data.getDescription();
        if (descr != null)
            Description.createElement(piElement, descr);
        // Create the PartProperties element (if there are message properties)
        Collection<IProperty> partProps = data.getProperties();
        if (partProps != null && partProps.size() > 0)
            PartProperties.createElement(piElement, partProps);
        
        return piElement;
    }
    
    /**
     * Gets the {@link OMElement} object that represent the <code>PartInfo</code> 
     * child element of the <code>UserMessage</code> element.
     * 
     * @param umElement     The parent <code>UserMessage</code> element 
     * @return              The {@link OMElement} object representing the requested element
     *                      or <code>null</code> when the requested element is not found as
     *                      child of the given element.
     */
    public static OMElement getElement(OMElement umElement) {
        return umElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }
    
    /**
     * Reads the meta data about one payload from the <code>PartInfo</code> element 
     * and returns it as a {@link org.holodeckb2b.ebms3.persistent.message.Payload}
     * entity object.
     * <p><b>NOTE:</b> The entity object is not persisted by this method! It is 
     * the responsibility of the caller to store it.
     * 
     * @param piElement             The <code>PartInfo</code> element to read the
     *                              payload meta data from
     * @return                      A new {@link org.holodeckb2b.ebms3.persistent.message.Payload} 
     *                              object containing the meta data about the payload
     * @throws PackagingException   When the given element is not a valid
     *                              <code>PartInfo</code> element.
     */    
    public static Payload readElement(OMElement piElement) throws PackagingException {
        if (piElement == null)
            return null;
        
        // Create the entity object
        Payload plData = new Payload();
        
        // Read href attribute
        String href = piElement.getAttributeValue(new QName(HREF_ATTR));
        if (href == null || href.isEmpty()) 
            plData.setContainment(IPayload.Containment.BODY);
        else {
            if (href.startsWith("#")) {
                plData.setContainment(IPayload.Containment.BODY);
                href = href.substring(1);
            } else if (href.startsWith("cid:")) {
                plData.setContainment(IPayload.Containment.ATTACHMENT);
                href = href.substring(4);
            } else
                plData.setContainment(IPayload.Containment.EXTERNAL);
                
            plData.setPayloadURI(href);
        }

        // Read and proces Schema element (optional)
        plData.setSchemaReference(Schema.readElement(Schema.getElement(piElement)));
        // Read and proces Description element (optional)
        plData.setDescription(Description.readElement(Description.getElement(piElement)));
        // Read and proces the PartProperties element (optional)
        plData.setProperties(PartProperties.readElement(PartProperties.getElement(piElement)));
        
        return plData;
    }
}

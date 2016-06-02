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

import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.ebms3.persistency.entities.SchemaReference;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.ISchemaReference;

/**
 * Is a helper class for handling the ebMS Schema element in the ebMS SOAP 
 * header.
 * <p>This element is specified in section 5.2.2.13 of the ebMS 3 Core specification.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Schema {
    
    /**
     * The fully qualified name of the element as an {@link QName}
     */
    static final QName  Q_ELEMENT_NAME = new QName(EbMSConstants.EBMS3_NS_URI, "Schema");
    
    // The local name of the location attribute
    private static final String LN_ATTR_LOCATION = "location";
    
    // The local name of the version attribute
    private static final String LN_ATTR_VERSION = "version";

    // The local name of the namespace attribute
    private static final String LN_ATTR_NAMESPACE = "namespace";

    /**
     * Creates a <code>Schema</code> element and adds it to the given <code>PartInfo</code> element. 
     * 
     * @param piElement     The <code>PartInfo</code> element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(OMElement piElement, ISchemaReference data) {
        OMFactory f = piElement.getOMFactory();
        
        // Create the element
        OMElement schema = f.createOMElement(Q_ELEMENT_NAME, piElement);
        
        // Set attributes if data is specified for it
        schema.addAttribute(LN_ATTR_LOCATION, data.getLocation(), null); // location is required
        
        String version = data.getVersion();
        if ( version != null && !version.isEmpty())
            schema.addAttribute(LN_ATTR_VERSION, version, null);
        String namespace = data.getNamespace();
        if ( namespace != null && !namespace.isEmpty())
            schema.addAttribute(LN_ATTR_NAMESPACE, namespace, null);
        
        return schema;
    }
    
    /**
     * Gets the {@link OMElement} object that represent the <code>Schema</code> 
     * child element of the <code>PartInfo</code> element.
     * 
     * @param umElement     The parent <code>PartInfo</code> element 
     * @return              The {@link OMElement} object representing the requested element
     *                      or <code>null</code> when the requested element is not found as
     *                      child of the given element.
     */
    public static OMElement getElement(OMElement umElement) {
        return umElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }    
    
    /**
     * Reads the schema data about the payload from the <code>Schema</code> element 
     * and returns it as a {@link org.holodeckb2b.ebms3.persistency.entities.SchemaReference}
     * entity object.
     * <p><b>NOTE:</b> The entity object is not persisted by this method! It is 
     * the responsibility of the caller to store it.
     * 
     * @param siElement             The <code>Schema</code> element to read the
     *                              payload meta data from
     * @return                      A new {@link org.holodeckb2b.ebms3.persistency.entities.SchemaReference} 
     *                              object containing the meta data about the payload
     * @throws PackagingException   When the given element is not a valid
     *                              <code>Schema</code> element.
     */    
    public static SchemaReference readElement(OMElement siElement) throws PackagingException {
        if (siElement == null)
            return null;
        
        // Read the required location attribute
        String location = siElement.getAttributeValue(new QName(LN_ATTR_LOCATION));
        if (location == null || location.isEmpty())
            throw new PackagingException("Location attribute is required in Schema element");
        
        // Create entity object
        SchemaReference schemaData = new SchemaReference();
        schemaData.setLocation(location);
        
        // Read the optional version and namespace attributes
        schemaData.setNamespace(siElement.getAttributeValue(new QName(LN_ATTR_NAMESPACE)));
        schemaData.setVersion(siElement.getAttributeValue(new QName(LN_ATTR_VERSION)));
        
        return schemaData;
    }    
}

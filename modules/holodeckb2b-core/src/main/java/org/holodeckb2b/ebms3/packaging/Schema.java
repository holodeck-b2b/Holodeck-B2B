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
import org.holodeckb2b.common.messagemodel.SchemaReference;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.ISchemaReference;

/**
 * Is a helper class for handling the <code>Schema</code> element in the ebMS SOAP header.
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
    public static OMElement createElement(final OMElement piElement, final ISchemaReference data) {
        final OMFactory f = piElement.getOMFactory();

        // Create the element
        final OMElement schema = f.createOMElement(Q_ELEMENT_NAME, piElement);

        // Set attributes if data is specified for it
        schema.addAttribute(LN_ATTR_LOCATION, data.getLocation(), null); // location is required

        final String version = data.getVersion();
        if ( version != null && !version.isEmpty())
            schema.addAttribute(LN_ATTR_VERSION, version, null);
        final String namespace = data.getNamespace();
        if ( namespace != null && !namespace.isEmpty())
            schema.addAttribute(LN_ATTR_NAMESPACE, namespace, null);

        return schema;
    }

    /**
     * Gets the {@link OMElement} object that represent the <code>Schema</code> child element of the <code>PartInfo
     * </code> element.
     *
     * @param piElement     The parent <code>PartInfo</code> element
     * @return              The {@link OMElement} object representing the <code>Schema</code> element or,<br>
     *                      <code>null</code> when the requested element is not found as child of the given element.
     */
    public static OMElement getElement(final OMElement piElement) {
        return piElement.getFirstChildWithName(Q_ELEMENT_NAME);
    }

    /**
     * Reads the schema data about the payload from the <code>Schema</code> element and returns it as a {@link
     * org.holodeckb2b.common.messagemodel.SchemaReference} object.
     *
     * @param siElement             The <code>Schema</code> element to read the payload meta data from
     * @return                      A new {@link org.holodeckb2b.common.messagemodel.SchemaReference} object containing
     *                              the meta data about the payload
     */
    public static SchemaReference readElement(final OMElement siElement) {
        if (siElement == null)
            return null;

        // Create entity object
        final SchemaReference schemaData = new SchemaReference();
        // Read data from element
        schemaData.setLocation(siElement.getAttributeValue(new QName(LN_ATTR_LOCATION)));
        schemaData.setNamespace(siElement.getAttributeValue(new QName(LN_ATTR_NAMESPACE)));
        schemaData.setVersion(siElement.getAttributeValue(new QName(LN_ATTR_VERSION)));

        return schemaData;
    }
}

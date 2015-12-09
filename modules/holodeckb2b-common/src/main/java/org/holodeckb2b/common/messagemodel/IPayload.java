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
package org.holodeckb2b.common.messagemodel;

import java.util.Collection;
import org.holodeckb2b.common.delivery.IMessageDeliverer;
import org.holodeckb2b.common.general.IDescription;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.general.ISchemaReference;

/**
 * Defines an interface to exchange the meta-data about payloads contained in a User Message. The payloads of a user 
 * message contain the actual business documents. Because these documents can be very large they are not stored in 
 * memory. This interface therefor only defines a method to get the location where the actual content is located. This 
 * can be used to read the actual document.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IPayload {
    
    /**
     * Enumeration to indicate how a payload is included in the message.
     * 
     * @see #getContainment() 
     */
    public enum Containment { BODY, ATTACHMENT, EXTERNAL }
    
    /**
     * Gets the indication how the payload is contained in the ebMS message. A payload can be included in the message as
     * [part of] the SOAP body, a SOAP attachment or as an external referenced. 
     * <p>This information is not part of the ebMS messaging header but can be derived from the payload reference. See 
     * section 5.2.2.13 of the ebMS Core Specification. When the message is created by Holodeck B2B either the P-Mode of
     * the user message must specify how payloads must be included or it should be specified when the user message is
     * submitted to Holodeck B2B.
     * <p>How to business document is included in the user message is mostly relevant for Holodeck B2B internally when
     * reading or packaging an ebMS message. But when the business is not contained in the message itself, but only
     * a reference to an externally located document it is up to the business application to retrieve or ensure to
     * supply it at the given location.
     * <p><b>NOTE:</b> Containment in the SOAP body if only possible for XML documents. There is no support to include
     * binary document types in an encoded form in the SOAP body.
     * 
     * @return  {@link Containment#BODY}        When the payload is included in the SOAP body<br>
     *          {@link Containment#ATTACHMENT}  When the payload is included as an attachment<br>
     *          {@link Containment#EXTERNAL}    When the payload is external to the message
     */
    public Containment getContainment();
    
    /**
     * Gets the reference to the payload content in the message. 
     * <p>Corresponds to the <code>href</code> attribute of the <code>//eb:UserMessage/eb:IPayloadInfo/eb:PartInfo</code>
     * element in the ebMS header of the message. 
     * <p><b>NOTE 1:</b> This URI describes the location of the actual content within the ebMS message, not local in 
     * Holodeck B2B or the communication between Holodeck B2B and the business application. 
     * <p><b>NOTE 2:</b> It is not necessary to include the <i>"cid:"</i> or <i>"#"</i> prefix for references to 
     * payloads contained in as an attachment or in the SOAP body. Holodeck B2B will not include these prefixes when
     * delivering messages to the business application, i.e. in the message data provided in {@link 
     * IMessageDeliverer#deliver(org.holodeckb2b.common.messagemodel.IMessageUnit)}. 
     * <p><b>NOTE 3:</b> When the payloads <i>containment</i> is {@link Containment#EXTERNAL} the content of this payload 
     * is not contained in the ebMS message itself and MUST be retrieved from or made available at this URI <b>by the 
     * business application</b>.
     * 
     * @return  The URI that references the payload content within the context of the message
     */
    public String getPayloadURI();
    
    /**
     * Gets the <i>user defined</i> payload properties. These properties are generally intended for use by the 
     * business applications involved in the message exchange and not used by Holodeck B2B in the processing of the 
     * messages. Some additional features, like the compression feature defined in the AS4 profile however use these 
     * properties to specify how a payload should be handled. 
     * <p>Corresponds to the <code>//eb:PartInfo/eb:PartProperties/eb:Property</code> elements in the ebMS header. 
     * 
     * @return  The collection of properties defined for this payload
     */
    public Collection<IProperty> getProperties();
    
    /**
     * Gets the description of the payload. 
     * <p>Corresponds to the <code>//eb:PartInfo/eb:Description</code> element in the header.
     * <p><b>NOTE: </b>This element is defined in the XSD of the ebMS specification but not described in the 
     * specification document, see also issue 31 (https://tools.oasis-open.org/issues/browse/EBXMLMSG-31?jql=project%20%3D%20EBXMLMSG)
     * This element is deprecated and should not be used any more. Instead the payload properties should be used for 
     * describing the payload. 
     * 
     * @return      An {@link IDescription} object containing the description of the payload
     * @deprecated Use the payload properties instead
     */
    @Deprecated
    public IDescription getDescription();
    
    /**
     * Gets the schema reference information for this payload. This information can be used by the business application
     * to validate the payload content. Holodeck B2B will not perform payload validation, the schema information is just
     * passed to the business application.
     * <p>Corresponds to the <code>//eb:PartInfo/eb:Schema</code> element.
     * 
     * @return      An {@link ISchemaReference} containing information on the schema that defines this payload
     */
    public ISchemaReference getSchemaReference();
    
    /**
     * Gets the location where the content of this payload is stored locally for exchange between Holodeck B2B and the 
     * business application. 
     * <p><b>NOTE: </b>When the payload is not included in the message, i.e. {@link #getContainment()} == 
     * {@link Containment#EXTERNAL}, it will not be stored locally.
     * 
     * @return  The location where the payload content is stored
     */
    public String getContentLocation();
    
    /**
     * Gets the MIME type of the payload content. The MIME type is not included in the standard meta data included in
     * the ebMS header but can be available as part of the MIME part headers that contain the payload or as <i>payload</i>
     * property with name <i>"MimeType"</i>. 
     * <p>When sending a message providing this information on submission of the user message can help Holodeck B2B in
     * packaging the message. If no information is provided Holodeck B2B will try to determine the MIME type based on
     * the payload content.<br>
     * When receiving an user message Holodeck B2B will only return the MIME type if it given in the message. It will
     * not try to detect the MIME type if it can not be derived from the message content.
     * 
     * @return  The MIME type of this payload
     */
    public String getMimeType();
}

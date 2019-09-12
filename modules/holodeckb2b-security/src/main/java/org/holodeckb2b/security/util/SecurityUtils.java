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
package org.holodeckb2b.security.util;

import static org.holodeckb2b.interfaces.security.X509ReferenceType.BSTReference;
import static org.holodeckb2b.interfaces.security.X509ReferenceType.IssuerAndSerial;
import static org.holodeckb2b.interfaces.security.X509ReferenceType.KeyIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.str.STRParser;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.holodeckb2b.security.SecurityConstants;
import org.holodeckb2b.security.results.SignedPartMetadata;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Is a container for general security related functions.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public final class SecurityUtils {

    /**
     * Converts the X509 key identifier type to the values used by the WSS4J library.
     *
     * @param refType   The key identifier reference type expressed as {@link X509ReferenceType}
     * @return          The key identifier reference type for use with the WSS4J library
     */
    public static String getWSS4JX509KeyId(final X509ReferenceType refType) {
        switch (refType) {
            case BSTReference   : return "DirectReference";
            case KeyIdentifier  : return "SKIKeyIdentifier";
            default             : return "IssuerSerial";
        }
    }

    /**
     * Converts the X509 key reference method from the values used by the WSS4J library to Holodeck B2B type
     *
     * @param wss4jRefType   The key identifier reference type expressed as used by the WSS4J library
     * @return               The key identifier reference type
     */
    public static X509ReferenceType getKeyReferenceType(final STRParser.REFERENCE_TYPE wss4jRefType) {
        switch (wss4jRefType) {
            case DIRECT_REF     : return BSTReference;
            case KEY_IDENTIFIER : return KeyIdentifier;
            case ISSUER_SERIAL  : return IssuerAndSerial;
        }
        return null;
    }

   /**
     * Generates a 16 character long random string which can be used as a password.
     *
     * @return  The generated random string
     */
    public static char[] generatePassword() {
        return Long.toHexString(Double.doubleToLongBits(Math.random())).toCharArray();
    }

    /**
     * Gets the digest information related to the ebMS header and payloads contained in the message from the <code>
     * ds:Reference</code> elements included in the default WS-Security header.
     *
     * @param domEnvelope   The DOM representation of the SOAP envelope
     * @param messageUnits  The collection of message units included in the message
     * @return The digest information related to the ebMS header and payloads
     * @throws SecurityProcessingException  When the SOAP envelope does not contain an ebMS header
     */
    public static SignedMessagePartsInfo getSignedPartsInfo(final Document domEnvelope,
                                                            final Collection<? extends IMessageUnit> messageUnits)
                                                                                    throws SecurityProcessingException {
        // Get the ds:Reference elements from the header
        Collection<Element> sigRefs = getSignatureReferences(domEnvelope);
        if (Utils.isNullOrEmpty(sigRefs)) {
            return null;
        }
        // Get the data related to the ebMS header
        final String ebMSHeaderId = getEbMSHeaderId(domEnvelope);
        SignedPartMetadata ebMSHeaderDigest = null;
        if (ebMSHeaderId != null) {
			Optional<Element> ref = sigRefs.parallelStream()
										   .filter(r -> r.getAttribute("URI").endsWith(ebMSHeaderId)).findFirst();
			ebMSHeaderDigest = ref.isPresent() ? new SignedPartMetadata(ref.get()) : null;
        }

        // For each payload try to get the applicable reference element and create the SignedPartMetadata instance
        Map<IPayload, ISignedPartMetadata> payloadDigests = new HashMap<>();
        messageUnits.stream().filter(msgUnit -> msgUnit instanceof IUserMessage)
                             .map(userMsg -> ((IUserMessage) userMsg).getPayloads())
                             .filter(umPayloads -> !Utils.isNullOrEmpty(umPayloads))
                             .forEachOrdered(umPayloads ->
                                umPayloads.forEach((p) ->
                                          sigRefs.stream()
                                          .filter(ref -> isPayloadSigned(p, ref.getAttribute("URI"), domEnvelope))
                                          .forEach(match -> payloadDigests.put(p, new SignedPartMetadata(match)))));

        return new SignedMessagePartsInfo(ebMSHeaderDigest, payloadDigests);
    }

    /**
     * Checks whether the given signature reference applies to the given payload.
     *
     * @param pl            The payload to check
     * @param ref	        The reference to check
     * @param domEnvelope   The DOM representation of the SOAP envelope
     * @return              <code>true</code> if this reference applies to a payload of the message,<br>
     *                      <code>false</code> otherwise
     * @since HB2B_NEXT_VERSION
     */
    private static boolean isPayloadSigned(final IPayload pl, final String ref, final Document domEnvelope) {
    	final String plURI = pl.getPayloadURI();
    	/* For attached payloads we can simple compare the id from the reference with the Content-id of the payload
    	 * although we need to use endsWidth since the URI in the payload object does not contain prefix.
    	 * For payloads contained in the SOAP Body it may work if the payload includes a reference to the SOAP Body 
    	 * element. However such payloads commonly don't have a reference at all and otherwise may have one that refers
    	 * to a child element in the SOAP Body which is the actual root of the payload. Therefore a body payload is
    	 * considered referenced if the WSS4J reference is to the SOAP Body 
    	 */
    	if (plURI != null && ref.endsWith(plURI))
    		return true;
    	else if (pl.getContainment() == IPayload.Containment.BODY) {
    		final Element bodyElement = WSSecurityUtil.findBodyElement(domEnvelope);
    		return bodyElement != null && ref.endsWith(getId(bodyElement));
    	} else
    		return false;
    }
    
    /**
     * Checks whether the given encrypted data reference applies to the given payload.
     *
     * @param pl            The payload to check
     * @param ref	        The reference to check
     * @return              <code>true</code> if this reference applies to a payload of the message,<br>
     *                      <code>false</code> otherwise
     * @since HB2B_NEXT_VERSION
     */
    public static boolean isPayloadEncrypted(final IPayload pl, final WSDataRef ref) {
    	final String plURI = pl.getPayloadURI();
    	/* For attached payloads we can simple compare the id from the reference with the Content-id of the payload
    	 * although we need to use endsWidth since the URI in the payload object does not contain prefix.
    	 * For payloads contained in the SOAP Body this will not work because the reference is to the EncryptedData
    	 * element and not the original element itself. Besides that the payloads commonly don't have a reference at 
    	 * all and otherwise may have one that refers to a child element in the SOAP Body which is the actual root of 
    	 * the payload. Therefore a body payload is considered referenced if the referenced EncryptedData element 
    	 * applies to the SOAP Body 
    	 */
    	if (plURI != null && ref.getWsuId().endsWith(plURI))
    		return true;
    	else if (pl.getContainment() == IPayload.Containment.BODY) {
    		final Element protectedElement = ref.getProtectedElement();
    		return protectedElement != null && protectedElement.getLocalName().equals("Body")
    		   && (  protectedElement.getNamespaceURI().equals("http://schemas.xmlsoap.org/soap/envelope/")
    			  || protectedElement.getNamespaceURI().equals("http://www.w3.org/2003/05/soap-envelope"));
    	} else
    		return false;
    }    
    /**
     * Gets the DOM representation of <code>wsse:Security</code> element that is the WS-Security header target to the
     * specified role/actor.
     *
     * @param target    The target of the security header which element should be retrieved
     * @param envelope  The DOM representation of the SOAP envelope
     * @return          The WS-Security element of the security header targeted to specified role/actor if contained
     *                  in the message,<br><code>null</code> otherwise
     */
    public static Element getSecurityHeaderElement(final SecurityHeaderTarget target, final Document envelope) {
        Element wsSecHeader;
        try {
            wsSecHeader = WSSecurityUtil.findWsseSecurityHeaderBlock(envelope, envelope.getDocumentElement(),
                                                                     target.id(), false);
        } catch (WSSecurityException wse) {
            // Header block is not found
            wsSecHeader = null;
        }

        return wsSecHeader;
    }

    /**
     * Gets the <i>id</i> of the ebMS message header element. Normally this is the value of the <code>wsu:Id</code>
     * attribute of the <code>eb:Messaging</code> element, but if this attribute is not present the method checks for
     * the <code>xml:id</code> attribute and as last resort any attribute of type ID.
     *
     * @param envelope  The DOM representation of the SOAP envelope
     * @return  The id of the ebMS header if found,<br><code>null</code> otherwise
     * @throws SecurityProcessingException  When the given SOAP envelope doesn't contain an ebMS header
     */
    public static String getEbMSHeaderId(final Document envelope) throws SecurityProcessingException {
        NodeList ebMSHeaders = envelope.getElementsByTagNameNS(EbMSConstants.EBMS3_NS_URI, "Messaging");
        if (ebMSHeaders.getLength() != 1) {
            // No or multiple ebMS header(s) present => this is an invalid ebMS Message
            throw new SecurityProcessingException(
                                          "No ebMS message header present in SOAP envelope after signature operation");
        }

        return getId((Element) ebMSHeaders.item(0));
    }

    /**
     * Gets the id of the given element which can be the <code>wsu:Id</code>, <code>xml:id</code> or any other attribute
     * of type ID.
     *
     * @param el    The element to get the identifier value for
     * @return      The id value if the element has been assigned one,<code>null</code> otherwise
     */
    public static String getId(final Element el) {
        // First check for the wsu:Id attribute which is usually assigned to the ebMS header element
        String id = el.getAttributeNS(SecurityConstants.WSU_NAMESPACE_URI, "Id");

        if (Utils.isNullOrEmpty(id)) {
            // if not set, try xml:id
            id = el.getAttributeNS(EbMSConstants.QNAME_XMLID.getNamespaceURI(), "id");
            if (Utils.isNullOrEmpty(id)) {
                // if still nothing found, look for any other ID type attribute
                NamedNodeMap allAttrs = el.getAttributes();
                for(int i = 0; i < allAttrs.getLength() && id == null; i++) {
                    Attr a = (Attr) allAttrs.item(0);
                    if (a.isId())
                        id = a.getValue();
                }
            }
        }
        return Utils.isNullOrEmpty(id) ? null : id;
    }

    /**
     * Helper method to get all <code>ds:Reference</code> descendant elements from the signature in the given SOAP
     * envelope. In an ebMS there may only be one <code>ds:Signature</code> element in the <i>"default"</i> WS-Security
     * header so we can take the <code>ds:SignedInfo</code> element of the first one found to get access to the <code>
     * ds:Reference</code> elements.
     *
     * @param domEnvelope   The DOM representation of the SOAP envelope
     * @return      The {@link Collection} of <code>ds:Reference</code> elements contained in the signature,<br>
     *              <code>null</code> if there is no signature in the default security header.
     */
    private static Collection<Element> getSignatureReferences(final Document domEnvelope) {

        // First get the the default WS-Security header
        final Element wsSecHeader = getSecurityHeaderElement(SecurityHeaderTarget.DEFAULT, domEnvelope);

        // Get the ds:SignedInfo descendant in the default header.
        final NodeList signatureElems = wsSecHeader.getElementsByTagNameNS(
                                                                    SecurityConstants.SIGNATURE_ELEM.getNamespaceURI(),
                                                                    SecurityConstants.SIGNATURE_ELEM.getLocalPart());
        if (signatureElems == null || signatureElems.getLength() == 0)
            return null; // No Signature element available in header

        // Collect all ds:Reference elements contained in it. 
        NodeList referenceElems = ((Element) signatureElems.item(0)).getElementsByTagNameNS(
                                                                    SecurityConstants.REFERENCE_ELEM.getNamespaceURI(),
                                                                    SecurityConstants.REFERENCE_ELEM.getLocalPart());
        // Convert NodeList to Collection
        Collection<Element> references = new ArrayList<>();
        for (int i = 0; i < referenceElems.getLength(); i++)
            references.add((Element) referenceElems.item(i));

        return references;
    }
}

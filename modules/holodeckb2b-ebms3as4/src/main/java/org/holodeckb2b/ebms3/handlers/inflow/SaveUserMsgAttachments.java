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
package org.holodeckb2b.ebms3.handlers.inflow;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipException;

import javax.activation.DataHandler;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.as4.compression.DeCompressionFailure;
import org.holodeckb2b.common.errors.FailedDecryption;
import org.holodeckb2b.common.errors.MimeInconsistency;
import org.holodeckb2b.common.errors.OtherContentError;
import org.holodeckb2b.common.errors.ValueInconsistent;
import org.holodeckb2b.common.handlers.AbstractUserMessageHandler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is the <i>IN_FLOW</i> handler responsible for reading the payload content from the SOAP message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SaveUserMsgAttachments extends AbstractUserMessageHandler {

    /**
     * The name of the directory used for temporarily storing payloads
     */
    private static final String PAYLOAD_DIR = "plcin";

    /**
     * Saves the payload contents to storage
     *
     * @throws StorageException When a database problem occurs changing the processing state of the message unit
     */
    @Override
    protected InvocationResponse doProcessing(final IUserMessageEntity um, final IMessageProcessingContext procCtx,
    										  final Logger log) throws StorageException {
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();

        final Collection<? extends IPayloadEntity> payloads = um.getPayloads();
        // If there are no payloads in the UserMessage directly continue processing
        if (Utils.isNullOrEmpty(payloads)) {
            log.debug("UserMessage contains no payloads.");
            return InvocationResponse.CONTINUE;
        }

        log.debug("UserMessage contains " + payloads.size() + " payloads.");
        try {
            // Save each payload to storage
        	IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(um.getPModeId());
            for(final IPayloadEntity p : payloads) {
            	log.trace("Get the storage for payload {}", p.getPayloadURI());
            	IPayloadContent storage = updateManager.createStorageReceivedPayload(p);
            	if (storage.getContent() != null) {
	            	log.debug("Content of payload ({}) has already been saved", p.getPayloadURI());
	            	continue;
            	}
            	// The reference defines how the payload is contained in the message
                final String plRef = p.getPayloadURI();
                switch (p.getContainment()) {
                    case BODY:
                        // Payload is a element from the SOAP body
                        SOAPBody body = procCtx.getParentContext().getEnvelope().getBody();
                        OMElement plElement= null;
                        if (Utils.isNullOrEmpty(plRef)) {
                            log.trace("No reference included in payload meta data => SOAP body is the payload");
                            plElement = body.getFirstElement();
                        } else {
                            boolean bodyReferenced = false;
                            String bodyWsuId = body.getAttributeValue(EbMSConstants.QNAME_WSUID);
                            if (bodyWsuId != null && bodyWsuId.equals(plRef)) {
                                // Referenced using a wsu:Id. This is often also used as the signature reference URI.
                                bodyReferenced = true;
                            } else {
                                // Referenced using an xml:id.
                                String bodyXmlId = body.getAttributeValue(EbMSConstants.QNAME_XMLID);
                                bodyReferenced = (bodyXmlId != null && bodyXmlId.equals(plRef));
                            }

                            if (bodyReferenced) {
                                log.trace("Payload metadata references SOAP body element");
                                plElement = body.getFirstElement();
                            } else {
                                log.trace("Payload is element with id " + plRef + " of SOAP body");
                                plElement = getPayloadFromBody(body, plRef);
                            }
                        }
                        if (plElement == null) {
                            // The reference is invalid as no element exists in the body. This makes this an
                            // invalid UserMessage! Create an ValueInconsistent error and store it in the MessageContext
                            createInconsistentError(procCtx, um, null, log);
                            return InvocationResponse.CONTINUE;
                        } else {
                            // Found the referenced element, save it (and its children)
                            log.trace("Found payload element in SOAP body");
                            try (final OutputStream os = storage.openStorage()) {
                                final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
                                plElement.serialize(writer);
                                writer.flush();
                            }
                            log.trace("Payload ");
                            p.setMimeType("application/xml");
                        }
                        break;
                    case ATTACHMENT:
                        log.trace("Payload is contained in attachment with MIME Content-id= " + plRef);
                        // Get access to the actual content
                        log.debug("Get DataHandler for attachment");
                        final DataHandler dh = procCtx.getParentContext().getAttachment(plRef);
                        if (dh == null) {
                            // The reference is invalid as no attachment with this Cid is available in the message.
                        	// Create an ValueInconsistent error and store it in the MessageContext
                            createInconsistentError(procCtx, um, plRef, log);
                            return InvocationResponse.CONTINUE;
                        } else {
                            try (final OutputStream aOS = storage.openStorage())
                            {
                                dh.writeTo(aOS);
                            } catch (final IOException ioException) {
                                // Get root cause as this problem can be caused by failure to decompress, decrypt or
                                // writing to file system
                                Throwable rootCause = Utils.getRootCause(ioException);
                                // An error must be generated, which one depending on the what caused the exception
                                EbmsError writeFailure;
                                String  errMessage;
                                // Check if this IO exception is caused by decryption failure
                                if (rootCause instanceof ZipException) {
                                    errMessage = "decompressed";
                                    writeFailure = new DeCompressionFailure("Payload [" + plRef
                                                                        + "] in message could not be decompressed!",
                                                                        um.getMessageId());
                                } else if (Utils.getRootCause(ioException)
                                                                instanceof java.security.GeneralSecurityException) {
                                    errMessage = "decrypted";
                                    writeFailure = new FailedDecryption("Payload [" + plRef
                                                                        + "] in message could not be decrypted!",
                                                                        um.getMessageId());
                                } else {
                                    errMessage = "stored";
                                    writeFailure = new OtherContentError("Unexpected error in payload processing!",
                                                                        um.getMessageId());
                                }
                                log.info("Payload [" + plRef + "] in message [" + um.getMessageId() + "] could not be "
                                          + errMessage + "!\n\tDetails: " + rootCause.getMessage());
                                procCtx.addGeneratedError( writeFailure);
                                log.trace("Error generated and stored in MC, change processing state of user message");
                                updateManager.setProcessingState(um, ProcessingState.FAILURE);
                                return InvocationResponse.CONTINUE;
                            }
                            log.debug("Payload saved to storage, set Mime type in meta data");
                            p.setMimeType(dh.getContentType());
                        }
                        break;
                    default:
                        log.debug("Payload is not contained in message but located at " + plRef);
                        // External payload are not processed by Holodeck B2B, the URI is just passed to business app
                }
                // Update payload meta-data in database
                updateManager.updatePayloadInformation(p);
            }
            log.debug("All payloads saved to temp file");
        } catch (IOException | XMLStreamException | StorageException ex) {
            log.error("Payload(s) could not be saved to temporary file! Details:" + ex.getMessage());
            procCtx.addGeneratedError(new OtherContentError("Internal error", um.getMessageId()));
            updateManager.setProcessingState(um, ProcessingState.FAILURE);
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Searches for and returns the element in the SOAP body with the given id.
     * <p>This method only looks for the <code>xml:id</code> attribute of the elements.
     *
     * @param body  {@link SOAPBody} to search through
     * @param id    id to search for
     * @return      The first element with the given id,
     *              <code>null</code> if no element is found with the given id.
     */
    private OMElement getPayloadFromBody(final SOAPBody body, final String id) {
        // Search all children in the SOAP body for an element with given id
        final Iterator<?> bodyElements = body.getChildElements();
        OMElement e = null; boolean f = false;
        while (bodyElements.hasNext() && !f) {
            e = (OMElement) bodyElements.next();
            f = id.equals(e.getAttributeValue(EbMSConstants.QNAME_XMLID));
        }
        return (f ? e : null);
    }

    /**
     * Creates a <i>ValueInconsistent</i> or <i>MimeInconsistency</i> ebMS error as the reference in the user message
     * is invalid. Also changes the processing state of the user message to {@link ProcessingState#FAILURE} to
     * indicate the message can not be processed.
     *
     * @param mc            The current message context
     * @param um            The user message containing the invalid reference
     * @param invalidRef    The invalid payload reference, can be <code>null</code> if the body payload is missing
     * @param log			The Log to be used
     * @throws StorageException When updating the processing state fails.
     */
    private void createInconsistentError(final IMessageProcessingContext procCtx, final IUserMessageEntity um,
    									 final String invalidRef, final Logger log) throws StorageException {
        log.info("UserMessage with id " + um.getMessageId() + " can not be processed because payload"
                 + (invalidRef != null ? " with href=" + invalidRef  : "") + " is not included in message");
        EbmsError error = null;
        if(invalidRef == null || invalidRef.startsWith("#"))
            error = new ValueInconsistent();
        else
            error = new MimeInconsistency();
        error.setRefToMessageInError(um.getMessageId());
        error.setErrorDetail("The payload" + (invalidRef != null ? " with href=" + invalidRef  : "")
                                           + " could not be found in the message!");
        procCtx.addGeneratedError(error);
        log.trace("Error stored in message context, changing processing state of message");
        HolodeckB2BCore.getStorageManager().setProcessingState(um, ProcessingState.FAILURE);
    }
}

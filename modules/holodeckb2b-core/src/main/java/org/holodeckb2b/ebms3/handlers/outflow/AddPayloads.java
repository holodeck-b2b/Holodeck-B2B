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
package org.holodeckb2b.ebms3.handlers.outflow;

import static org.holodeckb2b.interfaces.messagemodel.IPayload.Containment.ATTACHMENT;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;

import javax.activation.FileDataSource;
import javax.xml.namespace.QName;

import org.apache.axiom.attachments.ConfigurableDataHandler;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.holodeckb2b.common.handlers.AbstractUserMessageHandler;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.util.MessageIdUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.handlers.MessageProcessingContext;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>OUT_FLOW</i> handler that will add the payloads to the SOAP message if there is a <i>User Message</i> to be
 * sent.
 * <p>Note that this is only the first step of a two step process as payload meta data must also be added to the ebMS
 * SOAP header. Adding the payload meta data to ebMS header is done by the {@link PackageUsermessageInfo} handler. This
 * is done later in the pipeline as meta data should only be added if the payload content can be successfully included
 * in the message.
 * <br>The content of the payloads are normally added as SOAP Attachments. But it is possible to include one or more XML
 * documents in the SOAP body. Alternatively the content can be external to message, for example stored on a web site,
 * in which case this handler does nothing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class AddPayloads extends AbstractUserMessageHandler {

    @Override
    protected InvocationResponse doProcessing(final IUserMessageEntity um, final MessageProcessingContext procCtx, 
    										  final Log log) throws PersistenceException {

        log.trace("Check that all meta-data of the User Message is available for processing");
        if (!um.isLoadedCompletely()) {
            log.trace("Not all info loaded, load now");
            HolodeckB2BCore.getQueryManager().ensureCompletelyLoaded(um);
        }

        log.trace("All meta-data of User Message available, check for payloads to include");
        final Collection<IPayload> payloads = um.getPayloads();

        if (Utils.isNullOrEmpty(payloads)) {
            // No payloads in this user message, so nothing to do
            log.debug("User message has no payloads");
        } else {
            // Add each payload to the message as described by the containment attribute
            log.trace("User message contains " + payloads.size() + " payload(s)");
            // If a MIME Content-Id is generated it should be saved to database as well, therefor we need to construct
            // new set of payload meta-data
            ArrayList<IPayload>  newPayloadInfo = new ArrayList<>(payloads.size());
            boolean cidGenerated = false;
            for (final IPayload pl : payloads) {
                // Create copy of existing meta-data
                Payload p = new Payload(pl);
                // First ensure that the payload is assigned a MIME Content-Id when it added as an attachment
                if (pl.getContainment() == ATTACHMENT && Utils.isNullOrEmpty(pl.getPayloadURI())) {
                    // No MIME Content-Id assigned on submission, assign now
                    final String cid = MessageIdUtils.createContentId(um.getMessageId());
                    log.trace("Generated a new Content-id [" + cid + "] for payload [" + pl.getContentLocation() + "]");
                    p.setPayloadURI(cid);
                    cidGenerated = true;
                }
                newPayloadInfo.add(p);
                // Add the content of the payload to the SOAP message
                try {
                    addContent(p, procCtx.getParentContext(), log);
                } catch (final Exception e) {
                    log.error("Adding the payload content to message failed. Error details: " + e.getMessage());
                    // If adding the payload fails, the message is in an incomplete state and should not
                    // be sent. Therefor cancel further processing
                    return InvocationResponse.ABORT;
                }
            }
            if (cidGenerated) {
                HolodeckB2BCore.getStorageManager().setPayloadInformation(um, newPayloadInfo);
                log.debug("Generated MIME Content-Id(s) saved to database");
            }
            log.debug("Payloads successfully added to User Message [msgId=" + um.getMessageId() + "]");
        }
        return InvocationResponse.CONTINUE;
    }

    /**
     * Adds the payload content to the SOAP message. How the content is added to the message depends on the
     * <i>containment</i> of the payload ({@link IPayload#getContainment()}).
     * <p>The default containment is {@link IPayload.Containment#ATTACHMENT} in which case the payload content as added
     * as a SOAP attachment. The payload will be referred to from the ebMS header by the MIME Content-id of the MIME
     * part. This Content-id MUST specified in the message meta-data ({@link IPayload#getPayloadURI()}).
     * <p>When the containment is specified as {@link IPayload.Containment#EXTERNAL} no content will
     * be added to SOAP message. It is assumed that transfer of the content takes place out of band.
     * <p>When {@link IPayload.Containment#BODY} is specified as containment the content should be added to the SOAP
     * Body. This requires the payload content to be a XML document. If the specified content is not, an exception is
     * thrown.<br>
     * <b>NOTE:</b> A payload included in the body is referred to from the ebMS header by the <code>id</code> attribute
     * of the root element of the XML Document. The submitted message meta data however can also included a reference
     * ({@see IPayload#getPayloadURI()}). In case both the payload and the message meta data included an id the
     * submitter MUST ensure that the value is the same. If not the payload will not be included in the message and this
     * method will throw an exception.
     *
     * @param p             The payload that should be added to the message
     * @param mc            The Axis2 message context for the outgoing message
     * @param log 			The Log to be used
     * @throws Exception    When a problem occurs while adding the payload contents to
     *                      the message
     */
    protected void addContent(final IPayload p, final MessageContext mc, Log log) throws Exception {
        File f = null;

        switch (p.getContainment()) {
            case ATTACHMENT :
                log.trace("Adding payload as attachment. Content located at " + p.getContentLocation());
                f = new File(p.getContentLocation());

                // Use specified MIME type or detect it when none is specified
                String mimeType = p.getMimeType();
                if (mimeType == null || mimeType.isEmpty()) {
                    log.trace("Detecting MIME type of payload");
                    mimeType = Utils.detectMimeType(f);
                }

                log.trace("Payload mime type is " + mimeType);
                // Use Axiom ConfigurableDataHandler to enable setting of mime type
                final ConfigurableDataHandler dh = new ConfigurableDataHandler(new FileDataSource(f));
                dh.setContentType(mimeType);
                final String cid = p.getPayloadURI();
                log.trace("Add payload to message as attachment with Content-id: " + cid);
                mc.addAttachment(cid, dh);
                log.debug("Payload content located at '" + p.getContentLocation() + "' added to message");
                return;
            case BODY :
                log.trace("Adding payload to SOAP body. Content located at " + p.getContentLocation());
                f = new File(p.getContentLocation());

                try {
                    log.trace("Parse the XML from file so it can be added to SOAP body");
                    final OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(new FileReader(f));
                    final OMElement documentElement = builder.getDocumentElement();

                    // Check that reference and id are equal if both specified
                    final String href = p.getPayloadURI();
                    final String xmlId = documentElement.getAttributeValue(new QName("id"));
                    if( href != null && xmlId != null && !href.equals(xmlId)) {
                        log.warn("Payload reference [" + href + "] and id of payload element [" + xmlId +
                                    "] are not equal! Can not create consistent message.");
                        throw new Exception("Payload reference [" + href + "] and id of payload element [" + xmlId +
                                    "] are not equal! Can not create consistent message.");
                    } else if (href != null && Utils.isNullOrEmpty(xmlId)) {
                        log.debug("Set specified reference in meta data [" + href + "] as xml:id on root element");
                        final OMNamespace xmlIdNS =
                                documentElement.declareNamespace(EbMSConstants.QNAME_XMLID.getNamespaceURI(), "xml");
                        documentElement.addAttribute(EbMSConstants.QNAME_XMLID.getLocalPart(), href, xmlIdNS);
                    }

                    log.trace("Add payload XML to SOAP Body");
                    mc.getEnvelope().getBody().addChild(documentElement);
                    log.debug("Payload content located at '" + p.getContentLocation() + "' added to message");
                } catch (final OMException parseError) {
                    // The given document could not be parsed, probably not an XML document. Reject it as body payload
                    log.warn("Failed to parse payload located at " + p.getContentLocation() + "!");
                    throw new Exception("Failed to parse payload located at " + p.getContentLocation() + "!",
                                        parseError);
                }
                return;
            case EXTERNAL : 
            	log.warn("Payload containment is set to EXTERNAL, handling by receiver unspecified!");
        }
    }
}

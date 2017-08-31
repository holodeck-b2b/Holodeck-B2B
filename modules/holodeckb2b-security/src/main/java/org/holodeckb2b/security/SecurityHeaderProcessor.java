/*
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
package org.holodeckb2b.security;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngine;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.str.STRParser;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.validate.Validator;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.security.ISecurityHeaderProcessor;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.holodeckb2b.security.callbackhandlers.AttachmentCallbackHandler;
import org.holodeckb2b.security.callbackhandlers.PasswordCallbackHandler;
import org.holodeckb2b.security.results.EncryptionProcessingResult;
import org.holodeckb2b.security.results.HeaderProcessingFailure;
import org.holodeckb2b.security.results.SignatureProcessingResult;
import org.holodeckb2b.security.results.UsernameTokenProcessingResult;
import org.holodeckb2b.security.util.Axis2XMLUtils;
import org.holodeckb2b.security.util.SecurityUtils;
import org.holodeckb2b.security.util.SignedMessagePartsInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Is the default security provider's implementation of the {@link ISecurityHeaderProcessor} which is responsible for
 * the processing of the WS-Security headers in a received message. It uses the WSS4J framework for the actual
 * processing of the headers.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class SecurityHeaderProcessor implements ISecurityHeaderProcessor {

    private static Logger  log = LogManager.getLogger(SecurityHeaderProcessor.class);

    /**
     * The Certificate manager of the security provider
     */
    private CertificateManager certManager;

    /**
     * Reference to the current message context
     */
    private MessageContext       msgContext;

    /**
     * Collection of all User Message units in the processed message
     */
    private Collection<IUserMessage> userMsgs;

    /**
     * DOM version of the SOAP envelope as this is needed for WSS4J processing
     */
    private Document domEnvelope;

    /**
     * The default digest algorithm for key transport is SHA-1
     */
    private static final String DEFAULT_KEYTRANSPORT_DIGEST_ALGO = "http://www.w3.org/2000/09/xmldsig#sha1";

    /**
     * Mapping of elements to action that is related to the element. This is used to register faults with the correct
     * action.
     */
    private static final Map<QName, Action> FAULT_CAUSES;
    static {
        final Map<QName, Action> tmp = new HashMap<>();
        tmp.put(WSSecurityEngine.ENCRYPTED_KEY, Action.DECRYPT);
        tmp.put(WSSecurityEngine.SIGNATURE, Action.VERIFY);
        tmp.put(WSSecurityEngine.USERNAME_TOKEN, Action.USERNAME_TOKEN);
        tmp.put(WSSecurityEngine.REFERENCE_LIST, Action.DECRYPT);
        FAULT_CAUSES = java.util.Collections.unmodifiableMap(tmp);
    }

    /**
     * Gets the action associated with the given element from the WS-Security header.
     *
     * @param el    The {@link QName} of the element
     * @return      The associated action expressed as using {@link Action} if an action is associated with the
     *              element, or<br>
     *              <code>null</code> if no action is associated with the element
     */
    private Action getAction(final QName el) {
        return FAULT_CAUSES.get(el);
    }

    /**
     * Creates a new <code>SecurityHeaderProcessor</code> instance
     *
     * @param certManager The certificate manager of the provider
     */
    SecurityHeaderProcessor(final CertificateManager certManager) {
        this.certManager = certManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ISecurityProcessingResult> processHeaders(MessageContext mc, Collection<IUserMessage> userMsgs,
                                                                ISecurityConfiguration config)
                                                                                    throws SecurityProcessingException {
        // Copy reference to message context
        this.msgContext = mc;
        this.userMsgs = userMsgs;

        // Convert the SOAP Envelope to standard DOM representation as this is required by the security processing
        // libraries
        log.debug("Converting envelope to DOM structure for processing");
        domEnvelope = Axis2XMLUtils.convertAxiomSOAPEnvToDOM(msgContext);

        if (domEnvelope == null) {
            log.error("Converting the SOAP envelope to DOM representation failed");
            throw new SecurityProcessingException("Could not convert SOAP envelope to DOM object!");
        }

        Collection<ISecurityProcessingResult> results = new ArrayList<>();
        log.debug("Prepare WSS4J context for processing of the security headers");
        RequestData reqData = prepare();
        //
        // First process the default header
        //
        // Configure access to the private key for decryption
        IEncryptionConfiguration encConfig = config != null ? config.getEncryptionConfiguration() : null;
        if (encConfig != null)
            ((PasswordCallbackHandler) reqData.getCallbackHandler()).addUser(encConfig.getKeystoreAlias(),
                                                                             encConfig.getCertificatePassword());
        try {
            log.debug("Process the default WS-Security header");
            results.addAll(processSecurityHeader(SecurityHeaderTarget.DEFAULT, reqData));
            log.debug("Succesfully processed the default WS-Security header");
        } catch (HeaderProcessingFailure hpf) {
            log.error("A problem occurred in the {} action when processing the default WS-Security header!" +
                      "\n\tDetails: {}", hpf.getFailedAction().name(), hpf.getMessage());
            switch (hpf.getFailedAction()) {
                case SIGN :
                    results.add(new SignatureProcessingResult(hpf));
                    break;
                case ENCRYPT :
                    results.add(new EncryptionProcessingResult(hpf));
                    break;
                case USERNAME_TOKEN :
                    results.add(new UsernameTokenProcessingResult(SecurityHeaderTarget.DEFAULT, hpf));
            }
        }
        //
        // Then process the ebms targeted header
        //
        try {
            log.debug("Process the ebms WS-Security header");
            results.addAll(processSecurityHeader(SecurityHeaderTarget.EBMS, reqData));
            log.debug("Succesfully processed the ebms WS-Security header");
        } catch (HeaderProcessingFailure hpf) {
            log.error("A problem occurred in processing of the ebms targeted WS-Security header!" +
                      "\n\tDetails: {}", hpf.getFailedAction().name(), hpf.getMessage());
            results.add(new UsernameTokenProcessingResult(SecurityHeaderTarget.EBMS, hpf));
        }

        // Convert the processed SOAP envelope back to the Axiom representation for further processing
        SOAPEnvelope SOAPenv = Axis2XMLUtils.convertDOMSOAPEnvToAxiom(domEnvelope);
        try {
            msgContext.setEnvelope(SOAPenv);
            log.debug("Mark security headers as processed");
            final SOAPHeader header = SOAPenv.getHeader();
            if (header != null) {
                final ArrayList<?> wsseHdrs = header.getHeaderBlocksWithNSURI(SecurityConstants.WSS_NAMESPACE_URI);
                wsseHdrs.stream().map(h -> (SOAPHeaderBlock) h)
                                 .filter(soapHdr -> Utils.isNullOrEmpty(soapHdr.getRole())
                                                    || soapHdr.getRole().equals(SecurityHeaderTarget.EBMS.id()))
                                 .forEach(secHdr -> secHdr.setProcessed());
            }
        } catch (AxisFault ex) {
            SOAPenv = null;
        }
        if (SOAPenv == null) {
            log.error("Could not set the SOAP Envelope back to Axiom representation!");
            throw new SecurityProcessingException("Could not set the SOAP Envelope back to Axiom representation!");
        }

        return results;
    }

    /**
     * Prepares the WSS4J context for processing of the WS-Security headers.
     *
     * @return          A correctly configured {@link RequestData} instance
     * @throws SecurityProcessingException When the WSS4J context cannot be created because a Crypto engine cannot be
     *                                     loaded
     */
    private RequestData prepare() throws SecurityProcessingException {
        final RequestData reqData = new RequestData();
        final WSSConfig wssConfig = WSSConfig.getNewInstance();
        // Disable validator of the Usernametoken so no validation is done by the security provider
        wssConfig.setValidator(WSSecurityEngine.USERNAME_TOKEN, (Validator) null);
        reqData.setWssConfig(wssConfig);
        reqData.setMsgContext(msgContext);

        // Disable BSP conformance check. Although AS4 requires conformance with BSP1.1 the check is disable to allow
        // more recent encryption algorithms.
        reqData.setDisableBSPEnforcement(true);

        // Register callback handler for attachments
        reqData.setAttachmentCallbackHandler(new AttachmentCallbackHandler(msgContext));

        // Usernametokens are checked for replay attacks in 5 minute window and their created timestamp may only be
        // one minute ahead
        wssConfig.setUtTTL(300);
        wssConfig.setUtFutureTTL(60);
        wssConfig.setAllowNamespaceQualifiedPasswordTypes(true);
        wssConfig.setAllowUsernameTokenNoPassword(true);

        // Configure signature verification action
        reqData.setSigVerCrypto(certManager.getWSS4JCrypto(org.holodeckb2b.security.Action.VERIFY));
        reqData.setEnableRevocation(false);
        wssConfig.setEnableSignatureConfirmation(false);
        // Configure decryption action
        reqData.setDecCrypto(certManager.getWSS4JCrypto(org.holodeckb2b.security.Action.DECRYPT));
        reqData.setCallbackHandler(new PasswordCallbackHandler());
        reqData.setAllowRSA15KeyTransportAlgorithm(false);
        reqData.setRequireSignedEncryptedDataElements(false);

        return reqData;
    }

    /**
     * Processes the security header targeted to the specified target.
     * <p>This implementation is based on the WSS4J security engine class {@link WSSecurityEngine} but instead of
     * throwing the standard {@link WSSecurityException} when there is a problem with the processing of an element it
     * will use the security provider's {@link HeaderProcessingFailure} exception to include the indication which action
     * failed.
     * <p>This function loops over all direct child elements of the <code>wsse:Security</code> header. If it finds a
     * known element, it transfers control to the appropriate WSS4J handling function. Although the ebMS V3 Core
     * Specification prescribes that signing must be done before encryption the elements are processed in order as per
     * WS-I BSP. Because the ebMS V3 Core Specification only specifies the use of signature, encryption and user name
     * tokens other elements in the security headers will be ignored.
     *
     * @param target        the targeted WS-Security header
     * @param requestData   the WSS4J RequestData
     * @return Collection of {@link ISecurityProcessingResult} objects representing the results of the performed
     *         security actions.
     * @throws SecurityProcessingException When there is a error in the processing of the specified header. If the
     *                                     problem is related to a specific action a {@link HeaderProcessingFailure}
     *                                     is thrown.
     */
    private Collection<ISecurityProcessingResult> processSecurityHeader(final SecurityHeaderTarget target,
                                                                        final RequestData requestData)
                                                                                    throws SecurityProcessingException {
        // Get the targeted security that needs to be processed
        Element securityHeader = SecurityUtils.getSecurityHeaderElement(target, domEnvelope);
        if (securityHeader == null) {
            log.debug("Message does not contain WS-Security header targeted to {} role.", target);
            return new ArrayList();
        }

        // Gather some info about the document to process and store it for retrieval by the processors.
        final WSDocInfo wsDocInfo = new WSDocInfo(securityHeader.getOwnerDocument());
        wsDocInfo.setCrypto(requestData.getSigVerCrypto());
        wsDocInfo.setSecurityHeader(securityHeader);
        final WSSConfig wssConfig = requestData.getWssConfig();

        // Handle all childs in the securty header, but only process the ones relevant for ebMS
        Node node = securityHeader.getFirstChild();
        final List<WSSecurityEngineResult> results = new LinkedList<>();
        while (node != null) {
            final Node nextSibling = node.getNextSibling();
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                final QName el = new QName(node.getNamespaceURI(), node.getLocalName());
                final Action action = getAction(el);

                // Call the WSS4J processors for the relevent headers . After the processor returns, store it for later
                // retrieval. The token processor may store some information about the processed token
                Processor p;
                try {
                    p = wssConfig.getProcessor(el);
                } catch (WSSecurityException ex) {
                    p = null;
                }
                if (p == null && action != null) {
                    log.error("No processor available to execute the {} action in the {} WS-Security header!"
                              , action, target);
                    throw new SecurityProcessingException("No processor available to execute the " + action
                                                         + " action in the " + target + " WS-Security header!");
                } else if (p != null) {
                    try {
                        log.debug("Performing {} action in {} WS-Security header", action, target);
                        List<WSSecurityEngineResult> actionResults = p.handleToken((Element) node, requestData,
                                                                                    wsDocInfo);
                        log.debug("Completed {} action in {} WS-Security header", action, target);
                        if (!actionResults.isEmpty())
                            results.addAll(actionResults);
                    } catch (final WSSecurityException ex) {
                        log.error("Execution of the {} action in the {} WS-Security header failed!"
                                  + "\n\tDetails: {};{}", getAction(el), target, ex.getMsgID(), ex.getMessage());
                        // Decorate the exception with the action before throwing it again
                        throw new HeaderProcessingFailure(getAction(el), ex);
                    }
                }
            }
            // If the next sibling is null and the stored next sibling is not null, then we have
            // encountered an EncryptedData element which was decrypted, and so the next sibling
            // of the current node is null. In that case, go on to the previously stored next
            // sibling
            if (node.getNextSibling() == null && nextSibling != null && nextSibling.getParentNode() != null)
                node = nextSibling;
            else
                node = node.getNextSibling();
        }

        return convertResults(target, results, wsDocInfo);
    }

    /**
     * Converts the results of processing represented in collection of WSS4J {@link WSSecurityEngineResult} objects to
     * the Holodeck B2B representation.
     *
     * @param target        The target of the processed security header
     * @param wss4jResults  The processing results in WSS4J representation
     * @param wss4jDocInfo  WSS4J object containing some additional info about the processed header
     * @return The processing results expressed as a Collection of {@link ISecurityProcessingResult} objects
     * @throws SecurityProcessingException When the ebMS header isn't available anymore.
     */
    private Collection<ISecurityProcessingResult> convertResults(final SecurityHeaderTarget target,
                                                                 final List<WSSecurityEngineResult> wss4jResults,
                                                                 final WSDocInfo wss4jDocInfo)
                                                                                    throws SecurityProcessingException {
        log.debug("Converting WSS4J results of processing the {} header to Holodeck B2B format", target);
        Collection<ISecurityProcessingResult> results = new ArrayList<>();
        // Check if a username token was processed
        final WSSecurityEngineResult utResult = WSSecurityUtil.fetchActionResult(wss4jResults, WSConstants.UT);
        if (utResult != null) {
            log.debug("Converting username token result");
            results.add(new UsernameTokenProcessingResult(target, (UsernameToken)
                                                        utResult.get(WSSecurityEngineResult.TAG_USERNAME_TOKEN)));
        }
        if (target == SecurityHeaderTarget.DEFAULT) {
            // Check if message was signed
            final WSSecurityEngineResult signResult = WSSecurityUtil.fetchActionResult(wss4jResults, WSConstants.SIGN);
            if (signResult != null) {
                log.debug("Converting signature verification result");
                // Collect information about the processed signature
                final String signAlgorithm = (String) signResult.get(WSSecurityEngineResult.TAG_SIGNATURE_METHOD);
                final X509Certificate signCert =
                                          (X509Certificate) signResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                final X509ReferenceType refType = SecurityUtils.getKeyReferenceType(
                             (STRParser.REFERENCE_TYPE) signResult.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
                final SignedMessagePartsInfo signedPartsInfo = SecurityUtils.getSignedPartsInfo(domEnvelope, userMsgs);
                // And create result object
                results.add(new SignatureProcessingResult(signCert, refType, signAlgorithm,
                                                          signedPartsInfo.getEbmsHeaderInfo(),
                                                          signedPartsInfo.getPayloadInfo()));
            }
            final WSSecurityEngineResult decResult = WSSecurityUtil.fetchActionResult(wss4jResults, WSConstants.ENCR);
            if (decResult != null) {
                log.debug("Converting decryption result");
                // Collect information about the performed decryption
                final X509Certificate encCert =
                                          (X509Certificate) decResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                final X509ReferenceType refMethod = SecurityUtils.getKeyReferenceType(
                             (STRParser.REFERENCE_TYPE) decResult.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
                final List<WSDataRef> refs = (List<WSDataRef>) decResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS);
                final String ktAlgorithm =
                                     (String) decResult.get(WSSecurityEngineResult.TAG_ENCRYPTED_KEY_TRANSPORT_METHOD);
                final Collection<IPayload>  payloads = new ArrayList<>();
                String encAlgorithm = null;
                String ktDigest = DEFAULT_KEYTRANSPORT_DIGEST_ALGO;
                String ktMGF = null;
                // It could be possible that the header only contained a EncryptedKey element without any EncryptedData
                // elements.
                if (!Utils.isNullOrEmpty(refs)) {
                    encAlgorithm = refs.get(0).getAlgorithm();
                    userMsgs.stream().map(userMsg -> userMsg.getPayloads())
                                     .filter(umPayloads -> !Utils.isNullOrEmpty(umPayloads))
                                     .forEachOrdered(umPayloads ->
                                       umPayloads.forEach((p) ->
                                                 refs.stream()
                                                 .filter(ref -> SecurityUtils.isPayloadReferenced(p,
                                                                                                  ref.getWsuId(),
                                                                                                  domEnvelope))
                                                 .forEach(match -> payloads.add(p))));
                }
                // As WSS4J does not report the key transport detail, we need to collect them directly from the
                // EncryptedKey element
                final Element encryptionMethod = wss4jDocInfo.getTokenElement(
                                                                 (String) decResult.get(WSSecurityEngineResult.TAG_ID));
                for(int i = 0; i < encryptionMethod.getChildNodes().getLength() ; i++) {
                    Node child = encryptionMethod.getChildNodes().item(i);
                    switch (child.getLocalName()) {
                        case "DigestMethod" :
                            ktDigest = ((Element) child).getAttribute("Algorithm");
                            break;
                        case "MGF" :
                            ktMGF = ((Element) child).getAttribute("Algorithm");
                    }
                }
                // And create result object
                results.add(new EncryptionProcessingResult(encCert, refMethod, ktAlgorithm, ktMGF, ktDigest,
                                                           encAlgorithm, payloads));
            }
        }
        return results;
    }

}

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
package org.holodeckb2b.security.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngine;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.saml.DOMSAMLUtil;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class WSSProcessingEngine extends WSSecurityEngine {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WSSProcessingEngine.class);

    /**
     * Mapping of elements to action that is related to the element. This is used to register faults with the correct
     * action.
     */
    private static final Map<QName, Integer> FAULT_CAUSES;
    static {
        final Map<QName, Integer> tmp = new HashMap<>();
        try {
            tmp.put(
                WSSecurityEngine.ENCRYPTED_KEY,
                WSConstants.ENCR
            );
            tmp.put(
                WSSecurityEngine.SIGNATURE,
                WSConstants.SIGN
            );
            tmp.put(
                WSSecurityEngine.TIMESTAMP,
                WSConstants.TS
            );
            tmp.put(
                WSSecurityEngine.USERNAME_TOKEN,
                WSConstants.UT
            );
            tmp.put(
                WSSecurityEngine.REFERENCE_LIST,
                WSConstants.ENCR
            );
        } catch (final Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug(ex.getMessage(), ex);
            }
        }
        FAULT_CAUSES = java.util.Collections.unmodifiableMap(tmp);
    }

    /**
     * Gets the action associated with the given element from the WS-Security header.
     *
     * @param el    The {@link QName} of the element
     * @return      The associated action expressed as using {@link WSConstants} if an action is associated with the
     *              element, or<br>
     *              <code>null</code> if no action is associated with the element
     */
    private Integer getAction(final QName el) {
        return FAULT_CAUSES.get(el);
    }

    /**
     * Processes the security header given the <code>wsse:Security</code> DOM Element.
     * <p>This implementation is based on the WSS4J security engine class {@link WSSecurityEngine} but does not throw
     * an exception when there is a problem with the processing of an element.
     *
     * This function loops over all direct child elements of the <code>wsse:Security</code> header. If it finds a
     * known element, it transfers control to the appropriate handling function. The method processes the known child
     * elements in the same order as they appear in the <code>wsse:Security</code> element. This is in accordance to the
     * WS Security specification.<p/>
     *
     * Currently the functions can handle the following child elements:
     * <ul>
     * <li>{@link #SIGNATURE <code>ds:Signature</code>}</li>
     * <li>{@link #ENCRYPTED_KEY <code>xenc:EncryptedKey</code>}</li>
     * <li>{@link #REFERENCE_LIST <code>xenc:ReferenceList</code>}</li>
     * <li>{@link #USERNAME_TOKEN <code>wsse:UsernameToken</code>}</li>
     * <li>{@link #TIMESTAMP <code>wsu:Timestamp</code>}</li>
     * </ul>
     *
     * Note that additional child elements can be processed if appropriate Processors have been registered with the
     * WSSCondig instance set on this class.
     *
     * @param securityHeader the <code>wsse:Security</code> header element
     * @param requestData    the RequestData associated with the request.  It should
     *                       be able to provide the callback handler, cryptos, etc...
     *                       as needed by the processing
     * @return a List of {@link WSSecurityEngineResult}. Each element in the
     *         the List represents the result of a security action. The elements
     *         are ordered according to the sequence of the security actions in the
     *         wsse:Signature header. The List may be empty if no security processing
     *         was performed.
     * @throws WSSecurityException
     */
    @Override
    public List<WSSecurityEngineResult> processSecurityHeader(final Element securityHeader, final RequestData requestData)
                                                                throws WSSecurityException {
        if (securityHeader == null) {
            return Collections.emptyList();
        }

        if (requestData.getWssConfig() == null) {
            requestData.setWssConfig(getWssConfig());
        }

        //
        // Gather some info about the document to process and store
        // it for retrieval. Store the implementation of signature crypto
        // (no need for encryption --- yet)
        //
        final WSDocInfo wsDocInfo = new WSDocInfo(securityHeader.getOwnerDocument());
        wsDocInfo.setCallbackLookup(getCallbackLookup());
        wsDocInfo.setCrypto(requestData.getSigVerCrypto());
        wsDocInfo.setSecurityHeader(securityHeader);

        final WSSConfig cfg = getWssConfig();
        Node node = securityHeader.getFirstChild();

        final List<WSSecurityEngineResult> returnResults = new LinkedList<>();
        boolean foundTimestamp = false;
        while (node != null) {
            final Node nextSibling = node.getNextSibling();
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                final QName el = new QName(node.getNamespaceURI(), node.getLocalName());

                // Check for multiple timestamps
                if (foundTimestamp && el.equals(TIMESTAMP)) {
                    requestData.getBSPEnforcer().handleBSPRule(BSPRule.R3227);
                } else if (el.equals(TIMESTAMP)) {
                    foundTimestamp = true;
                }
                //
                // Call the processor for this token. After the processor returns,
                // store it for later retrieval. The token processor may store some
                // information about the processed token
                //
                final Processor p = cfg.getProcessor(el);
                if (p != null) {
                    List<WSSecurityEngineResult> results = null;
                    try {
                        results = p.handleToken((Element) node, requestData, wsDocInfo);
                    } catch (final WSSecurityException ex) {
                        if (log.isInfoEnabled()) {
                            log.info("Processing of " + el.toString() + " element in WS-Sec header failed! Details:"
                                    + ex.getMsgID() + ";" + ex.getMessage()
                                    + (ex.getCause() != null ?  ";" + ex.getCause().getMessage() : ""));
                        }

                        // Register failure to process this element by adding an empty result for the action
                        //   associated with this element. If no action is known for this element the exception is
                        //   passed through
                        final Integer action = getAction(el);
                        if (action != null) {
                            final WSSecurityEngineResult result = new WSSecurityEngineResult(action);
                            result.put(SecurityConstants.WSS_PROCESSING_FAILURE, ex);
                            results = java.util.Collections.singletonList(result);

                            // Continuing processing the header does not make much sense, so return immediately
                            returnResults.addAll(0, results);
                            return returnResults;
                        } else {
                            throw ex;
                        }
                    }

                    if (!results.isEmpty()) {
                        returnResults.addAll(0, results);
                    }
                } else {
                    log.warn("Unknown Element: " + node.getLocalName() + " " + node.getNamespaceURI());
                }

            }
            //
            // If the next sibling is null and the stored next sibling is not null, then we have
            // encountered an EncryptedData element which was decrypted, and so the next sibling
            // of the current node is null. In that case, go on to the previously stored next
            // sibling
            //
            if (node.getNextSibling() == null && nextSibling != null
                && nextSibling.getParentNode() != null) {
                node = nextSibling;
            } else {
                node = node.getNextSibling();
            }
        }

        // Validate SAML Subject Confirmation requirements
        if (getWssConfig().isValidateSamlSubjectConfirmation()) {
            final Element bodyElement =
                WSSecurityUtil.findBodyElement(securityHeader.getOwnerDocument());

            DOMSAMLUtil.validateSAMLResults(returnResults, requestData.getTlsCerts(), bodyElement);
        }

        return returnResults;
    }
}

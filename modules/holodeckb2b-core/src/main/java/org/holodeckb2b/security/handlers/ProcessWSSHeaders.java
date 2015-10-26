/*
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
package org.holodeckb2b.security.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.ext.WSSecurityException.ErrorCode;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngine;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.KeyValuePair;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.axis2.Axis2Utils;
import org.holodeckb2b.security.callbackhandlers.AttachmentCallbackHandler;
import org.holodeckb2b.security.tokens.IAuthenticationInfo;
import org.holodeckb2b.security.tokens.UsernameToken;
import org.holodeckb2b.security.tokens.X509Certificate;
import org.holodeckb2b.security.util.NoOpValidator;
import org.holodeckb2b.security.util.WSSProcessingEngine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Is the <i>IN_FLOW</i> handler for processing the WS-Security headers contained in the message. It validates the 
 * headers and stores the resulting authentication information (i.e. Username tokens and X.509 certificates) in the
 * message context so it can be used for authorization purposes by later handlers.
 * <p>As described in the ebMS Core Specification an ebMS may contain two WSS headers with one targeted at the default
 * SOAP actor/role and one targeted to the custom "ebms" actor/role. The default header is used for signing and 
 * encryption. The ebms one is only used for authentication/authorization purposes and should only contain a WSS 
 * Username token.
 * <p>The authentication information found in the headers is converted to {@link IAuthenticationInfo} objects and put 
 * into a <code>Map&lt;String, IAuthenticationInfo&gt;</code> where the keys identify the type of and the header where 
 * the information was found. The keys are specified in {@link SecurityConstants} and are specified in the table below.
 * The map itself is stored in the MessageContext property named {@link SecurityConstants#MC_AUTHENTICATION_INFO}.
 * <p><table border="1">
 * <tr><td>Key</td><td>Identifies</td></tr>
 * <tr><td>{@link SecurityConstants#DEFAULT_USERNAMETOKEN}</td>
 *              <td>The <code>UsernameToken</code> targeted at the <i>default</i> role</td></tr>
 * <tr><td>{@link SecurityConstants#EBMS_USERNAMETOKEN}</td>
 *              <td>The <code>UsernameToken</code> targeted at the <i>ebms</i> role</td></tr>
 * <tr><td>{@link SecurityConstants#SIGNATURE}</td>
 *              <td>The X509 Certificate used for creating the <code>Signature</code> targeted at the <i>default</i> 
 *                  role</td></tr>
 * </table>
 * <p>When a problem is detected in the WS-Security header this is indicated using MessageContext properties named
 * {@link SecurityConstants#INVALID_DEFAULT_HEADER} and {@link SecurityConstants#INVALID_EBMS_HEADER}. The value of the
 * property is a key value pair that indicates what caused the problem. The key contains the what part of the security
 * header caused the problem and the value holds an error description.<br>
 * As the ebMS V3 Core Specification only describes three security errors: 
 * <i>FailedDecryption</i>, <i>FailedAuthentication</i> and <i>PolicyNoncompliance</i>, the problem indication (= key
 * value) is limited to:<ul>
 * <li>{@link SecurityConstants.WSS_FAILURES#DECRYPTION} : Problem in decrypting the message. Applies to default header 
 *                                                         only</li>
 * <li>{@link SecurityConstants.WSS_FAILURES#SIGNATURE} : Problem with the validation of the signature. Also applies 
 *                                                         only to default header. Will result in 
 *                                                         <i>FailedAuthentication</i> error</li>
 * <li>{@link SecurityConstants.WSS_FAILURES#UT} : Problem with the validation of the username token. Can apply to both
 *                                                          the default as well as the header targeted to "ebms" role. 
 *                                                          Will also result in <i>FailedAuthentication</i> error</li> 
 * <li>{@link SecurityConstants.WSS_FAILURES#UNKNOWN} : Some other error occurred in processing the security header. 
 *                                                       This is probably caused by another element that was present in
 *                                                       the WS-Security header. How this error is handled depends on
 *                                                       configuration settings.
 * </li></ul>
 * NOTE: Creating the correct ebMS error based on the problems in processing the security header is done in the 
 * {@link ProcessSecurityFault} handler.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class ProcessWSSHeaders extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws AxisFault, DatabaseException {
        WSSReceiveHandler processor = null;
        List<Integer> actions = null;
        List<WSSecurityEngineResult> results = null;
        Map<String, IAuthenticationInfo> authInfo = new HashMap<String, IAuthenticationInfo>();
                
        // Convert the SOAP Envelope to standard DOM representation as this is required by the security processing
        // libraries
        Document domEnvelope = Axis2Utils.convertToDOM(mc);
        
        if (domEnvelope == null) {
            handleWSSecurityException(mc, null, SecurityConstants.WSS_FAILURES.UNKNOWN, 
                                      new WSSecurityException(ErrorCode.FAILURE, 
                                                        "Converting the SOAP envelope to DOM representation failed")
                                     ); 
            return InvocationResponse.CONTINUE;
        }
        
        try {
            // Create security header processor
            processor = new WSSReceiveHandler(mc, domEnvelope, log);
        } catch (WSSecurityException ex) {
            handleWSSecurityException(mc, null, SecurityConstants.WSS_FAILURES.UNKNOWN, ex);
            return InvocationResponse.CONTINUE;
        }
        
        // All processing is contained in a try block to ensure that at the end of processing the security header
        // is correctly marked as processed
        try {
            try {
                // The security header targeted to the default actor can contain a Signature, UsernameToken and Encryption. 
                actions = new ArrayList<Integer>(3);
                actions.add(WSConstants.UT);
                actions.add(WSConstants.SIGN);
                actions.add(WSConstants.ENCR);

                log.debug("Process the WSS header targeted to default role");
                results = processor.processSecurityHeader(null, actions);            
            } catch (WSSecurityException ex) {
                // An exception indicates a unexpected problem in processing the default WS-Sec header 
                handleWSSecurityException(mc, SecurityConstants.DEFAULT_WSS_HEADER, 
                                          SecurityConstants.WSS_FAILURES.UNKNOWN, ex);                
                // Further processing of the security header is useless, so continue with next handler
                return InvocationResponse.CONTINUE;            
            }            

            log.debug("Check results of processing security header targeted to default role");

            // Check decryption result
            WSSecurityEngineResult decResult = WSSecurityUtil.fetchActionResult(results, WSConstants.ENCR);
            if (decResult != null && decResult.containsKey(SecurityConstants.WSS_PROCESSING_FAILURE)) {
                // An exception has occurred during decryption, so decrypting the message failed
                handleWSSecurityException(mc, SecurityConstants.DEFAULT_WSS_HEADER, 
                                          SecurityConstants.WSS_FAILURES.DECRYPTION,
                                          (WSSecurityException) decResult.get(SecurityConstants.WSS_PROCESSING_FAILURE)
                                         );
                // No need to further process result or header
                return InvocationResponse.CONTINUE;
            }

            // Process result of Signature
            WSSecurityEngineResult signResult = WSSecurityUtil.fetchActionResult(results, WSConstants.SIGN);
            if (signResult != null) {
                if (signResult.containsKey(SecurityConstants.WSS_PROCESSING_FAILURE)) {
                    // An exception has occurred during signature processing
                    handleWSSecurityException(mc, SecurityConstants.DEFAULT_WSS_HEADER, 
                                           SecurityConstants.WSS_FAILURES.SIGNATURE,
                                           (WSSecurityException) signResult.get(SecurityConstants.WSS_PROCESSING_FAILURE)
                                           );
                    // No need to further process result or header
                    return InvocationResponse.CONTINUE;                 
                } else {
                    log.debug("Default WSS header contained a valid Signature");
                    // Transform into non WSS4J related object
                    java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) 
                                                            signResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                    X509Certificate hb2bCert = new X509Certificate(cert);
                    // Add the information to set of collected authentication info
                    authInfo.put(SecurityConstants.SIGNATURE, hb2bCert);            
                }
            }

            // Process result of UT 
            WSSecurityEngineResult utResult = WSSecurityUtil.fetchActionResult(results, WSConstants.UT);
            if (!processResultUT(utResult, null, authInfo)) {            
                // Indicate that username token processing failed
                handleWSSecurityException(mc, SecurityConstants.DEFAULT_WSS_HEADER, 
                                          SecurityConstants.WSS_FAILURES.UT,
                                          (WSSecurityException) decResult.get(SecurityConstants.WSS_PROCESSING_FAILURE)
                                         );
                // No need to further process result or header
                return InvocationResponse.CONTINUE;            
            }            
            log.debug("Done processing results of default WSS header");

            try {
                // The security header targeted to the ebms actor should only contain a Username token. 
                actions = new ArrayList<Integer>(1);
                actions.add(WSConstants.UT);

                log.debug("Process the WSS header targeted to ebms role");
                results = processor.processSecurityHeader(SecurityConstants.EBMS_WSS_HEADER, actions);            
            } catch (WSSecurityException ex) {
                // An exception here means there were unexpected elements in the WSS header targeted at the "ebms" role.
                handleWSSecurityException(mc, SecurityConstants.EBMS_WSS_HEADER, 
                                          SecurityConstants.WSS_FAILURES.UNKNOWN, ex);                
                // Further processing of the security header is useless, so continue with next handler
                return InvocationResponse.CONTINUE;            
            } 

            log.debug("Check results of processing security header targeted to ebms role");
            // Get result of UT processing
            utResult = WSSecurityUtil.fetchActionResult(results, WSConstants.UT);
            if (!processResultUT(utResult, SecurityConstants.EBMS_WSS_HEADER, authInfo)) {            
                // Indicate that username token processing failed
                handleWSSecurityException(mc, SecurityConstants.EBMS_WSS_HEADER, 
                                          SecurityConstants.WSS_FAILURES.UT,
                                          (WSSecurityException) utResult.get(SecurityConstants.WSS_PROCESSING_FAILURE)
                                         );
                // No need to further process result or header
                return InvocationResponse.CONTINUE;            
            }    
            // Check that there were no other elements in the "ebms" header
            if (results.size() > 1) {
                // Indicate problem as problem with "other" elements as we do not expect anything else than a UT
                handleWSSecurityException(mc, SecurityConstants.EBMS_WSS_HEADER, 
                                          SecurityConstants.WSS_FAILURES.UNKNOWN, 
                                          new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                                "WS-Security header targeted to \"ebms\" contains unexpected elements!")
                                         );                
                // No need to further process result or header
                return InvocationResponse.CONTINUE;                        
            }

            // Convert the processed SOAP envelope back to the Axiom representation for further processing
            SOAPEnvelope SOAPenv = Axis2Utils.convertToAxiom(domEnvelope);
            if (SOAPenv == null) {
                log.error("Converting the SOAP envelope to Axiom representation failed");
            } else {
                mc.setEnvelope(SOAPenv);
            }
        
            // Finally store all collected authentication info in the message context for further use
            mc.setProperty(SecurityConstants.MC_AUTHENTICATION_INFO, authInfo);            
        } finally {
            log.debug("Mark security headers as processed");
            SOAPHeader header = mc.getEnvelope().getHeader();
            if (header != null) {
                ArrayList<?> wsseHdrs = header.getHeaderBlocksWithNSURI(SecurityConstants.WSS_NAMESPACE_URI);
                for(Object h : wsseHdrs) {
                    SOAPHeaderBlock soapHdr = (SOAPHeaderBlock) h;
                    String target = soapHdr.getRole();
                    if (target == null || target.isEmpty() || SecurityConstants.EBMS_WSS_HEADER.equals(target))
                        soapHdr.setProcessed();
                }
                
                // Because the document structure has been rebuild the ebMS header also has to be set to processed again
                SOAPHeaderBlock messagingHdr = Messaging.getElement(mc.getEnvelope());
                if (messagingHdr != null) {
                    messagingHdr.setProcessed();
                } 
            }
            log.debug("Done processing security headers");
        }        
        
        return InvocationResponse.CONTINUE;
    }

    /**
     * Processes the result of username token processing. If successful the information from the WS-Security is 
     * converted to an {@link IAuthenticationInfo} object for later use. Because the username token can be provided in
     * both the default security header as well as in the header targeted at the "ebms" role the target of the header
     * must be supplied.
     * 
     * @param utResult  The WSS4J result object for the username token processing
     * @param role      The targeted role the WS-Security header the UT was found in
     * @param authInfo  The list of authentication information contained in the WS-Security headers
     * @return            <code>false</code> if the username token was not processed successfully,<br>
     *                    <code>true</code> otherwise     */
    protected boolean processResultUT(WSSecurityEngineResult utResult, String role, 
                                    Map<String, IAuthenticationInfo> authInfo) {
        if (utResult == null) {
            // No signature process, so no fault either
            return true;
        } else {
            if (utResult.containsKey(SecurityConstants.WSS_PROCESSING_FAILURE)) {
                // An exception has occurred during signature processing
                WSSecurityException ex = (WSSecurityException) utResult.get(SecurityConstants.WSS_PROCESSING_FAILURE);
                log.warn("Processing signature of message failed! Details: " + ex.getMsgID() + ";" + ex.getMessage());
                
                return false;
            } else {
                log.debug("WSS header targeted to " + role == null ? "default" : role  + " role contained UT");            
                // Transform into non WSS4J related object
                UsernameTokenPrincipal principal = (UsernameTokenPrincipal) 
                                                            utResult.get(WSSecurityEngineResult.TAG_PRINCIPAL);
                UsernameToken hb2bUT = new UsernameToken(principal);
                // Add the information to set of collected authentication info
                if (SecurityConstants.EBMS_WSS_HEADER.equals(role))
                    authInfo.put(SecurityConstants.EBMS_USERNAMETOKEN, hb2bUT);
                else
                    authInfo.put(SecurityConstants.DEFAULT_USERNAMETOKEN, hb2bUT);
                
                return true;
            }
        }   
    }

    /**
     * Helper method to handle a {@link WSSecurityException} that occurred in processing of the WS-Security header.
     * <p>This method logs the error and sets the message context property to indicate that the processing of the header
     * failed.
     * 
     * @param mc        The current message context
     * @param header    The target actor/role of the security header in which the problem occurred
     * @param part      The part (signature, encryption, ut, other) the problem occurred in
     * @param ex        The {@link WSSecurityException} that occurred
     */
    private void handleWSSecurityException(final MessageContext mc, 
                                           final String header, 
                                           final SecurityConstants.WSS_FAILURES part, 
                                           final WSSecurityException ex) {
        // Build the error message
        StringBuffer    errorMsg = new StringBuffer();        
        switch (part) {
            case DECRYPTION :
                errorMsg.append("Failed decryption: "); break;
            case SIGNATURE :
                errorMsg.append("Failed signature validation: "); break;
            case UT :
                errorMsg.append("Failed username token processing: "); break;
            case UNKNOWN :
                errorMsg.append("Processing of WS-Security header failed: "); break;
        }        
        errorMsg.append(ex.getMsgID()).append(';').append(ex.getMessage());
        
        // Drill down to root cause
        Throwable cause = ex.getCause();
        while (cause != null) {
            errorMsg.append(". Caused by: ").append(cause.getClass().getSimpleName());
            String msg = cause.getMessage();
            if ( msg != null && !msg.isEmpty())
                errorMsg.append(';').append(msg);
            
            cause = cause.getCause();
        }
        String errorMessage = errorMsg.toString();

        // Write error message to log
        log.warn(errorMessage);
        
        // Set the message context property to indicate the failed processing
        mc.setProperty( (SecurityConstants.EBMS_WSS_HEADER.equals(header) ? 
                            SecurityConstants.INVALID_EBMS_HEADER : SecurityConstants.INVALID_DEFAULT_HEADER)
                      , new KeyValuePair<SecurityConstants.WSS_FAILURES, String>(part, errorMessage)
                      );
    }
    
        

    /**
     * Is the inner class that does the actual processing of the WS Security header. It is based on {@link WSHandler}
     * provided by the Apache WSS4J library. Because the overall class already extends {@link BaseHandler} the inner
     * class construct is used. 
     */
    class WSSReceiveHandler extends WSHandler {

        /**
         * Logging facility
         */
        private Log log;
        /**
         * The current message context
         */
        private MessageContext msgCtx;
        /**
         * The WSS4J security engine that does the actual security processing
         */
        private WSSecurityEngine engine;
        /**
         * The DOM representation of the SOAP envelope
         */
        private Document domEnvelope;
        /**
         * Callback handler that provides access to the SOAP attachments
         */
        private CallbackHandler attachmentCBHandler;

        /**
         * Creates a WSSReceiveHandler for processing the security headers in the given message.
         *
         * @param mc    The {@link MessageContext} for the message
         * @param doc   The standard DOM representation of the SOAP Envelope of the message
         * @param log   The log to use. We use the log of the handler to hide this class.
         */
        WSSReceiveHandler(MessageContext mc, Document doc, Log handlerLog) throws WSSecurityException {
            this.msgCtx = mc;
            this.log = handlerLog;
            this.domEnvelope = doc;
            
            log.debug("Create security processing engine");
            engine = createSecurityEngine();
            attachmentCBHandler = new AttachmentCallbackHandler(msgCtx);
        }

        @Override
        public Object getOption(String string) {
            return msgCtx.getProperty(string);
        }

        @Override
        public Object getProperty(Object o, String string) {
            return getOption(string);
        }

        @Override
        public void setProperty(Object o, String string, Object o1) {
            msgCtx.setProperty(string, o1);
        }

        @Override
        public String getPassword(Object o) {
            return null;
        }

        @Override
        public void setPassword(Object o, String string) {
        }

        /**
         * Creates a {@link WSSecurityEngine} with a customized configuration to prevent checking the password of
         * username tokens.
         *
         * @return A {@link WSSecurityEngine} that will not validate username token passwords.
         */
        protected WSSecurityEngine createSecurityEngine() {
            final WSSConfig config = WSSConfig.getNewInstance();
            config.setValidator(WSSecurityEngine.USERNAME_TOKEN, new NoOpValidator());

            final WSSecurityEngine engine = new WSSProcessingEngine();
            engine.setWssConfig(config);

            return engine;
        }

        List<WSSecurityEngineResult> processSecurityHeader(String actor, List<Integer> actions)
                throws WSSecurityException {
            
            if (actions == null || actions.isEmpty()) {
                log.warn("No security actions specified!");
                return null;
            }
            
            log.debug("Configure security processing environment");
            RequestData reqData = new RequestData();
            reqData.setWssConfig(engine.getWssConfig());
            reqData.setMsgContext(msgCtx);
            
            // Register callback handler for attachments
            reqData.setAttachmentCallbackHandler(this.attachmentCBHandler);

            // Set up a WSS4J AlgorithmSuite. Using the decodeAlgorithmSuite allows for using specific signing and
            // hashing algorithms using message context properties (see WSS4J documentation) 
            super.decodeAlgorithmSuite(reqData);

            // Set up configuration for specified actions
            doReceiverAction(actions, reqData);            

            // Set up revocation check
            boolean enableRevocation = reqData.isRevocationEnabled();
            reqData.setEnableRevocation(enableRevocation);
            
            log.debug("Get security header element for target: " + actor);
            Element headerElem = WSSecurityUtil.getSecurityHeader(domEnvelope, actor);

            return engine.processSecurityHeader(headerElem, reqData);
        }
    }
}

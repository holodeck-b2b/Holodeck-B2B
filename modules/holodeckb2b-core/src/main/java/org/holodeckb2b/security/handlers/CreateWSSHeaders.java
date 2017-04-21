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
package org.holodeckb2b.security.handlers;

import java.util.List;
import java.util.Properties;
import javax.security.auth.callback.CallbackHandler;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.handler.HandlerAction;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.holodeckb2b.axis2.Axis2Utils;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.DefaultSecurityAlgorithm;
import org.holodeckb2b.ebms3.constants.SecurityConstants;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.pmode.security.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.security.IKeyTransport;
import org.holodeckb2b.interfaces.pmode.security.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.security.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.pmode.security.X509ReferenceType;
import org.holodeckb2b.security.callbackhandlers.AttachmentCallbackHandler;
import org.holodeckb2b.security.callbackhandlers.PasswordCallbackHandler;
import org.holodeckb2b.security.util.SecurityUtils;
import org.w3c.dom.Document;

/**
 * Is the <i>OUT_FLOW</i> handler that creates the WS-Security headers in the outgoing message. It uses the WSS4J
 * library for the actual work of adding the headers.
 * <p>The handler can add two security headers, one default and one targeted to the "ebms" role/actor. The latter can
 * only contain a <code>wsse:UsernameToken</code> element for authentication and authorization purposes (see section
 * 7.10 of the ebMS v3 Core Specification). The default can also contain the signature and encrypted data of the message
 * <p>Whether security headers must be create should be indicated by the message context property {@link
 * SecurityConstants#ADD_SECURITY_HEADERS}. If it contains the <code>Boolean.TRUE</code> the headers will be created.
 * The configuration for the information to be included in the headers should also be specified in message properties
 * as shown in the table below:
 * <table border="1">
 * <tr><td>Property key</td><td>Configuration interface</td><td>Contains configuration for</td></tr>
 * <tr><td>{@link SecurityConstants#EBMS_USERNAMETOKEN}</td>
 *              <td>{@link IUsernameTokenConfiguration}</td>
 *              <td>The <code>UsernameToken</code> targeted at the <i>ebms</i> role</td></tr>
 * <tr><td>{@link SecurityConstants#DEFAULT_USERNAMETOKEN}</td>
 *              <td>{@link IUsernameTokenConfiguration}</td>
 *              <td>The <code>UsernameToken</code> targeted at the <i>default</i> role</td></tr>
 * <tr><td>{@link SecurityConstants#SIGNATURE}</td>
 *              <td>{@link ISigningConfiguration}</td>
 *              <td>The <code>Signature</code> to create in the <i>default</i> header</td></tr>
 * </table>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CreateWSSHeaders extends BaseHandler {

    protected static final String WSS4J_PART_EBMS_HEADER = "{}{" + EbMSConstants.EBMS3_NS_URI + "}Messaging;";

    protected static final String WSS4J_PART_S11_BODY = "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;";
    protected static final String WSS4J_PART_S12_BODY = "{}{http://www.w3.org/2003/05/soap-envelope}Body;";

    protected static final String WSS4J_PART_UT = "{}{" + SecurityConstants.WSS_NAMESPACE_URI + "}UsernameToken;";

    protected static final String WSS4J_PART_ATTACHMENTS = "{}cid:Attachments;";

    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws AxisFault {
        CreateWSSHeaders.WSSSendHandler processor = null;

        // Check if security headers must be added
        final Boolean addHeaders = (Boolean) mc.getProperty(SecurityConstants.ADD_SECURITY_HEADERS);
        if (addHeaders == null || !addHeaders) {
            log.debug("No security headers to add, skip processing");
            return InvocationResponse.CONTINUE;
        }

        // Convert the SOAP Envelope to standard DOM representation as this is required by the security processing
        // libraries
        final Document domEnvelope = Axis2Utils.convertToDOM(mc);

        if (domEnvelope == null) {
            logError(mc, null, "Converting the SOAP envelope to DOM representation failed");
            return InvocationResponse.ABORT;
        }

        try {
            // Create security header processor
            processor = new CreateWSSHeaders.WSSSendHandler(mc, domEnvelope, log);
        } catch (final WSSecurityException ex) {
            logError(mc, null, "Setting up the security processor failed (" + ex.getMessage() + ")");
            return InvocationResponse.ABORT;
        }

        // Set up the message context properties specific for the header targeted to ebms role. This can only contain
        // a username token
        IUsernameTokenConfiguration  utConfig = (IUsernameTokenConfiguration)
                                                                  mc.getProperty(SecurityConstants.EBMS_USERNAMETOKEN);

        if (utConfig != null) {
            log.debug("A UsernameToken element must be added to the security header targeted to ebms role");
            // Create a password callback handler to hand over the password
            final PasswordCallbackHandler pwdCBHandler = new PasswordCallbackHandler();
            mc.setProperty(ConfigurationConstants.PW_CALLBACK_REF, pwdCBHandler);

            setupUsernameToken(mc, utConfig, pwdCBHandler);
            try {
                // The security header targeted to the ebms actor should only contain a Username token.
                log.debug("Add the WSS header targeted to ebms role");
                processor.createSecurityHeader(SecurityConstants.EBMS_WSS_HEADER, ConfigurationConstants.USERNAME_TOKEN);
                log.debug("Added the WSS header targeted to ebms role");
            } catch (final WSSecurityException wse) {
                logError(mc, "ebms", wse);
                return InvocationResponse.ABORT;
            }
            utConfig = null; // reset usernametoken config
        }

        // Set up the message context properties for the default WSS header. This header can also include signing and
        // encryption
        // The actions that need to be executed
        String  actions = "";
        // Create a password callback handler to hand over the password
        final PasswordCallbackHandler pwdCBHandler = new PasswordCallbackHandler();
        mc.setProperty(ConfigurationConstants.PW_CALLBACK_REF, pwdCBHandler);

        // Check if a UsernameToken element should be added
        utConfig = (IUsernameTokenConfiguration) mc.getProperty(SecurityConstants.DEFAULT_USERNAMETOKEN);
        if (utConfig != null) {
            log.debug("A UsernameToken element must be added to the security header targeted to ebms role");
            // Add UT action
            actions = ConfigurationConstants.USERNAME_TOKEN;
            // Set up message context
            setupUsernameToken(mc, utConfig, pwdCBHandler);
        }

        final ISigningConfiguration signatureCfg = (ISigningConfiguration) mc.getProperty(SecurityConstants.SIGNATURE);
        if (signatureCfg != null) {
            log.debug("The message must be signed, set up signature configuration");
            // Add Signature action
            actions += " " + ConfigurationConstants.SIGNATURE;
            // Set up message context
            setupSignature(mc, signatureCfg, pwdCBHandler);
        }

        final IEncryptionConfiguration encryptCfg = (IEncryptionConfiguration) mc.getProperty(SecurityConstants.ENCRYPTION);
        if (encryptCfg != null) {
            log.debug("The message must be encrypted, set up encryption configuration");
            // Add encryption action
            actions += " " + ConfigurationConstants.ENCRYPT;
            // Set up message context
            setupEncryption(mc, encryptCfg, pwdCBHandler);
        }

        try {
            log.debug("Add the default WSS header");
            processor.createSecurityHeader(null, actions);
            log.debug("Added the default WSS header");
        } catch (final WSSecurityException wse) {
            logError(mc, "default", wse);
            return InvocationResponse.ABORT;
        }

        // The call of the Document.normalizeDocument() method is to fix the exception described here:
        // http://apache-xml-project.6118.n7.nabble.com/Undeclared-namespace-prefix-quot-ds-quot-error-td36346.html
        domEnvelope.normalizeDocument();
        // Convert the processed SOAP envelope back to the Axiom representation for further processing
        final SOAPEnvelope SOAPenv = Axis2Utils.convertToAxiom(domEnvelope);

        if (SOAPenv == null) {
            logError(mc, null, "Converting the SOAP envelope to Axiom representation failed");
            return InvocationResponse.ABORT;
        } else {
            mc.setEnvelope(SOAPenv);
            log.debug("Security header(s) successfully added");
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Sets the message context properties for adding a UsernameToken to the security header.
     * <p>Because other elements that need to be added to the header may also require a password the password callback
     * handler is not created in this method, but shared for the header.
     *
     * @param mc            The {@link MessageContext} to set up
     * @param utConfig      The configuration for the username token
     * @param pwdCBHandler  The {@link PasswordCallbackHandler} to use for handing over the password to WSS4J library
     */
    private void setupUsernameToken(final MessageContext mc, final IUsernameTokenConfiguration utConfig,
                                    final PasswordCallbackHandler pwdCBHandler) {
        mc.setProperty(ConfigurationConstants.USER, utConfig.getUsername());
        mc.setProperty(ConfigurationConstants.ADD_USERNAMETOKEN_CREATED, Boolean.toString(utConfig.includeCreated()));
        mc.setProperty(ConfigurationConstants.ADD_USERNAMETOKEN_NONCE, Boolean.toString(utConfig.includeNonce()));

        mc.setProperty(ConfigurationConstants.PASSWORD_TYPE,
                                        utConfig.getPasswordType() == IUsernameTokenConfiguration.PasswordType.DIGEST ?
                                        WSConstants.PW_DIGEST : WSConstants.PW_TEXT);

        pwdCBHandler.addUser(utConfig.getUsername(), utConfig.getPassword());
    }

    /**
     * Sets the message context properties for adding a Signature to the security header.
     * <p>Because other elements that need to be added to the header may also require a password the password callback
     * handler is not created in this method, but shared for the header.
     *
     * @param mc            The {@link MessageContext} to set up
     * @param sigCfg     The configuration for creating the signature
     * @param pwdCBHandler  The {@link PasswordCallbackHandler} to use for handing over the password to WSS4J library
     */
    private void setupSignature(final MessageContext mc, final ISigningConfiguration sigCfg, final PasswordCallbackHandler pwdCBHandler) {
        // Set up crypto engine
        final Properties sigProperties = SecurityUtils.createCryptoConfig(SecurityUtils.CertType.priv);
        mc.setProperty(ConfigurationConstants.SIG_PROP_REF_ID, "" + sigProperties.hashCode());
        mc.setProperty("" + sigProperties.hashCode(), sigProperties);

        // Set up signing config
        // AS4 requires that the ebMS message header (eb:Messaging element) and SOAP Body are signed
        mc.setProperty(ConfigurationConstants.SIGNATURE_PARTS, WSS4J_PART_EBMS_HEADER
                                                                            + (mc.isSOAP11() ? WSS4J_PART_S11_BODY :
                                                                                               WSS4J_PART_S12_BODY));
        // And if there are attachments also the attachments. Whether UsernameToken elements in the security header
        // should be signed is not specified. But to prevent manipulation Holodeck B2B includes them in the signature
        mc.setProperty(ConfigurationConstants.OPTIONAL_SIGNATURE_PARTS, WSS4J_PART_UT + WSS4J_PART_ATTACHMENTS);

        // The alias of the certificate to use for signing, converted to lower case because JKS aliasses are case
        // insensitive
        mc.setProperty(ConfigurationConstants.SIGNATURE_USER, sigCfg.getKeystoreAlias().toLowerCase());
        // The password to access the certificate in the keystore
        pwdCBHandler.addUser(sigCfg.getKeystoreAlias().toLowerCase(), sigCfg.getCertificatePassword());

        // How should certificate be referenced in header?
        mc.setProperty(ConfigurationConstants.SIG_KEY_ID,
                                    SecurityUtils.getWSS4JX509KeyId((sigCfg.getKeyReferenceMethod() != null ?
                                                                     sigCfg.getKeyReferenceMethod() :
                                                                     DefaultSecurityAlgorithm.KEY_REFERENCE)));
        // If BST is included, should complete cert path be included?
        if (sigCfg.getKeyReferenceMethod() == X509ReferenceType.BSTReference
                && (sigCfg.includeCertificatePath() != null ? sigCfg.includeCertificatePath() : false))
            mc.setProperty(ConfigurationConstants.USE_SINGLE_CERTIFICATE, "false");
        else
            mc.setProperty(ConfigurationConstants.USE_SINGLE_CERTIFICATE, "true");

        // Algorithms to use
        mc.setProperty(ConfigurationConstants.SIG_DIGEST_ALGO,
                                    Utils.getValue(sigCfg.getHashFunction(), DefaultSecurityAlgorithm.MESSAGE_DIGEST));
        mc.setProperty(ConfigurationConstants.SIG_ALGO,
                                    Utils.getValue(sigCfg.getSignatureAlgorithm(), DefaultSecurityAlgorithm.SIGNATURE));
    }


    /**
     * Sets the message context properties for adding encryption to the security header.
     * <p>Because other elements that need to be added to the header may also require a password the password callback
     * handler is not created in this method, but shared for the header.
     *
     * @param mc            The {@link MessageContext} to set up
     * @param encCfg     The configuration for creating the signature
     * @param pwdCBHandler  The {@link PasswordCallbackHandler} to use for handing over the password to WSS4J library
     */
    private void setupEncryption(final MessageContext mc, final IEncryptionConfiguration encCfg,
                                 final PasswordCallbackHandler pwdCBHandler) {
        // Set up crypto engine
        final Properties encProperties = SecurityUtils.createCryptoConfig(SecurityUtils.CertType.pub);
        mc.setProperty(ConfigurationConstants.ENC_PROP_REF_ID, "" + encProperties.hashCode());
        mc.setProperty("" + encProperties.hashCode(), encProperties);

        // Set up encryption config
        // AS4 requires that only the payloads are encrypted, so we encrypt the Body only when it contains a payload
        final Boolean includesBodyPayload = (Boolean) mc.getProperty(SecurityConstants.ENCRYPT_BODY);
        if (includesBodyPayload != null && includesBodyPayload)
            mc.setProperty(ConfigurationConstants.ENCRYPTION_PARTS, (mc.isSOAP11() ? WSS4J_PART_S11_BODY :
                                                                                 WSS4J_PART_S12_BODY));

        // And if there are attachments also the attachments must be encrypted.
        mc.setProperty(ConfigurationConstants.OPTIONAL_ENCRYPTION_PARTS, WSS4J_PART_ATTACHMENTS);

        // Symmetric encryption algorithms to use
        mc.setProperty(ConfigurationConstants.ENC_SYM_ALGO,
                                            Utils.getValue(encCfg.getAlgorithm(), DefaultSecurityAlgorithm.ENCRYPTION));

        // The alias of the certificate to use for encryption
        mc.setProperty(ConfigurationConstants.ENCRYPTION_USER, encCfg.getKeystoreAlias());

        // KeyTransport configuration defines settings for constructing the xenc:EncryptedKey
        // Set defaults
        String            ktAlgorithm = DefaultSecurityAlgorithm.KEY_TRANSPORT;
        X509ReferenceType ktKeyReference = DefaultSecurityAlgorithm.KEY_REFERENCE;
        String            ktDigest = DefaultSecurityAlgorithm.MESSAGE_DIGEST;

        final IKeyTransport   ktConfig = encCfg.getKeyTransport();

        if (ktConfig != null) {
            // Key encryption algorithm
            ktAlgorithm = Utils.getValue(ktConfig.getAlgorithm(), DefaultSecurityAlgorithm.KEY_TRANSPORT);
            // If key transport algorithm is RSA-OAEP also the MGF must be set
            if (WSConstants.KEYTRANSPORT_RSAOEP_XENC11.equalsIgnoreCase(ktAlgorithm))
                mc.setProperty(ConfigurationConstants.ENC_MGF_ALGO, ktConfig.getMGFAlgorithm());
            // Message digest
            ktDigest = Utils.getValue(ktConfig.getDigestAlgorithm(), DefaultSecurityAlgorithm.MESSAGE_DIGEST);

            // Key refence method
            if (ktConfig.getKeyReferenceMethod() != null)
                ktKeyReference = ktConfig.getKeyReferenceMethod();
        }

        // Set the relevant message context properties
        mc.setProperty(ConfigurationConstants.ENC_KEY_ID, SecurityUtils.getWSS4JX509KeyId(ktKeyReference));
        mc.setProperty(ConfigurationConstants.ENC_DIGEST_ALGO, ktDigest);
        mc.setProperty(ConfigurationConstants.ENC_KEY_TRANSPORT, ktAlgorithm);
    }

    /**
     * Helper method to log error about failure to create the security headers to the message.
     *
     * @param mc        The message context (used to get info about message being processed)
     * @param role      The role of the WS-Security that was being created, can be null for general error
     * @param message   Specific error message to log
     */
    private void logError(final MessageContext mc, final String role, final String message) {
        final StringBuffer logMsg = new StringBuffer();
        // Use primary message unit to log error
        final IMessageUnit  pMU = MessageContextUtils.getPrimaryMessageUnit(mc);

        logMsg.append("Could not create WS-Security header");
        if (Utils.isNullOrEmpty(role))
            logMsg.append("(s)");
        else
            logMsg.append(" targeted to ").append(role);
        logMsg.append(" for message with primary message unit [")
              .append(MessageUnitUtils.getMessageUnitName(pMU))
              .append(";msgID=").append(pMU.getMessageId()).append(']')
              .append("\n\tError details: ").append(message);

        log.error(logMsg.toString());
    }

    /**
     * Helper method to log error about failure to create the security headers to the message
     *
     * @param mc        The message context (used to get info about message being processed)
     * @param role      The role of the WS-Security that was being created, can be null for general error
     * @param e         The exception that occurred
     */
    private void logError(final MessageContext mc, final String role, final Exception e) {
       // Create a error message that drills down to root cause
       final StringBuffer exMsg = new StringBuffer();

       exMsg.append(e.getClass().getSimpleName()).append(" : ").append(e.getMessage());
       Throwable cause = e.getCause();
       if (cause != null) {
           do {
               exMsg.append('\n').append('\t').append("Caused by: ").append(cause.getClass().getSimpleName());
               exMsg.append(" : ").append(cause.getMessage());
               cause = cause.getCause();
           } while (cause != null);
       }

        logError(mc, role, exMsg.toString());
    }



    /**
     * Is the inner class that does the actual processing of the WS Security header. It is based on {@link WSHandler}
     * provided by the Apache WSS4J library. Because the overall class already extends {@link BaseHandler} the inner
     * class construct is used.
     */
    class WSSSendHandler extends WSHandler {
        /**
         * Logging facility
         */
        private final Log log;
        /**
         * The current message context
         */
        private final MessageContext msgCtx;
        /**
         * The WSS4J security engine configuration
         */
        private final WSSConfig wssConfig;
        /**
         * The DOM representation of the SOAP envelope
         */
        private final Document domEnvelope;
        /**
         * Callback handler that provides access to the SOAP attachments
         */
        private final CallbackHandler attachmentCBHandler;

        /**
         * Creates a WSSSendHandler for creating the security headers in the given message.
         *
         * @param mc    The {@link MessageContext} for the message
         * @param doc   The standard DOM representation of the SOAP Envelope of the message
         * @param handlerLog   The log to use. We use the log of the handler to hide this class.
         */
        WSSSendHandler(final MessageContext mc, final Document doc, final Log handlerLog) throws WSSecurityException {
            this.msgCtx = mc;
            this.log = handlerLog;
            this.domEnvelope = doc;

            log.debug("Set up security engine configuration");
            wssConfig = WSSConfig.getNewInstance();
            attachmentCBHandler = new AttachmentCallbackHandler(msgCtx);
        }

        @Override
        public Object getOption(final String string) {
            return msgCtx.getProperty(string);
        }

        @Override
        public Object getProperty(final Object o, final String string) {
            return getOption(string);
        }

        @Override
        public void setProperty(final Object o, final String string, final Object o1) {
            msgCtx.setProperty(string, o1);
        }

        @Override
        public String getPassword(final Object o) {
            return null;
        }

        @Override
        public void setPassword(final Object o, final String string) {
        }

        void createSecurityHeader(final String actor, final String actions) throws WSSecurityException {
            log.debug("Check actions to perform");
            if (actions == null || actions.isEmpty()) {
                log.info("No security actions specified!");
                return;
            }
            final List<HandlerAction> actionList = WSSecurityUtil.decodeHandlerAction(actions.trim(), wssConfig);
            if (actionList == null || actionList.isEmpty()) {
                log.info("No security actions specified!");
                return;
            }

            log.debug("Configure security processing environment");
            msgCtx.setProperty(ConfigurationConstants.ACTOR, actor);
            final RequestData reqData = new RequestData();
            reqData.setWssConfig(wssConfig);
            reqData.setMsgContext(msgCtx);
            // Register callback handler for attachments
            reqData.setAttachmentCallbackHandler(this.attachmentCBHandler);

            // Check if we need a username (for UsernameToken or Signature)
            boolean userNameRequired = false;
            for (final HandlerAction handlerAction : actionList) {
                if ((handlerAction.getAction() == WSConstants.SIGN
                    || handlerAction.getAction() == WSConstants.UT
                    || handlerAction.getAction() == WSConstants.UT_SIGN)
                    && (handlerAction.getActionToken() == null
                        || handlerAction.getActionToken().getUser() == null)) {
                    userNameRequired = true;
                    break;
                }
            }
            if (userNameRequired) {
                log.debug("A user name is needed because a username token or signature has to be inserted");
                String userName = (String) getOption(ConfigurationConstants.USER);
                if (userName == null || userName.isEmpty())
                    userName = (String) getOption(ConfigurationConstants.SIGNATURE_USER);
                log.debug("Username for creating the " + actor + " WSS header is set to " + userName);

                if (userName == null || userName.isEmpty()) {
                    // We need a username but don't have one :-(
                    log.error("Required username for creating the WSS header is missing!");
                    //throw new AxisFault("NO_USERNAME");
                } else
                    reqData.setUsername(userName);
            }

            // Are we processing a request or response?
            final boolean isRequest = isInFlow(INITIATOR);

            log.debug("Create the \"" + actor + "\" WSS headers for this " + (isRequest ? "request." : "response."));
            doSenderAction(domEnvelope, reqData, actionList, isRequest);
            log.debug("WSS header created.");
        }

    }
}

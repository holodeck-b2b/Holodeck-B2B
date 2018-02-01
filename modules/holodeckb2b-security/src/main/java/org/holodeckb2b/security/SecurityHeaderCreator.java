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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.EncryptionActionToken;
import org.apache.wss4j.common.SignatureActionToken;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.handler.HandlerAction;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.IKeyTransport;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.ICertificateManager.CertificateUsage;
import org.holodeckb2b.interfaces.security.ISecurityHeaderCreator;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.UTPasswordType;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import static org.holodeckb2b.security.Action.*;
import org.holodeckb2b.security.callbackhandlers.AttachmentCallbackHandler;
import org.holodeckb2b.security.callbackhandlers.PasswordCallbackHandler;
import org.holodeckb2b.security.results.EncryptionProcessingResult;
import org.holodeckb2b.security.results.HeaderProcessingFailure;
import org.holodeckb2b.security.results.SignatureProcessingResult;
import org.holodeckb2b.security.results.UsernameTokenProcessingResult;
import org.holodeckb2b.security.util.Axis2XMLUtils;
import org.holodeckb2b.security.util.EncryptionConfigWithDefaults;
import org.holodeckb2b.security.util.SecurityUtils;
import org.holodeckb2b.security.util.SignedMessagePartsInfo;
import org.holodeckb2b.security.util.SigningConfigWithDefaults;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Is the default security provider's implementation of the {@link ISecurityHeaderCreator} which is responsible for the
 * creation of the WS-Security headers in outgoing messages. It uses the WSS4J framework for the actual creation of the
 * headers.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class SecurityHeaderCreator extends WSHandler implements ISecurityHeaderCreator {

    private static Logger  log = LogManager.getLogger(SecurityHeaderCreator.class);

    /**
     * The Certificate manager of the security provider
     */
    private CertificateManager certManager;

    /**
     * Map for holding the WSS4J parameters for creating the header
     */
    private Map<String, Object>  processingParams;

    /**
     * Reference to the current message context
     */
    private MessageContext       msgContext;

    /**
     * Collection of all Message Units in the processed message
     */
    private Collection<? extends IMessageUnit> msgUnits;

    /**
     * DOM version of the SOAP envelope as this is needed for WSS4J processing
     */
    private Document domEnvelope;

    /**
     * The applied signing configuration
     */
    private HashMap<SecurityHeaderTarget, IUsernameTokenConfiguration>   usernameTokenConfigs = new HashMap<>(2);

    /**
     * The applied signing configuration
     */
    private ISigningConfiguration   signingConfig;

    /**
     * The applied encryption configuration
     */
    private IEncryptionConfiguration   encryptionConfig;

    /**
     * Creates a new <code>SecurityHeaderCreator</code> instance
     *
     * @param certManager The certificate manager of the provider
     */
    SecurityHeaderCreator(final CertificateManager certManager) {
        this.certManager = certManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ISecurityProcessingResult> createHeaders(MessageContext mc,
                                                               Collection<? extends IMessageUnit> msgUnits,
                                                               ISecurityConfiguration senderConfig,
                                                               ISecurityConfiguration receiverConfig)
                                                                                    throws SecurityProcessingException {
        // Copy reference to message context
        this.msgContext = mc;
        this.msgUnits = msgUnits;

        // Convert the SOAP Envelope to standard DOM representation as this is required by the security processing
        // libraries
        log.debug("Converting envelope to DOM structure for processing");
        domEnvelope = Axis2XMLUtils.convertAxiomSOAPEnvToDOM(msgContext);

        if (domEnvelope == null) {
            log.error("Converting the SOAP envelope to DOM representation failed");
            throw new SecurityProcessingException("Could not convert SOAP envelope to DOM object!");
        }

        // Setup WSS4J configuration
        log.debug("Set up security engine configuration");
        List<HandlerAction> actions = new ArrayList<>();
        Collection<ISecurityProcessingResult> results = new ArrayList<>();
        //
        // First create the ebms targeted header as it should be included in signature of default header (if signed)
        //
        // The header targeted to ebms role can only contain a username token
        IUsernameTokenConfiguration  utConfig = senderConfig != null ?
                                                   senderConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.EBMS)
                                                 : null;
        if (utConfig != null) {
            log.debug("A UsernameToken element must be added to the security header targeted to ebms role");
            // Store configuration for later use
            usernameTokenConfigs.put(SecurityHeaderTarget.EBMS, utConfig);
            // Set processing parameters for creating the username token
            actions.add(new HandlerAction(WSConstants.UT));
            processingParams = new HashMap<>();
            // Create a password callback handler to hand over the password(s) for username token
            processingParams.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
            setupUsernameToken(SecurityHeaderTarget.EBMS);
            try {
                log.debug("Add the WSS header targeted to ebms role");
                createSecurityHeader(SecurityHeaderTarget.EBMS, actions);
                log.debug("Added the WSS header targeted to ebms role, prepare result info for Core");
                results.add(getUsernameTokenResults(SecurityHeaderTarget.EBMS));
            } catch (HeaderProcessingFailure hpf) {
                log.error("A problem occurred when adding the username token to ebms header!" +
                          "\n\tDetails: {}", hpf.getMessage());
                results.add(new UsernameTokenProcessingResult(SecurityHeaderTarget.EBMS, hpf));
            }
        }
        //
        // Now create the "default" targeted header that includes the signing, encryption and maybe another username
        // token
        //
        log.debug("Setup configuration for default header");
        // Set up the processing parameters
        processingParams = new HashMap<>();
        // Create a password callback handler to hand over the password(s) for username token and access to private key
        // for signing
        processingParams.put(ConfigurationConstants.PW_CALLBACK_REF, new PasswordCallbackHandler());
        // Clear list of actions that need to be executed
        actions.clear();

        // Check if a UsernameToken element should be added
        utConfig = senderConfig != null ? senderConfig.getUsernameTokenConfiguration(SecurityHeaderTarget.DEFAULT)
                                        : null;
        if (utConfig != null) {
            log.debug("A UsernameToken element must be added to the default security header");
            usernameTokenConfigs.put(SecurityHeaderTarget.DEFAULT, utConfig);
            setupUsernameToken(SecurityHeaderTarget.DEFAULT);
            // Add UT action
            actions.add(new HandlerAction(WSConstants.UT));
        }
        // Check if message must be signed
        final ISigningConfiguration signatureCfg = senderConfig != null ? senderConfig.getSignatureConfiguration()
                                                                        : null;
        if (signatureCfg != null) {
            log.debug("The message must be signed, set up signature configuration");
            // To ensure use of default algorithms, use decorator
            signingConfig = new SigningConfigWithDefaults(signatureCfg);
            setupSigning();
            // Add Signature action
            actions.add(new HandlerAction(WSConstants.SIGN));
        }
        // Check if message must also be encrypted
        final IEncryptionConfiguration encryptionCfg = receiverConfig != null ?
                                                                            receiverConfig.getEncryptionConfiguration()
                                                                          : null;
        if (encryptionCfg != null) {
            log.debug("The message must be encrypted, set up encryption configuration");
            encryptionConfig = new EncryptionConfigWithDefaults(encryptionCfg);
            setupEncryption();
            // Add encryption action
            actions.add(new HandlerAction(WSConstants.ENCR));
        }
        if (Utils.isNullOrEmpty(actions))
            log.debug("No default WSS header needed");
        else {
            try {
                log.debug("Add the default WSS header");
                createSecurityHeader(SecurityHeaderTarget.DEFAULT, actions);
                log.debug("Added the default WSS header, prepare result info for Core");
                for (HandlerAction action : actions) {
                    switch (action.getAction()) {
                        case WSConstants.SIGN :
                            results.add(getSignatureResults());
                            break;
                        case WSConstants.ENCR :
                            results.add(getEncryptionResults());
                            break;
                        case WSConstants.UT :
                            results.add(getUsernameTokenResults(SecurityHeaderTarget.DEFAULT));
                    }
                }
                log.debug("Security header(s) successfully added");
            } catch (HeaderProcessingFailure hpf) {
                log.error("A problem occurred when executing the {} action in the default header!" +
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
        }
        // Convert the processed SOAP envelope back to the Axiom representation for further processing
        SOAPEnvelope SOAPenv = Axis2XMLUtils.convertDOMSOAPEnvToAxiom(domEnvelope);
        try {
            msgContext.setEnvelope(SOAPenv);
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
     * Sets the processing parameters for encryption of the message
     */
    private void setupEncryption() {
        // AS4 requires that only the payloads are encrypted, so we encrypt the Body only when it contains a payload
        if (!Utils.isNullOrEmpty(msgUnits)
           && msgUnits.stream().filter(msgUnit -> msgUnit instanceof IUserMessage)
                               .map(userMsg -> ((IUserMessage) userMsg).getPayloads())
                               .filter(umPayloads -> !Utils.isNullOrEmpty(umPayloads))
                               .anyMatch(umPayloads -> umPayloads.stream()
                                                  .anyMatch(p -> p.getContainment() == IPayload.Containment.BODY)))
            processingParams.put(ConfigurationConstants.ENCRYPTION_PARTS, (msgContext.isSOAP11() ?
                    SecurityConstants.WSS4J_PART_S11_BODY :
                    SecurityConstants.WSS4J_PART_S12_BODY));
        // And if there are attachments also the attachments must be encrypted.
        processingParams.put(ConfigurationConstants.OPTIONAL_ENCRYPTION_PARTS,
                             SecurityConstants.WSS4J_PART_ATTACHMENTS);
        // Symmetric encryption algorithms to use
        processingParams.put(ConfigurationConstants.ENC_SYM_ALGO, encryptionConfig.getAlgorithm());
        // The alias of the certificate to use for encryption
        processingParams.put(ConfigurationConstants.ENCRYPTION_USER, encryptionConfig.getKeystoreAlias());

        // KeyTransport configuration defines settings for constructing the xenc:EncryptedKey
        final IKeyTransport   ktConfig = encryptionConfig.getKeyTransport();
        // Key encryption algorithm
        final String ktAlgorithm = ktConfig.getAlgorithm();
        processingParams.put(ConfigurationConstants.ENC_KEY_TRANSPORT, ktAlgorithm);
        // If key transport algorithm is RSA-OAEP also the MGF must be set
        if (WSConstants.KEYTRANSPORT_RSAOEP_XENC11.equalsIgnoreCase(ktAlgorithm))
            processingParams.put(ConfigurationConstants.ENC_MGF_ALGO, ktConfig.getMGFAlgorithm());
        // Message digest
        processingParams.put(ConfigurationConstants.ENC_DIGEST_ALGO, ktConfig.getDigestAlgorithm());
        // Key refence method
        processingParams.put(ConfigurationConstants.ENC_KEY_ID,
                                SecurityUtils.getWSS4JX509KeyId(ktConfig.getKeyReferenceMethod()));
    }

    /**
     * Sets the processing parameters for signing the message
     */
    private void setupSigning() {
        // AS4 requires that the ebMS message header (eb:Messaging element) and SOAP Body are signed
        processingParams.put(ConfigurationConstants.SIGNATURE_PARTS, SecurityConstants.WSS4J_PART_EBMS_HEADER
                + (msgContext.isSOAP11() ? SecurityConstants.WSS4J_PART_S11_BODY :
                        SecurityConstants.WSS4J_PART_S12_BODY));
        // And if there are attachments also the attachments. Whether UsernameToken elements in the security header
        // should be signed is not specified. But to prevent manipulation Holodeck B2B includes them
        processingParams.put(ConfigurationConstants.OPTIONAL_SIGNATURE_PARTS, SecurityConstants.WSS4J_PART_UT
                + SecurityConstants.WSS4J_PART_ATTACHMENTS);

        // The alias of the certificate to use for signing, converted to lower case because JKS aliasses are case
        // insensitive
        processingParams.put(ConfigurationConstants.SIGNATURE_USER, signingConfig.getKeystoreAlias().toLowerCase());
        // The password to access the certificate in the keystore
        ((PasswordCallbackHandler) processingParams.get(ConfigurationConstants.PW_CALLBACK_REF))
                .addUser(signingConfig.getKeystoreAlias().toLowerCase(), signingConfig.getCertificatePassword());

        // How should certificate be referenced in header? We need to map the P-Mode value to the WSS4J value
        processingParams.put(ConfigurationConstants.SIG_KEY_ID,
                                SecurityUtils.getWSS4JX509KeyId(signingConfig.getKeyReferenceMethod()));
        // If BST is included, should complete cert path be included?
        if (signingConfig.getKeyReferenceMethod() == X509ReferenceType.BSTReference
                && (signingConfig.includeCertificatePath() != null ? signingConfig.includeCertificatePath() : false))
            processingParams.put(ConfigurationConstants.USE_SINGLE_CERTIFICATE, "false");
        else
            processingParams.put(ConfigurationConstants.USE_SINGLE_CERTIFICATE, "true");

        // Algorithms to use
        processingParams.put(ConfigurationConstants.SIG_DIGEST_ALGO, signingConfig.getHashFunction());
        processingParams.put(ConfigurationConstants.SIG_ALGO, signingConfig.getSignatureAlgorithm());
    }

    /**
     * Sets the processing parameters for creating a Username token header
     *
     * @param target    The security header for which to setup the Username token
     */
    private void setupUsernameToken(SecurityHeaderTarget target) {
        IUsernameTokenConfiguration utConfig = usernameTokenConfigs.get(target);
        ((PasswordCallbackHandler) processingParams.get(ConfigurationConstants.PW_CALLBACK_REF))
                .addUser(utConfig.getUsername(), utConfig.getPassword());
        processingParams.put(ConfigurationConstants.USER, utConfig.getUsername());
        processingParams.put(ConfigurationConstants.ADD_USERNAMETOKEN_CREATED,
                Boolean.toString(utConfig.includeCreated()));
        processingParams.put(ConfigurationConstants.ADD_USERNAMETOKEN_NONCE,
                Boolean.toString(utConfig.includeNonce()));
        processingParams.put(ConfigurationConstants.PASSWORD_TYPE,
                utConfig.getPasswordType() == UTPasswordType.DIGEST ?
                        WSConstants.PW_DIGEST : WSConstants.PW_TEXT);
    }

    /**
     * Creates the WS-Security header targeted to the specified role performing the requested actions (which can be
     * creating a Username token, signing and encrypting the message).
     *
     * @param target    The target of the header
     * @param actions   The actions to perform
     * @throws SecurityProcessingException
     */
    private void createSecurityHeader(final SecurityHeaderTarget target, final List<HandlerAction> actions)
                                                                                   throws SecurityProcessingException,
                                                                                          HeaderProcessingFailure {
        log.debug("Configure WSS4J processing environment");
        final RequestData   reqData;
        try {
            reqData = prepare(target, actions);
        } catch (WSSecurityException wse) {
            log.error("An error occurred setting up the WSS4J environment for creating the {} security header",
                      target.id());
            throw new SecurityProcessingException("Could not setup processing environment for creating the " +
                                                    target.name() + " security header", wse);
        }
        final WSSConfig wssConfig = reqData.getWssConfig();
        for (HandlerAction actionToDo : actions) {
            log.debug("Performing Action: " + convertAction(actionToDo));
            org.apache.wss4j.dom.action.Action doit = null;
            try {
                doit = wssConfig.getAction(actionToDo.getAction());
                if (doit == null)
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                   "No executor available for performing the " + convertAction(actionToDo) + " action");
            } catch (final WSSecurityException wse) {
                log.error("No executor available for performing the ({}) action!", convertAction(actionToDo));
                throw new SecurityProcessingException("Could not setup processing environment for performing the " +
                                convertAction(actionToDo) + "action in the " + target.id() + " security header", wse);
            }
            try {
                doit.execute(this, actionToDo.getActionToken(), domEnvelope, reqData);
            } catch (WSSecurityException ex) {
                log.error("A problem occurred when executing the ({}) action! Details: {}", convertAction(actionToDo),
                          ex.getMessage());
                throw new HeaderProcessingFailure(convertAction(actionToDo), ex);
            }
        }
    }

    /**
     * Prepares the WSS4J context for execution of the given actions.
     *
     * @param target    The target of the WS-Security header in which actions should be performed
     * @param actions   List of actions to be performed
     * @return          A correctly configured {@link RequestData} instance
     * @throws WSSecurityException When the WSS4J context cannot be created, for example because information is missing.
     */
    private RequestData prepare(final SecurityHeaderTarget target, final List<HandlerAction> actions)
                                                                                            throws WSSecurityException {
        final RequestData reqData = new RequestData();
        final WSSConfig wssConfig = WSSConfig.getNewInstance();
        reqData.setWssConfig(wssConfig);
        reqData.setMsgContext(msgContext);
        reqData.setActor(target.id());

        // Register callback handler for attachments
        reqData.setAttachmentCallbackHandler(new AttachmentCallbackHandler(msgContext));
        // Register the callback handler for passwords (needed for UT and signing)
        reqData.setCallbackHandler((CallbackHandler) processingParams.get(ConfigurationConstants.PW_CALLBACK_REF));

        // Are we processing a request or response?
        final boolean isRequest = !msgContext.isServerSide();
        wssConfig.setPasswordsAreEncoded(decodeUseEncodedPasswords(reqData));
        wssConfig.setPrecisionInMilliSeconds(decodeTimestampPrecision(reqData));

        WSSecHeader secHeader = new WSSecHeader(target.id(), true);
        secHeader.insertSecurityHeader(domEnvelope);

        reqData.setSecHeader(secHeader);
        reqData.setSoapConstants(WSSecurityUtil.getSOAPConstants(domEnvelope.getDocumentElement()));
        wssConfig.setAddInclusivePrefixes(decodeAddInclusivePrefixes(reqData));

        // Perform action specific configuration
        for (HandlerAction actionToDo : actions) {
            switch (actionToDo.getAction()) {
                case WSConstants.UT :
                    decodeUTParameter(reqData);
                    break;
                case WSConstants.SIGN : {
                    SignatureActionToken actionToken = new SignatureActionToken();
                    reqData.setSignatureToken(actionToken);
                    actionToken.setCrypto(loadSignatureCrypto(reqData));
                    decodeSignatureParameter(reqData);
                    break;
                } case WSConstants.ENCR : {
                    EncryptionActionToken actionToken = new EncryptionActionToken();
                    reqData.setEncryptionToken(actionToken);
                    actionToken.setCrypto(loadEncryptionCrypto(reqData));
                    if (getStringOption(ConfigurationConstants.ENC_KEY_TRANSPORT)
                                                                                .equals(WSConstants.KEYTRANSPORT_RSA15))
                        throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM,
                                                      "RSA-1_5 is not supported");
                    decodeEncryptionParameter(reqData);
                }
            }
        }

        return reqData;
    }

    /**
     * Overrides the WSS4J implementation so the username is correctly set in the <code>RequestData</code>.
     *
     * {@inheritDoc}
     */
    @Override
    protected void decodeUTParameter(RequestData reqData) throws WSSecurityException {
        super.decodeUTParameter(reqData);
        reqData.setUsername(getStringOption(ConfigurationConstants.USER));
    }

    /**
     * Creates a {@link SignatureProcessingResult} instance representing the result of the signing. The digest
     * information must be collected from the created security header as it's not directly provided by the WSS4J lib.
     * The other information like signing algorithm is taken from the configuration..
     *
     * @return  A <code>SignatureProcessingResult</code> object containing the result information about the signature
     *          action
     * @throws SecurityProcessingException When there is a problem retrieving the required information. This can be
     *                                     caused by a missing ebMS or signature header or issues with retrieving the
     *                                     certificate from the certificate manager.
     */
    private SignatureProcessingResult getSignatureResults() throws SecurityProcessingException {
        // Get the digest information from the SOAP envelope
        final SignedMessagePartsInfo signedPartsData = SecurityUtils.getSignedPartsInfo(domEnvelope, msgUnits);
        if (signedPartsData == null) {
            // Although a signing action was executed, nothing was signed?!
            log.error("Signing operation did not sign any part of the message!");
            return new SignatureProcessingResult(
                            new SecurityProcessingException("Signing operation did not sign any part of the message"));
        }
        // Get the Certificate used for signing
        final X509Certificate cert = (X509Certificate) certManager.getKeyPair(signingConfig.getKeystoreAlias(),
                                                                              signingConfig.getCertificatePassword())
                                                                  .getCertificate();
        // Finally create and return result object
        return new SignatureProcessingResult(cert, signingConfig.getKeyReferenceMethod(),
                                             signingConfig.getSignatureAlgorithm(), signedPartsData.getEbmsHeaderInfo(),
                                             signedPartsData.getPayloadInfo());
    }

    /**
     * Creates a {@link EncryptionProcessingResult} instance representing the result of the encryption of the message.
     *
     * @return  A <code>EncryptionProcessingResult</code> object containing the result information about the encryption
     *          action
     * @throws SecurityProcessingException When there is a problem retrieving the certificate used for encryption from
     *                                     the certificate manager.
     */
    private EncryptionProcessingResult getEncryptionResults() throws SecurityProcessingException {
        // Get all payloads included in the message, i.e. in attachment or body
        Collection<IPayload> encryptedPayloads = new HashSet<>();
        msgUnits.stream().filter(msgUnit -> msgUnit instanceof IUserMessage)
                         .map(userMsg -> ((IUserMessage) userMsg).getPayloads())
                         .filter(umPayloads -> !Utils.isNullOrEmpty(umPayloads))
                         .forEachOrdered((umPayloads) ->
                                umPayloads.stream().filter(p -> p.getContainment() != IPayload.Containment.EXTERNAL)
                                                   .forEach(p -> encryptedPayloads.add(p)));

        // Get the Certificate used for encryption
        final X509Certificate cert = (X509Certificate) certManager.getCertificate(CertificateUsage.Encryption,
                                                                                encryptionConfig.getKeystoreAlias());
        final IKeyTransport ktInfo = encryptionConfig.getKeyTransport();
        return new EncryptionProcessingResult(cert, ktInfo.getKeyReferenceMethod(), ktInfo.getAlgorithm(),
                                              ktInfo.getDigestAlgorithm(), ktInfo.getMGFAlgorithm(),
                                              encryptionConfig.getAlgorithm(), encryptedPayloads);
    }

    /**
     * Creates a {@link UsernameTokenProcessingResult} instance representing the result of adding the username token to
     * the message. As the information is not directly provided by the WSS4J lib it must be collected from the created
     * security header.
     *
     * @param target    The target of the security header to which the username token was added
     * @return  A <code>UsernameTokenProcessingResult</code> object containing the result information about the added
     *          username token
     */
    private UsernameTokenProcessingResult getUsernameTokenResults(SecurityHeaderTarget target) {
        // Get the security header in which the username token should be created
        Element wsSecHeader = SecurityUtils.getSecurityHeaderElement(target, domEnvelope);

        // Then get the Username token element, should only be one
        NodeList unameTokens = wsSecHeader.getElementsByTagNameNS(SecurityConstants.WSS_NAMESPACE_URI, "UsernameToken");
        if (unameTokens.getLength() == 0) {
            // Although a username token action was executed, no token was added?!
            log.error("Username token operation did not add a username token to the message!");
            return new UsernameTokenProcessingResult(target,
                                new SecurityProcessingException(
                                              "Username token operation did not add a username token to the message!"));
        }
        String password = null, nonce = null, created = null;
        for(int i = 0; i < unameTokens.item(0).getChildNodes().getLength() ; i++) {
            Node child = unameTokens.item(0).getChildNodes().item(i);
            switch (child.getLocalName()) {
                case "Password" :
                    password = getElementValue((Element) child);
                    break;
                case "Nonce" :
                    nonce = getElementValue((Element) child);
                    break;
                case "Created" :
                    created = getElementValue((Element) child);
            }
        }
        // Username and password type will be taken from configuration
        IUsernameTokenConfiguration utConfig = usernameTokenConfigs.get(target);
        return new UsernameTokenProcessingResult(target, utConfig.getUsername(), password, utConfig.getPasswordType(),
                                                 nonce, created);
    }

    /**
     * Helper method to get text content of an Element.
     *
     * @param e The Element
     * @return  Its text content
     */
    private String getElementValue(Element e) {
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Text)
                return child.getTextContent();
        }
        return null;
    }

    /**
     * Helper to convert the WSS4J HandlerAction into a {@link Action} value.
     */
    private org.holodeckb2b.security.Action convertAction(final HandlerAction action) {
        switch (action.getAction()) {
            case WSConstants.SIGN :
                return SIGN;
            case WSConstants.ENCR :
                return ENCRYPT;
            case WSConstants.UT :
                return USERNAME_TOKEN;
        }
        return null;
    }

    @Override
    protected Crypto loadEncryptionCrypto(RequestData requestData) throws WSSecurityException {
        try {
            return certManager.getWSS4JCrypto(org.holodeckb2b.security.Action.ENCRYPT);
        } catch (SecurityProcessingException ex) {
            // The thrown excepiton will contain a WSSecurityException as cause, so we can just rethrow that one
            throw (WSSecurityException) ex.getCause();
        }
    }

    @Override
    public Crypto loadSignatureCrypto(RequestData requestData) throws WSSecurityException {
        try {
            return certManager.getWSS4JCrypto(org.holodeckb2b.security.Action.SIGN);
        } catch (SecurityProcessingException ex) {
            // The thrown excepiton will contain a WSSecurityException as cause, so we can just rethrow that one
            throw (WSSecurityException) ex.getCause();
        }
    }

    @Override
    public Object getOption(String key) {
        Object  value = processingParams.get(key);
        if (value == null)
            return msgContext.getProperty(key);
        else
            return value;
    }

    @Override
    public Object getProperty(Object msgContext, String key) {
        return getOption(key);
    }

    @Override
    public void setProperty(Object msgContext, String key, Object value) {
        processingParams.put(key, value);
    }

    private final static String WSS4J_PWD_PROPERTY = "hb2b-def-sec-prov-wss4j-pwd";

    @Override
    public String getPassword(Object msgContext) {
        return (String) processingParams.get(WSS4J_PWD_PROPERTY);
    }

    @Override
    public void setPassword(Object msgContext, String password) {
        processingParams.put(WSS4J_PWD_PROPERTY, password);
    }
}

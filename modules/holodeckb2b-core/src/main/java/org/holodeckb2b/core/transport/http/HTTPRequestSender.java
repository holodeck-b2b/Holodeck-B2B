/*
 * Copyright (C) 2026 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.axiom.mime.ContentType;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.NamedValue;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.kernel.MessageFormatter;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPAuthenticator;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.http.HttpStatus;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.holodeckb2b.common.pmode.TLSConfiguration;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.axis2.Axis2Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.holodeckb2b.interfaces.pmode.ITLSConfiguration;
import org.holodeckb2b.interfaces.security.trust.TLSCertificateTrustManager;

/**
 * Handles the actual sending of the message as a new HTTP request using the Apache HttpClient components. Custom
 * settings, like TLS client side authentication, can be provided using a {@link IProtocol} instance in the {@link
 * HTTPTransportSender#MC_HTTP_CONFIG} message context property.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 * @see IProtocol
 */
/*
 * This class is based on the <code>org.apache.axis2.transport.http.HTTPSender</code> class and adapted to always
 * process the response to the request and not limit this to certain HTTP status codes. When a response context is
 * available, the response data is also added to it.
 */
class HTTPRequestSender {
	private static final Logger log = LogManager.getLogger(HTTPRequestSender.class);

	/**
	 * The list of HTTP methods supported by this sender
	 */
	private static final Set<String> SUPPORTED_HTTP_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS");

	/*
	 * Name of the HTTPClientContext attribute that contains the socket factory registry that should be used to create
	 * connections for this request (if needed).
	 */
	static final String SOCKET_FACTORY_REGISTRY = "http.socket-factory-registry";
	/*
	 * The connection manager to use for getting a connection to execute requests
	 */
	private final HttpClientConnectionManager connectionManager;

	HTTPRequestSender(HttpClientConnectionManager connManager) {
		this.connectionManager = connManager;
	}

	public void send(URL url, MessageContext msgContext) throws IOException {
        String httpMethod = (String) msgContext.getProperty(Constants.Configuration.HTTP_METHOD);
        if (httpMethod == null)
            httpMethod = Constants.Configuration.HTTP_METHOD_POST;
        else if (!SUPPORTED_HTTP_METHODS.contains(httpMethod.toUpperCase())) {
        	log.error("{} is not a supported HTTP method", httpMethod);
        	throw new AxisFault("Unsupported HTTP method");
        }

        OMOutputFormat format = (OMOutputFormat) msgContext.getProperty(HTTPTransportSender.MC_OMFORMAT);
        MessageFormatter messageFormatter = (MessageFormatter) msgContext.getProperty(HTTPTransportSender.MC_MSG_FORMATTER);
        url = messageFormatter != null ? messageFormatter.getTargetAddress(msgContext, format, url) : url;
        String soapAction = msgContext.getSoapAction();

        HTTPAuthenticator authenticator;
        Object obj = msgContext.getProperty(HTTPConstants.AUTHENTICATE);
        if (obj == null)
            authenticator = null;
        else  if (obj instanceof HTTPAuthenticator)
            authenticator = (HTTPAuthenticator) obj;
        else
            throw new AxisFault("HttpTransportProperties.Authenticator class cast exception");

        RequestEntity requestEntity = null;
        if ((Constants.Configuration.HTTP_METHOD_POST.equalsIgnoreCase(httpMethod)
             || Constants.Configuration.HTTP_METHOD_PUT.equalsIgnoreCase(httpMethod)
            ) && !msgContext.isPropertyTrue(HTTPTransportSender.MC_HTTP_EMPTY_BODY)) {
            requestEntity = new RequestEntity(msgContext, messageFormatter, format,
            								  msgContext.isPropertyTrue(HTTPConstants.CHUNKED),
            								  msgContext.isPropertyTrue(HTTPConstants.MC_GZIP_REQUEST),
            								  authenticator != null && authenticator.isAllowedRetry());
        }

        HTTPRequest request = createRequest(msgContext, httpMethod, url, requestEntity);

        if (msgContext.getOptions() != null && msgContext.getOptions().isManageSession()) {
            // setting the cookie in the out path
            Object cookieString = msgContext.getProperty(HTTPConstants.COOKIE_STRING);

            if (cookieString != null) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(cookieString);
                request.setHeader(HTTPConstants.HEADER_COOKIE, buffer.toString());
            }
        }

        if (msgContext.isSOAP11() && !Utils.isNullOrEmpty(soapAction))
        	request.setHeader(HTTPConstants.HEADER_SOAP_ACTION,
        					  messageFormatter.formatSOAPAction(msgContext, format, soapAction));

        addHeaders(msgContext, request);

        if (authenticator != null)
            request.enableAuthentication(authenticator);

        request.setConnectionTimeout((Integer) msgContext.getProperty(HTTPConstants.CONNECTION_TIMEOUT));
        request.setSocketTimeout((Integer) msgContext.getProperty(HTTPConstants.SO_TIMEOUT));
        try {
            request.execute();
            int statusCode = request.getStatusCode();
            log.trace("Handling response - " + statusCode);
            boolean fault = statusCode >= 400 && statusCode < 600;

            OperationContext opContext = msgContext.getOperationContext();
            MessageContext respMessageContext = opContext == null ? null
            		: opContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);

            processHTTPHeaderInformation(request, msgContext, respMessageContext);

        	// When doing SOAP, only 400, 404 and 500 should be handled as valid responses
            if (fault && !msgContext.isDoingREST() && !(statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
                       		|| statusCode == HttpStatus.SC_BAD_REQUEST || statusCode == HttpStatus.SC_NOT_FOUND))
                throw new AxisFault(Messages.getMessage("transportError", String.valueOf(statusCode),
                                                        request.getStatusText()));

            if (fault && respMessageContext != null)
            	respMessageContext.setProcessingFault(true);

            String ctHdr = request.getResponseHeader(HTTPConstants.HEADER_CONTENT_TYPE);
            InputStream in = request.getResponseContent();
            if (in != null && !Utils.isNullOrEmpty(ctHdr)) {
            	if (respMessageContext != null)
            		respMessageContext.setProperty(MessageContext.TRANSPORT_IN, in);
            	else if (opContext != null)
            		opContext.setProperty(MessageContext.TRANSPORT_IN, in);
            	else
					msgContext.setProperty(MessageContext.TRANSPORT_IN, in);
            }
        } catch (IOException e) {
            log.info("Unable to send to url[" + url + "]", e);
            throw AxisFault.makeFault(e);
        }
    }

	/**
	 * Adds the HTTP headers to the request.
	 *
	 * @param msgContext
	 * @param request
	 */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private void addHeaders(MessageContext msgContext, HTTPRequest request) {
        boolean isCustomUserAgentSet = false;
        // set the custom headers, if available
        Object httpHeadersObj = msgContext.getProperty(HTTPConstants.HTTP_HEADERS);
        if (httpHeadersObj != null) {
            if (httpHeadersObj instanceof List) {
                for (NamedValue nv : ((List<NamedValue>) httpHeadersObj)) {
                    if (nv != null) {
                        if (HTTPConstants.HEADER_USER_AGENT.equals(nv.getName()))
                            isCustomUserAgentSet = true;
                        request.addHeader(nv.getName(), nv.getValue());
                    }
                }
            }
            if (httpHeadersObj instanceof Map) {
                for (Map.Entry<String, String> entry  : ((Map<String, String>) httpHeadersObj).entrySet()) {
                    if (HTTPConstants.HEADER_USER_AGENT.equals(entry.getKey()))
                        isCustomUserAgentSet = true;
                    request.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        // For backward compatibility we have to consider the TRANSPORT_HEADERS map as well
        Map<Object, Object> transportHeaders = (Map<Object, Object>) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
        if (transportHeaders != null) {
            for(Map.Entry entry : transportHeaders.entrySet())
                request.addHeader(entry.getKey().toString(), entry.getValue().toString());
        }

        if (!isCustomUserAgentSet) {
        	String ua = (String) msgContext.getProperty(HTTPConstants.HEADER_USER_AGENT);
            request.setHeader(HTTPConstants.HEADER_USER_AGENT,
            				  !Utils.isNullOrEmpty(ua) ? ua : Axis2Utils.HTTP_PRODID_HEADER);
        }

    }


    private void processHTTPHeaderInformation(HTTPRequest request, MessageContext reqMsgContext,
			 								 MessageContext respMsgContext) throws AxisFault {
    	Map<String, String> respHeaders = request.getResponseHeaders();

    	ContentType contentType;
    	String charSetEnc = null;
    	String contentTypeString = request.getResponseHeader(HTTPConstants.HEADER_CONTENT_TYPE);
    	if (!Utils.isNullOrEmpty(contentTypeString)) {
    		try {
    			contentType = new ContentType(contentTypeString);
    		} catch (ParseException ex) {
    			throw AxisFault.makeFault(ex);
    		}
    		charSetEnc = contentType.getParameter(HTTPConstants.CHAR_SET_ENCODING);
    	}

    	if (respMsgContext != null) {
            respMsgContext.setProperty(MessageContext.TRANSPORT_HEADERS, respHeaders);
            respMsgContext.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, request.getStatusCode());
    		respMsgContext.setProperty(Constants.Configuration.CONTENT_TYPE, contentTypeString);
    		respMsgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEnc);
    	} else {
    		// Transport details will be stored in a HashMap so that anybody interested can retrieve them from
    		// the request context as well
    		Map<String,String> transportInfoMap = new HashMap<String,String>();
    		transportInfoMap.put(Constants.Configuration.CONTENT_TYPE, contentTypeString);
    		transportInfoMap.put(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEnc);
    		// the HashMap is stored in the outgoing message.
    		reqMsgContext.setProperty(Constants.Configuration.TRANSPORT_INFO_MAP, transportInfoMap);
    	}

    	// For backward compatibility the headers and status code are also added to the request context.
    	// @todo: Remove this?
		reqMsgContext.setProperty(MessageContext.TRANSPORT_HEADERS, respHeaders);
		reqMsgContext.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, request.getStatusCode());


        Map<String,String> cookies = request.getCookies();
        if (cookies != null) {
            String customCookieId = (String) reqMsgContext.getProperty(Constants.CUSTOM_COOKIE_ID);
            String cookieString = null;
            if (customCookieId != null) {
                cookieString = buildCookieString(cookies, customCookieId);
            }
            if (cookieString == null) {
                cookieString = buildCookieString(cookies, Constants.SESSION_COOKIE);
            }
            if (cookieString == null) {
                cookieString = buildCookieString(cookies, Constants.SESSION_COOKIE_JSESSIONID);
            }
            if (cookieString != null) {
                reqMsgContext.getServiceContext().setProperty(HTTPConstants.COOKIE_STRING, cookieString);
            }
        }
    }

    private String buildCookieString(Map<String,String> cookies, String name) {
        String value = cookies.get(name);
        return Utils.isNullOrEmpty(value) ? null : name + "=" + value;
    }

	private HTTPRequest createRequest(MessageContext msgContext, String methodName, URL url,
									RequestEntity requestEntity) throws AxisFault {
		log.debug("Setup request context for connection to {}", url.toString());
		HttpClientContext	clientCtx = HttpClientContext.create();

		// Check if this is a https connection that requires custom TLS configuration
		IProtocol connConfig = (IProtocol) msgContext.getProperty(HTTPTransportSender.MC_HTTP_CONFIG);
		ITLSConfiguration tlsConfig;
		if (url.getProtocol().equalsIgnoreCase("https") && connConfig != null
			&& (tlsConfig = connConfig.getTLSConfiguration()) != null) {
			log.trace("Apply request specific TLS settings");
			// We create a copy of the TLS configuration to ensure we have a good equals method as the TLSConfiguration
			// instance is used by the connection manager to determine if a pooled connection can be reused
    		if (!(tlsConfig instanceof TLSConfiguration))
    			tlsConfig = new TLSConfiguration(tlsConfig);
			// Set the TLS configuration as User Token so the connection manager will only select connections
			// to the server that use this configuration
			clientCtx.setUserToken(tlsConfig);

			String[] allowedProtocols = tlsConfig.getAllowedProtocols();
			if (allowedProtocols != null && allowedProtocols.length > 0)
				log.debug("Set allowed protocols to : {}", Arrays.toString(allowedProtocols));
			else
				// Use default of TLS 1.2 or 1.3
				allowedProtocols = new String[] { "TLSv1.2", "TLSv1.3" };

			final String[] allowedCipherSuites = tlsConfig.getAllowedCipherSuites();
			if (allowedCipherSuites != null && allowedCipherSuites.length > 0)
    			log.debug("Set allowed cipher suites to : {}", Arrays.toString(allowedCipherSuites));

			// Create and set the socket factory registry so the connection manager will use the correct TLS
			// settings when it needs to create connections for this request
			clientCtx.setAttribute(SOCKET_FACTORY_REGISTRY,
				RegistryBuilder.<ConnectionSocketFactory>create()
						.register("http", PlainConnectionSocketFactory.getSocketFactory())
						.register("https", new SSLConnectionSocketFactory(createSSLContext(tlsConfig),
																		allowedProtocols,
																		allowedCipherSuites,
																		(HostnameVerifier) null))
						.build());
		}

		HTTPRequest request = new HTTPRequest(HttpClientBuilder.create().setConnectionManager(connectionManager)
				  										 .setConnectionManagerShared(true)
				  										 .build(),
				  							msgContext, methodName, url, requestEntity, clientCtx);

		// Store the request in the operation context so we can close connection properly when operation finishes
		msgContext.getOperationContext().setProperty(HTTPTransportSender.OC_HTTP_REQUEST, request);

		return request;
	}

	/**
	 * Creates a customised SSLContext based on the TLS configuration provided in the P-Mode of the [primary] Message
	 * Unit that is being sent.
	 *
	 * @param tlsConfiguration  the custom TLS settings for this request
	 * @return the customised SSLContext
	 * @throws AxisFault if an error occurs loading the key pair to be used for TLS client authentication from the
	 * 					 installed Certificate Manager.
	 */
	private SSLContext createSSLContext(ITLSConfiguration tlsConfiguration) throws AxisFault {
		KeyManager[] kms = null;
		String clientCertAlias = tlsConfiguration.getClientCertificateAlias();
		if (!Utils.isNullOrEmpty(clientCertAlias)) {
			try {
				log.trace("Create custom KeyManager to use client certificate (alias={})", clientCertAlias);
				final String pwd = tlsConfiguration.getClientCertificatePassword();
				PrivateKeyEntry clientCert = HolodeckB2BCoreInterface.getCertificateManager().getKeyPair(clientCertAlias,
																pwd);
				KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
				ks.load(null, null);
				ks.setEntry(clientCertAlias, clientCert, new KeyStore.PasswordProtection(pwd.toCharArray()));
				final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		        kmfactory.init(ks, pwd.toCharArray());
		        kms = kmfactory.getKeyManagers();
		        if (log.isDebugEnabled())
		        	log.debug("Using TLS client certificate (CN={},issuer={}))",
			        			CertificateUtils.getSubjectCN((X509Certificate) clientCert.getCertificate()),
			        			CertificateUtils.getIssuerCN((X509Certificate) clientCert.getCertificate()));
			} catch (Exception clientCertError) {
				log.error("Could not load TLS client certificate ({}): {}", clientCertAlias,
							Utils.getExceptionTrace(clientCertError));
				throw new AxisFault("Could not load TLS client certificate", clientCertError);
			}
		}

		SSLContext sslContext;
		try {
			log.trace("Create custom SSLContext");
			sslContext = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
			sslContext.init(kms, new TrustManager[] {new TLSCertificateTrustManager(tlsConfiguration)}, null);
			return sslContext;
		} catch (NoSuchAlgorithmException | NoSuchProviderException | KeyManagementException e) {
			log.error("Could not create SSLContext: {}", Utils.getExceptionTrace(e));
			throw new AxisFault("Could not create SSLContext", e);
		}
	}
}

/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.axis2;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;
import java.util.Iterator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.kernel.MessageFormatter;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPSender;
import org.apache.axis2.transport.http.Request;
import org.apache.axis2.transport.http.impl.httpclient4.HTTPClient4TransportSender;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.TLSCertificateTrustManager;

/**
 * Is a customisation of the Axis2 {@link HTTPTransportSender} for sending requests to other servers using HTTP. The
 * sender is configured in the main Holodeck B2B configuration file where it should be registered as the "http" <code>
 * transportSender</code>. It will then automatically be used for both http and https requests.
 * <p>
 * The following parameters can be specified to set the default connection configuration:<ul>
 * <li>Content-Encoding : When sending large requests it is useful to compress them on transport. This is done by the
 *      use of the standard compression feature of HTTP/1.1. Currently Holodeck B2B only supports the <i>gzip</i>
 *      Transfer-Encoding. Therefore the value of this parameter MUST be "gzip" if included. If not included no
 *      compression is used.</li>
 * <li>Transfer-Encoding : Can be used to specify that the HTTP <i>"chunked"</i> transfer encoding should be used.<br/>
 * 		NOTE: This parameter is ignored if the <i>"gzip"</i> content encoding is used as this requires the use of the
 *     	chunked transfer encoding.</li>
 * <li>CONNECTION_TIMEOUT : The generic connection timeout in milliseconds. This is the time Holodeck B2B will wait to
 * 		establish the connection. If not set the default value is 60000 (= 1 minute).</li>
 * <li>SO_TIMEOUT : The generic read timeout in milliseconds. This is the time Holodeck B2B will wait for the response
 * 		from the other server once the request has been sent. If not set the default value is 60000 (= 1 minute).</li>
 * <li>MAX_CONNECTIONS : The maximum number of connections that can be open at any time. Because a connection pool is
 * 		used this is different from the number of concurrent requests which is limited by the number of <i>sender
 * 		workers</i> as configured in <code>workers.xml</code>. The default value is 10, which should be fine for
 * 		smaller gateways that have just one sender worker and a limited number of receivers. The maximum should be set
 * 		higher for larger gateways with more senders and receivers.</li>
 * <li>allowedECGroups : Can be used to specify the list of elliptic curve groups that are allowed to be used in the
 * 		TLS handshake. This is a generic setting that applies to all HTTPS connections. The value must be a comma
 * 		separated list of group names, in order of preference.</li>
 * <li>CLIENT_TLS : should contain one or more <code>KeystoreAlias</code> elements that reference the keypair(s) managed
 * 		by the <i>Certificate Manager</i> that by default should be used for TLS client authentication. The
 * 		<code>password</code> attribute of these elements can contain the literal (= clear text) password or reference
 * 		to a Java System Property or Environment Variable. In the latter cases the name of the property or environment
 * 		variable should be prefixed with <i>sys:</i> respectively <i>env:</i>.<br/>
 *      NOTE: For backward compatibility the default Java system properties <i>javax.net.ssl.keyStore</i> and
 *      <i>javax.net.ssl.keyStorePassword</i> will be used to retrieve the key pair if the transport sender parameter is
 *      not provided. This method however is deprecated and will be removed in a future version!</li>
 * </ul>
 * <p>
 * To use this transport sender for sending the <i>scheme</i> of the message's "target URL" must be set to the name of
 * the transport sender ("http" by default). The target URL should be provided in either the {@link
 * Constants.Configuration.TRANSPORT_URL} message context property or the <i>To</i> address of the
 * <code>EndpointReference</code>.<br/>
 * Request specific configuration can be provided through a {@link IProtocol} instance that should be set in the {@link
 * #MC_HTTP_CONFIG} message context property. Note that {@linkplain IProtocol#getAddress()} which can also provide the
 * target URL is ignored.<br/>
 * The message content is created using a {@link MessageFormatter} which can be explicitly configured in the {@link
 * Constants.Configuration.MESSAGE_FORMATTER} MessageContext property. If this property is not set the formatter
 * will be derived from the message type as contained in the {@link Constants.Configuration.MESSAGE_TYPE} MessageContext
 * property or parameter using the registered formatters in the main Holodeck B2B configuration file. The used formatter
 * MUST only write the content of the HTTP entity body and not include any headers in its output.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 * @see IProtocol
 */
public class HTTPTransportSender extends HTTPClient4TransportSender {
	private static final Logger log = LogManager.getLogger(HTTPTransportSender.class);

	/**
	 * Defines the name of the message context property that includes the request specific HTTP configuration.
	 *
	 * @since 8.2.0
	 */
	public static final String MC_HTTP_CONFIG = "hb2b-http-sender::ConnConfig";

	/*
	 * Name of the operation context property that contains the HTTP request. Needed to properly close the connection
	 * when the message is sent.
	 */
	protected static final String MC_HTTP_REQUEST = "hb2b::httpRequest";

	private static final char[] EMPTY_PWD = new char[] {};

	/**
	 * Indicates if the HTTP <i>"gzip"</i> compression content encoding should be used by default. When a message unit
	 * is sent, the P-Mode can override the default setting.
	 * The setting can be specified by adding the <i>Content-Encoding</i> parameter with value "gzip" to the <code>
	 * transportSender</code> element in the main Holodeck B2B configuration file.
	 */
	private boolean defaultCompression;
	/**
	 * Indicates if the HTTP <i>"chunked"</i> transfer encoding should be used by default. When a message unit
	 * is sent, the P-Mode can override the default setting.
	 * The setting can be specified by adding the <i>Transfer-Encoding</i> parameter with value "chunked" to the <code>
	 * transportSender</code> element in the main Holodeck B2B configuration file.<br/>
	 * NOTE: As HTTP compression requires chunked encoding, it will automatically be enabled by default when HTTP
	 * compression is as well.
	 */
	private boolean defaultChunked;
	/**
	 * The Apache HttpClient connection manager responsible for managing the connections used by this transport sender.
	 */
	private HttpClientConnectionManager connectionManager;

	@Override
	public void init(ConfigurationContext confContext, TransportOutDescription transportOut) throws AxisFault {
		super.init(confContext, transportOut);

		Parameter contentEncoding = transportOut.getParameter(HTTPConstants.HEADER_CONTENT_ENCODING);
		defaultCompression = contentEncoding != null && HTTPConstants.COMPRESSION_GZIP.equals(contentEncoding.getValue());
		log.debug("HTTP compression is {} by default", defaultCompression ? "enabled" : "disabled");

		Parameter transferEncoding = transportOut.getParameter(HTTPConstants.HEADER_TRANSFER_ENCODING);
		defaultChunked = defaultCompression ||
						(transferEncoding != null
						 && HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED.equals(transferEncoding.getValue()));
		log.debug("HTTP chunked transfer encoding is {} by default", defaultChunked ? "enabled" : "disabled");

		log.trace("Check that BC Providers are available");
		try {
			if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
				log.debug("Adding BouncyCastle JCE provider");
				Security.addProvider(new BouncyCastleProvider());
			}
			if (Security.getProvider(BouncyCastleJsseProvider.PROVIDER_NAME) == null) {
				log.debug("Adding BouncyCastle JSSE provider");
				Security.addProvider(new BouncyCastleJsseProvider());
			}
		} catch (Exception bcRegistrationFailure) {
			log.error("Could not install the BouncyCastle providers :", Utils.getExceptionTrace(bcRegistrationFailure));
			throw new AxisFault("Required BouncyCastle security providers could not be installed!");
		}

		Parameter ecGroups = transportOut.getParameter("allowedECGroups");
		if (ecGroups != null && ecGroups.getParameterType() == Parameter.TEXT_PARAMETER) {
			System.setProperty("jdk.tls.namedGroups", (String) ecGroups.getValue());
			log.debug("Set allowed TLS elliptic curve groups to : {}", ecGroups.getValue());
		}
		// Setup the default SSLContext to create TLS connections. If needed the P-Mode can override the TLS settings
		// and a custom context will be used.
		SSLContext sslContext = createDefaultSSLContext(transportOut);
		log.trace("Setup ConnectionManager");
		@SuppressWarnings("deprecation")
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
											.register("http", PlainConnectionSocketFactory.getSocketFactory())
											.register("https", new SSLConnectionSocketFactory(sslContext,
																	new String[] {"TLSv1.2", "TLSv1.3"}, null, null))
											.build();
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

		Parameter maxTotal = transportOut.getParameter("MAX_CONNECTIONS");
		if (maxTotal != null && maxTotal.getParameterType() == Parameter.TEXT_PARAMETER) {
			int maxConn = Integer.parseInt((String) maxTotal.getValue());
			connManager.setMaxTotal(maxConn);
			connManager.setDefaultMaxPerRoute(maxConn);
		} else {
			connManager.setMaxTotal(10);
			connManager.setDefaultMaxPerRoute(10);
		}
		log.debug("Maximum number of connections set to : {}", connManager.getMaxTotal());
		connectionManager = connManager;
	}

	@Override
	public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
		// Check if custom configuration is provided
		IProtocol connConfig = (IProtocol) msgContext.getProperty(MC_HTTP_CONFIG);
		if (connConfig != null) {
			log.debug("Apply request specific HTTP settings");

			// Get current set of options
	        final Options options = msgContext.getOptions();

	        // Check if HTTP compression and/or chunking should be used and set options accordingly
	        final boolean compress = (connConfig != null ? connConfig.useHTTPCompression() : defaultCompression);
	        log.debug("{} HTTP compression using gzip Content-Encoding", compress ? "Enable" : "Disable");
	        if (!msgContext.isServerSide())
	            // Holodeck B2B is sending the message, so request has to be compressed
	            options.setProperty(HTTPConstants.MC_GZIP_REQUEST, compress);
	        else
	            // Holodeck B2B is responding the message, so request has to be compressed
	            options.setProperty(HTTPConstants.MC_GZIP_RESPONSE, compress);

	        // Check if HTTP "chunking" should be used. In case of gzip CE, chunked TE is required. But as Axis2 does
	        // not automaticly enable this we also enable chunking here when compression is used
	        if (compress || (connConfig != null ? connConfig.useChunking() : defaultChunked)) {
	            log.debug("Enable chunked transfer-encoding");
	            options.setProperty(HTTPConstants.CHUNKED, Boolean.TRUE);
	        } else {
	            log.debug("Disable chunked transfer-encoding");
	            options.setProperty(HTTPConstants.CHUNKED, Boolean.FALSE);
	        }

	        // Check if custom time out settings are specified
        	Integer to = connConfig.getConnectionTimeout();
        	if (to != null) {
	    		log.debug("Set connection timeout to {} ms", to);
	    		msgContext.setProperty(HTTPConstants.CONNECTION_TIMEOUT, to);
        	}
    		to = connConfig.getReadTimeout();
    		if (to != null) {
	    		log.debug("Set read timeout to {} ms", to);
	    		msgContext.setProperty(HTTPConstants.SO_TIMEOUT, to);
        	}

	        // If the message does not contain any attachments we can disable SwA
	        boolean hasAttachments = !msgContext.getAttachmentMap().getContentIDSet().isEmpty();
	        log.debug("{} SwA as message does{} contain attachments", hasAttachments ? "Enable" : "Disable",
	        		  hasAttachments ? "" : " not");
	        options.setProperty(Constants.Configuration.ENABLE_SWA, hasAttachments);
		}

		return super.invoke(msgContext);
	}

	@Override
	protected HTTPSender createHTTPSender() {
		return new org.holodeckb2b.core.axis2.HTTPSender(connectionManager);
	}

	@Override
	public void cleanup(MessageContext msgContext) throws AxisFault {
		Request request = (Request) msgContext.getOperationContext().getProperty(MC_HTTP_REQUEST);
		if (request != null) {
			log.trace("Releasing connection used for request");
			request.releaseConnection();
		}

		// guard against multiple calls
		msgContext.removeProperty(HTTPConstants.HTTP_METHOD);
	}

	/**
	 * Creates a default SSLContext that will be used to create TLS connections if no specific TLS configuration is
	 * provided in the P-Mode or this transport sender is invoked to send something in another context (e.g. to the
	 * back-end).
	 *
	 * @param transportConfig 	transport sender configuration as provided in the Holodeck B2B config file
	 * @return	the default SSL context
	 * @throws AxisFault when there is an error creating the SSLContext, which can be caused by the fact that the
	 * 					 specified key store is not available
	 */
	private SSLContext createDefaultSSLContext(TransportOutDescription transportConfig) throws AxisFault {
		log.trace("Creating default SSLContext");
		KeyStore tlsClientKs = null;
		char[] tlsClientKsPwd = EMPTY_PWD;
		Parameter aliasP = transportConfig.getParameter("CLIENT_TLS");
		if (aliasP != null && aliasP.getParameterType() == Parameter.OM_PARAMETER) {
			ICertificateManager cm = HolodeckB2BCoreInterface.getCertificateManager();
			try {
				tlsClientKs = KeyStore.getInstance(KeyStore.getDefaultType());
				tlsClientKs.load(null, EMPTY_PWD);
			} catch (Exception kse) {
				log.fatal("Could not create TLS client cert key store : {}", Utils.getExceptionTrace(kse));
				throw new AxisFault("Error constructing TLS client cert key store", kse);
			}
			for(Iterator<OMElement> ksElems = aliasP.getParameterElement().getChildrenWithLocalName("KeystoreAlias");
				ksElems.hasNext();) {
				OMElement ksElem = ksElems.next();
				String alias = ksElem.getText();
				String pwd = ksElem.getAttributeValue(new QName("password"));
				if (pwd != null && pwd.startsWith("env:"))
					pwd = System.getenv(pwd.substring(4));
				else if (pwd != null && pwd.startsWith("sys:"))
					pwd = System.getProperty(pwd.substring(4));
				try {
					tlsClientKs.setEntry(alias, cm.getKeyPair(alias, pwd), new KeyStore.PasswordProtection(EMPTY_PWD));
					log.debug("Added {} as default TLS client cert", alias);
				} catch (SecurityProcessingException | KeyStoreException kpNotAvailable) {
					log.fatal("Could not load default TLS client cert {} : {}", alias,
							Utils.getExceptionTrace(kpNotAvailable));
					throw new AxisFault("Error in default TLS client cert config", kpNotAvailable);
				}
			}
		} else {
			String kspath = System.getProperty("javax.net.ssl.keyStore");
			String kspwd = System.getProperty("javax.net.ssl.keyStorePassword");
			if (!Utils.isNullOrEmpty(kspath)) {
				log.warn("Deprecated client TLS configuration method used! This method may be removed in feature version!");
				Path path = Paths.get(kspath);
				if (!path.isAbsolute())
					path = HolodeckB2BCoreInterface.getConfiguration().getHolodeckB2BHome().resolve(path);

				try {
					tlsClientKs = KeystoreUtils.load(path, kspwd);
					tlsClientKsPwd = kspwd.toCharArray();
					log.debug("Loaded default TLS client certs from {}", path.toString());
				} catch (KeyStoreException ksNotAvailable) {
					log.fatal("Could not load default TLS client certs from key store {} : {}", path.toString(),
							Utils.getExceptionTrace(ksNotAvailable));
					throw new AxisFault("Error in default TLS client cert config");
				}
			}
		}
		try {
			log.trace("Construct KeyManager for default SSLContext");
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		    kmf.init(tlsClientKs, tlsClientKsPwd);
		    SSLContext sslContext;
			sslContext = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
			sslContext.init(kmf.getKeyManagers(), new TrustManager[] {new TLSCertificateTrustManager()}, null);
			log.debug("Created default SSLContext");
			return sslContext;
		} catch (Exception ctxConstructionError) {
			log.error("Could not create default SSLContext: {}", Utils.getExceptionTrace(ctxConstructionError));
			throw new AxisFault("Could not create SSLContext", ctxConstructionError);
		}
	}
}

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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
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
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IProtocol;

/**
 * Is the Axis2 {@link HTTPTransportSender} implementation used for sending requests to other servers using HTTP. The
 * sender is configured in the main Holodeck B2B configuration file where it should be registered as the "http" <code>
 * transportSender</code>. The following parameters can be specified to set the default connection configuration:<ul>
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
 * </ul>
 * If the sender is invoked to send a message unit to another MSH, it will use the P-Mode governing the exchange of the
 * [primary] message unit to configure the HTTP(S) connection. The P-Mode can override the default settings specified in
 * the configuration of the <code>transportSender</code>.
 * <p>
 * <b>NOTE</b>: The sender does not determine the URL to which a request must be send. It is the responsibility of other
 * components in the send chain to provide it in the {@link Constants.Configuration#TRANSPORT_URL} <code>MessageContext
 * </code> property.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 * @see IProtocol
 */
public class HTTPTransportSender extends HTTPClient4TransportSender {
	private static final Logger log = LogManager.getLogger(HTTPTransportSender.class);

	/*
	 * Name of the operation context property that contains the HTTP request. Needed to properly close the connection
	 * when the message is sent.
	 */
	protected static final String MC_HTTP_REQUEST = "hb2b::httpRequest";

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

		log.trace("Prepare default SSLContext");
		Parameter ecGroups = transportOut.getParameter("allowedECGroups");
		if (ecGroups != null && ecGroups.getParameterType() == Parameter.TEXT_PARAMETER) {
			System.setProperty("jdk.tls.namedGroups", (String) ecGroups.getValue());
			log.debug("Set allowed TLS elliptic curve groups to : {}", ecGroups.getValue());
		}
		// Setup the default SSLContext to create TLS connections. If needed the P-Mode can override the TLS settings
		// and a custom context will be used.
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
			sslContext.init(null, new TrustManager[] {new TLSCertificateTrustManager()}, null);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | KeyManagementException e) {
			log.error("Could not create default SSLContext: {}", Utils.getExceptionTrace(e));
			throw new AxisFault("Could not create SSLContext", e);
		}

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
		MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(msgContext);
		IMessageUnit primaryMU = procCtx != null ? procCtx.getPrimarySentMessageUnit() : null;

		// If a message unit is being sent, check the P-Mode if specific http configuration is required
		if (primaryMU != null) {
			log.debug("Get P-Mode Leg for primary MU (msgID={})", primaryMU.getMessageId());
	        final ILeg leg = PModeUtils.getLeg(primaryMU);

	        // Get current set of options
	        final Options options = msgContext.getOptions();

	        // Check if HTTP compression and/or chunking should be used and set options accordingly
	        final IProtocol protocolCfg = leg != null ? leg.getProtocol() : null;
	        final boolean compress = (protocolCfg != null ? protocolCfg.useHTTPCompression() : defaultCompression);
	        log.debug("{} HTTP compression using gzip Content-Encoding", compress ? "Enable" : "Disable");
	        if (procCtx.isHB2BInitiated())
	            // Holodeck B2B is sending the message, so request has to be compressed
	            options.setProperty(HTTPConstants.MC_GZIP_REQUEST, compress);
	        else
	            // Holodeck B2B is responding the message, so request has to be compressed
	            options.setProperty(HTTPConstants.MC_GZIP_RESPONSE, compress);

	        // Check if HTTP "chunking" should be used. In case of gzip CE, chunked TE is required. But as Axis2 does
	        // not automaticly enable this we also enable chunking here when compression is used
	        if (compress || (protocolCfg != null ? protocolCfg.useChunking() : defaultChunked)) {
	            log.debug("Enable chunked transfer-encoding");
	            options.setProperty(HTTPConstants.CHUNKED, Boolean.TRUE);
	        } else {
	            log.debug("Disable chunked transfer-encoding");
	            options.setProperty(HTTPConstants.CHUNKED, Boolean.FALSE);
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
}

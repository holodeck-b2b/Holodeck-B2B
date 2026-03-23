/**
 * Copyright (C) 2025 The Holodeck B2B Team, Sander Fieten
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

import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.AxisRequestEntity;
import org.apache.axis2.transport.http.Request;
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
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.holodeckb2b.interfaces.pmode.ITLSConfiguration;
import org.holodeckb2b.interfaces.security.trust.TLSCertificateTrustManager;

/**
 * Extends {@link org.apache.axis2.transport.http.HTTPSender} to handle connection specific TLS configuration.
 * The custom TLS settings must be provided using a {@link IProtocol} instance in the {@link
 * HTTPTransportSender#MC_HTTP_CONFIG} message context property.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 * @see ITLSConfiguration
 */
class HTTPSender extends org.apache.axis2.transport.http.HTTPSender {
	private static final Logger log = LogManager.getLogger(HTTPSender.class);

	/*
	 * Name of the HTTPClientContext attribute that contains the socket factory registry that should be used to create
	 * connections for this request (if needed).
	 */
	static final String SOCKET_FACTORY_REGISTRY = "http.socket-factory-registry";
	/*
	 * The connection manager to use for getting a connection to execute requests
	 */
	private final HttpClientConnectionManager connectionManager;

	public HTTPSender(HttpClientConnectionManager connManager) {
		this.connectionManager = connManager;
	}

	@Override
	protected Request createRequest(MessageContext msgContext, String methodName, URL url,
									AxisRequestEntity requestEntity) throws AxisFault {
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

		RequestImpl request = new RequestImpl(HttpClientBuilder.create().setConnectionManager(connectionManager)
				  										 .setConnectionManagerShared(true)
				  										 .build(),
				  							msgContext, methodName, url, requestEntity, clientCtx);

		// Store the request in the operation context so we can close connection properly when operation finishes
		msgContext.getOperationContext().setProperty(HTTPTransportSender.MC_HTTP_REQUEST, request);

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

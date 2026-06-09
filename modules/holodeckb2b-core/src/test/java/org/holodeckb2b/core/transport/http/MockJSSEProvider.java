/*
 * Copyright (C) 2026 The Holodeck B2B Team
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

import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;

import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

/**
 * A mock Java Security Provider for testing the TLS configuration of {@link HTTPTransportSender}. Note that this mock
 * <b>only works</b> for testing the transport sender as it depends on the order of calls being made to provide the
 * expected data on calls.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.2.0
 */
public class MockJSSEProvider extends Provider {
	private final Service 	SSLCTX_SVC;
	private final Service	KMF_SVC;

	// Creating the factories here prevents that the provider itself is called during creation of the factories
	private static final SSLSocketFactory SSF = (SSLSocketFactory) SSLSocketFactory.getDefault();
	private static final SSLServerSocketFactory SSSF = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

	private static KeyStore	privateKeys;
	private static TrustManager[] trustManagers;

	public MockJSSEProvider() {
		super(BouncyCastleJsseProvider.PROVIDER_NAME, "6.6.6", null);
		SSLCTX_SVC = new Provider.Service(this, "SSLContext", "TLS",
							"org.holodeckb2b.core.transport.http.MockJSSEProvider$SSLContextMock", null, null);
		KMF_SVC = new Provider.Service(this, "KeyManagerFactory", "SunX509",
				"org.holodeckb2b.core.transport.http.MockJSSEProvider$KMFMock", null, null);
	}

	public static Collection<X509Certificate> getClientCerts() {
		if (privateKeys == null)
			return Collections.emptyList();

		try {
			Collection<X509Certificate> certs = new ArrayList<>(privateKeys.size());
			for(Enumeration<String> aliases = privateKeys.aliases(); aliases.hasMoreElements();)
				certs.add((X509Certificate) privateKeys.getCertificate(aliases.nextElement()));
			return certs;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static TrustManager getTrustManager() {
		return trustManagers != null && trustManagers.length > 0 ? trustManagers[0] : null;
	}


	@Override
	public Service getService(String type, String algorithm) {
		if ("SSLContext".equals(type) && "TLS".equals(algorithm))
			return SSLCTX_SVC;
		else if ("KeyManagerFactory".equals(type))
			return KMF_SVC;
		else
			return null;
	}

	public static class SSLContextMock extends SSLContextSpi {

		@Override
		protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) throws KeyManagementException {
			trustManagers = tm;
		}

		@Override
		protected SSLSocketFactory engineGetSocketFactory() {
			return SSF;
		}

		@Override
		protected SSLServerSocketFactory engineGetServerSocketFactory() {
			// TODO Auto-generated method stub
			return SSSF;
		}

		@Override
		protected SSLEngine engineCreateSSLEngine() {
			return null;
		}

		@Override
		protected SSLEngine engineCreateSSLEngine(String host, int port) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected SSLSessionContext engineGetServerSessionContext() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected SSLSessionContext engineGetClientSessionContext() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public static class KMFMock extends KeyManagerFactorySpi {
		private KeyManager keyManager;

		@Override
		protected void engineInit(KeyStore ks, char[] password)
				throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {

			privateKeys = ks;
            keyManager = new X509ExtendedKeyManager() {

				@Override
				public String[] getClientAliases(String keyType, Principal[] issuers) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String[] getServerAliases(String keyType, Principal[] issuers) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public X509Certificate[] getCertificateChain(String alias) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public PrivateKey getPrivateKey(String alias) {
					// TODO Auto-generated method stub
					return null;
				}

            };
		}

		@Override
		protected void engineInit(ManagerFactoryParameters spec) throws InvalidAlgorithmParameterException {
			// TODO Auto-generated method stub

		}

		@Override
		protected KeyManager[] engineGetKeyManagers() {
			return new KeyManager[] { keyManager };
		}

	}
}

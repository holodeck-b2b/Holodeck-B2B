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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.activation.DataHandler;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.kernel.TransportUtils;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.transport.http.impl.httpclient4.HttpTransportPropertiesImpl;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.holodeckb2b.axis2.BinaryFormatter;
import org.holodeckb2b.common.pmode.Protocol;
import org.holodeckb2b.common.pmode.TLSConfiguration;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestCertificateManager;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.testing.HttpBackendMock;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.axis2.Axis2Utils;
import org.holodeckb2b.core.axis2.ConfigurationContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.security.trust.TLSCertificateTrustManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HTTPRequestSenderTest {
	private static final char[] EMPTY_PWD = new char[] {};

	private static final String T_CONTENT_TYPE = "application/octet-stream-test";

	private static HttpBackendMock httpServer;
	private static HttpBackendMock httpsServer;

	private static HTTPRequestSender httpSender;
	private static HTTPRequestSender httpsSender;
	private static X509Certificate defaultClientCert;
	private static X509Certificate customClientCert;

	@BeforeAll
	static void setup() throws Exception {
		HolodeckB2BTestCore testCore = new HolodeckB2BTestCore();
		HolodeckB2BCoreInterface.setImplementation(testCore);

		httpServer = new HttpBackendMock();
		httpServer.start();

		httpSender = new HTTPRequestSender(new BasicHttpClientConnectionManager());

		PrivateKeyEntry serverCert =
							KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("localhost.p12"), "test123");
		X509Certificate caCert = CertificateUtils.getCertificate(TestUtils.getTestResource("ca.cert"));

		httpsServer = new HttpBackendMock(serverCert, caCert);
		httpsServer.start();

		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
			Security.addProvider(new BouncyCastleProvider());
		if (Security.getProvider(BouncyCastleJsseProvider.PROVIDER_NAME) == null)
			Security.addProvider(new BouncyCastleJsseProvider());

		PrivateKeyEntry defClientKp =
							KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("default.p12"), "test123");
		defaultClientCert = (X509Certificate) defClientKp.getCertificate();
		PrivateKeyEntry customClientKp =
							KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("custom.p12"), "test123");
		customClientCert = (X509Certificate) customClientKp.getCertificate();

		httpsSender = createHttpsSender(defClientKp);

		TestCertificateManager certman = (TestCertificateManager) testCore.getCertificateManager();
		certman.registerTrustedCertificate(caCert, "ca");
		certman.registerKeyPair(customClientKp, "custom", "test123");
	}

	@AfterAll
	static void shutdown() {
		httpServer.stop();
	}

	@BeforeEach
	void resetServer() throws Exception {
		httpServer.reset();
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS" })
	void testHttpMethod(String method) {
		MessageContext mc = prepareMessageContext();

		mc.setProperty(Constants.Configuration.HTTP_METHOD, method);
		mc.setProperty(HTTPTransportSender.MC_HTTP_EMPTY_BODY, true);

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		assertEquals(method, httpServer.getRequestMethod());
	}

	@Test
	void testDefaultPost() {
		MessageContext mc = prepareMessageContext();
		mc.setProperty(HTTPTransportSender.MC_HTTP_EMPTY_BODY, true);

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		MessageContext respMc = getResponseContext(mc);

		assertEquals("POST", httpServer.getRequestMethod());
		assertEquals(200, respMc.getProperty(HTTPConstants.MC_HTTP_STATUS_CODE));
		assertNull(respMc.getProperty(MessageContext.TRANSPORT_IN));
		assertEquals(Axis2Utils.HTTP_PRODID_HEADER, httpServer.getRcvdHeaders().get("user-agent"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "DELETE" })
	void testEmptyBody(String m) {
		MessageContext mc = prepareMessageContext();

		mc.setProperty(Constants.Configuration.HTTP_METHOD, m);
		if ("POST".equals(m) || "PUT".equals(m))
			mc.setProperty(HTTPTransportSender.MC_HTTP_EMPTY_BODY, true);

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));
		assertNull(httpServer.getRcvdData());

		if ("GET".equals(m) || "DELETE".equals(m))
			assertFalse(httpServer.getRcvdHeaders().containsKey("content-length"));
		else
			assertEquals("0", httpServer.getRcvdHeaders().get("content-length"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "CONNECT", "FAKE" })
	void testUnsupportedHttpMethod(String method) {
		MessageContext mc = prepareMessageContext();
		mc.setProperty(Constants.Configuration.HTTP_METHOD, method);
		mc.setProperty(HTTPTransportSender.MC_HTTP_EMPTY_BODY, true);

		assertThrows(AxisFault.class, () -> httpSender.send(httpUrl(), mc));
	}

	@Test
	void testRequestChunking() {
		MessageContext mc = prepareMessageContext();
		byte[] reqData = prepareRequestData(mc);
		mc.setProperty(HTTPConstants.CHUNKED, true);

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		assertEquals(T_CONTENT_TYPE, httpServer.getRcvdHeaders().get("content-type"));
		assertEquals("chunked", httpServer.getRcvdHeaders().get("transfer-encoding"));
		assertFalse(httpServer.getRcvdHeaders().containsKey("content-length"));
		assertArrayEquals(reqData, httpServer.getRcvdData());
	}

	@Test
	void testRequestCompression() {
		MessageContext mc = prepareMessageContext();
		byte[] reqData = prepareRequestData(mc);
		mc.setProperty(HTTPConstants.MC_GZIP_REQUEST, true);

		byte[] compressed = assertDoesNotThrow(() -> {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
					GZIPOutputStream zos = new GZIPOutputStream(baos)) {
				zos.write(reqData);
				zos.finish();
				return baos.toByteArray();
			}
		});

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		assertEquals(T_CONTENT_TYPE, httpServer.getRcvdHeaders().get("content-type"));

		assertFalse(httpServer.getRcvdHeaders().containsKey("transfer-encoding"));
		assertEquals(compressed.length, Integer.parseInt(httpServer.getRcvdHeaders().get("content-length")));

		assertEquals("gzip", httpServer.getRcvdHeaders().get("content-encoding"));
		assertArrayEquals(compressed, httpServer.getRcvdData());
	}

	@Test
	void testRequestHeaders() {
		MessageContext mc = prepareMessageContext();
		mc.setProperty(Constants.Configuration.HTTP_METHOD, "GET");

		Map<String, String> headers = new HashMap<>();
		headers.put("x-holodeckb2b-testheader-1", "value1");
		headers.put("x-holodeckb2b-testheader-2", "Value2");
		headers.put("x-holodeckb2b-testheader-3", "V@lue3");
		headers.put("User-Agent", "TestUserAgent");
		headers.put("Authorization", "Bearer some-token");

		mc.setProperty(HTTPConstants.HTTP_HEADERS, headers);

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		for (Map.Entry<String, String> header : headers.entrySet())
			assertEquals(header.getValue(), httpServer.getRcvdHeaders().get(header.getKey().toLowerCase()));

		// Ignore headers set by sender itself
		headers.put("Transfer-Encoding", "chunked");
		headers.put("Content-Encoding", "gzip");
		headers.put("Content-Length", "666");

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		assertFalse(httpServer.getRcvdHeaders().containsKey("transfer-encoding"));
		assertFalse(httpServer.getRcvdHeaders().containsKey("content-encoding"));
		assertFalse(httpServer.getRcvdHeaders().containsKey("content-length"));
	}

	@Test
	void testBasicAuth() {
		HttpTransportPropertiesImpl.Authenticator utCfg = new HttpTransportPropertiesImpl.Authenticator();
		utCfg.setUsername("some-test-user");
		utCfg.setPassword("not-a-very-secret-password");
		utCfg.setPreemptiveAuthentication(true);
		utCfg.setAuthSchemes(Collections.singletonList(HttpTransportPropertiesImpl.Authenticator.BASIC));

		MessageContext mc = prepareMessageContext();
		mc.setProperty(Constants.Configuration.HTTP_METHOD, "GET");
		mc.setProperty(HTTPConstants.AUTHENTICATE, utCfg);

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		assertEquals(
			"Basic " + Base64.getEncoder().encodeToString((utCfg.getUsername() + ":" + utCfg.getPassword()).getBytes()),
			httpServer.getRcvdHeaders().get("authorization"));
	}


	@Test
	void testTimeouts() {
		MessageContext mc = prepareMessageContext();
		mc.setProperty(Constants.Configuration.HTTP_METHOD, "HEAD");

		mc.setProperty(HTTPConstants.CONNECTION_TIMEOUT, 100);

		AxisFault af = assertThrows(AxisFault.class, () -> httpSender.send(new URL("http://this-leads-to-timeout.local"), mc));
		assertNotNull(af.getCause());
		assertTrue(af.getCause() instanceof IOException);

		mc.setProperty(HTTPConstants.SO_TIMEOUT, 100);
		httpServer.setSlowness(1000);

		af = assertThrows(AxisFault.class, () -> httpSender.send(httpUrl(), mc));
		assertNotNull(af.getCause());
		assertTrue(af.getCause() instanceof IOException);
	}

	@Test
	void testResponseBody() {
		MessageContext mc = prepareMessageContext();
		mc.setProperty(Constants.Configuration.HTTP_METHOD, "GET");

		byte[] responseData = new byte[2000];
		new Random().nextBytes(responseData);

		httpServer.setResponseEntityBody(responseData);
		Map<String, String> headers = httpServer.getResponseHeaders();
		headers.put("Content-Type", "application/octet-stream");
		headers.put("Content-Length", String.valueOf(responseData.length));

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		MessageContext respCtx = getResponseContext(mc);

		InputStream respStream = (InputStream) respCtx.getProperty(MessageContext.TRANSPORT_IN);
		assertNotNull(respStream);

		byte[] rcvdData = assertDoesNotThrow(() -> {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				Utils.copyStream(respStream, baos);
				return baos.toByteArray();
			}
		});

		assertArrayEquals(responseData, rcvdData);
	}

	@Test
	void testEmptyResponseBody() {
		MessageContext mc = prepareMessageContext();
		mc.setProperty(Constants.Configuration.HTTP_METHOD, "HEAD");

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		assertNull(getResponseContext(mc).getProperty(MessageContext.TRANSPORT_IN));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testResponseHeaders() {
		MessageContext mc = prepareMessageContext();
		mc.setProperty(Constants.Configuration.HTTP_METHOD, "OPTIONS");

		Map<String, String> headers = httpServer.getResponseHeaders();
		headers.put("x-holodeckb2b-testheader-1", "value1");
		headers.put("x-holodeckb2b-testheader-2", "Value2");
		headers.put("x-holodeckb2b-testheader-3", "V@lue3");

		assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));

		MessageContext respCtx = getResponseContext(mc);

		Map<String, String> rcvdHeaders = (Map<String, String>) respCtx.getProperty(MessageContext.TRANSPORT_HEADERS);

		for (Map.Entry<String, String> header : headers.entrySet())
			assertEquals(header.getValue(), rcvdHeaders.get(header.getKey().toLowerCase()));
	}

	@Test
	void testSOAPFault() {
		Set<Integer> noFailure = Set.of(400, 404, 500);

		for (int i = 400; i < 600; i++) {
			MessageContext mc = prepareMessageContext();
			mc.setProperty(Constants.Configuration.HTTP_METHOD, "GET");
			mc.setDoingREST(false);
			httpServer.setResponseCode(i);
			if (noFailure.contains(i)) {
				assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));
				assertTrue(getResponseContext(mc).isProcessingFault());
			} else {
				assertThrows(AxisFault.class, () -> httpSender.send(httpUrl(), mc));
				assertFalse(getResponseContext(mc).isProcessingFault());
			}
			assertEquals(i, mc.getProperty(HTTPConstants.MC_HTTP_STATUS_CODE));
		}
	}

	@Test
	void testRESTFault() {
		for (int i = 400; i < 600; i++) {
			MessageContext mc = prepareMessageContext();
			mc.setProperty(Constants.Configuration.HTTP_METHOD, "GET");
			mc.setDoingREST(true);
			httpServer.setResponseCode(i);
			assertDoesNotThrow(() -> httpSender.send(httpUrl(), mc));
			assertTrue(getResponseContext(mc).isProcessingFault());
			assertEquals(i, mc.getProperty(HTTPConstants.MC_HTTP_STATUS_CODE));
		}
	}

	@Test
	void testDefaultTLS() {
		MessageContext mc = prepareMessageContext();
		mc.setProperty(Constants.Configuration.HTTP_METHOD, "HEAD");
		mc.setDoingREST(true);

		assertDoesNotThrow(() -> httpsSender.send(httpsUrl(), mc));

		assertEquals(200, getResponseContext(mc).getProperty(HTTPConstants.MC_HTTP_STATUS_CODE));
		assertEquals(defaultClientCert, httpsServer.getTLSClientCertificate());
	}

	@Test
	void testCustomTLS() {
		MessageContext mc = prepareMessageContext();
		mc.setProperty(Constants.Configuration.HTTP_METHOD, "OPTIONS");
		mc.setDoingREST(true);

		Protocol protCfg = new Protocol();
		TLSConfiguration tlsCfg = new TLSConfiguration();
		protCfg.setTLSConfiguration(tlsCfg);
		tlsCfg.setClientCertificate("custom", "test123");

		mc.setProperty(HTTPTransportSender.MC_HTTP_CONFIG, protCfg);

		assertDoesNotThrow(() -> httpsSender.send(httpsUrl(), mc));

		assertEquals(200, getResponseContext(mc).getProperty(HTTPConstants.MC_HTTP_STATUS_CODE));
		assertEquals(customClientCert, httpsServer.getTLSClientCertificate());
	}




	private static HTTPRequestSender createHttpsSender(PrivateKeyEntry clientkp) {
		try {
			KeyStore tlsClientKs = KeyStore.getInstance(KeyStore.getDefaultType());
			tlsClientKs.load(null, EMPTY_PWD);
			tlsClientKs.setEntry("clientcert", clientkp, new KeyStore.PasswordProtection(EMPTY_PWD));
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		    kmf.init(tlsClientKs, EMPTY_PWD);
		    SSLContext sslContext = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
			sslContext.init(kmf.getKeyManagers(), new TrustManager[] {new TLSCertificateTrustManager()}, null);

			Registry<ConnectionSocketFactory> sfr = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", new SSLConnectionSocketFactory(sslContext,
											new String[] {"TLSv1.2", "TLSv1.3"}, null, null))
					.build();

			return new HTTPRequestSender(new BasicHttpClientConnectionManager(sfr));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private MessageContext prepareMessageContext() {
		try {
			OperationContext oc = mock(OperationContext.class);
			MessageContext respMc = new MessageContext();
			respMc.setOperationContext(oc);
			when(oc.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE)).thenReturn(respMc);
			MessageContext reqMc = new MessageContext();
			reqMc.setConfigurationContext(new ConfigurationContext((AxisConfiguration) HolodeckB2BCoreInterface.getConfiguration()));
			reqMc.setOperationContext(oc);
			reqMc.setProperty(HTTPConstants.CONNECTION_TIMEOUT, 60000);
			reqMc.setProperty(HTTPConstants.SO_TIMEOUT, 60000);
			reqMc.setProperty(HTTPTransportSender.MC_OMFORMAT, new OMOutputFormat());
			reqMc.setEnvelope(TransportUtils.createSOAPEnvelope(null));
			return reqMc;
		} catch (AxisFault e) {
			fail("Could not create mock operation context");
			return null;
		}
	}

	private byte[] prepareRequestData(MessageContext reqCtx) {
		byte[] data = new byte[2000];
		new Random().nextBytes(data);

		DataHandler dh = new DataHandler(new ByteArrayDataSource(data, T_CONTENT_TYPE));
		reqCtx.getAttachmentMap().addDataHandler("test", dh);

		reqCtx.setProperty(HTTPTransportSender.MC_MSG_FORMATTER, new BinaryFormatter());

		return data;
	}

	private MessageContext getResponseContext(MessageContext reqCtx) {
		try {
			return reqCtx.getOperationContext().getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
		} catch (Throwable t) {
			return null;
		}
	}

	private URL httpUrl() {
		try {
			int port = httpServer.getPort();
			return new URL("http://localhost:" + port);
		} catch (MalformedURLException e) {
			fail("Could not create URL for HTTP mock server");
			return null;
		}
	}

	private URL httpsUrl() {
		try {
			int port = httpsServer.getPort();
			return new URL("https://localhost:" + port);
		} catch (MalformedURLException e) {
			fail("Could not create URL for HTTP mock server");
			return null;
		}
	}
}

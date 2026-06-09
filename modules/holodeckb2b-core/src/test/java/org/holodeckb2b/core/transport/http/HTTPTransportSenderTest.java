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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.kernel.MessageFormatter;
import org.apache.axis2.kernel.TransportUtils;
import org.apache.axis2.kernel.http.ApplicationXMLFormatter;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.kernel.http.SOAPMessageFormatter;
import org.apache.axis2.kernel.http.XFormURLEncodedFormatter;
import org.apache.axis2.transport.http.server.AxisHttpConnection;
import org.apache.axis2.transport.http.server.AxisHttpResponse;
import org.apache.axis2.transport.http.server.AxisHttpResponseImpl;
import org.apache.http.HttpResponse;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpProcessor;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.holodeckb2b.axis2.BinaryFormatter;
import org.holodeckb2b.common.pmode.Protocol;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestCertificateManager;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.core.axis2.ConfigurationContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.security.trust.TLSCertificateTrustManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HTTPTransportSenderTest {

	private static PrivateKeyEntry partyaKp;
	private static PrivateKeyEntry partybKp;

	@BeforeAll
	static void setup() throws Exception {
		HolodeckB2BTestCore testCore = new HolodeckB2BTestCore();
		HolodeckB2BCoreInterface.setImplementation(testCore);

		if (Security.getProvider(BouncyCastleJsseProvider.PROVIDER_NAME) != null)
			Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);

		Security.insertProviderAt(new MockJSSEProvider(), 1);

		partyaKp = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("partya.p12"), "test123");
		partybKp = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("partyb.p12"), "test123");

		TestCertificateManager certman = (TestCertificateManager) testCore.getCertificateManager();
		certman.registerKeyPair(partyaKp, "partya", "test123");
		certman.registerKeyPair(partybKp, "partyb", "test123");
	}

	@AfterAll
	static void cleanup() {
		Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
	}

	@Nested
	class Configuration {
		@Test
		void testDefaultInit() throws AxisFault {
			TransportOutDescription transportOut = new TransportOutDescription("test");

			String ecgroups = System.getProperty("jdk.tls.namedGroups");

			HTTPTransportSender sender = new HTTPTransportSender();
			assertDoesNotThrow(() -> sender.init(null, transportOut));

			assertFalse((Boolean) getFieldValue("defaultCompression", sender));
			assertFalse((Boolean) getFieldValue("defaultChunked", sender));
			assertEquals(60000, (Integer) getFieldValue("defaultConnectTimeout", sender));
			assertEquals(60000, (Integer) getFieldValue("defaultReadTimeout", sender));
			assertEquals(ecgroups, System.getProperty("jdk.tls.namedGroups"));

			assertTrue(MockJSSEProvider.getClientCerts().isEmpty());
		}

		@Test
		void testCustomInit() throws AxisFault {
			TransportOutDescription transportOut = new TransportOutDescription("test");

			transportOut.addParameter(new Parameter("Content-Encoding", "gzip"));
			transportOut.addParameter(new Parameter("Transfer-Encoding", "chunked"));
			transportOut.addParameter(new Parameter("CONNECTION_TIMEOUT", "500"));
			transportOut.addParameter(new Parameter("SO_TIMEOUT", "1500"));
			transportOut.addParameter(new Parameter("MAX_CONNECTIONS", "50"));
			transportOut.addParameter(new Parameter("allowedECGroups", "brainp256r, brainp512r"));

			OMFactory omf = OMAbstractFactory.getOMFactory();
			OMElement clientCertRefs = omf.createOMElement(new QName("ClientCertRefs"));
		    OMElement clientCert1 = omf.createOMElement(new QName("KeystoreAlias"), clientCertRefs);
			clientCert1.setText("partya");
			clientCert1.addAttribute("password", "test123", null);
			OMElement clientCert2 = omf.createOMElement(new QName("KeystoreAlias"), clientCertRefs);
			clientCert2.setText("partyb");
			clientCert2.addAttribute("password", "test123", null);

			transportOut.addParameter(new Parameter("CLIENT_TLS", clientCertRefs));

			HTTPTransportSender sender = new HTTPTransportSender();
			assertDoesNotThrow(() -> sender.init(null, transportOut));

			assertTrue((Boolean) getFieldValue("defaultCompression", sender));
			assertTrue((Boolean) getFieldValue("defaultChunked", sender));
			assertEquals(500, (Integer) getFieldValue("defaultConnectTimeout", sender));
			assertEquals(1500, (Integer) getFieldValue("defaultReadTimeout", sender));

			assertEquals("brainp256r, brainp512r", System.getProperty("jdk.tls.namedGroups"));

			PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager)
												getFieldValue("connectionManager", getFieldValue("requestSender", sender));

			assertEquals(50, connMgr.getMaxTotal());
			assertEquals(50, connMgr.getDefaultMaxPerRoute());

			assertTrue(MockJSSEProvider.getTrustManager() instanceof TLSCertificateTrustManager);

			Collection<X509Certificate> clientCerts = MockJSSEProvider.getClientCerts();
			assertEquals(2, clientCerts.size());
			assertTrue(clientCerts.contains(partyaKp.getCertificate()));
			assertTrue(clientCerts.contains(partybKp.getCertificate()));
		}

		@Test
		void testOldClientCertConfig() throws AxisFault {
			TransportOutDescription transportOut = new TransportOutDescription("test");

			OMFactory omf = OMAbstractFactory.getOMFactory();
			OMElement clientCert1 = omf.createOMElement(new QName("KeystoreAlias"));
			clientCert1.setText("partya");
			clientCert1.addAttribute("password", "test123", null);

			transportOut.addParameter(new Parameter("CLIENT_TLS", clientCert1));

			HTTPTransportSender sender = new HTTPTransportSender();
			assertDoesNotThrow(() -> sender.init(null, transportOut));

			Collection<X509Certificate> clientCerts = MockJSSEProvider.getClientCerts();
			assertEquals(1, clientCerts.size());
			assertTrue(clientCerts.contains(partyaKp.getCertificate()));
		}

		@Test
		void testExternalPassword() throws AxisFault {
			System.setProperty("tls.partya.password", "test123");

			TransportOutDescription transportOut = new TransportOutDescription("test");

			OMFactory omf = OMAbstractFactory.getOMFactory();
			OMElement clientCertRefs = omf.createOMElement(new QName("ClientCertRefs"));
		    OMElement clientCert1 = omf.createOMElement(new QName("KeystoreAlias"), clientCertRefs);
			clientCert1.setText("partya");
			clientCert1.addAttribute("password", "sys:tls.partya.password", null);

			transportOut.addParameter(new Parameter("CLIENT_TLS", clientCert1));

			HTTPTransportSender sender = new HTTPTransportSender();
			assertDoesNotThrow(() -> sender.init(null, transportOut));

			Collection<X509Certificate> clientCerts = MockJSSEProvider.getClientCerts();
			assertEquals(1, clientCerts.size());
			assertTrue(clientCerts.contains(partyaKp.getCertificate()));
		}

		@Test
		void testGenericMessageSettings() throws AxisFault {
			TransportOutDescription transportOut = new TransportOutDescription("test");
			transportOut.addParameter(new Parameter("Content-Encoding", "gzip"));
			transportOut.addParameter(new Parameter("CONNECTION_TIMEOUT", "500"));
			transportOut.addParameter(new Parameter("SO_TIMEOUT", "1500"));

			HTTPTransportSender sender = new HTTPTransportSender();
			assertDoesNotThrow(() -> sender.init(null, transportOut));

			MessageContext mc = new MessageContext();
			mc.setServerSide(true);
			mc.setEnvelope(TransportUtils.createSOAPEnvelope(null));

			assertDoesNotThrow(() -> {
				try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					AxisHttpResponse httpResponse = mock(AxisHttpResponse.class);
					when(httpResponse.getOutputStream()).thenReturn(out);
					mc.setProperty(Constants.OUT_TRANSPORT_INFO, httpResponse);
					InvocationResponse response = sender.invoke(mc);
					assertEquals(InvocationResponse.CONTINUE, response);
				}
			});

			assertTrue(mc.isPropertyTrue(HTTPConstants.CHUNKED));
			assertTrue(mc.isPropertyTrue(HTTPConstants.MC_GZIP_RESPONSE));
			assertEquals(500, mc.getProperty(HTTPConstants.CONNECTION_TIMEOUT));
			assertEquals(1500, mc.getProperty(HTTPConstants.SO_TIMEOUT));
		}

		@Test
		void testCustomMessageSettings() throws Exception {
			TransportOutDescription transportOut = new TransportOutDescription("test");
			transportOut.addParameter(new Parameter("Content-Encoding", "gzip"));
			transportOut.addParameter(new Parameter("CONNECTION_TIMEOUT", "500"));
			transportOut.addParameter(new Parameter("SO_TIMEOUT", "1500"));

			HTTPTransportSender sender = new HTTPTransportSender();
			assertDoesNotThrow(() -> sender.init(null, transportOut));

			Protocol customCfg = new Protocol();
			customCfg.setChunking(true);
			customCfg.setHTTPCompression(false);
			customCfg.setConnectionTimeout(2000);
			customCfg.setReadTimeout(2000);

			OperationContext oc = mock(OperationContext.class);
			MessageContext mc = new MessageContext();
			mc.setOperationContext(oc);
			mc.setConfigurationContext(new ConfigurationContext((AxisConfiguration) HolodeckB2BCoreInterface.getConfiguration()));
			mc.setEnvelope(TransportUtils.createSOAPEnvelope(null));
			mc.setProperty(Constants.Configuration.TRANSPORT_URL, "http://just.for.testing");
			mc.setProperty(HTTPTransportSender.MC_HTTP_CONFIG, customCfg);

			HTTPRequestSender requestSender = mock(HTTPRequestSender.class);
			assertDoesNotThrow(() -> doNothing().when(requestSender).send(any(), any()));
			Field field = HTTPTransportSender.class.getDeclaredField("requestSender");
			field.setAccessible(true);
			field.set(sender, requestSender);

			InvocationResponse response = assertDoesNotThrow(() -> sender.invoke(mc));
			assertEquals(InvocationResponse.CONTINUE, response);

			assertTrue(mc.isPropertyTrue(HTTPConstants.CHUNKED));
			assertFalse(mc.isPropertyTrue(HTTPConstants.MC_GZIP_REQUEST));
			assertEquals(2000, mc.getProperty(HTTPConstants.CONNECTION_TIMEOUT));
			assertEquals(2000, mc.getProperty(HTTPConstants.SO_TIMEOUT));
		}
	}

	@Nested
	class MessageFormatterSelection {
		private HTTPTransportSender sender;
		private MessageContext mc;

		@BeforeEach
		void setUp() throws Exception {
			TransportOutDescription transportOut = new TransportOutDescription("test");

			sender = new HTTPTransportSender();
			assertDoesNotThrow(() -> sender.init(null, transportOut));

			OperationContext oc = mock(OperationContext.class);
			mc = new MessageContext();
			mc.setOperationContext(oc);
			mc.setConfigurationContext(new ConfigurationContext((AxisConfiguration) HolodeckB2BCoreInterface.getConfiguration()));
			mc.setProperty(Constants.Configuration.TRANSPORT_URL, "http://just.for.testing");

			HTTPRequestSender requestSender = mock(HTTPRequestSender.class);
			assertDoesNotThrow(() -> doNothing().when(requestSender).send(any(), any()));
			Field field = HTTPTransportSender.class.getDeclaredField("requestSender");
			field.setAccessible(true);
			field.set(sender, requestSender);
		}

		@Test
		void testExplicit() throws AxisFault, SOAPProcessingException {
			MessageFormatter mf = new BinaryFormatter();
			mc.setProperty(Constants.Configuration.MESSAGE_FORMATTER, mf);

			mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
			mc.setDoingREST(false);

			assertDoesNotThrow(() -> sender.invoke(mc));

			assertEquals(mf, mc.getProperty(HTTPTransportSender.MC_MSG_FORMATTER));
		}

		@ParameterizedTest
		@ValueSource(strings = { "POST", "PUT", "HEAD", "OPTIONS" })
		void testRESTWithEntityBody(String method) throws AxisFault, SOAPProcessingException {

			mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
			mc.setDoingREST(true);
			mc.setProperty(Constants.Configuration.HTTP_METHOD, method);

			assertDoesNotThrow(() -> sender.invoke(mc));

			assertTrue(mc.getProperty(HTTPTransportSender.MC_MSG_FORMATTER) instanceof ApplicationXMLFormatter);
		}

		@ParameterizedTest
		@ValueSource(strings = { "GET", "DELETE" })
		void testRESTWithoutEntityBody(String method) throws AxisFault, SOAPProcessingException {

			mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
			mc.setDoingREST(true);
			mc.setProperty(Constants.Configuration.HTTP_METHOD, method);

			assertDoesNotThrow(() -> sender.invoke(mc));

			assertTrue(mc.getProperty(HTTPTransportSender.MC_MSG_FORMATTER) instanceof XFormURLEncodedFormatter);
		}

		@Test
		void testSwA() throws Exception {
			TransportOutDescription transportOut = new TransportOutDescription("test");

			HTTPTransportSender sender = new HTTPTransportSender();
			assertDoesNotThrow(() -> sender.init(null, transportOut));

			OperationContext oc = mock(OperationContext.class);
			MessageContext mc = new MessageContext();
			mc.setOperationContext(oc);
			mc.setConfigurationContext(new ConfigurationContext((AxisConfiguration) HolodeckB2BCoreInterface.getConfiguration()));
			mc.setEnvelope(TransportUtils.createSOAPEnvelope(null));
			mc.setProperty(Constants.Configuration.TRANSPORT_URL, "http://just.for.testing");

			HTTPRequestSender requestSender = mock(HTTPRequestSender.class);
			assertDoesNotThrow(() -> doNothing().when(requestSender).send(any(), any()));
			Field field = HTTPTransportSender.class.getDeclaredField("requestSender");
			field.setAccessible(true);
			field.set(sender, requestSender);

			assertDoesNotThrow(() -> sender.invoke(mc));
			assertFalse((Boolean) mc.getProperty(Constants.Configuration.ENABLE_SWA));
			assertFalse(mc.isDoingSwA());

			OperationContext oc2 = mock(OperationContext.class);
			MessageContext mc2 = new MessageContext();
			mc2.setOperationContext(oc2);
			mc2.setConfigurationContext(new ConfigurationContext((AxisConfiguration) HolodeckB2BCoreInterface.getConfiguration()));
			mc2.setEnvelope(TransportUtils.createSOAPEnvelope(null));
			mc2.setProperty(Constants.Configuration.TRANSPORT_URL, "http://just.for.testing");

			byte[] data = new byte[2000];
			new Random().nextBytes(data);
			DataHandler dh = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
			mc2.getAttachmentMap().addDataHandler("test", dh);

			assertDoesNotThrow(() -> sender.invoke(mc2));
			assertTrue((Boolean) mc2.getProperty(Constants.Configuration.ENABLE_SWA));
			assertTrue(mc2.isDoingSwA());
		}

		@Test
		void testSOAP() throws AxisFault, SOAPProcessingException {

			mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
			mc.setDoingREST(false);

			assertDoesNotThrow(() -> sender.invoke(mc));

			assertTrue(mc.getProperty(HTTPTransportSender.MC_MSG_FORMATTER) instanceof SOAPMessageFormatter);
		}
	}

	@Nested
	class ResponseSending {
		private HTTPTransportSender sender;
		private MessageContext mc;
		private AxisHttpResponseMock httpResponse;

		@BeforeEach
		void setUp() throws Exception {
			TransportOutDescription transportOut = new TransportOutDescription("test");

			sender = new HTTPTransportSender();
			assertDoesNotThrow(() -> sender.init(null, transportOut));

			OperationContext oc = mock(OperationContext.class);
			mc = new MessageContext();
			mc.setServerSide(true);
			mc.setOperationContext(oc);
			mc.setConfigurationContext(new ConfigurationContext((AxisConfiguration) HolodeckB2BCoreInterface.getConfiguration()));

			httpResponse = new AxisHttpResponseMock(new ByteArrayOutputStream());
			mc.setProperty(Constants.OUT_TRANSPORT_INFO, httpResponse);

			MessageFormatter mf = new BinaryFormatter();
			mc.setProperty(Constants.Configuration.MESSAGE_FORMATTER, mf);

			DataHandler dh = new DataHandler(new ByteArrayDataSource("Hello World!".getBytes(), "text/plain"));
			mc.getAttachmentMap().addDataHandler("content", dh);
			mc.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_FORMATTER, new BinaryFormatter());
		}

		@Test
		void testNoContent() {
			mc.getAttachmentMap().removeDataHandler("content");

			assertDoesNotThrow(() -> sender.invoke(mc));

			assertTrue(httpResponse.getSetHeaders().isEmpty());
			assertTrue(httpResponse.getOutputStream().toByteArray().length == 0);
		}

		@Test
		void testNoBodyProperty() {
			mc.setProperty(HTTPTransportSender.MC_HTTP_EMPTY_BODY, true);

			assertDoesNotThrow(() -> sender.invoke(mc));

			assertTrue(httpResponse.getSetHeaders().isEmpty());
			assertTrue(httpResponse.getOutputStream().toByteArray().length == 0);
		}

		@Test
		void testUncompressedEntityBody() {
			assertDoesNotThrow(() -> sender.invoke(mc));

			assertFalse(httpResponse.getSetHeaders().containsKey("content-encoding"));
			assertArrayEquals("Hello World!".getBytes(), httpResponse.getOutputStream().toByteArray());
		}

		@Test
		void testCompressedEntityBody() {
			Protocol httpCfg = new Protocol();
			httpCfg.setHTTPCompression(true);
			mc.setProperty(HTTPTransportSender.MC_HTTP_CONFIG, httpCfg);

			assertDoesNotThrow(() -> sender.invoke(mc));

			assertEquals("gzip", httpResponse.getSetHeaders().get("content-encoding"));

			byte[] compressed = assertDoesNotThrow(() -> {
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
					 GZIPOutputStream zos = new GZIPOutputStream(baos)) {
					zos.write("Hello World!".getBytes());
					zos.finish();
					return baos.toByteArray();
				}
			});

			assertArrayEquals(compressed, httpResponse.getOutputStream().toByteArray());
		}


		@Test
		void testHeaders() {
			Map<String, String> headers = new HashMap<>();
			headers.put("x-holodeckb2b-testheader-1", "value1");
			headers.put("x-holodeckb2b-testheader-2", "Value2");
			headers.put("x-holodeckb2b-testheader-3", "V@lue3");
			// These headers should be ignored as their set by the sender itself
			headers.put("Transfer-Encoding", "chunked");
			headers.put("Content-Length", "666");

			mc.setProperty(HTTPConstants.HTTP_HEADERS, headers);

			assertDoesNotThrow(() -> sender.invoke(mc));

			Map<String,String> setHeaders = httpResponse.getSetHeaders();

			for (int i = 1; i <= 3; i++) {
				assertEquals(headers.get("x-holodeckb2b-testheader-" + i),
							 setHeaders.get("x-holodeckb2b-testheader-" + i));
			}

			assertFalse(setHeaders.containsKey("transfer-encoding"));
			assertFalse(setHeaders.containsKey("content-length"));
		}

		void testCompressed() {

		}

		class AxisHttpResponseMock extends AxisHttpResponseImpl {
			private Map<String, String> headers = new HashMap<>();
			private ByteArrayOutputStream os;

			public AxisHttpResponseMock(ByteArrayOutputStream os) {
				super(mock(AxisHttpConnection.class), mock(HttpResponse.class), mock(HttpProcessor.class),
			          mock(org.apache.http.protocol.HttpContext.class));
				this.os = os;
			}

			@Override
			public ByteArrayOutputStream getOutputStream() {
				return os;
			}

			@Override
			public void addHeader(String name, String value) {
				headers.put(name.toLowerCase(), value);
			}

			@Override
			public void removeHeaders(String name) {
				headers.remove(name.toLowerCase());
			}

			public Map<String, String> getSetHeaders() {
				return headers;
			}
		}
	}

	private Object getFieldValue(String fieldName, Object object) {
		try {
			Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}

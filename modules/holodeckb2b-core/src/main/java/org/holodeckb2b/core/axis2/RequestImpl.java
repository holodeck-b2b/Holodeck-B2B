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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axiom.mime.Header;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.transport.http.AxisRequestEntity;
import org.apache.axis2.transport.http.HTTPAuthenticator;
import org.apache.axis2.transport.http.HTTPTransportConstants;
import org.apache.axis2.transport.http.Request;
import org.apache.axis2.transport.http.impl.httpclient4.AxisRequestEntityImpl;
import org.apache.axis2.transport.http.impl.httpclient4.HTTPProxyConfigurator;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;

/**
 * Is a customised verion of <code>org.apache.axis2.transport.http.impl.httpclient4.RequestImpl</code> that allows to
 * set the {@link HttpClientContext} of the request. Because the Axis2 class is <code>final</code> we can't subclass it
 * and have to make a modified copy.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 */
class RequestImpl implements Request {
	private static final Logger log = LogManager.getLogger(RequestImpl.class);

    private static final String[] COOKIE_HEADER_NAMES = { HTTPConstants.HEADER_SET_COOKIE, HTTPConstants.HEADER_SET_COOKIE2 };

    private final HttpClient httpClient;
    private final MessageContext msgContext;
    private final URL url;
    private final HttpRequestBase method;
    private final HttpHost httpHost;
    private final RequestConfig.Builder requestConfig = RequestConfig.custom();
    private final HttpClientContext clientContext;
    private HttpResponse response;

    RequestImpl(HttpClient httpClient, MessageContext msgContext, final String methodName, URL url,
            	AxisRequestEntity requestEntity, HttpClientContext clientContext) throws AxisFault {
        this.httpClient = httpClient;
        this.msgContext = msgContext;
        this.clientContext = clientContext;
        this.url = url;
        if (requestEntity == null) {
            method = new HttpRequestBase() {
                @Override
                public String getMethod() {
                    return methodName;
                }
            };
        } else {
            HttpEntityEnclosingRequestBase entityEnclosingRequest = new HttpEntityEnclosingRequestBase() {
                @Override
                public String getMethod() {
                    return methodName;
                }
            };
            entityEnclosingRequest.setEntity(new AxisRequestEntityImpl(requestEntity));
            method = entityEnclosingRequest;
        }
        try {
            method.setURI(url.toURI());
        } catch (URISyntaxException ex) {
            throw AxisFault.makeFault(ex);
        }
        int port = url.getPort();
        String protocol = url.getProtocol();
        if (port == -1) {
            if (HTTPTransportConstants.PROTOCOL_HTTP.equals(protocol)) {
                port = 80;
            } else if (HTTPTransportConstants.PROTOCOL_HTTPS.equals(protocol)) {
                port = 443;
            }
        }
        httpHost = new HttpHost(url.getHost(), port, url.getProtocol());
    }

    @Override
    public void enableHTTP10() {
        method.setProtocolVersion(HttpVersion.HTTP_1_0);
    }

    @Override
    public void setHeader(String name, String value) {
        method.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        method.addHeader(name, value);
    }

    private static Header[] convertHeaders(org.apache.http.Header[] headers) {
        Header[] result = new Header[headers.length];
        for (int i=0; i<headers.length; i++) {
            result[i] = new Header(headers[i].getName(), headers[i].getValue());
        }
        return result;
    }

    @Override
    public Header[] getRequestHeaders() {
        return convertHeaders(method.getAllHeaders());
    }

    @Override
    public void setConnectionTimeout(int timeout) {
        requestConfig.setConnectTimeout(timeout);
    }

    @Override
    public void setSocketTimeout(int timeout) {
        requestConfig.setSocketTimeout(timeout);
    }

    @Override
    public int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public String getStatusText() {
        return response.getStatusLine().getReasonPhrase();
    }

    @Override
    public String getResponseHeader(String name) {
        org.apache.http.Header header = response.getFirstHeader(name);
        return header == null ? null : header.getValue();
    }

    @Override
    public Header[] getResponseHeaders() {
        return convertHeaders(response.getAllHeaders());
    }

    @Override
    public Map<String,String> getCookies() {
        Map<String,String> cookies = null;
        for (String name : COOKIE_HEADER_NAMES) {
            for (org.apache.http.Header header : response.getHeaders(name)) {
                for (HeaderElement element : header.getElements()) {
                    if (cookies == null) {
                        cookies = new HashMap<String,String>();
                    }
                    cookies.put(element.getName(), element.getValue());
                }
            }
        }
        return cookies;
    }

    @Override
    public InputStream getResponseContent() throws IOException {
        HttpEntity entity = response.getEntity();
        return entity == null ? null : entity.getContent();
    }

    @Override
    public void execute() throws IOException {
        populateHostConfiguration();

        // add compression headers if needed
        if (msgContext.isPropertyTrue(HTTPConstants.MC_ACCEPT_GZIP)) {
            method.addHeader(HTTPConstants.HEADER_ACCEPT_ENCODING,
                             HTTPConstants.COMPRESSION_GZIP);
        }

        String cookiePolicy = (String) msgContext.getProperty(HTTPConstants.COOKIE_POLICY);
        if (cookiePolicy != null) {
            requestConfig.setCookieSpec(cookiePolicy);
        }

        method.setConfig(requestConfig.build());

        response = httpClient.execute(httpHost, method, clientContext);
    }

    @Override
    public void releaseConnection() {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
        	log.trace("Cleanup response and release connection");
            try {
                EntityUtils.consume(entity);
            } catch (IOException e) {
                log.error("Error while cleaning response : {}", Utils.getExceptionTrace(e));
            }
        } else if (response instanceof CloseableHttpResponse) {
        	log.trace("Release connection");
        	try {
        		((CloseableHttpResponse) response).close();
			} catch (IOException e) {
				log.error("Error while closing the connection: {}", Utils.getExceptionTrace(e));
        	}
        }
    }

    /**
     * getting host configuration to support standard http/s, proxy and NTLM
     * support
     *
     * @return a HostConfiguration set up with proxy information
     * @throws org.apache.axis2.AxisFault if problems occur
     */
    private void populateHostConfiguration() throws AxisFault {
        // proxy configuration

        if (HTTPProxyConfigurator.isProxyEnabled(msgContext, url)) {
            if (log.isDebugEnabled()) {
                log.debug("Configuring HTTP proxy.");
            }
            HTTPProxyConfigurator.configure(msgContext, requestConfig, clientContext);
        }
    }

    /*
     * This will handle server Authentication, It could be either NTLM, Digest
     * or Basic Authentication. Apart from that user can change the priory or
     * add a custom authentication scheme.
     */
    @Override
    public void enableAuthentication(HTTPAuthenticator authenticator) {
        requestConfig.setAuthenticationEnabled(true);

        String username = authenticator.getUsername();
        String password = authenticator.getPassword();
        String host = authenticator.getHost();
        String domain = authenticator.getDomain();

        int port = authenticator.getPort();
        String realm = authenticator.getRealm();

        Credentials creds;
        CredentialsProvider credsProvider = clientContext.getCredentialsProvider();
        if (credsProvider == null) {
            credsProvider = new BasicCredentialsProvider();
            clientContext.setCredentialsProvider(credsProvider);
        }
        if (host != null) {
            if (domain != null) {
                /* Credentials for NTLM Authentication */
                creds = new NTCredentials(username, password, host, domain);
            } else {
                /* Credentials for Digest and Basic Authentication */
                creds = new UsernamePasswordCredentials(username, password);
            }
            credsProvider.setCredentials(new AuthScope(host, port, realm), creds);
        } else {
            if (domain != null) {
                /*
                 * Credentials for NTLM Authentication when host is
                 * ANY_HOST
                 */
                creds = new NTCredentials(username, password, AuthScope.ANY_HOST, domain);
                credsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, port, realm), creds);
            } else {
                /* Credentials only for Digest and Basic Authentication */
                creds = new UsernamePasswordCredentials(username, password);
                credsProvider.setCredentials(new AuthScope(AuthScope.ANY), creds);
            }
        }

        /* Customizing the priority Order */
        List schemes = authenticator.getAuthSchemes();
        if (schemes != null && schemes.size() > 0) {
            List authPrefs = new ArrayList(3);
            for (int i = 0; i < schemes.size(); i++) {
                if (schemes.get(i) instanceof AuthPolicy) {
                    authPrefs.add(schemes.get(i));
                    continue;
                }
                String scheme = (String) schemes.get(i);
                authPrefs.add(authenticator.getAuthPolicyPref(scheme));

            }
            requestConfig.setTargetPreferredAuthSchemes(authPrefs);
        }
    }
}

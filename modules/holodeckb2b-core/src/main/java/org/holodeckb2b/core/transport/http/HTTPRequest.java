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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPAuthenticator;
import org.apache.axis2.transport.http.HTTPTransportConstants;
import org.apache.axis2.transport.http.impl.httpclient4.HTTPProxyConfigurator;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;

/**
 * Represents a HTTP request and provides information about the response to the request.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 9.0.0
 */
/*
 * This class is a customised version of <code>org.apache.axis2.transport.http.impl.httpclient4.RequestImpl</code> that
 * allows to set the {@link HttpClientContext} of the request which is needed for correct functioning of the connection
 * manager. Because the Axis2 class is <code>final</code> we can't subclass it and have to make a modified copy.
 */
class HTTPRequest {
	private static final Logger log = LogManager.getLogger();

    private static final String[] COOKIE_HEADER_NAMES = { HTTPConstants.HEADER_SET_COOKIE, HTTPConstants.HEADER_SET_COOKIE2 };

    private final HttpClient httpClient;
    private final MessageContext msgContext;
    private final URL url;
    private final HttpRequestBase method;
    private final HttpHost httpHost;
    private final RequestConfig.Builder requestConfig = RequestConfig.custom();
    private final HttpClientContext clientContext;
    private HttpResponse response;

    HTTPRequest(HttpClient httpClient, MessageContext msgContext, final String methodName, URL url,
            	RequestEntity requestEntity, HttpClientContext clientContext) throws AxisFault {
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
            if ("POST".equals(methodName) || "PUT".equals(methodName))
            	// HTTP 1.1 recommends setting the Content-Length header if no entity is included with POST or PUT
				method.addHeader(HTTPConstants.HEADER_CONTENT_LENGTH, "0");
        } else {
            HttpEntityEnclosingRequestBase entityEnclosingRequest = new HttpEntityEnclosingRequestBase() {
                @Override
                public String getMethod() {
                    return methodName;
                }
            };
            entityEnclosingRequest.setEntity(requestEntity);
            method = entityEnclosingRequest;
            method.addHeader(requestEntity.getContentType());
            if (requestEntity.isCompressed())
            	method.addHeader(HTTPConstants.HEADER_CONTENT_ENCODING, HTTPConstants.COMPRESSION_GZIP);
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

    /**
     * Sets the HTTP header with the given name to the given value. If a header with this name was already set, its
     * value is overwritten.
     * <p>
     * Note that this method can only be used to add headers that are not created by this sender or the HttpClient
     * components. If the name of such header is specified, the method does nothing.
	 *
     * @param name		name of the HTTP header to set
     * @param value		value of the HTTP header
     */
    public void setHeader(String name, String value) {
    	if (allowedHeader(name))
    		method.setHeader(name, value);
    }

    /**
     * Adds the HTTP header with the given name to the given value. If a header with this name was already set, the
     * specified value is appended to the existing value.
     * <p>
     * Note that this method can only be used to add headers that are not created by this sender or the HttpClient
     * components. If the name of such header is specified, the method does nothing.
	 *
     * @param name		name of the HTTP header to set
     * @param value		value of the HTTP header     */
    public void addHeader(String name, String value) {
    	if (allowedHeader(name))
    		method.addHeader(name, value);
    }

    /**
     * Gets all headers included in this request. As HTTP header names are case-insentive, the header names are
     * converted to lower-case.
     *
     * @return array with all headers included in this request
     */
    public Map<String, String> getRequestHeaders() {
        return convertHeaders(method.getAllHeaders());
    }

    /**
     * Sets the connection timeout for this request
     *
     * @param timeout the timeout in milliseconds
     */
    public void setConnectionTimeout(int timeout) {
        requestConfig.setConnectTimeout(timeout);
    }

    /**
     * Sets the read/socket timeout for this request
     *
     * @param timeout the timeout in milliseconds
     */
    public void setSocketTimeout(int timeout) {
        requestConfig.setSocketTimeout(timeout);
    }

    /**
     * @return the HTTP status code of the response
     */
    public int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    /**
     * @return	the HTTP status text of the response
     */
    public String getStatusText() {
        return response.getStatusLine().getReasonPhrase();
    }

    /**
     * Gets the value of the HTTP header with the given name from the response. As HTTP header names are case-insentive,
     * this method also performs a case-insensitive lookup.
     *
     * @param name the name of the header to retrieve
     * @return the value of the header if it exists in the response, <code>null</code> otherwise
     */
    public String getResponseHeader(String name) {
        org.apache.http.Header header = response.getFirstHeader(name);
        return header == null ? null : header.getValue();
    }

    /**
     * Gets all headers included in this request. As HTTP header names are case-insentive, the header names are
     * converted to lower-case.
     *
     * @return array with all headers included in this request
     */
    public Map<String, String> getResponseHeaders() {
        return convertHeaders(response.getAllHeaders());
    }

    /**
     * Gets all cookies included in the response.
     *
     * @return a map with all cookies included in the response
     */
    public Map<String,String> getCookies() {
        Map<String,String> cookies = null;
        for (String name : COOKIE_HEADER_NAMES) {
            for (org.apache.http.Header header : response.getHeaders(name)) {
                for (HeaderElement element : header.getElements()) {
                    if (cookies == null) {
                        cookies = new HashMap<>();
                    }
                    cookies.put(element.getName(), element.getValue());
                }
            }
        }
        return cookies;
    }

    /**
     * Gets an input stream to read the entity body of the HTTP response.
     *
     * @return	if the response contains an entity body, an input stream to access it, <code>null</code> otherwise
     * @throws IOException if a connection error occurs
     */
    public InputStream getResponseContent() throws IOException {
        HttpEntity entity = response.getEntity();
        return entity == null ? null : entity.getContent();
    }

    /**
     * Executes the HTTP request.
     *
     * @throws IOException	when a connection error occurs.
     */
    public void execute() throws IOException {
        if (HTTPProxyConfigurator.isProxyEnabled(msgContext, url)) {
            if (log.isDebugEnabled()) {
                log.debug("Configuring HTTP proxy.");
            }
            HTTPProxyConfigurator.configure(msgContext, requestConfig, clientContext);
        }

        // add compression headers if needed
        if (msgContext.isPropertyTrue(HTTPConstants.MC_ACCEPT_GZIP))
			method.addHeader(HTTPConstants.HEADER_ACCEPT_ENCODING, HTTPConstants.COMPRESSION_GZIP);

        String cookiePolicy = (String) msgContext.getProperty(HTTPConstants.COOKIE_POLICY);
        if (cookiePolicy != null) {
			requestConfig.setCookieSpec(cookiePolicy);
		}

        method.setConfig(requestConfig.build());

        response = httpClient.execute(httpHost, method, clientContext);
    }

    /**
     * Releases the connection used by this request.
     */
    public void releaseConnection() {
        HttpEntity entity = response != null ? response.getEntity() : null;
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
     * Enables authentication for this request. Authentication could be either NTLM, Digest or Basic Authentication.
     *
     * @param authenticator	the authentication meta-data to use
     */
    public void enableAuthentication(HTTPAuthenticator authenticator) {
        requestConfig.setAuthenticationEnabled(true);

        String username = authenticator.getUsername();
        String password = authenticator.getPassword();
        String realm = authenticator.getRealm();

        String domain = authenticator.getDomain();
        String host = authenticator.getHost();
        int port = authenticator.getPort();

        Credentials creds;
        CredentialsProvider credsProvider = clientContext.getCredentialsProvider();
        if (credsProvider == null) {
            credsProvider = new BasicCredentialsProvider();
            clientContext.setCredentialsProvider(credsProvider);
        }
        if (clientContext.getAuthCache() == null) {
        	AuthCache authCache = new BasicAuthCache();
        	authCache.put(httpHost, new BasicScheme());
        	clientContext.setAuthCache(authCache);
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


    /**
     * Checks if a user defined header with the given name is allowed.
     *
     * @param name name of the header to be added to the request
     * @return <code>true</code> if a header with this name can be added,
     * 		   <code>false</code> if this header is created by this code or the HttpClient components
     */
    private boolean allowedHeader(String name) {
    	return !HTTP.CONN_DIRECTIVE.equalsIgnoreCase(name)
            && !HTTP.TRANSFER_ENCODING.equalsIgnoreCase(name)
            && !HTTP.CONTENT_ENCODING.equalsIgnoreCase(name)
            && !HTTP.DATE_HEADER.equalsIgnoreCase(name)
            && !HTTP.CONTENT_TYPE.equalsIgnoreCase(name)
            && !HTTP.CONTENT_LEN.equalsIgnoreCase(name);
    }

    /**
     * Helper method to convert an array of Header objects to a map.
     *
     * @param headers	the array of headers represented by {@link Header} objects
     * @return a map of header names to header values. The header names are converted to lower-case.
     */
    private Map<String, String> convertHeaders(Header[] headers) {
		Map<String, String> result = new HashMap<>();
		for (Header header : headers) {
			result.put(header.getName().toLowerCase(), header.getValue());
		}
		return result;
	}
}

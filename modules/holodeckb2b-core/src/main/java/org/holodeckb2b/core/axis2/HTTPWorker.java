/*
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
import java.io.OutputStream;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.dispatchers.RequestURIBasedDispatcher;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.transport.http.server.AxisHttpRequest;
import org.apache.axis2.transport.http.server.AxisHttpResponse;
import org.apache.axis2.transport.http.server.HttpUtils;
import org.apache.axis2.transport.http.server.Worker;
import org.apache.axis2.transport.http.util.RESTUtil;
import org.apache.commons.fileupload.util.Streams;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EncodingUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;

/**
 * Is an Axis2 {@link Worker} implementation to handle a single HTTP request. If the HTTP POST method is used it will 
 * first search the Service that handles request for the given URL. If a Service is found and has a "hb2b:builder"
 * parameter that class will be used to prepare the {@link MessageContext} and/or {@link IMessageProcessingContext}. If 
 * no builder is specified the default Axis2 process is followed and the message content is processed by the Builder 
 * applicable to the Content-Type of the request is used.   
 * <p>This class is based on the default Axis2 implementation but removes all functionality not needed by Holodeck B2B,
 * so as WSDL or XSD retrieval. It also only supports the GET and POST methods. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class HTTPWorker implements Worker {
	private static final Logger log = LogManager.getLogger(HTTPWorker.class);
	
	private static final String STATIC_HTML = 
			"<html>\n" + 
			"	<head>\n" + 
			"		<title>Holodeck B2B</title>\n" + 
			"	</head>\n" + 
			"	<body>\n" + 
			"		<center>\n" + 
			"		<a href=\"http://www.holodeck-b2b.org\">" +	
			"		<img style=\"padding: 20px\" src=\"logo.png\">" +
			"		</a>\n" +
			"		</center>\n" + 
			"	</body>\n" + 
			"</html>";
	
	/**
	 * The configuration of the http transport as specified in the Holodeck B2B configuration file
	 */
	private TransportInDescription	httpConfiguration;
	
	/**
	 * Creates a new worker that uses the given http transport configuration.
	 * 
	 * @param httpConfiguration		the http configuration
	 */
	public HTTPWorker(TransportInDescription httpConfiguration) {
		this.httpConfiguration = httpConfiguration;
	}

	@Override
    public void service(
            final AxisHttpRequest request,
            final AxisHttpResponse response,
            final MessageContext msgContext) throws HttpException, IOException {
        
		ConfigurationContext configurationContext = msgContext.getConfigurationContext();
        final String servicePath = configurationContext.getServiceContextPath();
        final String contextPath = (servicePath.startsWith("/") ? servicePath : "/" + servicePath);

        String url = request.getRequestURI();
        String method = request.getMethod();
        String contentType = request.getContentType();
        String soapAction = HttpUtils.getSoapAction(request);

        InvocationResponse pi = InvocationResponse.CONTINUE;
        
        log.trace("Handling request for URL: {}", url);
        // First handle non service URLs to display a HB2B landing page
        if (method.equals(HTTPConstants.HEADER_GET)) {
        	if (url.equals(servicePath + "/logo.png")) {
        		log.trace("Handling GET request for logo");
                response.setStatus(HttpStatus.SC_OK);
                response.setContentType("image/png");                
                try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("logo.png")) {
                	Streams.copy(is, response.getOutputStream(), false);
                } catch (IOException e) {
                	log.warn("Could not send logo to client, probably connection was closed already");
                	throw new HttpException();
                }
                return;
            }
            if (!url.startsWith(contextPath)) {
            	log.trace("Request for unknown URL");
                response.setStatus(HttpStatus.SC_NOT_FOUND);                
                return;
            }
            if (url.endsWith(contextPath) || url.endsWith(contextPath + "/")) {
            	log.trace("Handling GET request for gateway home page");
                response.setStatus(HttpStatus.SC_OK);
                response.setContentType("text/html");
                OutputStream out = response.getOutputStream();
                try {
					out.write(EncodingUtils.getBytes(STATIC_HTML, HTTP.ISO_8859_1));
				} catch (IOException e) {
					log.warn("Could not send landing page HTML to client, probably connection was closed already");
                	throw new HttpException();
				}
                return;
            } 
        }
        
        log.trace("Find Service for request URL: " + url);
		try {
			RequestURIBasedDispatcher requestDispatcher = new RequestURIBasedDispatcher();		
			requestDispatcher.invoke(msgContext);
		} catch (AxisFault notFound) {}        
        
		final AxisService axisService = msgContext.getAxisService();
		if (axisService == null) {
			log.warn("No service configured for Request");
			response.setStatus(HttpStatus.SC_NOT_FOUND);
			return;
		} 
		
		try {			
			prepareMessageContext(msgContext, url, contentType, request, response);
			if (method.equals(HTTPConstants.HEADER_GET)) {
				log.debug("GET request for service {}", axisService.getName());
	            int index = !Utils.isNullOrEmpty(contentType) ? contentType.indexOf(';') : 0;
	            if (index > 0)
	                contentType = contentType.substring(0, index);	        
	        	// deal with GET request
	            pi = RESTUtil.processURLRequest(
	                    msgContext, 
	                    response.getOutputStream(), 
	                    contentType);
			} else if (method.equals(HTTPConstants.HEADER_POST)) {
				log.debug("POST request for service {}", axisService.getName());				
				
				final Builder msgBuilder = Axis2Utils.getBuilderFromService(axisService);				
				if (msgBuilder != null) {
					log.debug("Using " + msgBuilder.getClass().getSimpleName() 
																		+ " Builder to prepare messaging processing");					
					InputStream is;
					try {
						log.trace("Handle possible HTTP GZip transfer-encoding");
						is = HTTPTransportUtils.handleGZip(msgContext, request.getInputStream());
					} catch (IOException httpDecompressionError) {
						log.error("An error occured while processing the GZIP encoded HTTP entity body: " 
									+ httpDecompressionError.getMessage());
						throw new AxisFault("Error processing the GZIP encoded HTTP entity body");
					}
					// The builder SHOULD return a SOAP info-set, but for safety we do an extra check
					msgContext.setEnvelope(TransportUtils.createSOAPEnvelope(msgBuilder.processDocument(is, 
																							contentType, msgContext)));					
					pi = AxisEngine.receive(msgContext);
				} else {
					log.trace("No specific Builder specified, use default Axis2 process");
	        		// deal with POST request		            	            
		            if (HTTPTransportUtils.isRESTRequest(contentType)) {
		            	log.debug("Using REST message builder");
		                pi = RESTUtil.processXMLRequest(
		                        msgContext, 
		                        request.getInputStream(),
		                        response.getOutputStream(), 
		                        contentType);
		            } else {
		            	log.debug("Using SOAP message builder");
		            	final String ip = (String)msgContext.getProperty(MessageContext.TRANSPORT_ADDR);
		                final String requestURL = (!Utils.isNullOrEmpty(ip) ? ip : "") + url;
		                pi = HTTPTransportUtils.processHTTPPostRequest(
		                        msgContext, 
		                        request.getInputStream(),
		                        response.getOutputStream(),
		                        contentType, 
		                        soapAction, 
		                        requestURL);
		            }
				}				
			} else {
	            throw new MethodNotSupportedException(method + " method not supported");
	        }
        } catch (AxisFault f) {
        	log.error("An error occurred while processing the request. Error message: " + f.getMessage());
        	throw f;
		}
		               
        Boolean holdResponse = (Boolean) msgContext.getProperty(RequestResponseTransport.HOLD_RESPONSE);
        if (pi.equals(InvocationResponse.SUSPEND) ||
                (holdResponse != null && Boolean.TRUE.equals(holdResponse))) {
            try {
                ((RequestResponseTransport) msgContext
                        .getProperty(RequestResponseTransport.TRANSPORT_CONTROL)).awaitResponse();
            }
            catch (InterruptedException | AxisFault e) {
                throw new HttpException("We were interrupted, so this may not function correctly:" +
                        e.getMessage());
            }
        }
        
        // Finalize response
        RequestResponseTransport requestResponseTransportControl =
            (RequestResponseTransport) msgContext.
            getProperty(RequestResponseTransport.TRANSPORT_CONTROL);

        if (TransportUtils.isResponseWritten(msgContext) ||
            ((requestResponseTransportControl != null) &&
             requestResponseTransportControl.getStatus().equals(
                RequestResponseTransport.RequestResponseTransportStatus.SIGNALLED))) {
            // The response is written or signalled.  The current status is used (probably SC_OK).
        } else {
        	/* Although there was no response entity body there may be specific HTTP headers to set on the response.
        	 * These should be set as MessageContext property named HTTPConstants.RESPONSE_HEADERS with a 
        	 * Map<String, String> as value. 
        	 */
        	Object responseHdrs = msgContext.getProperty(HTTPConstants.RESPONSE_HEADERS);
        	if (responseHdrs != null && (responseHdrs instanceof Map)) 
        		((Map<String, String>) responseHdrs).forEach((n, v) -> response.addHeader(n, v));        		
        	
            // Mark the status as accepted, unless already set by service
        	Object status = msgContext.getProperty(HTTPConstants.MC_HTTP_STATUS_CODE);
        	if (status != null && status instanceof Integer) {
        		response.setStatus(((Integer) status).intValue());
        	} else
        		response.setStatus(HttpStatus.SC_ACCEPTED);
        }
    }

	/**
	 * Prepares the Axis2 Message Context by setting some generic properties that apply to all messages.
	 * 
	 * @param msgContext	
	 * @param request		
	 * @param response		
	 * @throws AxisFault 
	 */
	private void prepareMessageContext(final MessageContext msgContext, final String uriPath, final String contentType,
									   final AxisHttpRequest request, final AxisHttpResponse response) 
			throws AxisFault {
		
		msgContext.setTransportIn(httpConfiguration);
		msgContext.setIncomingTransportName(httpConfiguration.getName());
        final String ip = (String)msgContext.getProperty(MessageContext.TRANSPORT_ADDR);
        final String requestURL = (!Utils.isNullOrEmpty(ip) ? ip : "") + uriPath; 		
        msgContext.setTo(new EndpointReference(requestURL));
        msgContext.setServerSide(true);

        // get the type of char encoding
        String charSetEnc = BuilderUtil.getCharSetEncoding(contentType);
        if (Utils.isNullOrEmpty(charSetEnc))
            charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;       
        msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEnc);        
        msgContext.setProperty(MessageContext.TRANSPORT_OUT, response.getOutputStream());
        MessageProcessingContext.getFromMessageContext(msgContext);
	}
}

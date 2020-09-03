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

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.transport.http.server.SimpleHttpServer;

/**
 * Is an Axis2 {@link TransportListener} implementation that will create a stand alone HTTP listener that will use the 
 * {@link Builder} implementation specified by the service configured at the requested URL. The Builder to use can be
 * specified in the <i>Sevice</i> parameter "hb2b:builder". If no builder is specified the default Axis2 Builder 
 * applicable to the Content-Type of the request is used.   
 * <p>To implement this functionality this listener uses the HTTP sever built into Axis2 but initialises its with a 
 * custom {@link HTTPFactory} to create workers that provide the required functionality. 
 * <p>As this transport listener replaces the default Axis2 listener it uses the same parameters, the main one being
 * the port parameter where the message should be received. If that parameter is not provided the default port 8080
 * is used. 
 *
 * @author Sander Fieten (sander at chasquis-consulting.com)
 * @since 5.0.0
 */
public class HTTPListener implements TransportListener {
	/**
	 * The Axis2 http server
	 */
	private SimpleHttpServer embedded = null;
	/**
	 * The Axis2 configuration of this instance
	 */
    private ConfigurationContext configurationContext;
    /**
     * The transport configuration for this listener
     */
    private TransportInDescription	transportConfig;
    /**
     * The factory for HTTP workers
     */
    private HTTPFactory httpFactory;

	@Override
	public void init(ConfigurationContext axisConf, TransportInDescription transprtIn) throws AxisFault {
        this.configurationContext = axisConf;
        this.transportConfig = transprtIn;
        if (httpFactory == null)
            httpFactory = new HTTPFactory(configurationContext, transprtIn);        
	}

	@Override
	public void start() throws AxisFault {
        try {
            embedded = new SimpleHttpServer(httpFactory, httpFactory.getPort());
            embedded.init();
            embedded.start();
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        }		
    }

	@Override
	public void stop() throws AxisFault {
        if (embedded != null) {
            try {
                embedded.destroy();
            } catch (Exception e) {
            }
        }
	}

	@Override
	public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        if (embedded == null) {
            throw new AxisFault("Unable to generate EPR for the transport : http");
        }
        return HTTPTransportUtils.getEPRsForService(configurationContext, transportConfig, 
        											serviceName, ip, embedded.getPort());
	}

	@Override
	public SessionContext getSessionContext(MessageContext messageContext) {
		// Session support isn't needed for Holodeck B2B 
		return null;
	}

	@Override
	public void destroy() {
		this.configurationContext = null;
	}
}

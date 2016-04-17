/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.handlers.outflow;

import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.server.AxisHttpResponse;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.ProductId;

/**
 * Is the <i>OUT_FLOW</i> header responsible for setting the HTTP headers that provide information on the application
 * that executes or handles a request, i.e. the <code>Server</code> respectively <code>User-Agent</code> header. The 
 * value of these header is the string constructed from the information provided in the {@link ProductId} interface.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.0.1
 */
public class HTTPProductIdentifier extends BaseHandler {

    // The value to use in the HTTP header
    private static final String   HTTP_HDR_VALUE =  ProductId.FULL_NAME.replaceAll(" ","") 
                                                    + "/" + ProductId.MAJOR_VERSION + "." + ProductId.MINOR_VERSION;
    
    /*
     * This handler runs in all out flows to ensure that the headers are added to all requests and responses
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {
        // Get current set of options
        Options options = mc.getOptions();
        
        if (isInFlow(RESPONDER)) {
            // Acting as server, add HTTP Server header. Due to a bug in Axis we can't use the MC HTTPConstants.SERVER 
            // Option and must set a response parameter directly
            AxisHttpResponse resp = (AxisHttpResponse) mc.getProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO);
            resp.setHeader("Server", HTTP_HDR_VALUE);
        } else
            // Acting as client. add HTTP User-Agent header
            options.setProperty(HTTPConstants.USER_AGENT, HTTP_HDR_VALUE);
                    
        return InvocationResponse.CONTINUE;
    }
    
}

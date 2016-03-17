/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.multihop;

import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.holodeckb2b.ebms3.packaging.UserMessage;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Represent the <code>ebint:RoutingInput</code> WS-A EPR parameter that is used to include routing information to 
 * Signal message when exchanged using multi-hop.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class RoutingInput {
    
    /**
     * QName for the ebint:RoutingInput element
     */
    public static final QName Q_ELEMENT_NAME = new QName(MultiHopConstants.ROUTING_INPUT_NS_URI, "RoutingInput", 
                                                        "ebint");
 
    /**
     * Creates a new <code>ebint:RoutingInput</code> element in the context of the given SOAP envelope using the data 
     * form the given User Message.
     * <p>NOTE: The element is not added to the SOAP envelope by this method! This done later by the WS-A module as the
     * created element will be added as a EPR parameter.
     * 
     * @param soapEnv       The SOAP envelope the <code>RoutingInput</code> element must be added to
     * @param routinginfo   The User Message data that is the routing info
     * @return              A new <code>ebint:RoutingInput</code> element  
     */
    public static OMElement createElement(SOAPEnvelope soapEnv, IUserMessage routinginfo) {
        OMFactory f = soapEnv.getOMFactory();
        
        // Create the RoutingInput element
        OMElement routingInput = f.createOMElement(Q_ELEMENT_NAME);
        routingInput.declareNamespace(EbMSConstants.EBMS3_NS_URI, EbMSConstants.EBMS3_NS_PREFIX);
        
        // And add a regular UserMessage child to it
        OMElement usrMsgElem = UserMessage.createElement(routingInput, routinginfo);
        // This UserMessage element however has incorrect namespace, so change it to the multi-hop namespace
        usrMsgElem.setNamespace(routingInput.getNamespace());
        
        return routingInput;
    }
}

/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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

import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;

/**
 * If there is a <i>User Message unit</i> that must be sent, this handler adds 
 * the <code>eb:UserMessage</code> element to the ebMS header (which is created
 * by {@link CreateSOAPEnvelopeHandler}).
 * <p>If a user message unit is to be sent, the corresponding {@link UserMessage}
 * object MUST be included in the Axis2 <code>MessageContext</code> parameter
 * {@link MessageContextProperties#OUT_USER_MESSAGE}.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PackageUsermessageInfo extends AbstractUserMessageHandler {

    /**
     * This handler should run only in a normal <i>OUT_FLOW</i> 
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW;
    }
    
    @Override
    protected InvocationResponse doProcessing(MessageContext mc, UserMessage um) {
        
        log.debug("Get the eb:Messaging header from the message");
        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        log.debug("Add eb:UserMessage element to the existing eb:Messaging header");
        org.holodeckb2b.ebms3.packaging.UserMessage.createElement(messaging, um);
        log.debug("eb:UserMessage element succesfully added to header");

        return InvocationResponse.CONTINUE;
    }
    
}

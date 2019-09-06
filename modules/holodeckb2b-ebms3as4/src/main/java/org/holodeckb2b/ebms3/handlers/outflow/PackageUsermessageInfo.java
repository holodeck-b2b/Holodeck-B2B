/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.handlers.AbstractUserMessageHandler;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for creating the <code>eb:UserMessage</code> element in the ebMS messaging
 * header when a User Message must be sent.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PackageUsermessageInfo extends AbstractUserMessageHandler {

    @Override
    protected InvocationResponse doProcessing(final IUserMessageEntity um, final IMessageProcessingContext procCtx, 
    										  final Logger log) {

        log.trace("Get the eb:Messaging header from the message");
        final SOAPHeaderBlock messaging = Messaging.getElement(procCtx.getParentContext().getEnvelope());
        log.trace("Add eb:UserMessage element to the existing eb:Messaging header");
        UserMessageElement.createElement(messaging, um);
        log.debug("eb:UserMessage element for User Message [msgId=" + um.getMessageId() 
        			+ "] succesfully added to header");

        return InvocationResponse.CONTINUE;
    }

}

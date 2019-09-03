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
import org.apache.axis2.AxisFault;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.core.handlers.MessageProcessingContext;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.PullRequestElement;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for creating the <code>eb:PullRequest</code> element in the ebMS messaging
 * header if a pull request signal should be sent.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PackagePullRequestSignal extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final MessageProcessingContext procCtx, final Logger log) throws AxisFault {
        // First check if there is a pull request to include
        IPullRequestEntity pullReq = procCtx.getSendingPullRequest();
        		
        if (pullReq == null)
            // No pull request in this message, continue processing
            return InvocationResponse.CONTINUE;

        // There is a pull request signal to be sent, add to the message
        log.trace("Adding pull request signal to the message");
        // Get the eb:Messaging header from the message
        final SOAPHeaderBlock messaging = Messaging.getElement(procCtx.getParentContext().getEnvelope());

        log.trace("Add eb:SignalMessage element to the existing eb:Messaging header");
        PullRequestElement.createElement(messaging, pullReq);
        log.trace("eb:SignalMessage element for Pull Request succesfully added to header");

        return InvocationResponse.CONTINUE;
    }

}

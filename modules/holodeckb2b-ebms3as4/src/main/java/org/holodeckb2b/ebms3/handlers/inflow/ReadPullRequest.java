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
package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.PullRequestElement;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is an in flow handler that checks if this message contains a Pull Request, i.e. contains a <eb:PullRequest> element
 * in the ebMS header. When such a pull request message unit is found the information is read from the message and
 * stored both in the database and message processing context. The processing state of the pull request is set to 
 * {@link ProcessingState#RECEIVED}.
 * <p><b>NOTE:</b> The XML schema definition from the ebMS specification allows multiple <code>eb:SignalMessage</code>
 * elements in the ebMS header, so there could be more than one pull request in the message. The ebMS Core Specification
 * however limits the number of pull request units in the message to just one. Holodeck B2B therefor only uses the first
 * occurrence of <code>eb:SignalMessage</code> that has a <code>eb:PullRequest</code> child and ignores others.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ReadPullRequest extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log) 
    																					throws PersistenceException {
        // First get the ebMS header block, that is the eb:Messaging element
        final SOAPHeaderBlock messaging = Messaging.getElement(procCtx.getParentContext().getEnvelope());

        if (messaging != null) {
            // Check if there is a Pull Request signal
            log.trace("Check for PullRequest element to determine if message contains pull request");
            final OMElement prElement = PullRequestElement.getElement(messaging);
            if (prElement != null) {
                log.debug("PullRequest found, read information from message");
                // Read information into PullRequest object
                org.holodeckb2b.common.messagemodel.PullRequest pullRequest = PullRequestElement.readElement(prElement);
                // And store in database and message context for further processing
                log.trace("Store PullRequest in database and message context");
                procCtx.setPullRequest(HolodeckB2BCore.getStorageManager().storeIncomingMessageUnit(pullRequest));
                log.info("PullRequest [msgId=" + pullRequest.getMessageId() + "] for MPC " + pullRequest.getMPC()
                        + " received.");
            } 
        }

        return InvocationResponse.CONTINUE;
    }

}

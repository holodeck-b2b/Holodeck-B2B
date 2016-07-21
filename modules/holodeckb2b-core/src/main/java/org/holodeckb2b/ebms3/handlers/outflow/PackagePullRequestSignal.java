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
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistency.entities.PullRequest;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for creating the <code>eb:PullRequest</code> element in the ebMS messaging
 * header if a pull request signal should be sent.
 * <p>Whether a Pull Request signal must be sent is determined by the existence of the
 * {@link MessageContextProperties#OUT_PULL_REQUEST} property. It contains the <code>EntityProxy</code> for the
 * {@link PullRequest} object containing the data on the pull request signal to include.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PackagePullRequestSignal extends BaseHandler {

    /**
     * @return Indication that this handler should run in the OUT_FLOW
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws AxisFault {
        // First check if there is a pull request to include
        EntityProxy<PullRequest> pullReq = null;

        try {
            pullReq = (EntityProxy<PullRequest>) mc.getProperty(MessageContextProperties.OUT_PULL_REQUEST);
        } catch (final ClassCastException cce) {
            log.fatal("Illegal state of processing! MessageContext contained a "
                        + pullReq.getClass().getName() + " object as PullRequest!");
            return InvocationResponse.ABORT;
        }

        if (pullReq == null)
            // No pull request in this message, continue processing
            return InvocationResponse.CONTINUE;

        // There is a pull request signal to be sent, add to the message
        log.debug("Adding pull request signal to the message");

        log.debug("Get the eb:Messaging header from the message");
        final SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());

        log.debug("Add eb:SignalMessage element to the existing eb:Messaging header");
        org.holodeckb2b.ebms3.packaging.PullRequest.createElement(messaging, pullReq.entity);
        log.debug("eb:SignalMessage element succesfully added to header");

        return InvocationResponse.CONTINUE;
    }

}

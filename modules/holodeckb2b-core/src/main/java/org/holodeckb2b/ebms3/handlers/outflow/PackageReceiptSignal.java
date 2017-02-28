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

import java.util.Collection;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.ReceiptElement;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for creating the <code>eb:Receipt</code> element in the ebMS messaging
 * header if a receipt signal should be sent.
 * <p>Whether a receipt signal must be sent is determined by the existence of the {@link
 * MessageContextProperties#OUT_RECEIPTS} property.  It contains an collection of entity objects representing the
 * Receipt Signal message units to include.
 * <p>NOTE: This handler will insert more than one Receipt Signal if the collection contains multiple objects. The
 * resulting ebMS message will <b>not conform</b> to the Core Spec and AS4 profile as they do not allow bundling signal
 * message of the same type. It is the responsibility of the other handlers not to insert more than one receipt if
 * conformance is required.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PackageReceiptSignal extends BaseHandler {

    /**
     * @return Indication that this handler should run in the OUT_FLOW
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {
        // First check if there is a receipt to include
        Collection<IReceiptEntity> receipts = null;

        try {
            receipts =  (Collection<IReceiptEntity>) mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
        } catch (final ClassCastException e) {
            log.fatal("Illegal state of processing! MessageContext contained a "
                        + mc.getProperty(MessageContextProperties.OUT_RECEIPTS).getClass().getName()
                        + " object as collection of receipts!");
            throw new IllegalStateException("MessageContext contained a wrong object as collection of receipts!");
        }
        if (Utils.isNullOrEmpty(receipts))
            // No receipt in this message, continue processing
            return InvocationResponse.CONTINUE;

        // There is a receipt signal to be sent, add to the message
        log.debug("Adding receipt signal(s) to the message");

        log.debug("Get the eb:Messaging header from the message");
        final SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());

        for(final IReceiptEntity r : receipts) {
            log.debug("Make sure that all meta-data on the Receipt is loaded");
            if (!r.isLoadedCompletely()) {
                log.debug("Not all meta-data is available, load now");
                HolodeckB2BCore.getQueryManager().ensureCompletelyLoaded(r);
            }
            log.debug("Add eb:SignalMessage element to the existing eb:Messaging header");
            ReceiptElement.createElement(messaging, r);
            log.debug("eb:SignalMessage element succesfully added to header");
        }

        return InvocationResponse.CONTINUE;
    }

}

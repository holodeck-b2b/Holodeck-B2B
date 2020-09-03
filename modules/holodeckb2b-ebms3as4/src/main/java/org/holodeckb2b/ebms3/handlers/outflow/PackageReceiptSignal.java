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
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.ReceiptElement;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;

/**
 * Is the <i>OUT_FLOW</i> handler responsible for creating the <code>eb:Receipt</code> element in the ebMS messaging
 * header if a receipt signal should be sent.
 * <p>NOTE: This handler will insert more than one Receipt Signal if the collection contains multiple objects. The
 * resulting ebMS message will <b>not conform</b> to the Core Spec and AS4 profile as they do not allow bundling signal
 * message of the same type. It is the responsibility of the other handlers not to insert more than one receipt if
 * conformance is required.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PackageReceiptSignal extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log) 
    																					throws PersistenceException {
        // First check if there is a receipt to include
        Collection<IReceiptEntity> receipts = procCtx.getSendingReceipts(); 
        
		if (Utils.isNullOrEmpty(receipts))
            // No receipt in this message, continue processing
            return InvocationResponse.CONTINUE;

        // There is a receipt signal to be sent, add to the message
        log.trace("Adding receipt signal(s) to the message");
        // Get the eb:Messaging header from the message
        final SOAPHeaderBlock messaging = Messaging.getElement(procCtx.getParentContext().getEnvelope());

        for(final IReceiptEntity r : receipts) {
            log.trace("Make sure that all meta-data on the Receipt is loaded");
            if (!r.isLoadedCompletely()) {
                log.trace("Not all meta-data is available, load now");
                HolodeckB2BCore.getQueryManager().ensureCompletelyLoaded(r);
            }
            log.trace("Add eb:SignalMessage element to the existing eb:Messaging header");
            ReceiptElement.createElement(messaging, r);
            log.debug("eb:SignalMessage element for Receipt [msgId=" + r.getMessageId() 
            			+ "] succesfully added to header");
        }
        return InvocationResponse.CONTINUE;
    }

}

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

import java.util.Iterator;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.ReceiptElement;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the handler that checks if this message contains one or more Receipt signals, i.e. contains one or more
 * <code>eb:Receipt</code> elements in the ebMS header. When such signal message units are found the information is read
 * from the message into a array of {@link ReceiptElement} objects and stored in both database and message context (under key
 * {@link MessageContextProperties#IN_RECEIPTS}). The processing state of the new receipts will be set to {@link
 * ProcessingState#RECEIVED}.
 * <p><b>NOTE: </b>This handler will process all receipt signals that are in the message although the ebMS V3 Core
 * Specification does not allow more than one.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ReadReceipt extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {
        // First get the ebMS header block, that is the eb:Messaging element
        final SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());

        if (messaging != null) {
            // Check if there are Receipt signals
            log.trace("Check for Receipt elements to determine if message contains one or more receipts");
            final Iterator<OMElement> rcpts = ReceiptElement.getElements(messaging);

            if (!Utils.isNullOrEmpty(rcpts)) {
                log.debug("Receipt(s) found, read information from message");
                while (rcpts.hasNext()) {
                    final OMElement rcptElem = rcpts.next();
                    // Read information into Receipt object
                    Receipt receipt = ReceiptElement.readElement(rcptElem);

                    // And store in database and message context for further processing
                    log.trace("Store Receipt in database");
                    MessageContextUtils.addRcvdReceipt(mc, (IReceiptEntity) HolodeckB2BCore.getStorageManager()
                                                                                  .storeIncomingMessageUnit(receipt));
                    log.info("Receipt [msgId=" + receipt.getMessageId() + "] received for message with id:"
                             + receipt.getRefToMessageId());
                }
            } 
        }

        return InvocationResponse.CONTINUE;
    }
}

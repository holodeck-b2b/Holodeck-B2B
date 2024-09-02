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
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.ebms3.packaging.ErrorSignalElement;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.providers.StorageException;


/**
 * Is the handler that checks if this message contains one or more Error signals, i.e. the ebMS header contains one or
 * more <code>eb:SignalMessage</code> elements that have a <code>eb:Error</code> child. When such signal message units
 * are found the information is read from the message into a array of {@link ErrorMessage} objects and stored in the
 * database and in the message processing context. Its processing state will be set to {@link ProcessingState#RECEIVED}.
 * <p><b>NOTE: </b>This handler will process all error signals that are in the message although the ebMS Core
 * Specification does not allow more than one.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ReadError extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log)
    																					throws StorageException {
        // First get the ebMS header block, that is the eb:Messaging element
        final SOAPHeaderBlock messaging = Messaging.getElement(procCtx.getParentContext().getEnvelope());

        if (messaging != null) {
            // Check if there are Error signals
            log.trace("Check for Error elements to determine if message contains one or more errors");
            final Iterator<OMElement> errorSigs = ErrorSignalElement.getElements(messaging);

            if (!Utils.isNullOrEmpty(errorSigs)) {
                log.debug("Error Signal(s) found, read information from message");

                while (errorSigs.hasNext()) {
                    final OMElement errElem = errorSigs.next();
                    // Read information into ErrorMessage object
                    ErrorMessage errorSignal = ErrorSignalElement.readElement(errElem);
                    log.debug("Succesfully read Error message meta data from header. Msg-id="
                            + errorSignal.getMessageId());
                    // And store in database and message context for further processing
                    log.trace("Store Error Signal in database and message context");
                    procCtx.addReceivedError(HolodeckB2BCore.getStorageManager().storeReceivedMessageUnit(errorSignal));
                    log.info("Error signal with msgId " + errorSignal.getMessageId() + " succesfully read");
                }
            }
        }

        return InvocationResponse.CONTINUE;
    }
}


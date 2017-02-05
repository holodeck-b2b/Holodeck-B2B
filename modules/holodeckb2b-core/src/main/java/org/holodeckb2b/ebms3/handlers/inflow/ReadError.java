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
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.packaging.ErrorSignal;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.persistency.entities.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;

/**
 * Is the handler that checks if this message contains one or more Error signals, i.e. the ebMS header contains one or
 * more <code>eb:SignalMessage</code> elements that have a <code>eb:Error</code> child. When such signal message units
 * are found the information is read from the message into a array of {@link ErrorMessage} objects and stored in the
 * database and in the message context (under key {@link MessageContextProperties#IN_ERRORS}). Its processing state will
 * be set to {@link ProcessingStates#RECEIVED}.
 * <p><b>NOTE: </b>This handler will process all error signals that are in the message although the ebMS Core
 * Specification does not allow more than one.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ReadError extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws DatabaseException {
        // First get the ebMS header block, that is the eb:Messaging element
        final SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());

        if (messaging != null) {
            // Check if there are Error signals
            log.debug("Check for Error elements to determine if message contains one or more errors");
            final Iterator<OMElement> errorSigs = ErrorSignal.getElements(messaging);

            if (!Utils.isNullOrEmpty(errorSigs)) {
                log.debug("Error Signal(s) found, read information from message");

                while (errorSigs.hasNext()) {
                    final OMElement errElem = errorSigs.next();
                    // Read information into ErrorMessage object
                    org.holodeckb2b.ebms3.persistency.entities.ErrorMessage errorSignal =
                                                                                      ErrorSignal.readElement(errElem);
                    log.info("Succesfully read Error message meta data from header. Msg-id="
                            + errorSignal.getMessageId());
                    // And store in database and message context for further processing
                    log.debug("Store Error Signal in database");
                    final EntityProxy<ErrorMessage> errSigProxy = MessageUnitDAO.storeReceivedMessageUnit(errorSignal);
                    // Add to message context for further processing
                    MessageContextUtils.addRcvdError(mc, errSigProxy);
                    log.debug("Error signal with msgId " + errorSignal.getMessageId() + " succesfully read");
                }
            } else
                log.debug("ebMS message does not contain Error(s)");
        } else {
            log.debug("Not an ebMS message, nothing to do.");
        }

        return InvocationResponse.CONTINUE;
    }
}


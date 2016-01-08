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
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.InvalidHeader;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.PackagingException;
import org.holodeckb2b.ebms3.packaging.Receipt;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.axis2.MessageContextUtils;

/**
 * Is the handler that checks if this message contains one or more Receipt signals, i.e. contains one or more 
 * <code>eb:Receipt</code> elements in the ebMS header. When such signal message units are found the information is read 
 * from the message into a array of {@link Receipt} objects and stored in both database and message context (under key 
 * {@link MessageContextProperties#IN_RECEIPTS}). The processing state of the new receipts will be set to {@link 
 * ProcessingStates#RECEIVED}.
 * <p>The Receipt signal only has value if it refers to another message unit, therefor it must contain a <code> 
 * eb:RefToMessageId</code> element in the ebMS message header (see section 5.2.3.3 of the ebMS V3 Core Specification). 
 * If missing a <i>InvalidHeader</i> error is generated and processing of the Receipt is stopped.
 * <p><b>NOTE: </b>This handler will process all receipt signals that are in the message although the ebMS Core 
 * Specification does not allow more than one.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ReadReceipt extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        // First get the ebMS header block, that is the eb:Messaging element
        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        
        if (messaging != null) {
            // Check if there are Receipt signals
            log.debug("Check for Receipt elements to determine if message contains one or more receipts");
            Iterator<OMElement> rcpts = Receipt.getElements(messaging);
            
            if (!Utils.isNullOrEmpty(rcpts)) {
                log.debug("Receipt(s) found, read information from message");
                
                while (rcpts.hasNext()) {
                    OMElement rcptElem = rcpts.next();
                    org.holodeckb2b.ebms3.persistency.entities.Receipt receipt = null;
                    try {
                        // Read information into Receipt object
                        receipt = Receipt.readElement(rcptElem);

                        // And store in database and message context for further processing
                        log.debug("Store Receipt in database");
                        MessageUnitDAO.storeReceivedMessageUnit(receipt);
                        
                        String refToMsgId = receipt.getRefToMessageId();
                        if (Utils.isNullOrEmpty(refToMsgId)) {
                            log.info("Received receipt [msgId=" + receipt.getMessageId() 
                                                            + "] does not contain reference");
                            // The receipt can not be processed if we don't know for which message it is intended!
                            receipt = MessageUnitDAO.setFailed(receipt);
                            // Create error and add to message context
                            createInvalidHeaderError(mc, rcptElem, "Missing required refToMessageId");
                        } else {
                            MessageContextUtils.addRcvdReceipt(mc, receipt);
                            log.info("Receipt [msgId=" + receipt.getMessageId() + "] received for message with id:" 
                                            + receipt.getRefToMessageId());
                        }
                    } catch (PackagingException ex) {
                        log.error("Received receipt could not read from message! Details: " + ex.getMessage());
                        // Create error and add to message context
                        createInvalidHeaderError(mc, rcptElem, ex.getMessage());
                    } 
                }
            } else
                log.debug("ebMS message does not contain Receipt(s)");
        } else {
            log.debug("Not an ebMS message, nothing to do.");
        }
        
        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Helper to create an <i>InvalidHeader</i> ebMS error and add it to the message context for further processing.
     * 
     * @param mc        The current message context
     * @param element   The element that was found to be invalid
     * @param msg       The message to set as the <i>error detail</i>
     */
    private void createInvalidHeaderError(MessageContext mc, OMElement element, String msg) {
        InvalidHeader   invalidHdrError = new InvalidHeader();
        invalidHdrError.setShortDescription(msg);
        invalidHdrError.setErrorDetail("Source of header containing the error:" + element.toString());
        MessageContextUtils.addGeneratedError(mc, invalidHdrError);                    
    }
}

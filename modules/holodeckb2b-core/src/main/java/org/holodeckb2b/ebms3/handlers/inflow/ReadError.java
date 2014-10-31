/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.InvalidHeader;
import org.holodeckb2b.ebms3.packaging.ErrorSignal;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.PackagingException;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.util.MessageContextUtils;

/**
 * Is the handler that checks if this message contains one or more Error signals, i.e. the ebMS header contains one or 
 * more <code>eb:SignalMessage</code> elements that have a <code>eb:Error</code> child. When such signal message units 
 * are found the information is read from the message into a array of {@link ErrorMessage} objects and stored in both 
 * database and message context (under key {@link MessageContextProperties#IN_ERRORS}). The processing state of the 
 * errors will be set to {@link ProcessingStates#RECEIVED}.
 * <p>All received error signals will also be logged to a separate log (<code>org.holodeckb2b.msgproc.errors.received</code>)
 * to enable easy monitoring. 
 * <p><b>NOTE: </b>This handler will process all error signals that are in the message although the ebMS Core 
 * Specification does not allow more than one.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ReadError extends BaseHandler {

    /**
     * Errors will always be logged to a special error log. Using the logging
     * configuration users can decide if this logging should be enabled and
     * how errors should be logged.
     */
    private Log     errorLog = LogFactory.getLog("org.holodeckb2b.msgproc.errors.received");
    
    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) {
        // First get the ebMS header block, that is the eb:Messaging element
        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        
        if (messaging != null) {
            // Check if there are Error signals
            log.debug("Check for Error elements to determine if message contains one or more errors");
            Iterator errorSigs = ErrorSignal.getElements(messaging);
            
            if (errorSigs != null) {
                log.debug("Error(s) found, read information from message");
                
                while (errorSigs.hasNext()) {
                    OMElement errElem = (OMElement) errorSigs.next();
                    org.holodeckb2b.ebms3.persistent.message.ErrorMessage errorSignal = null;
                    try {
                        // Read information into ErrorMessage object
                        errorSignal = ErrorSignal.readElement(errElem);    
                        // And store in database and message context for further processing
                        log.debug("Store Error Signal in database");
                        MessageUnitDAO.storeReceivedMessageUnit(errorSignal);
                        // And add to message context for further processing
                        MessageContextUtils.addRcvdError(mc, errorSignal);
                    } catch (PackagingException ex) {
                        log.error("Received error could not read from message! Details: " + ex.getMessage());
                        // Create errorSignal and add to message context
                        InvalidHeader   invalidHdrError = new InvalidHeader();
                        invalidHdrError.setErrorDetail(ex.getMessage()
                                                    + "\nSource of header containing the error:" + errElem.toString());
                        MessageContextUtils.addGeneratedError(mc, invalidHdrError);                       
                    } catch (DatabaseException ex) {
                        // Ai, something went wrong when storing the errorSignal
                        log.error("An error occurred when saving the Error to the database. Details: " + ex.getMessage());
                        // Although the errorSignal could not be stored, other processing may finish succesfully
                    }
                }
            } else
                log.debug("ebMS message does not contain Error(s)");
        } else {
            log.debug("Not an ebMS message, nothing to do.");
        }
        
        return InvocationResponse.CONTINUE;
    }
}


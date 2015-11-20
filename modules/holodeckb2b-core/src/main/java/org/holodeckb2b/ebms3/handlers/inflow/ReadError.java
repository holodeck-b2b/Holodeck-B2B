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
import org.holodeckb2b.axis2.MessageContextUtils;
import org.holodeckb2b.common.config.Config;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.IEbmsError;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.InvalidHeader;
import org.holodeckb2b.ebms3.errors.ValueInconsistent;
import org.holodeckb2b.ebms3.packaging.ErrorSignal;
import org.holodeckb2b.ebms3.packaging.Messaging;
import org.holodeckb2b.ebms3.packaging.PackagingException;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;

/**
 * Is the handler that checks if this message contains one or more Error signals, i.e. the ebMS header contains one or 
 * more <code>eb:SignalMessage</code> elements that have a <code>eb:Error</code> child. When such signal message units 
 * are found the information is read from the message into a array of {@link ErrorMessage} objects and stored in the 
 * database. Its processing state will be set to {@link ProcessingStates#RECEIVED}.
 * <p>Each Error signal is also checked for consistency of the referenced message. The consistency is defined in the 
 * ebMS Core Specification (section 6.3):<br>
 * <i>"If <code>eb:RefToMessageId</code> is present as a child of <code>eb:SignalMessage/eb:MessageInfo</code>, then 
 * every <code>eb:Error</code> element MUST be related to the ebMS message (message-in-error) identified by 
 * <code>eb:RefToMessageId</code>.<br>
 * If the element <code>eb:SignalMessage/eb:MessageInfo</code> does not contain <code>eb:RefToMessageId</code>, then
 * the <code>eb:Error</code> element(s) MUST NOT be related to a particular ebMS message."</i>
 * <p>Holodeck B2B allows a bit more flexibility as it allows for an empty or absent <code>eb:RefToMessageId</code> 
 * element while the <code>eb:Error</code> element(s) refer to a message unit. However it does require that all
 * <code>eb:Error</code> elements refer the same message unit, i.e. contain the same value for the <code> 
 * refToMessageInError</code> attribute.
 * <br>If you want to use the more strict check as defined in the specification you can turn this on by setting the
 * value of the <i>StrictErrorReferencesCheck</i> module parameter to <i>"true"</i>.
 * <p>If the references are consistent the Error signal will be added to the message context (under key 
 * {@link MessageContextProperties#IN_ERRORS}). If the references are not consistent the processing state of the Error 
 * is set to {@link ProcessingStates#FAILURE} and it is not added to the message context.
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
    protected InvocationResponse doProcessing(MessageContext mc) throws DatabaseException {
        // First get the ebMS header block, that is the eb:Messaging element
        SOAPHeaderBlock messaging = Messaging.getElement(mc.getEnvelope());
        
        if (messaging != null) {
            // Check if there are Error signals
            log.debug("Check for Error elements to determine if message contains one or more errors");
            Iterator<OMElement> errorSigs = ErrorSignal.getElements(messaging);
            
            if (errorSigs != null) {
                log.debug("Error Signal(s) found, read information from message");
                
                while (errorSigs.hasNext()) {
                    OMElement errElem = errorSigs.next();
                    org.holodeckb2b.ebms3.persistent.message.ErrorMessage errorSignal = null;
                    try {
                        // Read information into ErrorMessage object
                        errorSignal = ErrorSignal.readElement(errElem);    
                        // And store in database and message context for further processing
                        log.debug("Store Error Signal in database");
                        MessageUnitDAO.storeReceivedMessageUnit(errorSignal);
                        // Add to message context for further processing
                        MessageContextUtils.addRcvdError(mc, errorSignal);
                        log.debug("Check consistency of references");
                        if (!checkRefConsistency(errorSignal)) {
                            log.warn("The references containd in Error signal [msgId=" + errorSignal.getMessageId()
                                       + "] are not consistent!");
                            // Create error and add to context
                            ValueInconsistent   viError = new ValueInconsistent();
                            viError.setErrorDetail("Error contains inconsistent references");
                            viError.setRefToMessageInError(errorSignal.getMessageId());
                            MessageContextUtils.addGeneratedError(mc, viError);  
                            MessageUnitDAO.setFailed(errorSignal);
                        } else {
                            log.debug("References are consistent");
                        }
                    } catch (PackagingException ex) {
                        log.error("Received error could not read from message! Details: " + ex.getMessage());
                        // Create errorSignal and add to message context
                        InvalidHeader   invalidHdrError = new InvalidHeader();
                        invalidHdrError.setErrorDetail(ex.getMessage()
                                                    + "\nSource of header containing the error:" + errElem.toString());
                        MessageContextUtils.addGeneratedError(mc, invalidHdrError);                       
                    } 
                }
            } else
                log.debug("ebMS message does not contain Error(s)");
        } else {
            log.debug("Not an ebMS message, nothing to do.");
        }
        
        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Checks if the references contained in the Error signal are consistent.
     * 
     * @param errSignal     The Error signal to check
     * @return              <code>true</code> if the references are consistent, <code>false</code> if not.
     */
    private boolean checkRefConsistency(final ErrorMessage errSignal) {
        // First get RefToMessageId from header
        String refToMessageId = errSignal.getRefToMessageId();
        
        // Then check individual errors, if the RefToMessageId element from header:
        // - contained a id: all individual ids should be the same to the signal level id or null;
        // - is null: all individual ids should equal if non-strict checking, or should also be null if strict
        boolean consistent = true; 
        Iterator<IEbmsError> it = errSignal.getErrors().iterator();
        String errorRefToMsgId = it.next().getRefToMessageInError(); 
        if (refToMessageId == null && !Config.useStrictErrorRefCheck()) {
            // Signal level ref == null => all individual refs should be same, take first as leading
            refToMessageId = errorRefToMsgId;
        } 
        
        while (it.hasNext() && consistent) {
            consistent = (errorRefToMsgId == refToMessageId 
                        || (errorRefToMsgId != null && errorRefToMsgId.equals(refToMessageId)));
            errorRefToMsgId = it.next().getRefToMessageInError();
        }
        
        if (!consistent) {
            log.warn("Error signal [msgId=" + errSignal.getMessageId() +"] contains inconsistent references!");
        }

        return consistent;
    }
}


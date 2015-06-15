/*
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
package org.holodeckb2b.ebms3.util;

import java.util.Collections;
import java.util.Date;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.IEbmsError;
import org.holodeckb2b.common.util.MessageIdGenerator;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.OtherContentError;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.EbmsError;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CatchAxisFault extends BaseHandler {
    
    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW | OUT_FLOW | RESPONDER;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws AxisFault {
        return InvocationResponse.CONTINUE;
    }
    
    @Override
    public void doFlowComplete(MessageContext mc) {
        
        if (mc.getFailureReason() != null) {
            log.debug("A Fault was raised during processing. Reported cause= " + mc.getFailureReason().getMessage());
            
            // Create an ebMS Error to indicate the problem. For security reasons we only provide a general description
            OtherContentError   otherError = new OtherContentError();
            otherError.setErrorDetail("An internal error occurred while processing the message.");
            otherError.setSeverity(IEbmsError.Severity.FAILURE);
            
            // Also set this general description on the fault
            mc.setFailureReason(new Exception("An internal error occurred"));
            
            ErrorMessage newErrorMU = null;
            try {
                log.debug("Create the Error signal message");
                newErrorMU = MessageUnitDAO.createOutgoingErrorMessageUnit(
                                                                    Collections.singletonList((EbmsError) otherError), 
                                                                    null, null, true);
            } catch (DatabaseException dbe) {
                // (Still) a problem with the database, create the Error signal message without storing it 
                log.fatal("Could not store error signal message in database!");
                newErrorMU = new ErrorMessage();
                newErrorMU.addError(otherError);
                // Generate a message id and timestamp for the new error message unit
                newErrorMU.setMessageId(MessageIdGenerator.createMessageId());
                newErrorMU.setTimestamp(new Date());
            }
            
            log.debug("Created a new Error signal message");                
            mc.setProperty(MessageContextProperties.OUT_ERROR_SIGNALS, Collections.singletonList(newErrorMU));
            log.debug("Set the Error signal as the only ebMS message to return");
            
            log.debug("Clear other possible return messages");
            mc.removeProperty(MessageContextProperties.OUT_RECEIPTS);            
        } else 
            log.debug("Not an ebMS message or no Fault raised, nothing to do");
    }
}

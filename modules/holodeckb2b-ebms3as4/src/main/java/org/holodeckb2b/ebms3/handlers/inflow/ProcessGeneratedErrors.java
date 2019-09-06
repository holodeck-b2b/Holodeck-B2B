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

import java.util.Collection;
import java.util.Map;

import org.apache.axis2.description.HandlerDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;

/**
 * Is the <i>in flow</i> handler that collects all ebMS errors generated during the processing of the received message
 * unit(s). The errors are grouped by the message unit they apply to. For each group a new <i>Error Signal</i> (a {@link 
 * ErrorMessage} object) is created and saved in both the database and message context. 
 * <p>This handler will log all created <i>Error Signals</i> in a special log (<i>"org.holodeckb2b.msgproc.errors.generated."
 * + «message protocol»</i>) but it is up to a next handler in the flow to decide if and how the Error Signals should be 
 * reported to the Sender of the message unit in error.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0 This handler only bundles the individually generated Errors into Error Signals. If and how to report
 *  			the signals is now done in {@link DetermineErrorReporting}
 */
public class ProcessGeneratedErrors extends AbstractBaseHandler {
	
    /**
     * Errors will always be logged to a special error log. Using the logging
     * configuration users can decide if this logging should be enabled and
     * how errors should be logged.
     */
    private Log     errorLog;
    
    @Override
	public void init(HandlerDescription handlerdesc) {
    	super.init(handlerdesc);
    	errorLog = LogFactory.getLog("org.holodeckb2b.msgproc.errors.generated." + handledMsgProtocol);
    }
    
    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log) 
    																					throws PersistenceException {
        log.debug("Check if errors were generated");
        final Map<String, Collection<IEbmsError>> errors = procCtx.getGeneratedErrors();

        if (Utils.isNullOrEmpty(errors)) {
            log.debug("No errors were generated during this in flow");
            return InvocationResponse.CONTINUE;
        }
        
        log.trace("Processing error(s) generated during this in flow");
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();      
        // Create Error signal for each bundle, i.e. each referenced message
        for(final String refToMsgId : errors.keySet()) {
        	final boolean noRef = MessageProcessingContext.UNREFD_ERRORS.equals(refToMsgId);
            log.debug("Creating new Error Signal for errors " + (noRef ? "without reference" : 
            													"referencing messsage unit with msgId=" + refToMsgId));
            ErrorMessage tErrorMessage = new ErrorMessage(errors.get(refToMsgId));
            tErrorMessage.setRefToMessageId(noRef ? null : refToMsgId);
            log.trace("Saving new Error Signal in database");
            final IErrorMessageEntity errorMessage = storageManager.storeOutGoingMessageUnit(tErrorMessage);
            // Log to the error log
            if (MessageUnitUtils.isWarning(errorMessage))
            	errorLog.warn(MessageUnitUtils.errorSignalToString(errorMessage));
        	else
        		errorLog.error(MessageUnitUtils.errorSignalToString(errorMessage));
            
            log.trace("Determine P-Mode for new Error Signal");
            if (!noRef) {
                // This collection of errors references one of the received message units.
                final IMessageUnitEntity muInError = procCtx.getReceivedMessageUnits()
                											.parallelStream()
                											.filter(mu -> refToMsgId.equals(mu.getMessageId()))
                											.findFirst()
                											.get();
                final String pmodeId = muInError.getPModeId();
                if (!Utils.isNullOrEmpty(pmodeId)) {
                    log.trace("Set P-Mode Id [" + pmodeId + "] for generated Error Message");
                    storageManager.setPModeId(errorMessage, pmodeId);
                }
            }
            // Save the new Error Message in the message context 
            procCtx.addSendingError(errorMessage);
        }
        
        return InvocationResponse.CONTINUE;
    }    
}

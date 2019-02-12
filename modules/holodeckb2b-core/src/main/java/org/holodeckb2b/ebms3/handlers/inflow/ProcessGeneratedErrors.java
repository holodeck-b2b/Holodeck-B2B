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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;

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
public class ProcessGeneratedErrors extends BaseHandler {
	
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
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {
        log.debug("Check if errors were generated");
        @SuppressWarnings("unchecked")
		final ArrayList<EbmsError>    errors = (ArrayList<EbmsError>)
                                                            mc.getProperty(MessageContextProperties.GENERATED_ERRORS);

        if (Utils.isNullOrEmpty(errors)) {
            log.debug("No errors were generated during this in flow");
            return InvocationResponse.CONTINUE;
        }
        
        log.debug(errors.size() + " error(s) were generated during this in flow");
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();      
        log.trace("Bundling errors per message unit");
        final HashMap<String, Collection<IEbmsError>> segmentedErrors = segmentErrors(errors);
        final ArrayList<IErrorMessageEntity> newSignals = new ArrayList<IErrorMessageEntity>(segmentedErrors.size());
        mc.setProperty(MessageContextProperties.GENERATED_ERROR_SIGNALS, newSignals);
        // Create Error signal for each bundle, i.e. each referenced message
        for(final String refToMsgId : segmentedErrors.keySet()) {
            log.debug("Creating new Error Signal for errors " 
            			+ (Utils.isNullOrEmpty(refToMsgId) ? "without reference" : 
            												 "referencing messsage unit with msgId=" + refToMsgId));
            ErrorMessage tErrorMessage = new ErrorMessage(segmentedErrors.get(refToMsgId));
            tErrorMessage.setRefToMessageId(refToMsgId.equals("null") ? null : refToMsgId);
            log.trace("Saving new Error Signal in database");
            final IErrorMessageEntity errorMessage = storageManager.storeOutGoingMessageUnit(tErrorMessage);
            // Log to the error log
            if (MessageUnitUtils.isWarning(errorMessage))
            	errorLog.warn(MessageUnitUtils.errorSignalToString(errorMessage));
        	else
        		errorLog.error(MessageUnitUtils.errorSignalToString(errorMessage));
            
            log.trace("Determine P-Mode for new Error Signal");
            if (!Utils.isNullOrEmpty(errorMessage.getRefToMessageId())) {
                // This collection of errors references one of the received message units.
                final IMessageUnitEntity muInError = getRefdMessageUnit(mc, refToMsgId);
                final String pmodeId = muInError.getPModeId();
                if (!Utils.isNullOrEmpty(pmodeId)) {
                    log.trace("Set P-Mode Id [" + pmodeId + "] for generated Error Message");
                    storageManager.setPModeId(errorMessage, pmodeId);
                }
            }
            // Save the new Error Message in the message context 
            newSignals.add(errorMessage);
        }
        
        return InvocationResponse.CONTINUE;
    }
    
    /**
     * Segments the given collection of errors into bundles of errors based on the referenced message unit.
     *
     * @param errors        The collection of errors to segment
     * @return              A collection of errors per messageId. Errors that do not reference another message unit are
     *                      bundled under key <i>"null"</i>
     */
    private HashMap<String, Collection<IEbmsError>> segmentErrors(final Collection<EbmsError> errors) {
        final HashMap<String, Collection<IEbmsError>> segmentedErrors = new HashMap<>();
        for(final EbmsError e : errors) {
            String  refToMsgId = e.getRefToMessageInError();
            // Check if the error is related to another message (it may be a general error)
            if (Utils.isNullOrEmpty(refToMsgId))
                refToMsgId = "null"; // general error
            Collection<IEbmsError> errForMsg = segmentedErrors.get(refToMsgId);
            if (errForMsg == null) {
                // No errors known for this referenced message unit
                errForMsg = new ArrayList<>();
                segmentedErrors.put(refToMsgId, errForMsg);
            }
            errForMsg.add(e);
        }
        return segmentedErrors;
    }  

    /**
     * Helper method to get the meta-data on the received message unit that is referenced by the error.
     *
     * @param mc            The current message context
     * @param refToMsgId    The messageId that the error refers to
     * @return  The referenced message unit
     */
    private IMessageUnitEntity getRefdMessageUnit(MessageContext mc, String refToMsgId) {
        for(IMessageUnitEntity m : MessageContextUtils.getReceivedMessageUnits(mc))
            if (m.getMessageId().equals(refToMsgId))
                return m;
        return null;
    }    
}

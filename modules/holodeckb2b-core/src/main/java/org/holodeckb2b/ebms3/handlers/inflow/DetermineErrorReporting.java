/**
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;

/**
 * Is the <i>in flow handler</i> responsible for determining if and how Error Signals generated during the processing of 
 * the received message unit(s) should be reported to the Sender of the message unit(s) in error. This depends on the 
 * P-Mode settings for error handling and some general parameters. If the Error Signal needs to be send as a response it
 * will be added to the {@link MessageContextProperties#OUT_ERRORS} message context property and the {@link 
 * MessageContextProperties#RESPONSE_REQUIRED} will be set.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public class DetermineErrorReporting extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc, Log log) throws PersistenceException {

    	log.debug("Check if error signals were generated");
        @SuppressWarnings("unchecked")
		final ArrayList<IErrorMessageEntity>  errors = (ArrayList<IErrorMessageEntity>)
                                                       mc.getProperty(MessageContextProperties.GENERATED_ERROR_SIGNALS);

        if (Utils.isNullOrEmpty(errors)) {
            log.debug("No error signals were generated");
            return InvocationResponse.CONTINUE;
        }   
        
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        log.debug(errors.size() + " error signal(s) were generated during processing");
        for(final IErrorMessageEntity error : errors) {
        	// Error Signals without P-Mode should generally be reported as response
        	if (Utils.isNullOrEmpty(error.getPModeId())) {
                /* If the Error Signal however also doesn't reference a message unit in error, the Signal can only be
                 * returned if the received message contained no other message units or if they all failed. Otherwise
                 * returning the non reference-able Signal creates ambiguity as the Sender can only set all sent
                 * message units to failed.
                 * Therefore we only sent out this error if there no message unit with a processing state different 
                 * then FAILURE
                 */ 
                if (isInFlow(RESPONSE) && 
                	 (!Utils.isNullOrEmpty(error.getRefToMessageId()) || onlyFailedMessageUnits(mc))) {
                    log.debug("Error Signal [msgId=" + error.getMessageId() + "] should be send as response");
                    MessageContextUtils.addErrorSignalToSend(mc, error);
                    mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                } else {
                    log.warn("Error Signal [msgId=" + error.getMessageId() 
                    		+ "] can not be sent because it conflicts with successful message units "
                            + " or because message in error was received as response");
                    // As we can not do anything with the error change its processing state to WARNING and then DONE
                    storageManager.setProcessingState(error, ProcessingState.WARNING, 
		                    								 	isInFlow(REQUEST) ? "No P-Mode available for sending" 
		                    								 						: "Ambigious error");
                    storageManager.setProcessingState(error, ProcessingState.DONE);
                }
            } else {
            	final IMessageUnitEntity msgInError = getRefdMessageUnit(mc, error.getRefToMessageId());
            	if (msgInError instanceof IPullRequest) {
            		log.debug("Error Signal [msgId=" + error.getMessageId() 
            					+ "] references Pull Request => send as response");
            		MessageContextUtils.addErrorSignalToSend(mc, error);
                    mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
            	} else {
	                log.debug("Get P-Mode information to determine if and how Error Signal must be reported");
	                // Errorhandling config is contained in flow
	                final IUserMessageFlow flow = HolodeckB2BCoreInterface.getPModeSet()
	                													  .get(error.getPModeId())
	                													  .getLeg(msgInError.getLeg())
	                													  .getUserMessageFlow();
                    final IErrorHandling errorHandling = (flow != null ? flow.getErrorHandlingConfiguration()
                                                                       : null);

                    /* Reporting Errors on Signals is optional, so check if messsge unit in error is a Signal and if
                     * P-Mode or when not configured the general configuration is set to report such errors. 
                     * Default is not to report errors for Signals.
                     */
                    boolean sendError = true;
                    if (msgInError instanceof IErrorMessage)
                        sendError = errorHandling != null && errorHandling.shouldReportErrorOnError() != null ?
                                    errorHandling.shouldReportErrorOnError() :
                                    HolodeckB2BCore.getConfiguration().shouldReportErrorOnError();
                    if (msgInError instanceof IReceipt)
                        sendError = errorHandling != null && errorHandling.shouldReportErrorOnReceipt() != null?
                                    errorHandling.shouldReportErrorOnReceipt() :
                                    HolodeckB2BCore.getConfiguration().shouldReportErrorOnReceipt();
                                                      
                    // Default is to send error as response when the message in error is received as a request
                    final boolean asResponse = errorHandling != null ?
                                                    errorHandling.getPattern() == ReplyPattern.RESPONSE :
                                                    isInFlow(RESPONSE);
                    /* Should this error signal be combined with a SOAP Fault? Because SOAP Faults can confuse
                     * the MSH that receives the error Holodeck B2B by default does not add the SOAP, it should
                     * be configured explicitly in the P-Mode
                     */
                    if (errorHandling != null && errorHandling.shouldAddSOAPFault() != null &&
                    										errorHandling.shouldAddSOAPFault().booleanValue()) {
                    	log.trace("A SOAP Fault should be added to the Error Signal");
                    	HolodeckB2BCore.getStorageManager().setAddSOAPFault(error, true);
                    }	
                    
                    if (sendError) {
                        log.debug("Error Signal [msgId=" + error.getMessageId() + "] should be sent"
                                    + (asResponse ?  " as a response" : " a using callback"));
                        if (asResponse) {
                            MessageContextUtils.addErrorSignalToSend(mc, error);
                            mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                        } else
                            storageManager.setProcessingState(error, ProcessingState.READY_TO_PUSH);
                    } else {
                        log.debug("Error doesn't need to be sent. Processing completed.");
                        storageManager.setProcessingState(error, ProcessingState.DONE);
                    }
            	}
            }        
        }
        return InvocationResponse.CONTINUE;
    }

    /**
     * Helper method to check whether all of the message units contained in the message have failed processing.
     *
     * @param mc        The current message context containing all info on the message units
     * @return          <code>true</code> when all received message units have failed to process,<br>
     *                  <code>false</code> if any has been successfully processed.
     */
    private boolean onlyFailedMessageUnits(final MessageContext mc) {
        boolean onlyFailed = true;
        Collection<IMessageUnitEntity> allRcvdMessageUnits = MessageContextUtils.getReceivedMessageUnits(mc);
        if (!Utils.isNullOrEmpty(allRcvdMessageUnits))
            for(final IMessageUnitEntity m : allRcvdMessageUnits)
                onlyFailed &= m.getCurrentProcessingState().getState() == ProcessingState.FAILURE;

        return onlyFailed;
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

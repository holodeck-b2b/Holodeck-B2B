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

import org.apache.commons.logging.Log;
import org.holodeckb2b.common.handler.AbstractBaseHandler;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.util.Utils;
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
import org.holodeckb2b.pmode.PModeUtils;

/**
 * Is the <i>in flow handler</i> responsible for determining if and how Error Signals generated during the processing of 
 * the received message unit(s) should be reported to the Sender of the message unit(s) in error. This depends on the 
 * P-Mode settings for error handling and some general parameters. If there is an Error Signal that needs to be send 
 * as a response the indication to send a response will be set in the message processing context.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public class DetermineErrorReporting extends AbstractBaseHandler {

    @Override
    protected InvocationResponse doProcessing(final MessageProcessingContext procCtx, final Log log) 
    																					throws PersistenceException {

    	log.debug("Check if error signals were generated");
        final Collection<IErrorMessageEntity>  errors = procCtx.getSendingErrors();

        if (Utils.isNullOrEmpty(errors)) {
            log.debug("No error signals were generated");
            return InvocationResponse.CONTINUE;
        }   
        
        StorageManager storageManager = HolodeckB2BCore.getStorageManager();
        log.debug(errors.size() + " error signal(s) were generated during processing");
        final boolean isResponseFlow = procCtx.getParentContext().isServerSide();
        final Collection<IErrorMessageEntity> nonResponseErrors = new ArrayList<>();
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
                if (isResponseFlow && (!Utils.isNullOrEmpty(error.getRefToMessageId()) 
                					  || procCtx.getReceivedMessageUnits().parallelStream()
                					  									  .allMatch(m -> m.getCurrentProcessingState()
                					  											  		  .getState() 
                					  											  		  == ProcessingState.FAILURE)
                					  									  		    )) {
                    log.debug("Error Signal [msgId=" + error.getMessageId() + "] should be send as response");
                    procCtx.setNeedsResponse(true);
                } else {
                    log.warn("Error Signal [msgId=" + error.getMessageId() 
                    		+ "] can not be sent because it conflicts with successful message units "
                            + " or because message in error was received as response");
                    // As we can not do anything with the error change its processing state to WARNING and then DONE
                    storageManager.setProcessingState(error, ProcessingState.WARNING, 
		                    								 	isResponseFlow ? "Ambigious error" : 
		                    								 					 "No P-Mode available for sending");
                    storageManager.setProcessingState(error, ProcessingState.DONE);
                    nonResponseErrors.add(error);
                }
            } else {
            	final IMessageUnitEntity msgInError = procCtx.getReceivedMessageUnits()
            												 .parallelStream()
            												 .filter(m -> m.getMessageId()
            														 	   .equals(error.getRefToMessageId()))
            												 .findFirst().get();
            	if (msgInError instanceof IPullRequest) {
            		log.debug("Error Signal [msgId=" + error.getMessageId() 
            					+ "] references Pull Request => send as response");
            		procCtx.setNeedsResponse(true);
            	} else {
	                log.debug("Get P-Mode information to determine if and how Error Signal must be reported");
	                // Errorhandling config is contained in flow
	                final IUserMessageFlow flow = PModeUtils.getLeg(msgInError).getUserMessageFlow();
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
                                                    isResponseFlow;
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
                        if (asResponse) 
                            procCtx.setNeedsResponse(true);
                        else {
                            storageManager.setProcessingState(error, ProcessingState.READY_TO_PUSH);
                            nonResponseErrors.add(error);
                        }
                    } else {
                        log.debug("Error doesn't need to be sent. Processing completed.");
                        storageManager.setProcessingState(error, ProcessingState.DONE);
                        nonResponseErrors.add(error);
                    }
            	}
            }        
        }
        // Remove all errors that will are not send as response
        errors.removeAll(nonResponseErrors);
        
        return InvocationResponse.CONTINUE;
    }
}

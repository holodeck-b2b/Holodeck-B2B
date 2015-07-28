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

package org.holodeckb2b.ebms3.handlers.inflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.config.Config;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.general.ReplyPattern;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.pmode.IErrorHandling;
import org.holodeckb2b.common.pmode.IUserMessageFlow;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.handlers.outflow.PrepareResponseMessage;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.EbmsError;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.MessageContextUtils;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the in flow handler that collects all ebMS errors generated during the processing of the received message. The
 * errors are grouped by the message unit they apply to. For each group a new error signal (a {@link ErrorMessage} 
 * object) is created and saved in the database. 
 * <p>Depending on the P-Mode settings for the error signal it is also stored in the message context to send it as a 
 * response. This can result in multiple error signals ready to be sent. The {@link PrepareResponseMessage} handler in 
 * the out flow is responsible to decide which signal(s) should be included in the response message.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ProcessGeneratedErrors extends BaseHandler {

    /**
     * Errors will always be logged to a special error log. Using the logging
     * configuration users can decide if this logging should be enabled and
     * how errors should be logged.
     */
    private Log     errorLog = LogFactory.getLog("org.holodeckb2b.msgproc.errors.generated.IN_FLOW");
            
    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws AxisFault {
        log.debug("Check if errors were generated");
        ArrayList<EbmsError>    errors = (ArrayList<EbmsError>) mc.getProperty(MessageContextProperties.GENERATED_ERRORS);
        
        if (errors == null || errors.isEmpty()) {
            log.debug("No errors were generated during this in flow, nothing to do");
        } else {
            log.debug(errors.size() + " error(s) were generated during this in flow");
            
            // First bundle the errors per message in error
            log.debug("Bundling errors per message unit");
            HashMap<String, Collection<EbmsError>> segmentedErrors = segmentErrors(errors);
            
            // Create Error signal for each bundle
            for(String refToMsgId : segmentedErrors.keySet()) {
                Collection<EbmsError> errForMsg = segmentedErrors.get(refToMsgId);
                try {
                    if (refToMsgId.equals("null")) {
                        // This collection of errors does not reference a specific message unit, should therefor be sent 
                        // as a response.
                        // If however the message contained multiple message units some can be processed succesfully and 
                        // returning an non referenceable error creates ambiguity as the sender can only set all sent 
                        // message units to failed.
                        // Therefor we only sent out this error if there no message unit with a processing state 
                        // different than FAILURE
                        // Anyhow the error should be registered
                        ErrorMessage errorMU = MessageUnitDAO.createOutgoingErrorMessageUnit(errForMsg, null, null,
                                                                                             false, true);
                        
                        if (onlyFailedMessageUnits(mc)) {
                            log.debug("All message units failed to process successfully, anonymous error can be sent");
                            MessageContextUtils.addErrorSignalToSend(mc, errorMU);
                            mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                        } else {
                            log.warn("Error without reference can not be sent because successfull message units exist");
                            // As we can not do anything with the error change its processing state to DONE
                            MessageUnitDAO.setDone(errorMU);
                        }
                    } else {
                        // This collection of errors references one of the received message units. 
                        log.debug("Check type of the message unit in error");
                        MessageUnit muInError = getAllMessageUnits(mc).get(refToMsgId);
                        if (muInError instanceof PullRequest) {
                            /* The message unit in error is a PullRequest. Because the PullRequest is not necessarily
                            bound to one P-Mode we can only sent the error as a response.
                            The ebMS specification however is not very clear about whether error reporting to another
                            URL should be possible and how that shoud be configured.
                            */
                            log.debug("Message unit in error is PullRequest, error must be sent as response");
                            ErrorMessage errorMU = MessageUnitDAO.createOutgoingErrorMessageUnit(errForMsg, 
                                                                                                 refToMsgId, 
                                                                                                 null, 
                                                                                                 false,
                                                                                                 true);
                            MessageContextUtils.addErrorSignalToSend(mc, errorMU);
                            mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                        } else {
                            log.debug("Get P-Mode information to determine how new signal must be processed");
                            // Errorhandling config is contained in flow 
                            String pmodeId = muInError.getPMode();
                            IUserMessageFlow flow = (pmodeId != null ? HolodeckB2BCore.getPModeSet().get(pmodeId)
                                                                    .getLegs().iterator().next().getUserMessageFlow()
                                                            : null);
                            IErrorHandling errorHandling = (flow != null ? flow.getErrorHandlingConfiguration() : null);
                            // Default is to send error as response when the message in error is received as a
                            //  request
                            boolean asResponse = errorHandling != null ? 
                                                            errorHandling.getPattern() == ReplyPattern.RESPONSE :
                                                            isInFlow(RESPONDER);
                            // Should this error signal be combined with a SOAP Fault? Because SOAP Faults can confuse
                            // the MSH that receives the error Holodeck B2B by default does not add the SOAP, it should
                            // be configured explicitly in the P-Mode
                            boolean addSOAPFault = errorHandling != null ?  errorHandling.shouldAddSOAPFault() : false;                            
                            // For signals error reporting is optional, so check if error is for a signal and if P-Mode
                            // is configured to report errors. Default is not to report errors for signals
                            boolean sendError = true; 
                            if (muInError instanceof ErrorMessage)
                                sendError = errorHandling != null ? errorHandling.shouldReportErrorOnError() 
                                                                  : Config.shouldReportErrorOnError();
                            if (muInError instanceof Receipt)
                                sendError = errorHandling != null ? errorHandling.shouldReportErrorOnReceipt()
                                                                  : Config.shouldReportErrorOnReceipt();

                            if (sendError) {
                                log.debug("Error should be returned as response? " + asResponse);
                                log.debug("Create Error signal and store in message database");
                                ErrorMessage errorMU = MessageUnitDAO.createOutgoingErrorMessageUnit(errForMsg, 
                                                                                                     refToMsgId, 
                                                                                                     muInError.getPMode(), 
                                                                                                     addSOAPFault,
                                                                                                     asResponse);
                                log.debug("Error signal stored in datase");
                                if (sendError && asResponse) {
                                    log.debug("This error signal should be returned as a response, prepare MessageContext");
                                    MessageContextUtils.addErrorSignalToSend(mc, errorMU);
                                    mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                                }                      
                            } else
                                log.debug("Error doesn't need to be sent. Processing completed.");
                        }
                    }
                } catch (DatabaseException ex) {
                    // Creating the new Error signal failed! As this invalidates the message processing
                    //  we must stop processing this flow
                    log.fatal("Creating the Error signal in repsonse to message unit [msgId=" 
                                    + refToMsgId + "] failed! Details: " + ex.getMessage());
                    // Signal this problem using a SOAP Fault
                    throw new AxisFault(ex.getMessage());
                }      
            }
        }
        
        // Continue processing
        return InvocationResponse.CONTINUE;
    }

    /**
     * Segments the given collection of errors into bundles of errors based on the referenced message unit.
     * 
     * @param errors        The collection of errors to segment
     * @return              A collection of errors per messageId. Errors that do not reference another message unit are
     *                      bundled under key <i>"null"</i>
     */
    private HashMap<String, Collection<EbmsError>> segmentErrors(Collection<EbmsError> errors) {
        HashMap<String, Collection<EbmsError>> segmentedErrors = new HashMap<String, Collection<EbmsError>>();
        for(EbmsError e : errors) {
            String  refToMsgId = e.getRefToMessageInError();
            // Check if the error is related to another message (it may be a general error)
            if (refToMsgId == null || refToMsgId.isEmpty())
                refToMsgId = "null"; // general error
            Collection<EbmsError> errForMsg = segmentedErrors.get(refToMsgId);
            if (errForMsg == null) {
                // No errors known for this referenced message unit
                errForMsg = new ArrayList<EbmsError>();
                segmentedErrors.put(refToMsgId, errForMsg);
            }
            errForMsg.add(e);                
        }
        return segmentedErrors;
    }
    
    /**
     * Helper method to check whether all of the message units contained in the message have failed processing. 
     * 
     * @param mc        The current message context containing all info on the message units
     * @return          <code>true</code> when all received message units have failed to process,<br>
     *                  <code>false</code> if any has been successfully processed.
     */
    private boolean onlyFailedMessageUnits(MessageContext mc) {
        boolean onlyFailed = true;
        Map<String, MessageUnit> allMUs = getAllMessageUnits(mc);
        for(String id : allMUs.keySet())
            onlyFailed &= ProcessingStates.FAILURE.equals(allMUs.get(id).getCurrentProcessingState().getName());        
        return onlyFailed;
    }
     
    /**
     * Helper to get all received message units.
     * 
     * @param mc    The current message context
     * @return      Collection of all {@link MessageUnit}s in the received message indexed on their messageId
     */
    private Map<String, MessageUnit> getAllMessageUnits(MessageContext mc) {   
        Map<String, MessageUnit>     allMUs = new HashMap<String, MessageUnit>();
        
        UserMessage userMsg = (UserMessage)
                MessageContextUtils.getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_USER_MESSAGE);
        if (userMsg != null) {
            log.debug("Request message contained an User Message, check if in error");
            allMUs.put(userMsg.getMessageId(), userMsg);
        }            
        PullRequest pullReq = (PullRequest) 
                MessageContextUtils.getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_PULL_REQUEST);
        if (pullReq != null) {
            log.debug("Request message contained a PullRequest");
            allMUs.put(pullReq.getMessageId(), pullReq);
        }
        Collection<Receipt> receipts = (ArrayList<Receipt>)
                MessageContextUtils.getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_RECEIPTS);
        if (receipts != null && !receipts.isEmpty()) {
            log.debug("Request message contained one or more Receipts signals");
            for(Receipt r : receipts)
                allMUs.put(r.getMessageId(), r);
        }
        Collection<ErrorMessage> errors = (ArrayList<ErrorMessage>) 
                MessageContextUtils.getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_ERRORS);
        if (errors != null && !errors.isEmpty()) {
            log.debug("Request message contained one or more Error signals");
            for(ErrorMessage e : errors)
                allMUs.put(e.getMessageId(), e);
        }
        
        return allMUs;
    }
     
}

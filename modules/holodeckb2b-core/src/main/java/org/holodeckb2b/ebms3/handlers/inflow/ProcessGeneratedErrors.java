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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.handlers.outflow.PrepareResponseMessage;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
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
 * Is the in flow handler that collects all ebMS errors generated during the processing of the received message. The
 * errors are grouped by the message unit they apply to. For each group a new error signal (a {@link ErrorMessage}
 * object) is created and saved in the database.
 * <p>Depending on the P-Mode settings for the error signal it is also stored in the message context to send it as a
 * response. This can result in multiple error signals ready to be sent. The {@link PrepareResponseMessage} handler in
 * the out flow is responsible to decide which signal(s) should be included in the response message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ProcessGeneratedErrors extends BaseHandler {

    /**
     * Errors will always be logged to a special error log. Using the logging
     * configuration users can decide if this logging should be enabled and
     * how errors should be logged.
     */
    private final Log     errorLog = LogFactory.getLog("org.holodeckb2b.msgproc.errors.generated.IN_FLOW");

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {
        log.debug("Check if errors were generated");
        final ArrayList<EbmsError>    errors = (ArrayList<EbmsError>)
                                                            mc.getProperty(MessageContextProperties.GENERATED_ERRORS);

        if (Utils.isNullOrEmpty(errors)) {
            log.debug("No errors were generated during this in flow, nothing to do");
        } else {
            StorageManager storageManager = HolodeckB2BCore.getStorageManager();
            log.debug(errors.size() + " error(s) were generated during this in flow");

            // First bundle the errors per message in error
            log.debug("Bundling errors per message unit");
            final HashMap<String, Collection<IEbmsError>> segmentedErrors = segmentErrors(errors);
            // Create Error signal for each bundle, i.e. each referenced message
            for(final String refToMsgId : segmentedErrors.keySet()) {
                log.debug("Create the Error Signal and save to database");
                ErrorMessage tErrorMessage = new ErrorMessage(segmentedErrors.get(refToMsgId));
                tErrorMessage.setRefToMessageId(refToMsgId.equals("null") ? null : refToMsgId);
                final IErrorMessageEntity errorMessage = storageManager.storeOutGoingMessageUnit(tErrorMessage);
                if (refToMsgId.equals("null")) {
                    // This collection of errors does not reference a specific message unit, should therefor be sent
                    // as a response.
                    // If however the message contained multiple message units some can be processed succesfully and
                    // returning an non referenceable error creates ambiguity as the sender can only set all sent
                    // message units to failed.
                    // Therefor we only sent out this error if there no message unit with a processing state
                    // different than FAILURE
                    if (isInFlow(INITIATOR) || onlyFailedMessageUnits(mc)) {
                        log.debug("All message units failed to process successfully, anonymous error can be sent");
                        MessageContextUtils.addErrorSignalToSend(mc, errorMessage);
                        mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                    } else {
                        log.warn("Error without reference can not be sent because successfull message units exist"
                                + " or message received as response");
                        // As we can not do anything with the error change its processing state to DONE
                        storageManager.setProcessingState(errorMessage, ProcessingState.WARNING);
                    }
                } else {
                    // This collection of errors references one of the received message units.
                    log.debug("Check type of the message unit in error");
                    final IMessageUnitEntity muInError = getRefdMessageUnit(mc, refToMsgId);
                    final String pmodeId = muInError.getPModeId();
                    if (!Utils.isNullOrEmpty(pmodeId)) {
                        log.debug("Set P-Mode Id [" + pmodeId + "] for generated Error Message");
                        storageManager.setPModeId(errorMessage, pmodeId);
                    }
                    if (muInError instanceof IPullRequest) {
                        /* The message unit in error is a PullRequest. Because the PullRequest is not necessarily
                        bound to one P-Mode we can only sent the error as a response.
                        The ebMS specification however is not very clear about whether error reporting to another
                        URL should be possible and how that shoud be configured.
                        */
                        log.debug("Message unit in error is PullRequest, error must be sent as response");
                        MessageContextUtils.addErrorSignalToSend(mc, errorMessage);
                        mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                    } else {
                        log.debug("Get P-Mode information to determine how new signal must be processed");
                        // Errorhandling config is contained in flow
                        final IUserMessageFlow flow = pmodeId == null ? null :
                                                            HolodeckB2BCoreInterface.getPModeSet().get(pmodeId)
                                                                   .getLeg(muInError.getLeg()).getUserMessageFlow();
                        final IErrorHandling errorHandling = (flow != null ? flow.getErrorHandlingConfiguration()
                                                                           : null);
                        // Default is to send error as response when the message in error is received as a request
                        final boolean asResponse = errorHandling != null ?
                                                        errorHandling.getPattern() == ReplyPattern.RESPONSE :
                                                        isInFlow(RESPONDER);
                        // Should this error signal be combined with a SOAP Fault? Because SOAP Faults can confuse
                        // the MSH that receives the error Holodeck B2B by default does not add the SOAP, it should
                        // be configured explicitly in the P-Mode
                        final boolean addSOAPFault = errorHandling != null ?  errorHandling.shouldAddSOAPFault()
                                                                           : false;
                        // For signals error reporting is optional, so check if error is for a signal and if P-Mode
                        // is configured to report errors. Default is not to report errors for signals
                        boolean sendError = true;
                        if (muInError instanceof IErrorMessage)
                            sendError = errorHandling != null && errorHandling.shouldReportErrorOnError() != null ?
                                        errorHandling.shouldReportErrorOnError() :
                                        HolodeckB2BCore.getConfiguration().shouldReportErrorOnError();

                        if (muInError instanceof IReceipt)
                            sendError = errorHandling != null && errorHandling.shouldReportErrorOnReceipt() != null?
                                        errorHandling.shouldReportErrorOnReceipt() :
                                        HolodeckB2BCore.getConfiguration().shouldReportErrorOnReceipt();

                        if (sendError) {
                            log.debug("This error signal should be sent"
                                        + (asResponse ?  " as a response" : " a using callback"));
                            if (asResponse) {
                                MessageContextUtils.addErrorSignalToSend(mc, errorMessage);
                                mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
                            } else
                                storageManager.setProcessingState(errorMessage, ProcessingState.READY_TO_PUSH);
                        } else {
                            log.debug("Error doesn't need to be sent. Processing completed.");
                            storageManager.setProcessingState(errorMessage, ProcessingState.DONE);
                        }
                    }
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

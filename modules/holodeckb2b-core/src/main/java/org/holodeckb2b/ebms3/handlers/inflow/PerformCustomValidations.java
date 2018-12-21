/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.validation.CustomValidationFailure;
import org.holodeckb2b.core.validation.ValidationResult;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.errors.OtherContentError;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN_FLOW</i> handler responsible for the execution of the custom validation of the User Message message unit
 * as specified in the P-Mode. How validation errors are handled depends on the validation configuration. When it
 * specifies that the message unit should be rejected an ebMS Error with error code EBMS:0004 (Other) will be generated.
 * The handler always raises a {@link CustomValidationFailure} to signal that validation issues were encountered.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 * @see IMessageValidationSpecification
 */
public class PerformCustomValidations extends AbstractUserMessageHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc, IUserMessageEntity userMessage) throws Exception {
        // For the execution of the validation a separate component is used. This component will also raise the event
        log.trace("Validate user message if specified");
        try {
            // Get custom validation specifcation from P-Mode
            IMessageValidationSpecification validationSpec = null;
            try {
                validationSpec = HolodeckB2BCore.getPModeSet().get(userMessage.getPModeId())
                                                              .getLeg(ILeg.Label.REQUEST)
                                                              .getUserMessageFlow().getCustomValidationConfiguration();
            } catch (NullPointerException npe) {
                // Some element in the path to the validation spec is not available, so there is nothing to do
            }

            if (validationSpec == null) {
                log.debug("No custom validation specified for user message, ready for delivery");
                HolodeckB2BCore.getStorageManager().setProcessingState(userMessage, 
                													   ProcessingState.READY_FOR_DELIVERY);                
                return InvocationResponse.CONTINUE;
            }

            ValidationResult validationResult = HolodeckB2BCore.getValidationExecutor().validate(userMessage,
                                                                                                 validationSpec);

            if (validationResult == null || Utils.isNullOrEmpty(validationResult.getValidationErrors())) {
                log.debug("User message is valid");
            	HolodeckB2BCore.getStorageManager().setProcessingState(userMessage, 
            														   ProcessingState.READY_FOR_DELIVERY);                
            } else {
                if (!validationResult.shouldRejectMessage()) {
                    log.info("User message [" + userMessage.getMessageId() 
                    										+ "] contains validation errors, but can be processed");
                	HolodeckB2BCore.getStorageManager().setProcessingState(userMessage, 
                														   ProcessingState.READY_FOR_DELIVERY);                
                } else {
                    log.info("User message [" + userMessage.getMessageId() 
                    								+ "] is not valid and must be rejected, generate Other error.");
                    OtherContentError otherError = new OtherContentError(
                                                           buildErrorDetailText(validationResult.getValidationErrors()),
                                                           userMessage.getMessageId());
                    MessageContextUtils.addGeneratedError(mc, otherError);
                    HolodeckB2BCore.getStorageManager().setProcessingState(userMessage, ProcessingState.FAILURE);
                }
                // Raise message processing event to inform other components of the validation issue
                HolodeckB2BCore.getEventProcessor().raiseEvent(new CustomValidationFailure(userMessage,
                                                                                               validationResult), mc);
            }
        } catch (MessageValidationException ve) {
            log.error("An error occurred when performing the validation of User Message [" + userMessage.getMessageId()
                        + "]!\n\tDetails: " + ve.getMessage());
            // As we don't know what happened exactly, we reject the message and generate an ebMS error
            OtherContentError otherError = new OtherContentError(
                                                    "An error occurred when performing the validation of User Message",
                                                    userMessage.getMessageId());
            MessageContextUtils.addGeneratedError(mc, otherError);
            HolodeckB2BCore.getStorageManager().setProcessingState(userMessage, ProcessingState.FAILURE);
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Creates the text to include in the <i>error detail</i> field of the ebMS error.
     *
     * @param validationErrors  The validation errors found in the message (grouped by validator)
     * @return  The text to include in the ebMS error
     */
    private String buildErrorDetailText(final Map<String, Collection<MessageValidationError>> validationErrors) {
        StringBuilder   errorText = new StringBuilder();
        // Add a line describing each error and count number of errors
        int totalErrors = 0;
        for(String validator : validationErrors.keySet())
            for(MessageValidationError error : validationErrors.get(validator)) {
                errorText.append('[').append(validator).append("] - ").append(error.getDescription()).append('\n');
                totalErrors++;
            }
        // Add intro, including number of errors found.
        errorText.insert(0, String.valueOf(totalErrors)).append(" validation errors were found in the message:\n");
        errorText.insert(0, "The message was found to be invalid!\n\n");

        return errorText.toString();
    }
}

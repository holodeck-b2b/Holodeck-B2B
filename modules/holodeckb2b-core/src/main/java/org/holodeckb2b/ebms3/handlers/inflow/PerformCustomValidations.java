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
import org.holodeckb2b.customvalidation.CustomValidationFailedEvent;
import org.holodeckb2b.customvalidation.ValidationResult;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.errors.OtherContentError;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN_FLOW</i> handler responsible for the execution of the custom validation of the User Message message unit
 * as specified in the P-Mode. How validation errors are handled depends on the validation configuration. When it
 * specifies that the message unit should be rejected an ebMS Error with error code EBMS:0004 (Other) will be generated.
 * The handler always raises a {@link CustomValidationFailedEvent} to signal that validation issues were encountered.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
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
        log.debug("Validate user message if specified");
        try {
            ValidationResult validationResult = HolodeckB2BCore.getValidationExecutor().validate(userMessage);

            System.out.println("validationResult: " + validationResult);

            if (validationResult == null || Utils.isNullOrEmpty(validationResult.getValidationErrors()))
                log.debug("User message is valid or no custom validation specified");
            else if (!validationResult.shouldRejectMessage())
                log.warn("User message contains validation errors, but can be processed");
            else {
                log.warn("User message is not valid and must be rejected, generate Other error.");
                OtherContentError otherError = new OtherContentError(
                                                        buildErrorDetailText(validationResult.getValidationErrors()),
                                                        userMessage.getMessageId());
                MessageContextUtils.addGeneratedError(mc, otherError);
                HolodeckB2BCore.getStorageManager().setProcessingState(userMessage, ProcessingState.FAILURE);
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

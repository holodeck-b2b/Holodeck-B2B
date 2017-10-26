/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.core.validation.ValidationResult;
import org.holodeckb2b.core.validation.header.HeaderValidationSpecification;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.errors.InvalidHeader;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Is the <i>IN FLOW</i> handler responsible for checking conformance of the received ebMS header meta data to the ebMS
 * specifications.
 * <p>The validation performed has two modes, <i>basic</i> and <i>strict</i> validation. The <i>basic
 * validation</i> is only ensure that the messages can be processed by the Holodeck B2B Core. These validations don't
 * include detailed checks on allowed combinations or format of values, like for example the requirement from the ebMS
 * Specification that the Service name must be an URL if no type is given. These are part of the <i>strict validation
 * </i> mode. The validation mode to use is specified in the configuration of the Holodeck B2B gateway ({@link
 * IConfiguration#useStrictHeaderValidation()}) or in the P-Mode ({@link IPMode#useStrictHeaderValidation()}) with the
 * strongest validation mode having priority.
 * <p>Note that additional validations can be used for User Message message units by using custom @todo: IMessageValidators.
 * These validation can also include checks on payloads included in the User Message and are separately configured in
 * the P-Mode.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
public class HeaderValidation extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {

        // Get all message units and then validate each one at configured mode
        Collection<IMessageUnitEntity> msgUnits = MessageContextUtils.getReceivedMessageUnits(mc);

        if (!Utils.isNullOrEmpty(msgUnits)) {
            for (IMessageUnitEntity m : msgUnits) {
                // Determine the validation to use
                boolean useStrictValidation = shouldUseStrictMode(m);

                log.debug("Validate " + MessageUnitUtils.getMessageUnitName(m) + " header meta-data using "
                         + (useStrictValidation ? "strict" : "basic") + " validation");

                ValidationResult validationResult = HolodeckB2BCore.getValidationExecutor().validate(m,
                                               HeaderValidationSpecification.getValidationSpec(m, useStrictValidation));

                if (validationResult == null || Utils.isNullOrEmpty(validationResult.getValidationErrors()))
                    log.debug("Header of " + MessageUnitUtils.getMessageUnitName(m) + " [" + m.getMessageId()
                            + "] successfully validated");
                else {
                    log.warn("Header of " + MessageUnitUtils.getMessageUnitName(m) + " [" + m.getMessageId()
                            + "] is invalid!\n\tDetails: " + validationResult);
                    MessageContextUtils.addGeneratedError(mc, buildError(m.getMessageId(),
                                                                         validationResult.getValidationErrors()));
                    HolodeckB2BCore.getStorageManager().setProcessingState(m, ProcessingState.FAILURE);
                }
            }
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Creates the text to include in the <i>error detail</i> field of the ebMS error.
     *
     * @param validationErrors  The validation errors found in the message (grouped by validator)
     * @return  The text to include in the ebMS error
     */
    private InvalidHeader buildError(final String messageId,
                              final Map<String, Collection<MessageValidationError>> validationErrors) {

        StringBuilder   errorText = new StringBuilder();
        // Add a line describing each error, count number of errors and check the maximum severity level
        int totalErrors = 0;
        MessageValidationError.Severity maxSeverity = MessageValidationError.Severity.Warning;
        for(MessageValidationError error : validationErrors.values().iterator().next()) {
            errorText.append(error.getDescription()).append('\n');
            if (error.getSeverityLevel().compareTo(maxSeverity) > 0)
                maxSeverity = error.getSeverityLevel();
            totalErrors++;
        }
        // Add intro, including number of errors found.
        errorText.insert(0, String.valueOf(totalErrors)).append(" validation errors were found in the message:\n");
        errorText.insert(0, "The message was found to be invalid!\n\n");

        // Create the error
        InvalidHeader error = new InvalidHeader(errorText.toString(), messageId);
        error.setSeverity(maxSeverity == MessageValidationError.Severity.Failure ? IEbmsError.Severity.FAILURE
                                                                                 : IEbmsError.Severity.WARNING);

        return error;
    }


    /**
     * Helper method to determine which validation mode should be used for the given message unit.
     *
     * @param m     The message unit for which the validation mode should be determined
     * @return      <code>true</code> if strict validation should be used,<code>false</code> otherwise
     */
    private boolean shouldUseStrictMode(IMessageUnitEntity m) throws NullPointerException {
        // First get global setting which may be enough when it is set to strict
        boolean useStrictValidation = HolodeckB2BCore.getConfiguration().useStrictHeaderValidation();
        if (!useStrictValidation && !Utils.isNullOrEmpty(m.getPModeId()))
            useStrictValidation = HolodeckB2BCore.getPModeSet().get(m.getPModeId()).useStrictHeaderValidation();

        return useStrictValidation;
    }
}

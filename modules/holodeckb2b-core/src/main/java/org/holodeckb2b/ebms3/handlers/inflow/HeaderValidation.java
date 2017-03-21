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
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.errors.InvalidHeader;
import org.holodeckb2b.ebms3.errors.OtherContentError;
import org.holodeckb2b.ebms3.headervalidation.HeaderValidatorFactory;
import org.holodeckb2b.ebms3.headervalidation.IHeaderValidator;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
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
 * @author Sander Fieten <sander at chasquis-services.com>
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
                boolean useStrictValidation;
                try {
                    useStrictValidation = shouldUseStrictMode(m);
                } catch (NullPointerException pModeNotAvailable) {
                    // Signal problem of non available P-Mode in log and raise Other error
                    log.error("P-Mode [" + m.getPModeId() + "] of message unit not available any more!");
                    OtherContentError otherError = new OtherContentError("Internal processing error", m.getMessageId());
                    // Change processing state of messsage unit
                    HolodeckB2BCore.getStoreManager().setProcessingState(m, ProcessingState.FAILURE);
                    MessageContextUtils.addGeneratedError(mc, otherError);
                    continue;
                }

                log.debug("Validate " + MessageUnitUtils.getMessageUnitName(m) + " header meta-data using "
                         + (useStrictValidation ? "strict" : "basic") + " validation");
                IHeaderValidator validator = HeaderValidatorFactory.getValidator(m);
                String validationResult = validator.validate(m, useStrictValidation);
                if (!Utils.isNullOrEmpty(validationResult)) {
                    log.warn("Header of " + MessageUnitUtils.getMessageUnitName(m) + " [" + m.getMessageId()
                            + "] is invalid!\n\tDetails: " + validationResult);
                    InvalidHeader invalidHeaderError = new InvalidHeader(validationResult, m.getMessageId());
                    MessageContextUtils.addGeneratedError(mc, invalidHeaderError);
                    HolodeckB2BCore.getStoreManager().setProcessingState(m, ProcessingState.FAILURE);
                } else
                    log.debug("Header of " + MessageUnitUtils.getMessageUnitName(m) + " [" + m.getMessageId()
                            + "] successfully validated");
            }
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Saves a <i>InvalidHeader</i> error that was result of validation of the given message unit to the message context
     * and sets the processing state of the message unit to <i>FAILURE</i>.
     *
     * @param invalidHeaderError    The error that was generated during validation
     * @param messageUnit           The message unit that was validated
     * @param mc                    The current message context
     * @throws PersistenceException    When there is a problem changing the processing state of the invalid message unit
     */
    private void saveError(InvalidHeader invalidHeaderError, IMessageUnitEntity messageUnit, MessageContext mc)
                                                                                          throws PersistenceException {
        log.warn("Received " + MessageUnitUtils.getMessageUnitName(messageUnit) + " [" + messageUnit.getMessageId()
                 + "] is not valid");
        log.debug("Save the error to message context");
        MessageContextUtils.addGeneratedError(mc, invalidHeaderError);
        log.debug("Set processing state for invalid message to failure");
        HolodeckB2BCore.getStoreManager().setProcessingState(messageUnit, ProcessingState.FAILURE);
        log.debug("Processed InvalidHeader error");
    }

    /**
     * Helper method to determine which validation mode should be used for the given message unit.
     *
     * @param m     The message unit for which the validation mode should be determined
     * @return      <code>true</code> if strict validation should be used,<code>false</code> otherwise
     * @throws NullPointerException  When the P-Mode that was found for the message unit isn't available anymore.
     */
    private boolean shouldUseStrictMode(IMessageUnitEntity m) throws NullPointerException {
        // First get global setting which may be enough when it is set to strict
        boolean useStrictValidation = HolodeckB2BCore.getConfiguration().useStrictHeaderValidation();
        if (!useStrictValidation)
            useStrictValidation = HolodeckB2BCore.getPModeSet().get(m.getPModeId()).useStrictHeaderValidation();

        return useStrictValidation;
    }
}

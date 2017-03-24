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

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.errors.InvalidHeader;
import org.holodeckb2b.ebms3.headervalidators.ErrorSignal;
import org.holodeckb2b.ebms3.headervalidators.PullRequest;
import org.holodeckb2b.ebms3.headervalidators.Receipt;
import org.holodeckb2b.ebms3.headervalidators.UserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.*;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;

import java.util.Collection;

/**
 * Is the <i>IN FLOW</i> handler responsible for checking basic conformance of the received ebMS header meta data. The
 * validations performed by this handler are to ensure that the messages can be processed by the Holodeck B2B Core.
 * <br>These validations don't include detailed checks on allowed combinations or format of values, like for example the
 * requirement from the ebMS Specification that the Service name must be an URL if no type is given. Additional
 * validations for User Message message units can be implemented in custom @todo: IMessageValidators and configured through
 * the P-Mode.
 *
 * @author Sander Fieten (sander at chasquis-services.com)
 * @since HB2B_NEXT_VERSION
 */
public class BasicHeaderValidation extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc) throws Exception {
        // Validate User Message
        IUserMessageEntity userMessage = (IUserMessageEntity)
                mc.getProperty(MessageContextProperties.IN_USER_MESSAGE);
        if (userMessage != null) {
            log.debug("Check received User Message");
            InvalidHeader   invalidHeaderError = UserMessage.validate(userMessage);
            if (invalidHeaderError != null)
                saveError(invalidHeaderError, userMessage, mc);
            else
                log.debug("Received User Message satisfies basic validations");
        }
        // Validate Pull Request
        IPullRequestEntity pullRequest = (IPullRequestEntity)
                mc.getProperty(MessageContextProperties.IN_PULL_REQUEST);
        if (pullRequest != null) {
            log.debug("Check received Pull Request");
            InvalidHeader   invalidHeaderError = PullRequest.validate(pullRequest);
            if (invalidHeaderError != null)
                saveError(invalidHeaderError, pullRequest, mc);
            else
                log.debug("Received Pull Request satisfies basic validations");
        }
        // Validate Receipts
        Collection<IReceiptEntity> receipts = (Collection<IReceiptEntity>)
                                                                mc.getProperty(MessageContextProperties.IN_RECEIPTS);
        if (!Utils.isNullOrEmpty(receipts)) {
            log.debug("Check received Receipts");
            for (IReceiptEntity rcpt : receipts) {
                InvalidHeader   invalidHeaderError = Receipt.validate(rcpt);
                if (invalidHeaderError != null)
                    saveError(invalidHeaderError, rcpt, mc);
                else
                    log.debug("Received Receipt satisfies basic validations");
            }
        }
        // Validate Errors
        Collection<IErrorMessageEntity> errors = (Collection<IErrorMessageEntity>)
                                                                mc.getProperty(MessageContextProperties.IN_ERRORS);
        if (!Utils.isNullOrEmpty(errors)) {
            log.debug("Check received Errors");
            for (IErrorMessageEntity error : errors) {
                InvalidHeader   invalidHeaderError = ErrorSignal.validate(error);
                if (invalidHeaderError != null)
                    saveError(invalidHeaderError, error, mc);
                else
                    log.debug("Received Error satisfies basic validations");
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
}

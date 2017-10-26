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
package org.holodeckb2b.core.validation.header;

import java.util.Collection;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Provides the validation of the ebMS header information specific for <i>User Message</i> message units.
 * <p>NOTE: Using custom validation additional validations can be used for User Message message units that can also
 * include checks on payloads included in the User Message and are separately configured in the P-Mode.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  HB2B_NEXT_VERSION
 */
class UserMessageValidator extends GeneralMessageUnitValidator<IUserMessage>
                           implements IMessageValidator<IUserMessage> {

    UserMessageValidator(boolean useStrictValidation) {
        super(useStrictValidation);
    }

    /**
     * Performs the basic validation of the ebMS header meta-data specific for a User Message message unit.
     * <p>This includes a check on the general message meta-data and information about the sender and receiver of the
     * message as well as business information like service and action are available.
     *
     * @param messageUnit       The User Message message unit which header must be validated
     * @param validationErrors  Collection of {@link MessageValidationError}s to which validation errors must be added
     */
    @Override
    protected void doBasicValidation(final IUserMessage messageUnit,
                                     Collection<MessageValidationError> validationErrors) {
        // First do genereal validation
        super.doBasicValidation(messageUnit, validationErrors);

        // Cast to IUserMessage
        IUserMessage userMessageInfo = (IUserMessage) messageUnit;
        // Check that To and From party are included
        doBasicPartyInfoValidation(userMessageInfo.getSender(), "Sender", validationErrors);
        doBasicPartyInfoValidation(userMessageInfo.getReceiver(), "Receiver", validationErrors);

        // Check collaboration information, i.e. Service, Action and ConversationId
        final ICollaborationInfo collabInfo = userMessageInfo.getCollaborationInfo();
        if (collabInfo == null)
            validationErrors.add(new MessageValidationError("Collaboration information is missing"));
        else {
            if (Utils.isNullOrEmpty(collabInfo.getAction()))
                validationErrors.add(new MessageValidationError("Action is missing"));
            if (collabInfo.getService() == null || Utils.isNullOrEmpty(collabInfo.getService().getName()))
                validationErrors.add(new MessageValidationError("Service information is missing"));
        }
        // Check MessageProperties (if provided)
        checkProperties(userMessageInfo.getMessageProperties(), "Message", validationErrors);

        // Check PayloadInfo, in this validator only the part properties are checked
        final Collection<IPayload> payloadInfo = (Collection<IPayload>) userMessageInfo.getPayloads();
        if (!Utils.isNullOrEmpty(payloadInfo))
            for (IPayload p : payloadInfo)
                checkProperties(p.getProperties(), "Part", validationErrors);
    }

    /**
     * Performs the strict validation of the ebMS header meta-data specific for a User Message message unit
     * <p>Checks that
     *
     * @param messageUnit       The User Message message unit which header must be validated
     * @param validationErrors  Collection of {@link MessageValidationError}s to which validation errors must be added
     */
    @Override
    protected void doStrictValidation(final IUserMessage messageUnit,
                                      Collection<MessageValidationError> validationErrors) {
        // First do genereal validation
        super.doStrictValidation(messageUnit, validationErrors);
    }

    /**
     * Perform a basic checks of information about the sender or receiver contained in the User Message
     *
     * @param partnerInfo       The meta-data of the sender or receiver
     * @parem partnerName       The text to identify this partner in error message, i.e. "Sender" or "Receiver"
     * @param validationErrors  Collection of {@link MessageValidationError}s to which validation errors must be added
     */
    private void doBasicPartyInfoValidation(final ITradingPartner partnerInfo, final String partnerName,
                                            Collection<MessageValidationError> validationErrors) {
        if (partnerInfo == null) {
            validationErrors.add(new MessageValidationError(partnerName + " information is missing"));
        } else {
            if (Utils.isNullOrEmpty(partnerInfo.getRole()))
                validationErrors.add(new MessageValidationError("Role of " + partnerName + " is missing"));
            if (Utils.isNullOrEmpty(partnerInfo.getPartyIds()))
                validationErrors.add(new MessageValidationError("Identification of " + partnerName + " is missing"));
            else {
                for (IPartyId pid : partnerInfo.getPartyIds()) {
                    if (Utils.isNullOrEmpty(pid.getId()))
                        validationErrors.add(new MessageValidationError("Empty PartyId for " + partnerName
                                                                        + " is not allowed"));
                }
            }
        }
    }

    /**
     * Checks whether all properties in a set of properties have a name and value.
     *
     * @param properties        The set of properties to check
     * @param propSetName       The name of the set, i.e. "Message" or "Part"
     * @param validationErrors  Collection of {@link MessageValidationError}s to which validation errors must be added
     */
    private void checkProperties(final Collection<IProperty> properties, final String propSetName,
                                          Collection<MessageValidationError> validationErrors) {
        if (!Utils.isNullOrEmpty(properties))
            for(IProperty p : properties) {
                if (Utils.isNullOrEmpty(p.getName()))
                    validationErrors.add(
                                     new MessageValidationError("Unnamed " + propSetName + " property is not allowed"));
                if (Utils.isNullOrEmpty(p.getValue()))
                    validationErrors.add(new MessageValidationError(propSetName + " property \"" + p.getName()
                                                                                                  + "\" has no value"));
            }
    }
}

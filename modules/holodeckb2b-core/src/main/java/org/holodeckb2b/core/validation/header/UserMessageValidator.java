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
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Provides the validation of the ebMS header information specific for <i>User Message</i> message units.
 * <p>NOTE: Using custom @todo: IMessageValidators additional validations can be used for User Message message units
 * that can also include checks on payloads included in the User Message and are separately configured in the P-Mode.
 *
 * @author Sander Fieten <sander at chasquis-services.com>
 * @since  HB2B_NEXT_VERSION
 */
class UserMessageValidator extends GeneralMessageUnitValidator {

    /**
     * Performs the basic validation of the ebMS header meta-data specific for a Receipt signal message unit.
     * <p>This includes a check on the general message meta-data and information about the sender and receiver of the
     * message as well as business information like service and action are available.
     *
     * @param messageUnit           The message unit which header must be validated
     * @param validationErrors      The string that is being build containing a description of all validation errors
     *                              found for the header in the message header
     */
    @Override
    protected void doBasicValidation(final IMessageUnit messageUnit, final StringBuilder validationErrors) {
        // First do genereal validation
        super.doBasicValidation(messageUnit, validationErrors);

        // Cast to IUserMessage
        IUserMessage userMessageInfo = (IUserMessage) messageUnit;
        // Check that To and From party are included
        validationErrors.append(doBasicPartyInfoValidation(userMessageInfo.getSender(), "Sender"));
        validationErrors.append(doBasicPartyInfoValidation(userMessageInfo.getReceiver(), "Receiver"));

        // Check collaboration information, i.e. Service, Action and ConversationId
        final ICollaborationInfo collabInfo = userMessageInfo.getCollaborationInfo();
        if (collabInfo == null)
            validationErrors.append("Collaboration information is missing\n");
        else {
            if (Utils.isNullOrEmpty(collabInfo.getAction()))
                validationErrors.append("Action is missing\n");
            if (collabInfo.getService() == null || Utils.isNullOrEmpty(collabInfo.getService().getName()))
                validationErrors.append("Service information is missing\n");
        }
        // Check MessageProperties (if provided)
        validationErrors.append(checkProperties(userMessageInfo.getMessageProperties(), "Message"));

        // Check PayloadInfo, in this validator only the part properties are checked
        final Collection<IPayload> payloadInfo = (Collection<IPayload>) userMessageInfo.getPayloads();
        if (!Utils.isNullOrEmpty(payloadInfo))
            for (IPayload p : payloadInfo)
                validationErrors.append(checkProperties(p.getProperties(), "Part"));
    }

    /**
     * Performs the strict validation of the ebMS header meta-data specific for a User Message message unit
     * <p>Checks that
     *
     * @param messageUnit           The message unit which header must be validated
     * @param validationErrors      The string that is being build containing a description of all validation errors
     *                              found for the header in the message header
     */
    @Override
    protected void doStrictValidation(final IMessageUnit messageUnit, final StringBuilder validationErrors) {
        // First do genereal validation
        super.doStrictValidation(messageUnit, validationErrors);
    }

    /**
     * Perform a basic checks of information about the sender or receiver contained in the User Message
     *
     * @param partnerInfo   The meta-data of the sender or receiver
     * @parem partnerName   The text to identify this partner in error message, i.e. "Sender" or "Receiver"
     * @return              An empty String when no errors where found, else a description of the errors found
     */
    private static String doBasicPartyInfoValidation(final ITradingPartner partnerInfo, final String partnerName) {
        StringBuilder errors = new StringBuilder();

        if (partnerInfo == null) {
            errors.append(partnerName).append(" information is missing");
        } else {
            if (Utils.isNullOrEmpty(partnerInfo.getRole()))
                errors.append("Role of ").append(partnerName).append(" is missing\n");
            if (Utils.isNullOrEmpty(partnerInfo.getPartyIds()))
                errors.append("Identification of ").append(partnerName).append(" is missing\n");
            else {
                for (IPartyId pid : partnerInfo.getPartyIds()) {
                    if (Utils.isNullOrEmpty(pid.getId()))
                        errors.append("Empty PartyId is not allowed\n");
                }
            }
        }

        return errors.toString();
    }

    /**
     * Checks whether all properties in a set of properties have a name and value.
     *
     * @param properties    The set of properties to check
     * @param propSetName   The name of the set, i.e. "Message" or "Part"
     * @return              An empty String when no errors where found, else a description of the errors found
     */
    private static String checkProperties(final Collection<? extends IProperty> properties, final String propSetName) {
        StringBuilder errors = new StringBuilder();
        if (!Utils.isNullOrEmpty(properties))
            for(IProperty p : properties) {
                if (Utils.isNullOrEmpty(p.getName()))
                    errors.append("Unnamed ").append(propSetName).append(" property is not allowed");
                if (Utils.isNullOrEmpty(p.getValue()))
                    errors.append(propSetName).append(" property \"").append(p.getName()).append("\" has no value");
            }

        return errors.toString();
    }
}

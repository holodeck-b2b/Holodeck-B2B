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
package org.holodeckb2b.ebms3.headervalidators;

import java.util.Collection;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.errors.InvalidHeader;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Provides basic validation of the ebMS header information specific for <i>User Message</i> message units.
 * <p>This incudes a check on the general message meta-data and information about the sender and receiver of the message
 * as well as business information like service and action are available. If an error is detected an ebMS
 * <i>InvalidHeader</i> is created and reported back.
 *
 * @author Sander Fieten <sander at chasquis-services.com>
 * @since  HB2B_NEXT_VERSION
 */
public class UserMessage {

    /**
     * Checks the meta-data on a User Message to contain at least the required information for further processing.
     * <br>For the validation of the general meta-data like messageId and timestamp the {@link MessageUnit} is
     * used.
     *
     * @param userMessageInfo   The meta-data on the User Message to check
     * @return                  If a problem was found an {@link InvalidHeader} ebMS error is returned with the <code>
     *                          ErrorDetails</code> containing the list of found issues,<br>
     *                          If no problems are found <code>null</code> is returned.
     */
    public static InvalidHeader validate(final IUserMessage userMessageInfo) {
       StringBuilder    errDetails = new StringBuilder();

       // First validate the general meta-data
       errDetails.append(MessageUnit.validate(userMessageInfo));

       // Check that To and From party are included
       errDetails.append(checkPartyInfo(userMessageInfo.getSender(), "Sender"));
       errDetails.append(checkPartyInfo(userMessageInfo.getReceiver(), "Receiver"));

       // Check collaboration information, i.e. Service, Action and ConversationId
       final ICollaborationInfo collabInfo = userMessageInfo.getCollaborationInfo();
       if (collabInfo == null)
           errDetails.append("Collaboration information is missing\n");
       else {
           if (Utils.isNullOrEmpty(collabInfo.getAction()))
               errDetails.append("Action is missing\n");
           if (Utils.isNullOrEmpty(collabInfo.getConversationId()))
               errDetails.append("ConversationId is missing\n");
           if (collabInfo.getService() == null || Utils.isNullOrEmpty(collabInfo.getService().getName()))
               errDetails.append("Service information is missing\n");
       }
       // Check MessageProperties (if provided)
       errDetails.append(checkProperties(userMessageInfo.getMessageProperties(), "Message"));

       // Check PayloadInfo, in this validator only the part properties are checked
       final Collection<IPayload> payloadInfo = userMessageInfo.getPayloads();
       if (!Utils.isNullOrEmpty(payloadInfo))
           for (IPayload p : payloadInfo)
               errDetails.append(checkProperties(p.getProperties(), "Part"));

       // Create the ebMS error if any problems were found
       String errorDetails = errDetails.toString().trim();
       if (!errorDetails.isEmpty())
           return new InvalidHeader(errorDetails);
       else
           return null;
    }

    /**
     * Checks whether the User Message meta-data contains information about the sender or receiver.
     *
     * @param partnerInfo   The meta-data of the sender or receiver
     * @parem partnerName   The text to identify this partner in error message, i.e. "Sender" or "Receiver"
     * @return              An empty String when no errors where found, else a description of the errors found
     */
    private static String checkPartyInfo(final ITradingPartner partnerInfo, final String partnerName) {
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
    private static String checkProperties(final Collection<IProperty> properties, final String propSetName) {
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

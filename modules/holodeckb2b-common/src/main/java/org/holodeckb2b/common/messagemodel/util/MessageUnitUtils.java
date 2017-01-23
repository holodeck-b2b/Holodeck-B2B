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
package org.holodeckb2b.common.messagemodel.util;

import java.util.Collection;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is a container class for helper methods around message units.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */
public class MessageUnitUtils {

    /**
     * Returns the type of the given message unit as a String. This is the name as described in the ebMS V3
     * Specification and not the Java class/interface type.
     *
     * @param msgUnit   The message unit to get the type name for
     * @return          Descriptive name of the message unit's type
     */
    public static String getMessageUnitName(final IMessageUnit msgUnit) {
        if (msgUnit instanceof IUserMessage)
            return "User Message";
        else if (msgUnit instanceof IPullRequest)
            return "Pull Request";
        else if (msgUnit instanceof IReceipt)
            return "Receipt";
        else if (msgUnit instanceof IErrorMessage)
            return "Error Message";
        else
            return "Unknown message type ";
    }

    /**
     * Returns the type of the given message unit as a Class object of the interface from the Holodeck B2B message model
     * that represents the message unit type of the given message unit.
     *
     * @param msgUnit   The message unit to get the type for
     * @return          The message unit's type
     */
    public static Class<? extends IMessageUnit> getMessageUnitType(final IMessageUnit msgUnit) {
        if (msgUnit instanceof IUserMessage)
            return IUserMessage.class;
        else if (msgUnit instanceof IPullRequest)
            return IPullRequest.class;
        else if (msgUnit instanceof IReceipt)
            return IReceipt.class;
        else if (msgUnit instanceof IErrorMessage)
            return IErrorMessage.class;
        else
            throw new IllegalArgumentException("Given object is not a message unit object!");
    }

    /**
     * Gets the messageId the given message unit refers to. With exception of the Error Signal this is the value of the
     * the <code>refToMessageId</code> attribute in the header of the message unit. For Error Signals however the
     * reference can also be included in the <code>refToMessageInError</code> attribute included in the individual
     * errors.
     *
     * @param msgUnit   The message unit to get the reference from
     * @return  The messageId of the referenced message unit if it is available in the given message unit, or<br>
     *          <code>null</code> if not available.
     * @since HB2B_NEXT_VERSION
     */
    public static String getRefToMessageId(final IMessageUnit msgUnit) {
        String refToMessageId = msgUnit.getRefToMessageId();
        if (Utils.isNullOrEmpty(refToMessageId) && msgUnit instanceof IErrorMessage) {
            // For errors the reference can also be included in the Error elements, but they all need to be the same,
            // so take the first one
            final Collection<IEbmsError> errors = (Collection<IEbmsError>) ((IErrorMessage) msgUnit).getErrors();
            refToMessageId = errors.isEmpty() ? null : errors.iterator().next().getRefToMessageInError();
        }
        return refToMessageId;
    }

    /**
     * Returns a String representation of the given ebMS <i>Error Signal</i> message unit.
     * <p>It This method can be
     * used for easily logging the error to text files, etc.
     *
     * @return String representation of the Error Signal
     */
    public static String errorSignalToString(final IErrorMessage errorSignal) {
        final StringBuilder  errorMsg = new StringBuilder("ErrorSignal: msgId=");
        errorMsg.append(errorSignal.getMessageId())
                .append(", referenced message id=")
                .append(getRefToMessageId(errorSignal))
                .append("\n")
                .append("List of errors:");

        for(final IEbmsError error : errorSignal.getErrors()) {
            errorMsg.append("ebMS error:\n")
                    .append("\tCode=").append(error.getErrorCode())
                    .append("\tMessage=").append(error.getMessage())
                    .append("\tSeverity= ").append(error.getSeverity());
            if (!Utils.isNullOrEmpty(error.getOrigin()))
                errorMsg.append("\tOrigin= ").append(error.getOrigin());
            if (!Utils.isNullOrEmpty(error.getCategory()))
                errorMsg.append("\tCategory= ").append(error.getCategory());
            if (!Utils.isNullOrEmpty(error.getErrorDetail()))
                errorMsg.append("\tDetails= ").append(error.getErrorDetail());
            if (error.getDescription() != null) {
                errorMsg.append("\tDescription= ");
                if (!Utils.isNullOrEmpty(error.getDescription().getLanguage()))
                    errorMsg.append("[").append(error.getDescription().getLanguage()).append("]");
                errorMsg.append(error.getDescription().getText());
            }
        }
        return errorMsg.toString();
    }
}

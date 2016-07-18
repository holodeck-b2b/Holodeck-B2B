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
     * Returns the type of the given message unit. This is the name as described in the ebMS V3 Specification and not 
     * the Java class/interface type.
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
}

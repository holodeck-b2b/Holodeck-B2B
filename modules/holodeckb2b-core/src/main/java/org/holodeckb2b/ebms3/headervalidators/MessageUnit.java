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

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Provides basic validation of the ebMS header information that should be available in all message units.
 * <p>It will check that the MessageId and timestamp are available. If an error is detected an ebMS <i>InvalidHeader</i>
 * is created and reported back.
 * todo Rename to MessageUnitValidator
 * @author Sander Fieten (sander at chasquis-services.com)
 * @since  2.2
 */
public abstract class MessageUnit {

    /**
     * Checks a MessageId and timestamp are available in the given message unit.
     *
     * @param messageUnit   The meta-data on the message unit to check
     * @return              An empty String when no errors where found, else a description of the errors found
     */
    static String validate(IMessageUnit messageUnit) {
        StringBuilder    errDetails = new StringBuilder();

        if (Utils.isNullOrEmpty(messageUnit.getMessageId()))
            errDetails.append("MessageId is missing\n");
        if (messageUnit.getTimestamp() == null)
            errDetails.append("Timestamp is missing\n");

        // Return trimmed string so it is empty when nothing is reported
        return  errDetails.toString().trim();
    }
}

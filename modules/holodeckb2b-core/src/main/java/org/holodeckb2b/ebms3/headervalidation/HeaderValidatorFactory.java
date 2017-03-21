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
package org.holodeckb2b.ebms3.headervalidation;

import java.util.HashMap;
import java.util.Map;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.ebms3.headervalidation.validators.ErrorSignalValidator;
import org.holodeckb2b.ebms3.headervalidation.validators.PullRequestValidator;
import org.holodeckb2b.ebms3.headervalidation.validators.ReceiptValidator;
import org.holodeckb2b.ebms3.headervalidation.validators.UserMessageValidator;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is the factory class for creating <i>ebMS header validators</i> that check whether the ebMS message header of a
 * message unit conforms to the ebMS specifications.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
public class HeaderValidatorFactory {

    /**
     * Storage of header validators structured per message unit type and validation mode
     */
    private static final Map<Class<? extends IMessageUnit>, IHeaderValidator>   headerValidators;

    static {
        // Create the validator instances
        //
        headerValidators = new HashMap<>();

        headerValidators.put(IUserMessage.class, new UserMessageValidator());
        headerValidators.put(IPullRequest.class, new PullRequestValidator());
        headerValidators.put(IReceipt.class, new ReceiptValidator());
        headerValidators.put(IErrorMessage.class, new ErrorSignalValidator());
    }

    /**
     * Gets the validator for the given message unit depending on whether a basic or strict validation should be
     * executed.
     *
     * @param messageUnit   The message unit to validate
     * @return              The validator that can check the given message in the requested mode.
     */
    public static IHeaderValidator getValidator(final IMessageUnit messageUnit) {
        // Get the validator for the specified mode
        return headerValidators.get(MessageUnitUtils.getMessageUnitType(messageUnit));
    }
}

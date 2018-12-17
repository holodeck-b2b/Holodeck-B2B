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
package org.holodeckb2b.ebms3.validation.header;

import java.util.HashMap;

import org.holodeckb2b.core.validation.header.AbstractHeaderValidatorFactory;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is the factory class for creating <i>ebMS header validators</i> that check whether the ebMS V3 message header 
 * of a message unit conforms to the ebMS specifications.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  HB2B_NEXT_VERSION
 */
public class Ebms3HeaderValidatorFactory extends AbstractHeaderValidatorFactory
																		implements IMessageValidator.Factory {
    static {
        // Create the validator instances
        //
        laxValidators = new HashMap<>();
        laxValidators.put(IUserMessage.class, new UserMessageValidator(false));
        laxValidators.put(IPullRequest.class, new PullRequestValidator(false));
        laxValidators.put(IReceipt.class, new ReceiptValidator(false));
        laxValidators.put(IErrorMessage.class, new ErrorSignalValidator(false));
        strictValidators = new HashMap<>();
        strictValidators.put(IUserMessage.class, new UserMessageValidator(true));
        strictValidators.put(IPullRequest.class, new PullRequestValidator(true));
        strictValidators.put(IReceipt.class, new ReceiptValidator(true));
        strictValidators.put(IErrorMessage.class, new ErrorSignalValidator(true));
    }
}

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
package org.holodeckb2b.core.validation.header;

import java.util.Map;

import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is the factory class for creating <i>ebMS header validators</i> that check whether the ebMS message header of a
 * message unit conforms to the ebMS specifications.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since  4.1.0
 */
public abstract class AbstractHeaderValidatorFactory implements IMessageValidator.Factory {
    /**
     * Name of the parameter that indicates whether lax or strict validation should occur
     */
    public static final String P_VALIDATION_MODE = "hdr-valid-strict";
    /**
     * Name of the parameter that contains the interface class indicating the message unit type to be validated
     */
    public static final String P_MSGUNIT_TYPE = "hdr-valid-type";

    /**
     * Maps holding the singletons of both the lax and strict header validators structured per message unit type
     */
    protected static Map<Class<? extends IMessageUnit>, IMessageValidator>   laxValidators;
    protected static Map<Class<? extends IMessageUnit>, IMessageValidator>   strictValidators;

    /**
     * The validator requested by this instance of the factory
     */
    private IMessageValidator   validator;

    @Override
    public void init(Map<String, ?> parameters) throws MessageValidationException {
        // Get the correct validator depending on the preferred validation mode and message type
        if (Boolean.TRUE.equals(parameters.get(P_VALIDATION_MODE)))
            validator = strictValidators.get(parameters.get(P_MSGUNIT_TYPE));
        else
            validator = laxValidators.get(parameters.get(P_MSGUNIT_TYPE));
    }

    @Override
    public IMessageValidator createMessageValidator() throws MessageValidationException {
        return validator;
    }
}

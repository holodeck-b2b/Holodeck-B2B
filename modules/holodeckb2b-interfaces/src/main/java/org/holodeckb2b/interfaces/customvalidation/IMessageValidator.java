/*
 * Copyright (C) 2017 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.customvalidation;

import java.util.Collection;
import java.util.Map;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;

/**
 * Defines both the interface of a validator that can be used to perform custom validation on a <i>User Message</i>
 * message unit and in an inner interface its associated factory.
 * <p>Custom validations are intended to check specific business domain requirements on the message meta-data. As the
 * validators however have access to the payloads of the User Message they can also validate the business meta-data.
 * <p>Whether User Messages should be validated is configured in the <i>User Message flow</i> of the P-Mode, see {@link
 * IUserMessageFlow#getCustomValidationConfiguration()}.
 * <p>The <i>validation executor</i> manages the execution of the validations and may decide to run multiple validators
 * in parallel. Therefore validator implementations MUST NOT lock the payload files in any way.
 * <p>NOTE: As these custom validation are performed in the normal processing pipe line they influence the performance
 * of the message processing. Custom validation overhead should therefore be minimized.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface IMessageValidator {

    /**
     * Validates the given <i>User Message</i> message unit.
     *
     * @param userMessage   The User Message that must be validated.
     * @return              A Collection of {@link MessageValidationError}s when there are validation errors.<br>
     *                      When no problems were detected an empty Collection or <code>null</code>
     * @throws MessageValidationException   When the validator can not complete the validation of the message unit
     */
    Collection<MessageValidationError> validate(final IUserMessage userMessage) throws MessageValidationException;

    /**
     * Defines the interface for factory object that is responsible for creating the validator objects.
     * <p>Using the factory pattern, implementations can decide to re-use validator objects for different executions.
     * For example to save costly initialization. The parameters to use for an actual delivery are specified in the
     * {@link IMessageValidationSpecification} that is part of the P-Mode.
     *
     * @author Sander Fieten (sander at holodeck-b2b.org)
     * @since HB2B_NEXT_VERSION
     */
    public interface Factory {
        /**
         * Initializes the message validator factory with the parameters as provided in the P-Mode.
         *
         * @param parameters    The parameters for initialization of the factory
         * @throws MessageValidationException   When the factory can not successfully initialized using the given
         *                                      parameters.
         * @see     IUserMessageFlow#getCustomValidationConfiguration()
         * @see     IMessageValidatorConfiguration
         */
        void init(final Map<String, ?> parameters) throws MessageValidationException;

        /**
         * Gets a validator that can be used to perform the custom validation of a user message.
         *
         * @return  The validator to use
         * @throws MessageValidationException   When the factory can not provide a validator instance ready for use.
         */
       IMessageValidator createMessageValidator() throws MessageValidationException;
    }
}

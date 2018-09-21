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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.ebms3.handlers.inflow.HeaderValidation;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is an implementation of {@link IMessageValidationSpecification} tailored for the validation of the ebMS header of
 * message units. It contains a set of predefined specifications for both the lax and strict validation of each message
 * unit type.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class HeaderValidationSpecification implements IMessageValidationSpecification {
    /**
     * Maps holding the singletons of both the lax and strict header validator configs structured per message unit type
     */
    private static final Map<Class<? extends IMessageUnit>, HeaderValidationSpecification>   laxValidatorSpecs;
    private static final Map<Class<? extends IMessageUnit>, HeaderValidationSpecification>   strictValidatorSpecs;
    static {
        // Create the validator instances
        //
        laxValidatorSpecs = new HashMap<>();
        laxValidatorSpecs.put(IUserMessage.class, new HeaderValidationSpecification(IUserMessage.class, false));
        laxValidatorSpecs.put(IPullRequest.class, new HeaderValidationSpecification(IPullRequest.class, false));
        laxValidatorSpecs.put(IReceipt.class, new HeaderValidationSpecification(IReceipt.class, false));
        laxValidatorSpecs.put(IErrorMessage.class, new HeaderValidationSpecification(IErrorMessage.class, false));
        strictValidatorSpecs = new HashMap<>();
        strictValidatorSpecs.put(IUserMessage.class, new HeaderValidationSpecification(IUserMessage.class, true));
        strictValidatorSpecs.put(IPullRequest.class, new HeaderValidationSpecification(IPullRequest.class, true));
        strictValidatorSpecs.put(IReceipt.class, new HeaderValidationSpecification(IReceipt.class, true));
        strictValidatorSpecs.put(IErrorMessage.class, new HeaderValidationSpecification(IErrorMessage.class, true));
    }

    /**
     * Get a {@link IMessageValidationSpecification} instance for the header validation of the given message unit and
     * validation mode.
     * <p>The validation spec for the header validation is very simple and only contains one validator specific for the
     * type of message unit. The other parameters are less relevant because these are handled by the {@link
     * HeaderValidation} handler.
     *
     * @param messageUnit           The message unit to validate
     * @param useStrictValidation   Indicates whether strict validation should be applied
     * @return                      A {@link IMessageValidationSpecification} for the validation of the ebMS header of
     *                              this message unit
     */
    public static IMessageValidationSpecification getValidationSpec(final IMessageUnit messageUnit,
                                                             final boolean useStrictValidation) {
        // Get the correct validation spec depending on the preferred validation mode and message type
        return useStrictValidation ? strictValidatorSpecs.get(MessageUnitUtils.getMessageUnitType(messageUnit))
                                   : laxValidatorSpecs.get(MessageUnitUtils.getMessageUnitType(messageUnit));
    }

    /**
     * The list of validators included in this specification (will always be just one)
     */
    private final List<IMessageValidatorConfiguration> validatorCfg;

    HeaderValidationSpecification(final Class<? extends IMessageUnit> messageUnitType,
                                  final boolean useStrictValidation) {
        validatorCfg = Arrays.asList(new HeaderValidatorConfig[] {
                                                    new HeaderValidatorConfig(messageUnitType, useStrictValidation)});
    }

    @Override
    public List<IMessageValidatorConfiguration> getValidators() {
        return validatorCfg;
    }

    @Override
    public Boolean mustExecuteInOrder() {
        return true;
    }

    @Override
    public MessageValidationError.Severity getStopSeverity() {
        return MessageValidationError.Severity.Failure;
    }

    @Override
    public MessageValidationError.Severity getRejectionSeverity() {
        return MessageValidationError.Severity.Warning;
    }

    /**
     * Is a implementation of {@link IMessageValidatorConfiguration} tailored for creating the correct header validator.
     */
    class HeaderValidatorConfig implements IMessageValidatorConfiguration {
        private final String id;
        private final Map<String, Object> params;

        public HeaderValidatorConfig(Class<? extends IMessageUnit> messageUnitType, boolean useStrictValidation) {
            this.id = messageUnitType.getSimpleName() + "-" + (useStrictValidation ? "strict" : "lax");
            params = new HashMap<>(2);
            params.put(HeaderValidatorFactory.P_MSGUNIT_TYPE, messageUnitType);
            params.put(HeaderValidatorFactory.P_VALIDATION_MODE, useStrictValidation);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getFactory() {
            return HeaderValidatorFactory.class.getName();
        }

        @Override
        public Map<String, ?> getSettings() {
            return params;
        }
    }
}

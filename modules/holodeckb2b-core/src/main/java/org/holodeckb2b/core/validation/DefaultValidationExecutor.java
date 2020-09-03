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
package org.holodeckb2b.core.validation;

import java.util.Collection;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is the default implementation of {@link IValidationExecutor} to perform the validation of a message unit. This
 * implementation is not optimized for performance and just sequentially executes all specified validators for the
 * message unit. There is also no caching of validator factories.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class DefaultValidationExecutor implements IValidationExecutor {

    Logger  log = LogManager.getLogger(DefaultValidationExecutor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationResult validate(IMessageUnit userMessage, IMessageValidationSpecification validationSpec)
                                                                                    throws MessageValidationException {

        if (validationSpec == null || Utils.isNullOrEmpty(validationSpec.getValidators())) {
            log.trace("No custom validation specified in P-Mode, skipping custom validation");
            return null;
        }
        // Execute the validators as specified in the specification and collect found errors
        ValidationResult result = new ValidationResult();
        boolean stopValidation = false;
        boolean rejectMessage = false;
        Iterator<IMessageValidatorConfiguration> validatorsToRun = validationSpec.getValidators().iterator();
        // Execute each validator unless specified otherwise
        do {
            IMessageValidatorConfiguration validatorCfg = validatorsToRun.next();
            IMessageValidator validator = createValidator(validatorCfg);
            log.debug("Validating the message using validator {}", validatorCfg.getId());
            Collection<MessageValidationError>  errors = validator.validate(userMessage);
            if (Utils.isNullOrEmpty(errors))
                log.debug("Messagge successfully validated by [{}]", validatorCfg.getId());
            else {
                log.debug("Validator [{}] found {} errors in message", validatorCfg.getId(), errors.size());
                // Store the errors in result map
                result.addValidationErrors(validatorCfg.getId(), errors);
                // Check if validation should continue
                stopValidation = containsErrorAboveThreshold(validationSpec.getStopSeverity(), errors);
                // And check if message should be rejected
                rejectMessage |= containsErrorAboveThreshold(validationSpec.getRejectionSeverity(), errors);
            }
        } while (!stopValidation && validatorsToRun.hasNext());
        result.setExecutedAllValidators(!stopValidation);
        result.setShouldRejectMessage(rejectMessage);

        if (Utils.isNullOrEmpty(result.getValidationErrors()))
            log.debug("Successfully validated message unit [{}]", userMessage.getMessageId());
        else {
            log.info("Found {} validation errors in message unit [{}]", result.getValidationErrors().size()
                        ,userMessage.getMessageId());
        }
        return result;
    }

    /**
     * Creates an instance of the {@link IMessageValidator} based on the given {@link IMessageValidatorConfiguration}.
     *
     * @param validatorCfg  The message validator configuration
     * @return  A validator instance based on the given configuration
     * @throws MessageValidationException   When there is a problem creating the validator, for example because the
     *                                      specified factory class can not be loaded or initialized.
     */
    private IMessageValidator createValidator(final IMessageValidatorConfiguration validatorCfg)
                                                                                    throws MessageValidationException {
        log.trace("Creating validator [{}]", validatorCfg.getId());
        IMessageValidator.Factory factory = null;
        try {
            String factoryClassname = validatorCfg.getFactory();
            factory = (IMessageValidator.Factory) Class.forName(factoryClassname).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            log.error("Could not create validator factory [{}] due to a ", validatorCfg.getFactory(),
                        ex.getClass().getSimpleName());
            throw new MessageValidationException("Could not create validator factory class ["
                                                  + validatorCfg.getFactory() + "]!", ex);
        }
        log.trace("Initialize the factory");
        factory.init(validatorCfg.getSettings());
        log.trace("And create a validator");
        return factory.createMessageValidator();
    }

    /**
     * Determines whether one of the given errors has a severity level equal to or higher than the given treshold.
     *
     * @param threshold  The treshold value
     * @param errors    The found errors
     * @return          <code>true</code> when one of the errors has a severity level equal or higher than the trehsold,
     *                  ,<br><code>false</code> otherwise
     */
    private boolean containsErrorAboveThreshold(final MessageValidationError.Severity threshold,
                                                final Collection<MessageValidationError> errors) {
        if (threshold == null)
            // no threshold specified, which indicates that not in use
            return false;
        else {
            // A threshold has been defined, check if reached by any of the found errors
            boolean stop = false;
            for(MessageValidationError e : errors)
                stop = threshold.compareTo(e.getSeverityLevel()) <= 0;
            return stop;
        }
    }
}

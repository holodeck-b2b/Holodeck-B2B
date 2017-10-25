package org.holodeckb2b.customvalidation.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.holodeckb2b.interfaces.customvalidation.*;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.*;

/**
 * Created at 16:13 25.06.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CustomValidator implements IMessageValidator<IUserMessage> {
    /**
     * Validates the given <i>User Message</i> message unit.
     *
     * @param userMessage The User Message that must be validated.
     * @return A Collection of {@link MessageValidationError}s when there
     * are validation errors.<br>
     * When no problems were detected an empty Collection or <code>null</code>
     * @throws MessageValidationException When the validator can not complete
     * the validation of the message unit
     */
    @Override
    public Collection<MessageValidationError> validate(IUserMessage userMessage)
            throws MessageValidationException {
        Collection<MessageValidationError> errors = new ArrayList<>();
        errors.add(new MessageValidationError("Some error for testing.",
                MessageValidationError.Severity.Failure));
        return errors;
    }

    /**
     *
     */
    public static class Factory implements IMessageValidator.Factory {

        /**
         * Initializes the message validator factory with the parameters
         * as provided in the P-Mode.
         *
         * @param parameters The parameters for initialization of the factory
         * @throws MessageValidationException When the factory can not
         * successfully initialized using the given parameters.
         * @see IUserMessageFlow#getCustomValidationConfiguration()
         * @see IMessageValidatorConfiguration
         */
        @Override
        public void init(Map<String, ?> parameters) throws MessageValidationException {

        }

        /**
         * Gets a validator that can be used to perform the custom validation
         * of a user message.
         *
         * @return The validator to use
         * @throws MessageValidationException When the factory can not provide
         * a validator instance ready for use.
         */
        @Override
        public IMessageValidator createMessageValidator()
                throws MessageValidationException {
            return new CustomValidator();
        }
    }
}

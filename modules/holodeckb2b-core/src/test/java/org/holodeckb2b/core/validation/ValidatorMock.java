package org.holodeckb2b.core.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidator;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError.Severity;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationException;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Created at 16:13 25.06.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ValidatorMock implements IMessageValidator<IUserMessage> {

	static final String SEVERITY = "errorLevel";
	
	private Severity errorLevel;
	
	private ValidatorMock(Severity toGenerate) {
		this.errorLevel = toGenerate;
	}
	
	@Override
    public Collection<MessageValidationError> validate(IUserMessage userMessage)
            throws MessageValidationException {
		Collection<MessageValidationError> errors = new ArrayList<>();
        
		if (errorLevel != null)
			errors.add(new MessageValidationError("Some error for testing.", errorLevel));
		
        return errors;
    }

    /**
     *
     */
    public static class Factory implements IMessageValidator.Factory {
    	
    	private Severity errorLevel;
    	
        @Override
        public void init(Map<String, ?> parameters) throws MessageValidationException {
        	String levelString = (String) parameters.get(SEVERITY);
        	errorLevel = Utils.isNullOrEmpty(levelString) ? null : Severity.valueOf(levelString);
        }

        @Override
        public IMessageValidator createMessageValidator() throws MessageValidationException {
            return new ValidatorMock(errorLevel);
        }
    }
}

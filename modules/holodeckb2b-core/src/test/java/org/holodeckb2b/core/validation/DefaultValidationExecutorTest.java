package org.holodeckb2b.core.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.CustomValidationConfiguration;
import org.holodeckb2b.common.pmode.MessageValidatorConfiguration;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError.Severity;
import org.junit.Test;

/**
 * Created at 17:14 24.06.17
 *
 * todo complete the test
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class DefaultValidationExecutorTest {

    @Test
    public void testValid() throws Exception {
    	
    	CustomValidationConfiguration validationSpec = new CustomValidationConfiguration();
        validationSpec.setStopSeverity(Severity.Info);
        validationSpec.setRejectSeverity(Severity.Info);
        
        MessageValidatorConfiguration validator = new MessageValidatorConfiguration();
        validator.setFactory(ValidatorMock.Factory.class.getName());
        validator.addSetting(ValidatorMock.SEVERITY, "");        
        validationSpec.addValidator(validator);
        
        ValidationResult result = new DefaultValidationExecutor().validate(new UserMessage(), validationSpec);
               
        assertTrue(result.executedAllValidators());
        assertTrue(Utils.isNullOrEmpty(result.getValidationErrors()));
    }
    
    @Test
    public void testStop() throws Exception {
    	CustomValidationConfiguration validationSpec = new CustomValidationConfiguration();
    	validationSpec.setStopSeverity(Severity.Info);
    	validationSpec.setRejectSeverity(Severity.Warning);
    	
    	MessageValidatorConfiguration validator = new MessageValidatorConfiguration();
    	validator.setId("stoppingValidator");
    	validator.setFactory(ValidatorMock.Factory.class.getName());
    	validator.addSetting(ValidatorMock.SEVERITY, Severity.Info);        
    	validationSpec.addValidator(validator);
    	validator = new MessageValidatorConfiguration();
    	validator.setId("notExecutedValidator");
    	validator.setFactory(ValidatorMock.Factory.class.getName());
    	validator.addSetting(ValidatorMock.SEVERITY, Severity.Warning);        
    	validationSpec.addValidator(validator);
    	
    	ValidationResult result = new DefaultValidationExecutor().validate(new UserMessage(), validationSpec);
    	
    	assertFalse(result.executedAllValidators());
    	assertFalse(result.shouldRejectMessage());
    	assertFalse(Utils.isNullOrEmpty(result.getValidationErrors()));
    	assertNotNull(result.getValidationErrors().get("stoppingValidator"));
    	assertNull(result.getValidationErrors().get("notExecutedValidator"));    	
    }

    @Test
    public void testRejection() throws Exception {
    	CustomValidationConfiguration validationSpec = new CustomValidationConfiguration();
    	validationSpec.setStopSeverity(Severity.Failure);
    	validationSpec.setRejectSeverity(Severity.Warning);
    	
    	MessageValidatorConfiguration validator = new MessageValidatorConfiguration();
    	validator.setId("infoValidator");
    	validator.setFactory(ValidatorMock.Factory.class.getName());
    	validator.addSetting(ValidatorMock.SEVERITY, Severity.Info);        
    	validationSpec.addValidator(validator);
    	validator = new MessageValidatorConfiguration();
    	validator.setId("warningValidator");
    	validator.setFactory(ValidatorMock.Factory.class.getName());
    	validator.addSetting(ValidatorMock.SEVERITY, Severity.Warning);        
    	validationSpec.addValidator(validator);
    	
    	ValidationResult result = new DefaultValidationExecutor().validate(new UserMessage(), validationSpec);
    	
    	assertTrue(result.executedAllValidators());
    	assertTrue(result.shouldRejectMessage());
    	assertFalse(Utils.isNullOrEmpty(result.getValidationErrors()));
    	assertNotNull(result.getValidationErrors().get("infoValidator"));
    	assertNotNull(result.getValidationErrors().get("warningValidator"));    	
    }
    
    
}
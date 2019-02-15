package org.holodeckb2b.common.testhelpers.pmode;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;

import java.util.List;

/**
 * Created at 14:30 25.06.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CustomValidationSpec implements IMessageValidationSpecification {

    private Boolean executeInOrder = Boolean.FALSE;
    private String  stopValidationOn;
    private String  rejectMessageOn;
    private List<IMessageValidatorConfiguration> validators;

    public CustomValidationSpec(String stopValidationOn, String rejectMessageOn) {
        this.stopValidationOn = stopValidationOn;
        this.rejectMessageOn = rejectMessageOn;
    }

    @Override
    public List<IMessageValidatorConfiguration> getValidators() {
        return validators;
    }

    public void setValidators(List<IMessageValidatorConfiguration> validators) {
        this.validators = validators;
    }

    @Override
    public Boolean mustExecuteInOrder() {
        return executeInOrder;
    }

    @Override
    public MessageValidationError.Severity getStopSeverity() {
        return convertToLevel(stopValidationOn);
    }

    @Override
    public MessageValidationError.Severity getRejectionSeverity() {
        return convertToLevel(rejectMessageOn);
    }

    private MessageValidationError.Severity convertToLevel(final String tresholdString) {
        MessageValidationError.Severity level;

        if (Utils.isNullOrEmpty(tresholdString))
            level = null;
        else
            switch (tresholdString.toUpperCase()) {
                case "INFO" :
                    level = MessageValidationError.Severity.Info; break;
                case "WARN" :
                    level = MessageValidationError.Severity.Warning; break;
                case "FAILURE" :
                    level = MessageValidationError.Severity.Failure; break;
                default:
                    level = null;
            }
        return level;
    }
}

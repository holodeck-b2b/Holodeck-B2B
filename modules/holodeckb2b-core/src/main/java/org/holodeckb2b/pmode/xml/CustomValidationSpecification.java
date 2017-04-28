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
package org.holodeckb2b.pmode.xml;

import java.util.List;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Represents the <code>CustomValidation</code> XML element in the P-Mode document which is used to specify the custom
 * validation of message units.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class CustomValidationSpecification implements IMessageValidationSpecification {

    @Element (name = "ExecuteInOrder", required = false)
    Boolean executeInOrder = Boolean.FALSE;

    @Element (name = "StopValidationOn", required = false)
    String  stopValidationOn;

    @Element (name = "RejectMessageOn", required = false)
    String  rejectMessageOn;

    @ElementList(entry = "Validator", inline = true, type = ValidatorConfiguration.class)
    private List<IMessageValidatorConfiguration>    validators;

    @Override
    public List<IMessageValidatorConfiguration> getValidators() {
        return validators;
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

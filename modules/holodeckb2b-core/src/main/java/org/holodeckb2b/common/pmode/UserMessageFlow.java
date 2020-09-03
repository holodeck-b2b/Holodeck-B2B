/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import java.io.Serializable;

import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.pmode.IBusinessInfo;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.IPayloadProfile;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.simpleframework.xml.Element;

/**
 * Contains the parameters related to the processing of <i>User Message</i> message units.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class UserMessageFlow implements IUserMessageFlow, Serializable {
	private static final long serialVersionUID = 8319891949364207250L;
	
    @Element (name = "BusinessInfo", required = false)
    private BusinessInfo businessInfo;

    @Element (name = "ErrorHandling", required = false)
    private ErrorHandlingConfig errorHandling;

    @Element (name = "PayloadProfile", required = false)
    private PayloadProfile payloadProfile;

    @Element (name = "CustomValidation", required = false)
    private CustomValidationConfiguration customValidations;

    /**
     * Default constructor creates a new and empty <code>UserMessageFlow</code> instance.
     */
    public UserMessageFlow() {}

    /**
     * Creates a new <code>UserMessageFlow</code> instance using the parameters from the provided {@link
     * IUserMessageFlow} object.
     *
     * @param source The source object to copy the parameters from
     */
    public UserMessageFlow(final IUserMessageFlow source) {
        this.businessInfo = source.getBusinessInfo() != null ? new BusinessInfo(source.getBusinessInfo()) : null;
        this.payloadProfile = source.getPayloadProfile() != null ? new PayloadProfile(source.getPayloadProfile()) : null;
        this.errorHandling = source.getErrorHandlingConfiguration() != null ?
        										new ErrorHandlingConfig(source.getErrorHandlingConfiguration()) : null;
        this.customValidations = source.getCustomValidationConfiguration() != null ?
        							new CustomValidationConfiguration(source.getCustomValidationConfiguration()) : null;
    }

    @Override
    public IBusinessInfo getBusinessInfo() {
        return businessInfo;
    }

    public void setBusinnessInfo(final BusinessInfo busInfo) {
        this.businessInfo = busInfo;
    }

    @Override
    public IPayloadProfile getPayloadProfile() {
        return payloadProfile;
    }

    public void setPayloadProfile(final PayloadProfile payloadProfile) {
        this.payloadProfile = payloadProfile;
    }

    @Override
    public IErrorHandling getErrorHandlingConfiguration() {
        return errorHandling;
    }

    public void setErrorHandlingConfiguration(final ErrorHandlingConfig errorHandlingConfig) {
        this.errorHandling = errorHandlingConfig;
    }

    @Override
    public IMessageValidationSpecification getCustomValidationConfiguration() {
        return customValidations;
    }

    public void setCustomValidationConfiguration(final CustomValidationConfiguration customValidationCfg) {
        this.customValidations = customValidationCfg;
    }
}

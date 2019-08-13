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
import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.convert.Convert;

/**
 * Contains the parameters related to the custom validation that should be
 * applied to <i>User Message</i> message units exchanged using this P-Mode.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class CustomValidationConfiguration implements IMessageValidationSpecification, Serializable {
	private static final long serialVersionUID = -4301408795922706403L;

	@Element(name = "ExecuteInOrder", required = false)
	Boolean executeInOrder = Boolean.FALSE;

	@Element(name = "StopValidationOn", required = false)
	@Convert(ValidationLevelConverter.class)
	MessageValidationError.Severity stopValidationOn;

	@Element(name = "RejectMessageOn", required = false)
	@Convert(ValidationLevelConverter.class)
	MessageValidationError.Severity rejectMessageOn;

	@ElementList(entry = "Validator", inline = true, type = MessageValidatorConfiguration.class)
	private List<IMessageValidatorConfiguration> validatorConfigs;

	/**
	 * Default constructor creates a new and empty
	 * <code>CustomValidationConfiguration</code> instance.
	 */
	public CustomValidationConfiguration() {
	}

	/**
	 * Creates a new <code>CustomValidationConfiguration</code> instance using the
	 * parameters from the provided {@link IMessageValidationSpecification} object.
	 *
	 * @param source The source object to copy the parameters from
	 */
	public CustomValidationConfiguration(final IMessageValidationSpecification source) {
		this.executeInOrder = source.mustExecuteInOrder();
		this.stopValidationOn = source.getStopSeverity();
		this.rejectMessageOn = source.getRejectionSeverity();
		List<IMessageValidatorConfiguration> srcValidatorCfgs = source.getValidators();
		if (!Utils.isNullOrEmpty(srcValidatorCfgs)) {
			this.validatorConfigs = new ArrayList<>(srcValidatorCfgs.size());
			srcValidatorCfgs.forEach(v -> this.validatorConfigs.add(new MessageValidatorConfiguration(v)));
		}
	}

	@Override
	public List<IMessageValidatorConfiguration> getValidators() {
		return validatorConfigs;
	}

	public void setValidators(final List<MessageValidatorConfiguration> validators) {
		if (validators != null) {
			this.validatorConfigs = new ArrayList<>(validators.size());
			validators.forEach(v -> this.validatorConfigs.add(v));
		} else
			this.validatorConfigs = null;
	}

	public void addValidator(final MessageValidatorConfiguration validator) {
		if (this.validatorConfigs == null)
			this.validatorConfigs = new ArrayList<>();

		this.validatorConfigs.add(validator);
	}

	@Override
	public Boolean mustExecuteInOrder() {
		return executeInOrder;
	}

	public void setExecuteInOrder(final Boolean inOrder) {
		this.executeInOrder = inOrder;
	}

	@Override
	public MessageValidationError.Severity getStopSeverity() {
		return stopValidationOn;
	}

	public void setStopSeverity(final MessageValidationError.Severity stopSeverity) {
		this.stopValidationOn = stopSeverity;
	}

	@Override
	public MessageValidationError.Severity getRejectionSeverity() {
		return rejectMessageOn;
	}

	public void setRejectSeverity(final MessageValidationError.Severity rejectSeverity) {
		this.rejectMessageOn = rejectSeverity;
	}
}

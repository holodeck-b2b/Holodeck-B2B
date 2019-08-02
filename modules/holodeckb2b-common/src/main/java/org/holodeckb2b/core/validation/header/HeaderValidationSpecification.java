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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.holodeckb2b.interfaces.customvalidation.IMessageValidationSpecification;
import org.holodeckb2b.interfaces.customvalidation.IMessageValidatorConfiguration;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is an implementation of {@link IMessageValidationSpecification} preconfigured for the validation of the headers 
 * of message units. The only setting that needs to be set is the {@link AbstractHeaderValidatorFactory} 
 * implementation that must be used for creating the actual validators. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
class HeaderValidationSpecification implements IMessageValidationSpecification {

	/**
	 * The validator included in this specification 
	 */
	private final IMessageValidatorConfiguration validatorCfg;

	HeaderValidationSpecification(final String  factory,
								  final Class<? extends IMessageUnit> messageUnitType,
								  final boolean useStrictValidation) {
		validatorCfg = new HeaderValidatorConfig(factory, messageUnitType, useStrictValidation);
	}

	@Override
	public List<IMessageValidatorConfiguration> getValidators() {
		return Collections.singletonList(validatorCfg);
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
	 * Is a implementation of {@link IMessageValidatorConfiguration} tailored for creating the correct header 
	 * validator.
	 */
	class HeaderValidatorConfig implements IMessageValidatorConfiguration {
		private final String id;
		private final String factoryClass;
		private final Map<String, Object> params;

		public HeaderValidatorConfig(final String factory,
									 final Class<? extends IMessageUnit> messageUnitType,
									 boolean useStrictValidation) {
			this.id = messageUnitType.getSimpleName() + "-" + (useStrictValidation ? "strict" : "lax");
			this.factoryClass = factory;
			params = new HashMap<>(2);
			params.put(AbstractHeaderValidatorFactory.P_MSGUNIT_TYPE, messageUnitType);
			params.put(AbstractHeaderValidatorFactory.P_VALIDATION_MODE, useStrictValidation);
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getFactory() {
			return factoryClass;
		}

		@Override
		public Map<String, ?> getSettings() {
			return params;
		}
	}
}

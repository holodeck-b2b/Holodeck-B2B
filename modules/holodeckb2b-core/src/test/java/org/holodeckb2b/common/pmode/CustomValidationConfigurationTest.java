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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError.Severity;
import org.junit.Test;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class CustomValidationConfigurationTest extends AbstractBaseTest<CustomValidationConfiguration> {

	@Test
	public void readComplete() {
		final CustomValidationConfiguration cvc = createObject(
				"<CustomValidation  xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"	<ExecuteInOrder>true</ExecuteInOrder>" +
				"	<StopValidationOn>FAILURE</StopValidationOn>" +
				"	<RejectMessageOn>WARN</RejectMessageOn>" +
				"	<Validator>" +
				"		<id>validator-1</id>" +
				"		<ValidatorFactoryClass>org.holodeckb2b.test.FakeValidator</ValidatorFactoryClass>" +
				"	</Validator>" +
				"	<Validator>" +
				"		<id>validator-2</id>" +
				"		<ValidatorFactoryClass>org.holodeckb2b.test.NOPValidator</ValidatorFactoryClass>" +
				"	</Validator>" +
				"</CustomValidation>");
		
		assertNotNull(cvc);
		assertTrue(cvc.mustExecuteInOrder());
		assertEquals(MessageValidationError.Severity.Failure, cvc.getStopSeverity());
		assertEquals(MessageValidationError.Severity.Warning, cvc.getRejectionSeverity());
		assertFalse(Utils.isNullOrEmpty(cvc.getValidators()));
		assertTrue(cvc.getValidators().size() == 2);
	}
	
	@Test
	public void readDefaults() {
		final CustomValidationConfiguration cvc = createObject(
				"<CustomValidation  xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"	<Validator>" +
				"		<id>validator-1</id>" +
				"		<ValidatorFactoryClass>org.holodeckb2b.test.FakeValidator</ValidatorFactoryClass>" +
				"	</Validator>" +
				"</CustomValidation>");
		
		assertNotNull(cvc);
		assertFalse(cvc.mustExecuteInOrder());
		assertNull(cvc.getStopSeverity());
		assertNull(cvc.getRejectionSeverity());		
	}

	@Test
	public void readNoValidator() {
		final CustomValidationConfiguration cvc = createObject(
				"<CustomValidation  xmlns:tns=\"http://holodeck-b2b.org/schemas/2014/10/pmode\"" + 
		        " 					xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"	<ExecuteInOrder>true</ExecuteInOrder>" +
				"	<StopValidationOn>FAILURE</StopValidationOn>" +
				"	<RejectMessageOn>WARN</RejectMessageOn>" +						
				"</CustomValidation>");
		
		assertNull(cvc);
	}	
	
	@Test
	public void writeComplete() {
		final CustomValidationConfiguration cvc = new CustomValidationConfiguration();
		cvc.setExecuteInOrder(Boolean.TRUE);
		cvc.setStopSeverity(Severity.Failure);
		cvc.setRejectSeverity(Severity.Info);
		final MessageValidatorConfiguration validator = new MessageValidatorConfiguration();
		validator.setId("validator-1");
		validator.setFactory("org.holodeckb2b.test.NotReal");
		cvc.addValidator(validator);
		
		final String xml = createXML(cvc);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "ExecuteInOrder") == 1);
		assertEquals("true", findElements(xml, "ExecuteInOrder").get(0).getTextContent());
		assertTrue(countElements(xml, "StopValidationOn") == 1);
		assertEquals("FAILURE", findElements(xml, "StopValidationOn").get(0).getTextContent());
		assertTrue(countElements(xml, "RejectMessageOn") == 1);
		assertEquals("INFO", findElements(xml, "RejectMessageOn").get(0).getTextContent());
		assertTrue(countElements(xml, "Validator") == 1);		
	}	

	@Test
	public void writeDefaults() {
		final CustomValidationConfiguration cvc = new CustomValidationConfiguration();
		final MessageValidatorConfiguration validator = new MessageValidatorConfiguration();
		validator.setId("validator-1");
		validator.setFactory("org.holodeckb2b.test.NotReal");
		cvc.addValidator(validator);
		
		final String xml = createXML(cvc);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "ExecuteInOrder") == 1);
		assertEquals("false", findElements(xml, "ExecuteInOrder").get(0).getTextContent());
		assertTrue(countElements(xml, "StopValidationOn") == 0);
		assertTrue(countElements(xml, "RejectMessageOn") == 0);
		assertTrue(countElements(xml, "Validator") == 1);		
	}	

	@Test
	public void writeNoValidator() {
		final CustomValidationConfiguration cvc = new CustomValidationConfiguration();
		cvc.setExecuteInOrder(Boolean.TRUE);
		cvc.setStopSeverity(Severity.Failure);
		cvc.setRejectSeverity(Severity.Info);
		
		final String xml = createXML(cvc);
		
		assertTrue(Utils.isNullOrEmpty(xml));
	}	

	@Test
	public void writeMultipleValidators() {
		final CustomValidationConfiguration cvc = new CustomValidationConfiguration();
		final MessageValidatorConfiguration validator = new MessageValidatorConfiguration();
		validator.setId("validator-1");
		validator.setFactory("org.holodeckb2b.test.NotReal");
		cvc.addValidator(validator);
		final MessageValidatorConfiguration validator2 = new MessageValidatorConfiguration();
		validator2.setId("validator-2");
		validator2.setFactory("org.holodeckb2b.test.AlwaysGood");
		cvc.addValidator(validator2);
		
		final String xml = createXML(cvc);
		
		assertFalse(Utils.isNullOrEmpty(xml));
		assertTrue(countElements(xml, "Validator") == 2);		
	}
	
	@Test
	public void testCopy() {
		final CustomValidationConfiguration source = new CustomValidationConfiguration();
		source.setExecuteInOrder(Boolean.TRUE);
		source.setStopSeverity(Severity.Failure);
		source.setRejectSeverity(Severity.Info);
		final MessageValidatorConfiguration validator = new MessageValidatorConfiguration();
		validator.setId("validator-1");
		validator.setFactory("org.holodeckb2b.test.NotReal");
		source.addValidator(validator);
		final MessageValidatorConfiguration validator2 = new MessageValidatorConfiguration();
		validator2.setId("validator-2");
		validator2.setFactory("org.holodeckb2b.test.AlwaysGood");
		source.addValidator(validator2);

		final CustomValidationConfiguration copy = new CustomValidationConfiguration(source);
		
		assertEquals(source.mustExecuteInOrder(), copy.mustExecuteInOrder());
		assertEquals(source.getStopSeverity(), copy.getStopSeverity());
		assertEquals(source.getRejectionSeverity(), copy.getRejectionSeverity());
		assertFalse(Utils.isNullOrEmpty(copy.getValidators()));
		assertEquals(source.getValidators().size(), copy.getValidators().size());		
	}	
	
}

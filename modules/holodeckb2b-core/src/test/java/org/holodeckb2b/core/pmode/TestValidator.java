package org.holodeckb2b.core.pmode;

import java.util.Collection;
import java.util.Collections;

import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;

public class TestValidator implements IPModeValidator {

	@Override
	public Collection<PModeValidationError> validatePMode(IPMode pmode) {
		if (pmode.getAgreement() != null)
			return Collections.singletonList(new PModeValidationError("Agreement", "not allowed"));
		else
			return null;
	}    
}
package org.holodeckb2b.core.pmode;

import java.util.Collection;
import java.util.Collections;

import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;

public abstract class AbstractTestValidator implements IPModeValidator {

	private boolean isExecuted = false;
	
	protected abstract boolean shouldReject();
	
	public boolean isExecuted() {
		return isExecuted;
	}
	
	@Override
	public Collection<PModeValidationError> validatePMode(IPMode pmode) {
		isExecuted = true;
		if (shouldReject())
			return Collections.singletonList(new PModeValidationError("Agreement", "Nothing allowed"));
		else
			return null;
	}

	@Override
	public boolean doesValidate(String pmodeType) {
		return true;
	}    
}
package org.holodeckb2b.core.pmode;

import java.util.Collection;

import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;

public class TestValidator2 implements IPModeValidator {

	@Override
	public Collection<PModeValidationError> validatePMode(IPMode pmode) {
		return null;
	}

	@Override
	public String getName() {
		return "SecondValidator";
	}
	
	@Override
	public void init(String hb2bHomeDir) throws PModeSetException {
	}

	@Override
	public boolean doesValidate(String pmodeType) {
		return true;
	}    
}
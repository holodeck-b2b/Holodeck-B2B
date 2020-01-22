package org.holodeckb2b.core.pmode;

import java.util.Collection;

import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;

public class TestValidatorAllGood extends AbstractTestValidator {

	@Override
	protected boolean shouldReject() {
		return false;
	}

    
}
package org.holodeckb2b.core.pmode;

import org.holodeckb2b.interfaces.pmode.IPMode;

public class TestValidatorAllGood extends AbstractTestValidator {


	@Override
	protected boolean shouldReject(IPMode pmode) {
		return false;
	}


}
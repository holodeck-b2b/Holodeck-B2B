package org.holodeckb2b.core.pmode;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.IPMode;

public class TestValidatorRejectPull extends AbstractTestValidator {

	@Override
	protected boolean shouldReject(IPMode pmode) {
		return !Utils.isNullOrEmpty(pmode.getMepBinding()) && EbMSConstants.ONE_WAY_PULL.equals(pmode.getMepBinding());
	}


}
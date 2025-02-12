package org.holodeckb2b.core.pmode;

import java.util.Collection;
import java.util.Collections;

import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.validation.IPModeValidator;
import org.holodeckb2b.interfaces.pmode.validation.PModeValidationError;

public abstract class AbstractTestValidator implements IPModeValidator {

	private static ThreadLocal<Boolean> isLoaded = ThreadLocal.withInitial(() -> false);
	private static ThreadLocal<Boolean> isExecuted = ThreadLocal.withInitial(() -> false);

	AbstractTestValidator() {
		isLoaded.set(true);
	}

	protected abstract boolean shouldReject(IPMode pmode);

	public static boolean isLoaded() {
		return isLoaded.get();
	}

	public static boolean isExecuted() {
		return isExecuted.get();
	}

	@Override
	public Collection<PModeValidationError> validatePMode(IPMode pmode) {
		isExecuted.set(true);
		if (shouldReject(pmode))
			return Collections.singletonList(new PModeValidationError("Agreement", "Nothing allowed"));
		else
			return null;
	}

	@Override
	public boolean doesValidate(String pmodeType) {
		return true;
	}
}
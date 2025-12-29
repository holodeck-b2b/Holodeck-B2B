/**
 *
 */
package org.holodeckb2b.core.pmode;

import org.holodeckb2b.common.pmode.InMemoryPModeSet;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.pmode.PModeSetException;

/**
 * Extends the {@link InMemoryPModeSet} class for testing with added fail option on initialisation and check on whether
 * the class is loaded as storage implementation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class TestPModeStorage extends InMemoryPModeSet {
	public static boolean failOnInit = false;

	private static ThreadLocal<Boolean> isLoaded = ThreadLocal.withInitial(() -> false);

	@Override
	public void init(IConfiguration config) throws PModeSetException {
		if (failOnInit)
			throw new PModeSetException();
		isLoaded.set(true);
	}

	public static boolean isLoaded() {
		return isLoaded.get();
	}
}

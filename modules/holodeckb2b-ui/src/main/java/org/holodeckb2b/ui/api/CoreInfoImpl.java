package org.holodeckb2b.ui.api;

import java.rmi.RemoteException;
import java.util.Collection;

import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * Implements the {@link CoreInfo} interface to supply the UI app with information from the Holodeck B2B instance. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class CoreInfoImpl implements CoreInfo {
	
	@Override
	public PMode[] getPModes() throws RemoteException {
		Collection<IPMode> pmodes = HolodeckB2BCoreInterface.getPModeSet().getAll();
		PMode[] pmodeArray = new PMode[pmodes.size()];
		int i = 0;
		for(IPMode p : pmodes)
			pmodeArray[i++] = new PMode(p);
		return pmodeArray;
	}

	@Override
	public String getHostName() throws RemoteException {
		return HolodeckB2BCoreInterface.getConfiguration().getHostName();
	}
	
	
}

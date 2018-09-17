/**
 * 
 */
package org.holodeckb2b.common.testhelpers;

import java.util.Collection;
import java.util.HashMap;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.PModeSetException;

/**
 * A simple implementation of {@link IPModeSet} that uses a hash map to store the P-Modes.
 * 
 * @author Sander Fieten (sander at chasquis-consulting.com)
 */
public class SimplePModeSet implements IPModeSet {

	private HashMap<String, IPMode>		pmodes = new HashMap<>();
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.pmode.IPModeSet#get(java.lang.String)
	 */
	@Override
	public IPMode get(String id) {
		return pmodes.get(id);
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.pmode.IPModeSet#getAll()
	 */
	@Override
	public Collection<IPMode> getAll() {
		return pmodes.values();
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.pmode.IPModeSet#containsId(java.lang.String)
	 */
	@Override
	public boolean containsId(String id) {
		return pmodes.containsKey(id);
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.pmode.IPModeSet#add(org.holodeckb2b.interfaces.pmode.IPMode)
	 */
	@Override
	public String add(IPMode pmode) throws PModeSetException {
		if (Utils.isNullOrEmpty(pmode.getId()))
			throw new UnsupportedOperationException("Setting of P-Mode.id not supported yet");
		if (pmodes.containsKey(pmode.getId()))
			throw new PModeSetException("Already added");
		pmodes.put(pmode.getId(), pmode);
		return pmode.getId();
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.pmode.IPModeSet#replace(org.holodeckb2b.interfaces.pmode.IPMode)
	 */
	@Override
	public void replace(IPMode pmode) throws PModeSetException {
		if (!pmodes.containsKey(pmode.getId()))
			throw new PModeSetException("P-Mode does not exist");
		pmodes.put(pmode.getId(), pmode);		
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.pmode.IPModeSet#remove(java.lang.String)
	 */
	@Override
	public void remove(String id) throws PModeSetException {
		pmodes.remove(id);
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.interfaces.pmode.IPModeSet#removeAll()
	 */
	@Override
	public void removeAll() throws PModeSetException {
		pmodes.clear();
	}
}

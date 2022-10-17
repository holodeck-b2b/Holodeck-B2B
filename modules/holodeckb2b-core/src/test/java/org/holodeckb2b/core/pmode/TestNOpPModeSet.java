/**
 * 
 */
package org.holodeckb2b.core.pmode;

import java.util.Collection;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.PModeSetException;

/**
 * A implementation of {@link IPModeSet} to test loading of P-Mode set storage implementations. 
 * <p>DOES NOT PROVIDE THE ACTUAL FUNCTIONALITY OF STORING P-MODES! 
 * 
 * @author Sander Fieten (sander at chasquis-consulting.com)
 */
public class TestNOpPModeSet implements IPModeSet {
	
	public static boolean failOnInit = false;
	
	@Override
	public void init(IConfiguration config) throws PModeSetException {		
		if (failOnInit)
			throw new PModeSetException();
	}
	
	@Override
	public void shutdown() {
		// TODO Auto-generated method stub		
	}
	
	@Override
	public IPMode get(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<IPMode> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsId(String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String add(IPMode pmode) throws PModeSetException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void replace(IPMode pmode) throws PModeSetException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(String id) throws PModeSetException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAll() throws PModeSetException {
		// TODO Auto-generated method stub
		
	}


}

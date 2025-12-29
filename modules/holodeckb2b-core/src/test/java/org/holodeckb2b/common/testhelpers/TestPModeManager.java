/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.common.testhelpers;

import org.holodeckb2b.common.pmode.InMemoryPModeSet;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.IPModeSetListener;
import org.holodeckb2b.interfaces.pmode.PModeSetEvent.PModeSetAction;

/**
 * Is a simple implementation of {@link IPModeSet} that stores the P-Modes in memory and does not support event 
 * listeners.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.1.0
 */
public class TestPModeManager extends InMemoryPModeSet implements IPModeSet {

	@Override
	public void registerEventListener(IPModeSetListener listener, PModeSetAction... actions) {
		
	}

	@Override
	public void unregisterEventListener(IPModeSetListener listener, PModeSetAction... actions) {
		
	}

}

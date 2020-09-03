/**
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common;

import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.IVersionInfo;

/**
 * Provides version information about the Holodeck B2B Core based on the Maven project version information. 
 * <p>NOTE: This class replaces the <code>org.holodeckb2b.common.constants.ProductId</code> interface from the previous 
 * versions as this information is now made available to extensions as well through {@link 
 * HolodeckB2BCoreInterface#getVersion()}.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class VersionInfo implements IVersionInfo {

    public static final int majorVersion = ${hb2b.majorVersion};
    public static final int minorVersion = ${hb2b.minorVersion};
    public static final int patchVersion = ${hb2b.incrementalVersion};
    
    public static final String fullVersion = majorVersion + "." + minorVersion + "." + patchVersion;
 
	private final static VersionInfo instance;
	
	static {				
		instance = new VersionInfo(); 
	}

	private VersionInfo() {}
	
	/**
	 * @return the singleton instance.	
	 */
	public static VersionInfo getInstance() {
		return instance;
	}	
	
	@Override
	public String getFullVersion() {
		return fullVersion;
	}
	
	@Override
	public int getMajorVersion() {
		return majorVersion;
	}

	@Override
	public int getMinorVersion() {
		return minorVersion;
	}

	@Override
	public int getPatchVersion() {
		return patchVersion;
	}	
}

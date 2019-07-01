/**
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.general;

/**
 * Represents the version of the Holodeck B2B Core. 
 * <p>Holodeck B2B uses <a href="https://semver.org/>Semantic Versioning</a> to identify its versions. This interface
 * defines access to the different parts of the version number. As we don't use build numbers in Holodeck B2B and pre
 * release info is not relevant for production use these parts are not made available through this interface. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface IVersionInfo {

	/**
	 * @return String containing the complete version number  
	 */
	String getFullVersion();
	
	/**
	 * @return the major version.	
	 */
	int getMajorVersion();

	/**
	 * @return the minor version.	
	 */
	int getMinorVersion();
	
	/**
	 * @return the patch version.	
	 */
	int getPatchVersion();	
}

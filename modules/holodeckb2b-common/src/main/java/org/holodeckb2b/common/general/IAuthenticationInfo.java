/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.general;

/**
 * Represents the authentication info that can be included in a message containing a pull request message and should be 
 * used to authorize the message pulling. Because the format for transferring this info is not specified by the ebMS 
 * specifications this interface can only be very general and only defines a method to check whether two sets of 
 * authentication information are equal, i.e. represent the same entity (trading partner/user).
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IAuthenticationInfo  {
    
    /**
     * Checks whether both <code>IAuthenticationInfo</code> object represent the
     * same entity, i.e. trading partner or user.
     * 
     * @param ai    The other <code>IAuthenticationInfo</code> to compare with
     * @return      <code>true</code> when both object represent the same entity<br>
     *              <code>false</code> otherwise
     */
    public boolean equals(IAuthenticationInfo ai);
}

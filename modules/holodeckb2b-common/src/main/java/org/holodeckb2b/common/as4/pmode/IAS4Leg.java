/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.as4.pmode;

import org.holodeckb2b.common.pmode.ILeg;

/**
 * Is a specialized configuration for a leg that includes the AS4 <i>reception awareness feature</i>. The reception
 * awareness feature defines how the ebMS Receipt signal can be used for reliable messaging. See sections 3.2 and 3.4
 * of the AS4 profile for more information on this feature.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IAS4Leg extends ILeg {
   
    /**
     * Gets the P-Mode parameters for the reception awareness feature. 
     * 
     * @return  An {@link IReceptionAwareness} object containing the reception awareness parameters, or<br>
     *          <code>null</code> if reception awareness is not used on this leg.
     */
    public IReceptionAwareness getReceptionAwareness();
}

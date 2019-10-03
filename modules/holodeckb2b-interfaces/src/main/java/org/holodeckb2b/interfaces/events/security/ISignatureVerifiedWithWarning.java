/*
 * Copyright (C) 2019 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.events.security;

/**
 * Is the <i>message processing event</i> that indicates that the signature of a received message unit has been verified
 * but that there were warnings, most probably due to issues with the trust validation. This event is to inform the 
 * business application (or extensions) about the verified signature, such as the parts of the message that were 
 * included in the signature and the result of trust validation of certificate used to create the signature. These can 
 * be for example be used to create specific evidences.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public interface ISignatureVerifiedWithWarning extends ISignatureVerified {
	
	/**
	 * Gets a descriptive message about the warning(s) that were raised during the verification of the signature. More
	 * details about the warnings can probably be found using the other methods, most likely {@link 
	 * #getTrustValidationResult()} as warnings are mostly trust issues. 
	 * 
	 * @return	Text describing the warning(s) raised during the signature verification.
	 */
	@Override
	String getMessage();
}

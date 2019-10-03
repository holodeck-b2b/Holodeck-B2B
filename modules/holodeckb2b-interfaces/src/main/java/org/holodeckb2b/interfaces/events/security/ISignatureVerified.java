/*
 * Copyright (C) 2018 The Holodeck B2B Team.
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

import java.util.Map;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;

/**
 * Is the <i>message processing event</i> that indicates that the signature of a received message unit has been verified
 * successfully. This event is to inform the business application (or extensions) about the verified signature, such as
 * the parts of the message that were included in the signature and the result of trust validation of certificate used
 * to create the signature. These can be for example be used to create specific evidences.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.1.0
 */
public interface ISignatureVerified extends IMessageProcessingEvent {

	/**
	 * Gets the information on the trust validation executed by the installed {@link ICertificateManager} of the 
	 * certificate that was used for signing the message unit.
	 * <p>Although the validation of trust should always be executed by the Security Provider there may be cases where
	 * no validation occurs. In such cases this method should not return a result. 
	 *   
	 * @return	A {@link IValidationResult} object with the results of the trust validation. <code>null</code> if no 
	 * 			validation has been performed.
	 * @since 5.0.0
	 */
	IValidationResult	getTrustValidationResult();
	
    /**
     * Gets the information on the digest contained in the verified signature for the ebMS header of the message unit
     * that is the <i>subject</i> of this event.
     *
     * @return  A {@link ISignedPartMetadata} object with information on the calculated digest of the ebMS header.
     */
	ISignedPartMetadata getHeaderDigest();

    /**
     * Gets the information on the digests contained in the verified signature for the payloads of the User Message that
     * is the <i>subject</i> of this event.
     * <p>NOTE: This method should only be used when the subject of the event is a User Message.
     *
     * @return  A <code>Map</code> linking the digest meta-data to each payload from the user message.
     */    
	Map<IPayload, ISignedPartMetadata>   getPayloadDigests();
}

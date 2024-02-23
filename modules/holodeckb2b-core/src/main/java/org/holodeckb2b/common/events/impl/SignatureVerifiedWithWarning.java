/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.events.impl;

import java.util.Map;

import org.holodeckb2b.interfaces.events.security.ISignatureVerifiedWithWarning;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;

/**
 * Is the implementation class of {@link ISignatureVerifiedWithWarning} to indicate that a signature for a received User
 * Message is verified but with warnings. The information about the digests and trust validation can only be set when
 * the event is created and not be modified afterwards.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
public class SignatureVerifiedWithWarning extends SignatureVerified implements ISignatureVerifiedWithWarning {

	/**
     * Creates a new <code>SignatureVerifiedWithWarning</code> for the given Signal Message Unit.
     *
     * @param subject        The Signal Message Unit that was signed
     * @param headerDigest   The digest of the ebMS header
     * @param trustCheck	 The results of the certificate trust validation
     */
    public SignatureVerifiedWithWarning(final ISignalMessage subject, final ISignedPartMetadata headerDigest,
    						 final IValidationResult trustCheck) {
        super(subject, headerDigest, trustCheck);
        setMessage(trustCheck.getMessage());
    }

    /**
     * Creates a new <code>SignatureVerified</code> for the given User Message.
     *
     * @param subject         The User Message that was signed
     * @param headerDigest    The digest of the ebMS header
     * @param payloadDigests  The information about the digests for the payloads that were part of the signature
     * @param trustCheck	  The results of the certificate trust validation
     */
    public SignatureVerifiedWithWarning(final IUserMessage subject, final ISignedPartMetadata headerDigest,
                                  final Map<IPayloadEntity, ISignedPartMetadata> payloadDigests,
                                  final IValidationResult trustCheck) {
        super(subject, headerDigest, payloadDigests, trustCheck);
        setMessage(trustCheck.getMessage());
    }
}

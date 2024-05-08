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

import org.holodeckb2b.interfaces.events.security.ISignatureCreated;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;

/**
 * Is the implementation class of {@link ISignatureCreated} to indicate that a signature for a received User
 * Message is successfully verified. The information about the digests can only be set when the event is created and not
 * be modified afterwards.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.1.0
 */
public class SignatureCreated extends AbstractSignatureEvent implements ISignatureCreated {

    /**
     * Creates a new <code>SignatureCreated</code> for the given User Message and payload digests.
     *
     * @param subject   The User Message that was signed
     * @param digests   The information about the digests for the payloads that were part of the signature
     */
    public SignatureCreated(final IUserMessage subject, final Map<IPayloadEntity, ISignedPartMetadata> digests) {
        super(subject, null, digests);
    }

}

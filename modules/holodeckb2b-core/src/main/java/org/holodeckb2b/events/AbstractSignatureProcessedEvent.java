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
package org.holodeckb2b.events;

import java.util.Collections;
import java.util.Map;
import org.holodeckb2b.common.events.AbstractMessageProcessingEvent;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;

/**
 * Is an abstract implementation of a <i>message processing event</i> to indicate successful processing of a signature
 * in a message containing the User Message message unit which the subject of this event. This can either be the
 * creation or verification of the signature security header.
 * <p>Beside the standard event meta-data the event includes information about the digests of the User Message's
 * payloads included in the signature. These can only be set when the event is created and not be modified afterwards.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
abstract class AbstractSignatureProcessedEvent extends AbstractMessageProcessingEvent {

    private final Map<IPayload, ISignedPartMetadata>  digests;

    /**
     * Creates a new <code>SignatureCreatedEvent</code> for the given User Message and calculated payload digests.
     *
     * @param subject   The User Message that was signed
     * @param digests   The information about the digests that were calculated for the payloads contained in the User
     *                  Message
     */
    public AbstractSignatureProcessedEvent(final IUserMessage subject, final Map<IPayload, ISignedPartMetadata> digests)
    {
        super(subject);
        this.digests = (Map<IPayload, ISignedPartMetadata>) Collections.unmodifiableMap(digests);
    }

    /**
     * Gets the information on the digests contained in the processed signature for the payloads of the User Message
     * that is the <i>subject</i> of this event.
     *
     * @return  A <b>unmodifiable</b> <code>Collection</code> of {@link IPayloadDigest} objects with information on the
     *          calculated digests.
     */
    public Map<IPayload, ISignedPartMetadata> getPayloadDigests() {
        return digests;
    }

}

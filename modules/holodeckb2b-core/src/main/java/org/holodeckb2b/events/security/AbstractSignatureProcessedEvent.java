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
package org.holodeckb2b.events.security;

import java.util.Collections;
import java.util.Map;
import org.holodeckb2b.common.events.AbstractMessageProcessingEvent;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;

/**
 * Is an abstract implementation of a <i>message processing event</i> to indicate successful processing of a signature
 * in a message containing the message unit which the subject of this event. This can either be the creation or
 * verification of the signature security header.
 * <p>Beside the standard event meta-data the event includes information about the digests of the ebMS Header and in
 * case of a User Message its payloads included in the signature. These can only be set when the event is created and
 * not be modified afterwards.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0
 */
abstract class AbstractSignatureProcessedEvent extends AbstractMessageProcessingEvent {

    private final ISignedPartMetadata   headerDigest;
    private final Map<IPayload, ISignedPartMetadata>  payloadDigests;

    /**
     * Creates a new <code>SignatureCreatedEvent</code> for the given Signal Message message unit and digest of its ebMS
     * header.
     *
     * @param subject         The message unit that was signed
     * @param headerDigest    The digest of the ebMS header
     */
    public AbstractSignatureProcessedEvent(final ISignalMessage subject, final ISignedPartMetadata headerDigest) {
        super(subject);
        this.headerDigest = headerDigest;
        this.payloadDigests = null;
    }

    /**
     * Creates a new <code>SignatureCreatedEvent</code> for the given User Message and calculated header and payload
     * digests.
     *
     * @param subject         The User Message that was signed
     * @param headerDigest    The digest of the ebMS header
     * @param payloadDigests  The information about the digests that were calculated for the payloads contained
     *                        in the User Message
     */
    public AbstractSignatureProcessedEvent(final IUserMessage subject, final ISignedPartMetadata headerDigest,
                                           final Map<IPayload, ISignedPartMetadata> payloadDigests)
    {
        super(subject);
        this.headerDigest = headerDigest;
        this.payloadDigests = payloadDigests == null ? null :
        							(Map<IPayload, ISignedPartMetadata>) Collections.unmodifiableMap(payloadDigests) ;
    }

    /**
     * Gets the information on the digest contained in the processed signature for the ebMS header of the message unit
     * that is the <i>subject</i> of this event.
     *
     * @return  A {@link ISignedPartMetadata} object with information on the calculated digest of the ebMS header.
     */
    public ISignedPartMetadata getHeaderDigest() {
        return headerDigest;
    }

    /**
     * Gets the information on the digests contained in the processed signature for the payloads of the User Message
     * that is the <i>subject</i> of this event.
     * <p>NOTE: This method should only be used when the subject of the event is a User Message.
     *
     * @return  A <b>unmodifiable</b> <code>Collection</code> of {@link ISignedPartMetadata} objects with information on
     *          the calculated payload digests.
     */
    public Map<IPayload, ISignedPartMetadata> getPayloadDigests() {
        return payloadDigests;
    }

}

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

import java.util.Collection;
import java.util.Collections;

import org.holodeckb2b.common.events.AbstractMessageProcessingEvent;
import org.holodeckb2b.common.security.PayloadDigest;
import org.holodeckb2b.interfaces.events.types.ISignatureCreatedEvent;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.security.IPayloadDigest;

/**
 * Is the implementation class of {@link ISignatureCreatedEvent} to indicate that a signature is created for a User
 * Message to be sent. The information about the digests canonly be set when the event is created and not be modified
 * afterwards.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */
public class SignatureCreatedEvent extends AbstractMessageProcessingEvent implements ISignatureCreatedEvent {

    private final Collection<IPayloadDigest>  digests;

    /**
     * Creates a new <code>SignatureCreatedEvent</code> for the given User Message and calculated payload digests.
     *
     * @param subject   The User Message that was signed
     * @param digests   The information about the digests that were calculated for the payloads contained in the User
     *                  Message
     */
    public SignatureCreatedEvent(final IUserMessage subject, final Collection<PayloadDigest> digests) {
        super(subject);
        this.digests = Collections.unmodifiableCollection((Collection<? extends IPayloadDigest>) digests);
    }

    /**
     * Gets the information on the digests that were calculated for the payloads in the User Message that is the <i>
     * subject</i> of this event.
     *
     * @return  A <b>unmodifiable</b> <code>Collection</code> of {@link IPayloadDigest} objects with information on the
     *          calculated digests.
     */
    @Override
    public Collection<IPayloadDigest> getPayloadDigests() {
        return digests;
    }

}

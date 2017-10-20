/*
 * Copyright (C) 2016 The Holodeck B2B Team.
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
package org.holodeckb2b.interfaces.security;

import java.util.Map;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;

/**
 * Is the <i>message processing event</i> that indicates that a signature of a received <b>User Message Unit</b> has
 * been verified successfully. This event is to inform the business application (or extensions) about the digests part
 * of the message level signature which for example can be used to create evidences.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface ISignatureVerifiedEvent extends IMessageProcessingEvent {

    /**
     * Gets the information on the digests contained in the verified signature for the payloads of the User Message that
     * is the <i>subject</i> of this event.
     *
     * @return  A <code>Map</code> linking the digest meta-data to each payload from the user message.
     */
    public Map<IPayload, ISignedPartMetadata>   getPayloadDigests();
}

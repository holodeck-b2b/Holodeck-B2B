/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.security.util;

import java.util.Map;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.security.ISignedPartMetadata;

/**
 * Container for holding the {@link ISignedPartMetadata} objects related to the ebMS header and payloads contained in a
 * processed message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class SignedMessagePartsInfo {

    private final ISignedPartMetadata ebMSHeaderMetadata;
    private final Map<IPayload, ISignedPartMetadata> payloadMetadata;

    SignedMessagePartsInfo(ISignedPartMetadata ebMSHeader, Map<IPayload, ISignedPartMetadata> payloads) {
        ebMSHeaderMetadata = ebMSHeader;
        payloadMetadata = payloads;
    }

    /**
     * Gets the digest information on the ebMS header.
     *
     * @return {@link ISignedPartMetadata} related to the ebMS header
     */
    public ISignedPartMetadata getEbmsHeaderInfo() {
        return ebMSHeaderMetadata;
    }

    /**
     * Gets the digest information on the payloads included in the message.
     *
     * @return A mapping of {@link ISignedPartMetadata} objects to the payloads in the message
     */
    public Map<IPayload, ISignedPartMetadata> getPayloadInfo() {
        return payloadMetadata;
    }
}

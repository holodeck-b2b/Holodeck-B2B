/*
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

import org.holodeckb2b.common.pmode.IPayloadProfile;

/**
 * Extends the default {@link IPayloadProfile} interface to include setting for the AS4 Compression Feature.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IPayloadProfileAS4 extends IPayloadProfile {

    /**
     * Indicates whether the payload data of user messages should be compressed using the AS4 Compression Feature as
     * described in section 3.1 of the AS4 profile. Represents the <code>PMode[1].PayloadService.CompressionType</code>
     * P-Mode parameter.
     * <p>NOTE 1: Although the AS4 profiles states that payloads containing already compressed data do not need to be 
     * compressed Holodeck B2B will compress all payloads regardless of their content if indicated by this method.
     * <p>NOTE 2: Currently the only allowed compression type is GZip and the returned value of the method must therefor
     * be either <i>"application/gzip"</i> or <code>null</code>.
     * 
     * @return  When payloads should be compressed the MIME Type indicating which compression type should be used,<br>
     *          <code>null</code> if compression is not used
     */
    public String getCompressionType();
}

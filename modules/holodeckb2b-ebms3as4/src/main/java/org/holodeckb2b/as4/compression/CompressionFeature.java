/**
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
package org.holodeckb2b.as4.compression;

/**
 * Holds constants for AS4 Compression Feature.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public final class CompressionFeature {

    /**
     * Name of the <i>part property</i> that indicates the feature is used for the payload
     */
    public static final String FEATURE_PROPERTY_NAME = "CompressionType";

    /**
     * Name of the <i>part property</i> that indicates the MIME Type of the origin data
     */
    public static final String MIME_TYPE_PROPERTY_NAME = "MimeType";

    /**
     * The content type of the compressed data, also used as value of the <i>part property</i> that indicates the
     * feature is used for the payload
     */
    public static final String COMPRESSED_CONTENT_TYPE = "application/gzip";

    private CompressionFeature() {}
}

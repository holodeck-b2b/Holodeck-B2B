/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import java.io.Serializable;

import org.holodeckb2b.interfaces.as4.pmode.IAS4PayloadProfile;
import org.holodeckb2b.interfaces.pmode.IPayloadProfile;
import org.simpleframework.xml.Element;

/**
 * Contains the parameters related to the profiling of payload handling. This implementation supports the parameters
 * specific for the AS4 Compression Feature as defined in {@link IAS4PayloadProfile}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class PayloadProfile implements IAS4PayloadProfile, Serializable {
	private static final long serialVersionUID = -5886242178733861876L;

    @Element (name = "UseAS4Compression", required = false)
    private Boolean useAS4Compression = Boolean.FALSE;

    /**
     * Default constructor creates a new and empty <code>PayloadProfile</code> instance.
     */
    public PayloadProfile() {}

    /**
     * Creates a new <code>PayloadProfile</code> instance using the parameters from the provided {@link
     * IPayloadProfile} object.
     *
     * @param source The source object to copy the parameters from
     */
    public PayloadProfile(final IPayloadProfile source) {
        if (source != null && source instanceof IAS4PayloadProfile) {
            this.useAS4Compression = (((IAS4PayloadProfile) source).getCompressionType())
            											.equalsIgnoreCase(IAS4PayloadProfile.GZIP_CONTENT_TYPE);
        }
    }

    @Override
    public String getCompressionType() {
        return this.useAS4Compression ? IAS4PayloadProfile.GZIP_CONTENT_TYPE : null;
    }

    public void setCompressionType(final String compressionType) {
        this.useAS4Compression = compressionType.equalsIgnoreCase(IAS4PayloadProfile.GZIP_CONTENT_TYPE);
    }
}

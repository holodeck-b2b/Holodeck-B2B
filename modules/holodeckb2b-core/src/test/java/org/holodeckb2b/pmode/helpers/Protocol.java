/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.pmode.helpers;

import org.holodeckb2b.interfaces.pmode.IProtocol;

/**
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Protocol implements IProtocol {

    private String  address;
    private String  soapVersion;
    private boolean useChunking;
    private boolean useHTTPCompression;
    private boolean addActorAttribute;

    @Override
    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    @Override
    public String getSOAPVersion() {
        return soapVersion;
    }

    public void setSOAPVersion(final String soapVersion) {
        this.soapVersion = soapVersion;
    }

    @Override
    public boolean useChunking() {
        return useChunking;
    }

    public void setChunking(final boolean useChunking) {
        this.useChunking = useChunking;
    }

    @Override
    public boolean useHTTPCompression() {
        return useHTTPCompression;
    }

    public void setHTTPCompression(final boolean useHTTPCompression) {
        this.useHTTPCompression = useHTTPCompression;
    }

    @Override
    public boolean shouldAddActorOrRoleAttribute() {
        return addActorAttribute;
    }

    public void setAddActorOrRoleAttribute(final boolean shouldAddActorAttribute) {
        addActorAttribute = shouldAddActorAttribute;
    }
}

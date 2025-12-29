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

import org.holodeckb2b.interfaces.pmode.IProtocol;
import org.simpleframework.xml.Element;

/**
 * Contains the parameters related to the transport of the message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class Protocol implements IProtocol, Serializable {
	private static final long serialVersionUID = 3204165089495319565L;

    @Element (name = "Address", required = false)
    private String address;

    @Element (name = "ConnectionTimeout", required = false)
	private Integer connectionTimeout;

	@Element (name = "ReadTimeout", required = false)
	private Integer readTimeout;

    @Element (name = "TLSConfiguration", required = false)
    private TLSConfiguration tlsConfiguration;

    @Element (name = "AddActorOrRoleAttribute", required = false)
    private Boolean addActorAttribute;

    @Element (name = "UseChunking" , required = false)
    private Boolean useChunking;

    @Element (name = "SoapVersion", required = false)
    private String soapVersion = "1.2";

    @Element (name = "UseHTTPCompression", required = false)
    private Boolean useHTTPCompression;

    /**
     * Default constructor creates a new and empty <code>Protocol</code> instance.
     */
    public Protocol() {}

    /**
     * Creates a new <code>Protocol</code> instance using the parameters from the provided {@link IProtocol} object.
     *
     * @param source The source object to copy the parameters from
     */
    public Protocol(final IProtocol source) {
        this.address = source.getAddress();
        this.connectionTimeout = source.getConnectionTimeout();
        this.readTimeout = source.getReadTimeout();
        this.tlsConfiguration = source.getTLSConfiguration() != null ? new TLSConfiguration(source.getTLSConfiguration()) : null;
        this.soapVersion = source.getSOAPVersion();
        this.useChunking = source.useChunking();
        this.useHTTPCompression = source.useHTTPCompression();
        this.addActorAttribute = source.shouldAddActorOrRoleAttribute();
    }

    @Override
    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

	@Override
	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(final Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	@Override
	public Integer getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(final Integer readTimeout) {
		this.readTimeout = readTimeout;
	}

	@Override
	public TLSConfiguration getTLSConfiguration() {
		return tlsConfiguration;
	}

	public void setTLSConfiguration(final TLSConfiguration tlsConfiguration) {
		this.tlsConfiguration = tlsConfiguration;
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
        return useChunking != null ? useChunking.booleanValue() : false;
    }

    public void setChunking(final boolean useChunking) {
        this.useChunking = useChunking;
    }

    @Override
    public boolean useHTTPCompression() {
        return useHTTPCompression != null ? useHTTPCompression.booleanValue() : false;
    }

    public void setHTTPCompression(final boolean useHTTPCompression) {
        this.useHTTPCompression = useHTTPCompression;
    }

    @Override
    public boolean shouldAddActorOrRoleAttribute() {
        return addActorAttribute != null ? addActorAttribute.booleanValue() : false;
    }

    public void setAddActorOrRoleAttribute(final boolean shouldAddActorAttribute) {
        addActorAttribute = shouldAddActorAttribute;
    }
}

/**
 * Copyright (C) 2025 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.pmode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.pmode.ITLSConfiguration;
import org.holodeckb2b.interfaces.security.trust.IValidationParameters;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;
import org.simpleframework.xml.core.Validate;

/**
 * Contains the TLS parameters related to the transport of the message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 */
public class TLSConfiguration implements ITLSConfiguration, Serializable {
	private static final long serialVersionUID = -3263276993677778862L;

	@Element(name = "AllowedProtocols", required = false)
	private String allowedProtocolsValue;

	@Element(name = "AllowedCipherSuites", required = false)
	private String allowedCipherSuitesValue;

	@Element(name = "ClientCertificate", required = false)
	private KeystoreAlias clientCertRef;

	/*
	 * Internal list of the allowed protocols
	 */
	private List<String> allowedProtocols;
	/*
	 * Internal list of the allowed cipher suites
	 */
	private List<String> allowedCipherSuites;

	/**
	 * Default constructor creates a new and empty instance.
	 */
	public TLSConfiguration() {
	}

	/**
	 * Creates a new instance using the parameters from the provided {@link ITLSConfiguration} object.
	 *
	 * @param source The source object to copy the parameters from
	 */
	public TLSConfiguration(ITLSConfiguration source) {
		this.allowedProtocols = toList(source.getAllowedProtocols());
		this.allowedCipherSuites = toList(source.getAllowedCipherSuites());
		if (!Utils.isNullOrEmpty(source.getClientCertificateAlias())) {
			this.clientCertRef = new KeystoreAlias();
			this.clientCertRef.name = source.getClientCertificateAlias();
			this.clientCertRef.password = source.getClientCertificatePassword();
		}
	}

	private List<String> toList(String[] array) {
		return array == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * Converts the string values read from the XML to the internal lists.
	 */
	@Validate
	public void validate() {
		if (!Utils.isNullOrEmpty(allowedProtocolsValue))
			allowedProtocols = Arrays.asList(allowedProtocolsValue.split(","));
		else
			allowedProtocols = new ArrayList<>();
		if (!Utils.isNullOrEmpty(allowedCipherSuitesValue))
			allowedCipherSuites = Arrays.asList(allowedCipherSuitesValue.split(","));
		else
			allowedCipherSuites = new ArrayList<>();
	}

	/**
	 * Converts the internal list values to the comma separated list string values written to the XML.
	 */
	@Persist
	public void createListStrings() {
		allowedProtocolsValue = Utils.isNullOrEmpty(allowedProtocols) ? null
															: allowedProtocols.toString().replaceAll("\\[|\\]","");
		allowedCipherSuitesValue = Utils.isNullOrEmpty(allowedCipherSuites) ? null
															: allowedCipherSuites.toString().replaceAll("\\[|\\]","");
	}

	@Override
	public String[] getAllowedProtocols() {
		return !Utils.isNullOrEmpty(allowedProtocols) ? allowedProtocols.toArray(new String[allowedProtocols.size()]) : null;
	}

	public void setAllowedProtocols(List<String> allowedProtocols) {
		this.allowedProtocols = allowedProtocols == null ? new ArrayList<>() : new ArrayList<>(allowedProtocols);
	}

	public void addAllowedProtocol(String protocol) {
		this.allowedProtocols.add(protocol);
	}

	@Override
	public String[] getAllowedCipherSuites() {
		return !Utils.isNullOrEmpty(allowedCipherSuites) ? allowedCipherSuites.toArray(new String[allowedCipherSuites.size()]) : null;
	}

	public void setAllowedCipherSuites(List<String> allowedCipherSuites) {
		this.allowedCipherSuites = allowedCipherSuites == null ? new ArrayList<>() : new ArrayList<>(allowedCipherSuites);
	}

	public void addAllowedCipherSuite(String cipherSuite) {
		this.allowedCipherSuites.add(cipherSuite);
	}

	@Override
	public String getClientCertificateAlias() {
		return clientCertRef == null ? null : clientCertRef.name;
	}

	@Override
	public String getClientCertificatePassword() {
		return clientCertRef == null ? null : clientCertRef.password;
	}

	public void setClientCertificate(String alias, String password) {
		clientCertRef = new KeystoreAlias();
		clientCertRef.name = alias;
		clientCertRef.password = password;
	}

	@Override
	public IValidationParameters getValidationParameters() {
		return null;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof TLSConfiguration))
			return false;

		TLSConfiguration that = (TLSConfiguration) other;
		return Utils.areEqual(this.allowedProtocols, that.allowedProtocols)
			&& Utils.areEqual(this.allowedCipherSuites, that.allowedCipherSuites)
			&& Utils.nullSafeEqual(this.clientCertRef, that.clientCertRef);
	}
}

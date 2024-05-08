/*******************************************************************************
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.pmode.IKeyAgreement;
import org.holodeckb2b.interfaces.pmode.IKeyDerivationMethod;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * Contains the parameters related to the key agreement method used in the message level encryption.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class KeyAgreementConfig implements IKeyAgreement, Serializable {
	private static final long serialVersionUID = -491494006297109653L;

    @Element (name = "KeyEncryptionAlgorithm", required = false)
    private String encAlgorithm;

    @Element (name = "AgreementMethod", required = false)
    private String agreementMethod;

    @Element (name = "KeyDerivationMethod", required = false)
    private KeyDerivationConfig keyDerivationMethod;

    @Element(name = "CertReferenceMethod", required = false)
    @Convert(CertRefConverter.class)
    private X509ReferenceType certReferenceMethod;

    @ElementList(entry = "Parameter", inline = true, required = false)
    private Collection<Parameter>    parameters;

    /**
     * Default constructor creates a new and empty <code>KeyAgreementConfig</code> instance.
     */
    public KeyAgreementConfig() {}

    /**
     * Creates a new <code>KeyAgreementConfig</code> instance using the parameters from the provided {@link
     * IKeyAgreement} object.
     *
     * @param source The source object to copy the parameters from
     */
    public KeyAgreementConfig(final IKeyAgreement source) {
        this.encAlgorithm = source.getKeyEncryptionAlgorithm();
        this.agreementMethod = source.getAgreementMethod();
        if (source.getKeyDerivationMethod() != null)
			this.keyDerivationMethod = new KeyDerivationConfig(source.getKeyDerivationMethod());
        this.certReferenceMethod = source.getCertReferenceMethod();
        setParameters(source.getParameters());
    }

	@Override
	public String getKeyEncryptionAlgorithm() {
		return encAlgorithm;
	}

	public void setKeyEncryptionAlgorithm(final String algorithm) {
		this.encAlgorithm = algorithm;
	}

	@Override
	public String getAgreementMethod() {
		return agreementMethod;
	}

	public void setAgreementMethod(final String method) {
		this.agreementMethod = method;
	}

	@Override
	public IKeyDerivationMethod getKeyDerivationMethod() {
		return keyDerivationMethod;
	}

	public void setKeyDerivatonMethod(final IKeyDerivationMethod kdfMethod) {
		this.keyDerivationMethod = kdfMethod != null ? new KeyDerivationConfig(kdfMethod) : null;
	}

	@Override
	public X509ReferenceType getCertReferenceMethod() {
		return certReferenceMethod;
	}

	public void setCertReferenceMethod(final X509ReferenceType refMethod) {
        this.certReferenceMethod = refMethod;
    }

    @Override
    public Map<String, ?> getParameters() {
		if (!Utils.isNullOrEmpty(parameters)) {
	        HashMap<String, String> map = new HashMap<>(parameters.size());
	        parameters.forEach(p -> map.put(p.getName(), p.getValue()));
	        return map;
		} else
			return null;
	}

	public void setParameters(final Map<String, ?> sourceSettings) {
	    if (!Utils.isNullOrEmpty(sourceSettings)) {
	        this.parameters = new ArrayList<>(sourceSettings.size());
	        sourceSettings.forEach((n, v) -> this.parameters.add(new Parameter(n, v.toString())));
	    }
	}

	public void addParameter(final String name, final Object value) {
	    if (this.parameters == null)
	        this.parameters = new ArrayList<>();
	    this.parameters.add(new Parameter(name, value.toString()));
	}

	/**
	 * Converts the value of the {@link X509ReferenceType} enumeration to strings as used in the P-Mode XML document.
	 */
	public static class CertRefConverter implements Converter<X509ReferenceType> {
		private static final String ISSUER_SERIAL = "IssuerSerial";
		private static final String CERTIFICATE = "Certificate";
		private static final String SKI = "KeyIdentifier";

		@Override
		public X509ReferenceType read(InputNode node) throws Exception {
			switch (node.getValue()) {
			case ISSUER_SERIAL:
				return X509ReferenceType.IssuerAndSerial;
			case SKI:
				return X509ReferenceType.KeyIdentifier;
			default:
				return X509ReferenceType.BSTReference;
			}
		}

		@Override
		public void write(OutputNode node, X509ReferenceType value) throws Exception {
			switch (value) {
			case IssuerAndSerial:
				node.setValue(ISSUER_SERIAL);
				break;
			case KeyIdentifier:
				node.setValue(SKI);
				break;
			default:
				node.setValue(CERTIFICATE);
			}
		}
	}
}

/*
 * Copyright (C) 2018 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.security.util;

import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.ebms3.security.DefaultSecurityAlgorithms;
import org.holodeckb2b.interfaces.pmode.IKeyAgreement;
import org.holodeckb2b.interfaces.pmode.IKeyDerivationMethod;
import org.holodeckb2b.interfaces.security.X509ReferenceType;

/**
 * Is a decorator for a {@link IKeyAgreement} instance to ensure that the default security algorithms are returned
 * if none is specified by the source instance.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
class KAConfigWithDefaults implements IKeyAgreement {
    /**
     * The source configuration
     */
    private IKeyAgreement original;
    /**
     * The decorator for the key derivation settings
     */
    private KDConfigWithDefaults kdConfig;

    /**
     * Create a new decorator object
     *
     * @param original The source configuration
     */
    public KAConfigWithDefaults(IKeyAgreement original) {
    	this.original = original;
    	this.kdConfig = new KDConfigWithDefaults(original.getKeyDerivationMethod());
    }

	@Override
	public String getKeyEncryptionAlgorithm() {
		return (original != null && !Utils.isNullOrEmpty(original.getKeyEncryptionAlgorithm()))
				? original.getKeyEncryptionAlgorithm() : DefaultSecurityAlgorithms.KEY_ENCRYPTION;
	}

	@Override
	public String getAgreementMethod() {
		return (original != null && !Utils.isNullOrEmpty(original.getAgreementMethod()))
				? original.getAgreementMethod() : DefaultSecurityAlgorithms.KEY_AGREEMENT;
	}

	@Override
	public IKeyDerivationMethod getKeyDerivationMethod() {
		return kdConfig;
	}

	@Override
	public X509ReferenceType getCertReferenceMethod() {
		return original != null && original.getCertReferenceMethod() != null ? original.getCertReferenceMethod()
																			: DefaultSecurityAlgorithms.CERT_REFERENCE;
	}

	@Override
	public Map<String, ?> getParameters() {
		return original != null ? original.getParameters() : null;
	}

	/**
	 * Is a decorator for a {@link IKeyDerivationMethod} instance to ensure that the default algorithms are returned
	 * if none is specified by the source instance.
	 */
	class KDConfigWithDefaults implements IKeyDerivationMethod {
		private IKeyDerivationMethod original;

		public KDConfigWithDefaults(IKeyDerivationMethod original) {
			this.original = original;
		}

		@Override
		public String getAlgorithm() {
			return (original != null && !Utils.isNullOrEmpty(original.getAlgorithm()))
					? original.getAlgorithm() : DefaultSecurityAlgorithms.KEY_DERIVATION;
		}

		@Override
		public String getDigestAlgorithm() {
			return (original != null && !Utils.isNullOrEmpty(original.getDigestAlgorithm()))
					? original.getDigestAlgorithm() : DefaultSecurityAlgorithms.DIGEST;
		}

		@Override
		public Map<String, ?> getParameters() {
			return original != null ? original.getParameters() : null;
		}
	}
}
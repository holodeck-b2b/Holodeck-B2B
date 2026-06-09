/*
 * Copyright (C) 2026 The Holodeck B2B Team
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
package org.holodeckb2b.security.trust;

import java.lang.reflect.Constructor;
import java.security.InvalidAlgorithmParameterException;
import java.security.Provider;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertPathValidatorSpi;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.PKIXCertPathBuilderSpi_8;

/**
 * A mock Java Security Provider for testing the handling of the revocation checks by the default certificate manager.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 9.0.0
 */
public class RevocationCheckTestProvider extends Provider {
	enum Resp { ok, revoke, fail };

	private final Service 		BUILDER;
	private final Service 		VALIDATOR;
	private static TrustAnchor	trustAnchor;

	private static Resp next;

	public RevocationCheckTestProvider(X509Certificate trustAnchor) {
		super(BouncyCastleProvider.PROVIDER_NAME, "6.6.6", null);
		BUILDER = new Provider.Service(this, "CertPathBuilder", "PKIX", PKIXCertPathBuilderSpi_8.class.getName(), null, null);
		VALIDATOR = new Provider.Service(this, "CertPathValidator", "PKIX",
							"org.holodeckb2b.security.trust.RevocationCheckTestProvider$CPValidator", null, null);
		RevocationCheckTestProvider.trustAnchor = new TrustAnchor(trustAnchor, null);
	}

	public void validateNextRequest() {
		RevocationCheckTestProvider.next = Resp.ok;
	}

	public void invalidateNextRequest() {
		RevocationCheckTestProvider.next = Resp.revoke;
	}

	public void failNextRequest() {
		RevocationCheckTestProvider.next = Resp.fail;
	}

	@Override
	public Service getService(String type, String algorithm) {
		if ("CertPathBuilder".equals(type))
			return BUILDER;
		else
			return VALIDATOR;
	}

	public static class CPValidator extends CertPathValidatorSpi {

		@Override
		public CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters params)
				throws CertPathValidatorException, InvalidAlgorithmParameterException {
			if (!((PKIXParameters) params).isRevocationEnabled() || next == Resp.ok)
				return new PKIXCertPathValidatorResult(trustAnchor, null, certPath.getCertificates().get(0).getPublicKey());
			else if (next == Resp.revoke)
				throw new CertPathValidatorException("Invalid cert path", null, certPath, 0, BasicReason.REVOKED);
			else {
				CertPathValidatorException ex;
				try {
					Constructor<?> constructor = Class.forName("org.bouncycastle.jce.provider.RecoverableCertPathValidatorException")
					.getConstructor(String.class, Throwable.class, CertPath.class, int.class);
					constructor.setAccessible(true);
					ex = (CertPathValidatorException) constructor.newInstance("Invalid cert path", null, certPath, 0);
				} catch (Throwable t) {
					throw new RuntimeException();
				}
				throw ex;
			}

		}
	}
}

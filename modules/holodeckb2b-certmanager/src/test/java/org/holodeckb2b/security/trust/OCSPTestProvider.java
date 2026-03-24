package org.holodeckb2b.security.trust;

import java.security.InvalidAlgorithmParameterException;
import java.security.Provider;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.Reason;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertPathValidatorSpi;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * A mock Java Security Provider for testing the handling of the OCSP responses by the default certificate manager.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.2.0
 */
public class OCSPTestProvider extends Provider {

	private final Service 	OKAY_CP_VALIDATOR;
	private final Service 	FAILING_CP_VALIDATOR;

	static TrustAnchor		trustAnchor;

	static Reason			rejectReason;
	static Exception 		failure;


	public OCSPTestProvider(X509Certificate trustAnchor) {
		super(BouncyCastleProvider.PROVIDER_NAME, "6.6.6", null);
		OKAY_CP_VALIDATOR = new Provider.Service(this, "CertPathValidator", "PKIX",
									"org.holodeckb2b.security.trust.OCSPTestProvider$OkayCPValidator", null, null);
		FAILING_CP_VALIDATOR = new Provider.Service(this, "CertPathValidator", "PKIX",
									"org.holodeckb2b.security.trust.OCSPTestProvider$RejectCPValidator", null, null);
		OCSPTestProvider.trustAnchor = new TrustAnchor(trustAnchor, null);
	}

	public void validateNextResponse() {
		OCSPTestProvider.rejectReason = null;
		OCSPTestProvider.failure = null;
	}

	public void rejectNextResponse(Reason reason, Exception failure) {
		OCSPTestProvider.rejectReason = reason;
		OCSPTestProvider.failure = failure;
	}

	@Override
	public Service getService(String type, String algorithm) {
		if (rejectReason != null || failure != null)
			return FAILING_CP_VALIDATOR;
		else
			return OKAY_CP_VALIDATOR;
	}

	public static class OkayCPValidator extends CertPathValidatorSpi {

		@Override
		public CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters params)
				throws CertPathValidatorException, InvalidAlgorithmParameterException {
			return new PKIXCertPathValidatorResult(trustAnchor, null, certPath.getCertificates().get(0).getPublicKey());
		}
	}

	public static class RejectCPValidator extends CertPathValidatorSpi {

		@Override
		public CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters params)
				throws CertPathValidatorException, InvalidAlgorithmParameterException {
			if (((PKIXParameters) params).isRevocationEnabled())
				throw new CertPathValidatorException("Invalid cert path", failure, certPath, 0, rejectReason);
			else
				return new PKIXCertPathValidatorResult(trustAnchor, null, certPath.getCertificates().get(0).getPublicKey());
		}
	}
}

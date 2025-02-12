/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.testhelpers;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.security.trust.IValidationResult;
import org.holodeckb2b.interfaces.security.trust.IValidationResult.Trust;

/**
 * Is an implementation of the {@link ICertificateManager} for testing. It uses three in-memory Java key store to
 * store the key pairs and certificates.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class TestCertificateManager implements ICertificateManager {

    private final KeyStore privateKeys;
    private final KeyStore partnerCerts;
    private final KeyStore trustedCerts;

    TestCertificateManager() throws SecurityProcessingException {
    	try {
			privateKeys = KeyStore.getInstance("JKS");
			privateKeys.load(null, null);
			partnerCerts = KeyStore.getInstance("JKS");
			partnerCerts.load(null, null);
			trustedCerts = KeyStore.getInstance("JKS");
			trustedCerts.load(null, null);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
			throw new SecurityProcessingException();
		}
    }

    public synchronized void clear() throws SecurityProcessingException {
	    try {
			privateKeys.load(null, null);
			partnerCerts.load(null, null);
			trustedCerts.load(null, null);
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
			throw new SecurityProcessingException();
		}
    }

    public synchronized String registerKeyPair(final KeyStore.PrivateKeyEntry keypair, final String alias,
                                               final String password) throws SecurityProcessingException {
        // Generate a pasword if needed
        final char[] entryPwd = Utils.isNullOrEmpty(password) ?
        									Long.toHexString(Double.doubleToLongBits(Math.random())).toCharArray()
                                          : password.toCharArray();
        try {
            if (!(keypair.getCertificate() instanceof X509Certificate))
	    		throw new SecurityProcessingException("Not a X509 Certificate");
            privateKeys.setEntry(alias, keypair, new KeyStore.PasswordProtection(entryPwd));
            return new String(entryPwd);
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Can not registered key pair", ex);
        }
    }

    public synchronized void registerPartnerCertificate(final X509Certificate cert, final String alias)
    																				throws SecurityProcessingException {
        try {
        	partnerCerts.setCertificateEntry(alias, cert);
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Can not registered certificate", ex);
        }
    }

    public synchronized void registerTrustedCertificate(final X509Certificate cert, final String alias)
    		throws SecurityProcessingException {
    	try {
    		trustedCerts.setCertificateEntry(alias, cert);
    	} catch (KeyStoreException ex) {
    		throw new SecurityProcessingException("Can not registered certificate", ex);
    	}
    }

    @Override
    public String findKeyPair(X509Certificate cert) throws SecurityProcessingException {
    	return findEntry(privateKeys, c -> c.equals(cert));
    }

    @Override
    public String findKeyPair(byte[] skiBytes) throws SecurityProcessingException {
    	return findEntry(privateKeys, c -> CertificateUtils.hasSKI(c, skiBytes));
    }

    @Override
    public String findKeyPair(byte[] hash, MessageDigest digester) throws SecurityProcessingException {
    	return findEntry(privateKeys, c -> CertificateUtils.hasThumbprint(c, hash, digester));
    }

    @Override
    public String findKeyPair(X500Principal issuer, BigInteger serial) throws SecurityProcessingException {
    	return findEntry(privateKeys, c -> CertificateUtils.hasIssuerSerial(c, issuer, serial));
    }

    @Override
	public String findKeyPair(PublicKey key) throws SecurityProcessingException {
    	return findEntry(privateKeys, c -> c.getPublicKey().equals(key));
	}

    @Override
    public List<X509Certificate> getKeyPairCertificates(String alias) throws SecurityProcessingException {
    	try {
    		Certificate[] cc = privateKeys.getCertificateChain(alias);
    		if (cc != null && cc.length > 0) {
				List<X509Certificate> result = new ArrayList<>(cc.length);
				for(Certificate c : cc)
					result.add((X509Certificate) c);
				return result;
			} else
				return null;
		} catch (KeyStoreException e) {
			throw new SecurityProcessingException("KeyStore error", e);
		}
    }

    @Override
    public KeyStore.PrivateKeyEntry getKeyPair(final String alias, final String password)
                                                                                   throws SecurityProcessingException {
        try {
            // Check if the alias exists
            if (!privateKeys.containsAlias(alias))
                return null;
            else
                return (KeyStore.PrivateKeyEntry) privateKeys.getEntry(alias,
                                                              new KeyStore.PasswordProtection(password.toCharArray()));
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            throw new SecurityProcessingException("Error retrieving the keypair", ex);
        }
    }

    @Override
    public X509Certificate getPartnerCertificate(final String alias) throws SecurityProcessingException {
        try {
            return (X509Certificate) partnerCerts.getCertificate(alias);
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Error retrieving the certificate", ex);
        }
    }

    @Override
    public String findCertificate(final X509Certificate cert) throws SecurityProcessingException {
        try {
            // Check if the keystore contains the certificate and return the alias
            return partnerCerts.getCertificateAlias(cert);
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Error retrieving the certificate", ex);
        }
    }

    @Override
    public X509Certificate findCertificate(final X500Principal issuer, final BigInteger serial)
    																				throws SecurityProcessingException {
    	try {
    		return (X509Certificate) partnerCerts.getCertificate(
    							findEntry(partnerCerts, c -> CertificateUtils.hasIssuerSerial(c, issuer, serial)));
    	} catch (KeyStoreException ex) {
    		throw new SecurityProcessingException("Error retrieving the certificate", ex);
    	}
    }

    @Override
    public X509Certificate findCertificate(final byte[] skiBytes) throws SecurityProcessingException {
    	try {
    		return (X509Certificate) partnerCerts.getCertificate(
    							findEntry(partnerCerts, c -> CertificateUtils.hasSKI(c, skiBytes)));
    	} catch (KeyStoreException ex) {
    		throw new SecurityProcessingException("Error retrieving the certificate", ex);
    	}
    }

    @Override
    public X509Certificate findCertificate(byte[] hash, MessageDigest digester) throws SecurityProcessingException {
    	try {
    		return (X509Certificate) partnerCerts.getCertificate(
    							findEntry(partnerCerts, c -> CertificateUtils.hasThumbprint(c, hash, digester)));
    	} catch (KeyStoreException ex) {
    		throw new SecurityProcessingException("Error retrieving the certificate", ex);
    	}
    }

    private String findEntry(KeyStore ks, CertCondition cond) throws SecurityProcessingException {
		try {
	    	for (Enumeration<String> e = ks.aliases(); e.hasMoreElements();) {
	            String a = e.nextElement();
	            Certificate c = ks.getCertificate(a);
	            if (c != null && cond.matches((X509Certificate) c))
	            	return a;
			}
			return null;
		} catch (KeyStoreException e) {
			throw new SecurityProcessingException("KeyStore error", e);
		}
    }

    private interface CertCondition {
    	boolean matches(X509Certificate c);
    }

    public synchronized void removeKeyPair(String alias) throws SecurityProcessingException {
        try {
            // Check if the alias exists
            if (privateKeys.containsAlias(alias))
                privateKeys.deleteEntry(alias);
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Cannot remove key pair", ex);
        }
    }

    public synchronized void removePartnerCertificate(String alias) throws SecurityProcessingException {
    	try {
    		if (partnerCerts.containsAlias(alias))
    			partnerCerts.deleteEntry(alias);
    	} catch (KeyStoreException ex) {
    		throw new SecurityProcessingException("Cannot remove cert", ex);
    	}
    }

    public synchronized void removeTrustedCertificate(String alias) throws SecurityProcessingException {
        try {
            if (trustedCerts.containsAlias(alias))
                trustedCerts.deleteEntry(alias);
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Cannot remove cert", ex);
        }
    }

	@Override
	public String getName() {
		return TestCertificateManager.class.getName();
	}

	@Override
	public Collection<X509Certificate> getAllTlsCACertificates() {
		List<X509Certificate> certs = new ArrayList<>();
		try {
			for(Enumeration<String> aliases = trustedCerts.aliases(); aliases.hasMoreElements();)
				certs.add((X509Certificate) trustedCerts.getCertificate(aliases.nextElement()));
		} catch (KeyStoreException e) {
		}
		return certs;
	}

	@Override
	public IValidationResult validateMlsCertificate(List<X509Certificate> certs) throws SecurityProcessingException {
		return validateCert(certs);
	}

	@Override
	public IValidationResult validateTlsCertificate(List<X509Certificate> certs) throws SecurityProcessingException {
		return validateCert(certs);
	}

	private IValidationResult validateCert(List<X509Certificate> certs) throws SecurityProcessingException {
		if (Utils.isNullOrEmpty(certs))
			throw new SecurityProcessingException("No certs to validate!");

		try {
			if (trustedCerts.getCertificateAlias(certs.get(certs.size()-1)) != null)
				return new ValidationResult(certs, Trust.OK, "Found the root cert of path in trust store", null);
			else
				return
					new ValidationResult(certs, Trust.NOK, "Could not find the root cert of path in trust store", null);
		} catch (KeyStoreException e) {
			throw new SecurityProcessingException("Error accessing the trust store!");
		}

	}



	class ValidationResult implements IValidationResult {
		private List<X509Certificate> certpath;
		private Trust	trust;
		private String	message;
		private SecurityProcessingException details;

		ValidationResult(List<X509Certificate> certs, Trust result, String descr, SecurityProcessingException detail) {
			this.certpath = certs;
			this.trust = result;
			this.message = descr;
			this.details = detail;
		}

		@Override
		public List<X509Certificate> getValidatedCertPath() {
			return certpath;
		}

		@Override
		public Trust getTrust() {
			return trust;
		}

		@Override
		public String getMessage() {
			return message;
		}

		@Override
		public SecurityProcessingException getDetails() {
			return details;
		}

	}

	@Override
	public void init(IConfiguration config) throws SecurityProcessingException {
	}

	@Override
	public void shutdown() {
	}
}

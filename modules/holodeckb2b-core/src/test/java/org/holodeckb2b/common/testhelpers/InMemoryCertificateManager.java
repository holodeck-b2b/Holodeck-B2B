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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.holodeckb2b.common.util.Utils;
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
class InMemoryCertificateManager implements ICertificateManager {

    private final KeyStore privateKeys;
    private final KeyStore partnerCerts;
    private final KeyStore trustedCerts;
    
    InMemoryCertificateManager() throws SecurityProcessingException {
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

    public synchronized String registerKeyPair(final KeyStore.PrivateKeyEntry keypair, final String alias,
                                               final String password) throws SecurityProcessingException {
        // Generate a pasword if needed
        final char[] entryPwd = Utils.isNullOrEmpty(password) ? 
        									Long.toHexString(Double.doubleToLongBits(Math.random())).toCharArray()
                                          : password.toCharArray();
        X509Certificate cert = null;
        try {
            cert = (X509Certificate) keypair.getCertificate();
            privateKeys.setEntry(alias, keypair, new KeyStore.PasswordProtection(entryPwd));
            return new String(entryPwd);
        } catch (ClassCastException notX509) {
            throw new SecurityProcessingException("Not a X509 Certificate");
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
    public X509Certificate getCertificate(final String alias) throws SecurityProcessingException {
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
		return InMemoryCertificateManager.class.getName();
	}

	@Override
	public void init(String hb2bHome) throws SecurityProcessingException {		
	}

	@Override
	public IValidationResult validateTrust(List<X509Certificate> certs) throws SecurityProcessingException {
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
}

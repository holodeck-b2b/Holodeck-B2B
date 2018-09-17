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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.security.ICertificateManager;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 * Is an implementation of the {@link ICertificateManager} for testing. It uses three in-memory Java key store to 
 * store the key pairs and certificates.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
class InMemoryCertificateManager implements ICertificateManager {

    private final KeyStore privateKeys;
    private final KeyStore encryptionKeys;
    private final KeyStore trustedCerts;
    
    InMemoryCertificateManager() throws SecurityProcessingException {
    	try {
			privateKeys = KeyStore.getInstance("JKS");
			privateKeys.load(null, null);
			encryptionKeys = KeyStore.getInstance("JKS");
			encryptionKeys.load(null, null);
			trustedCerts = KeyStore.getInstance("JKS");
			trustedCerts.load(null, null);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
			throw new SecurityProcessingException();
		}
    }

    @Override
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

    @Override
    public synchronized void registerCertificate(final X509Certificate cert, final String alias,
                                                 final CertificateUsage... use) throws SecurityProcessingException {
        // If no usage is specified the Certificate will be registered for both uses
    	CertificateUsage[] forUsage;
    	if (use.length == 0)
    		forUsage = new CertificateUsage[] { CertificateUsage.Validation, CertificateUsage.Encryption };
    	else
    		forUsage = use;
    	
        CertificateUsage usage = null;
        try {
        	// We use the "classic" for loop here so we know the usage also in case an exception occurs
            for (int i = 0; i < forUsage.length; i++) {
                usage = forUsage[i];
                // Load the keystore and add the entry
                KeyStore ks;
                if (usage == CertificateUsage.Encryption)
                    ks = encryptionKeys;
                else
                    ks = trustedCerts;

                ks.setCertificateEntry(alias, cert);
            }
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Can not registered key pair", ex);
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
    public X509Certificate getCertificate(final CertificateUsage use, final String alias)
                                                                                   throws SecurityProcessingException {
        // Load keystore and retrieve the certificate from it
        KeyStore ks = use == CertificateUsage.Validation ? trustedCerts : encryptionKeys;
        try {
            return (X509Certificate) ks.getCertificate(alias);
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Error retrieving the certificate", ex);
        }
    }

    @Override
    public Collection<X509Certificate> getValidationCertificates() throws SecurityProcessingException {
        try {
	        final Collection<X509Certificate> result = new ArrayList<>(trustedCerts.size());
	        Enumeration<String> aliases = trustedCerts.aliases();
	        while (aliases.hasMoreElements()) 
	        	 result.add((X509Certificate) trustedCerts.getCertificate(aliases.nextElement()));       
	        return result;
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Error retrieving the certificates", ex);       	
        }
    }
    
    @Override
    public String getCertificateAlias(final CertificateUsage use, final X509Certificate cert)
                                                                                  throws SecurityProcessingException {
        // Load keystore and retrieve the certificate from it
        KeyStore ks = use == CertificateUsage.Validation ? trustedCerts : encryptionKeys;
        try {
            // Check if the keystore contains the certificate and return the alias
            return ks.getCertificateAlias(cert);
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Error retrieving the certificate", ex);
        }
    }

    /**
     * Removes the key pair registered under the given alias.
     * <p>The certificate manager does not check whether the key pair can be safely removed, i.e. if there is no active
     * P-Mode referencing it. It is the responsibility of the caller to make sure it is safe to remove the key pair.
     */
    @Override
    public synchronized void removeKeyPair(String alias) throws SecurityProcessingException {
        try {
            // Check if the alias exists
            if (privateKeys.containsAlias(alias))
                privateKeys.deleteEntry(alias);
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Error retrieving the keypair", ex);
        }
    }

    /**
     * Removes the certificate registered under the given alias for the specified usages (or all if none specified).
     * <p>The certificate manager does not check whether the certificate can be safely removed, i.e. if there is no
     * active P-Mode referencing it. It is the responsibility of the caller to make sure it is safe to remove the
     * certificate.
     */
    @Override
    public synchronized void removeCertificate(String alias, CertificateUsage... use)
                                                                                   throws SecurityProcessingException {
        // If no usage is specified the Certificate will be removed for all uses
    	CertificateUsage[] forUsage;
    	if (use.length == 0)
    		forUsage = new CertificateUsage[] { CertificateUsage.Validation, CertificateUsage.Encryption };
    	else
    		forUsage = use;
        CertificateUsage usage = null;
        try {
            for (int i = 0; i < forUsage.length; i++) {
                usage = forUsage[i];
                // Load the keystore and add the entry
                KeyStore ks;
                if (usage == CertificateUsage.Encryption)
                    ks = encryptionKeys;
                else
                    ks = trustedCerts;

                if (ks.containsAlias(alias))
                    ks.deleteEntry(alias);
            }
        } catch (KeyStoreException ex) {
            throw new SecurityProcessingException("Can not registered key pair", ex);
        }
    }
}

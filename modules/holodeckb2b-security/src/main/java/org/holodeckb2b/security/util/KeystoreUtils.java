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
package org.holodeckb2b.security.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public final class KeystoreUtils {

    /**
     * Checks whether the JKS keystore is available and can be loaded.
     *
     * @param path  Path to the keystore
     * @param pwd   Password to access the keystore
     * @throws SecurityProcessingException When the keystore could not be accessed.
     */
    public static void check(final String path, final String pwd) throws SecurityProcessingException {
        try {
            load(path, pwd);
        } catch (SecurityProcessingException spe) {
            throw new SecurityProcessingException("Cannot access the keystore [" + path + "]!", spe.getCause());
        }
    }

    /**
     * Loads the specified JKS keystore from disk
     *
     * @param path  The path to the keystore
     * @param pwd   Password to access the keystore
     * @return      The keystore loaded from the specified file
     * @throws SecurityProcessingException When the keystore could not be loaded from the specified location.
     */
    public static KeyStore load(final String path, final String pwd) throws SecurityProcessingException {
        try {
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new java.io.FileInputStream(path)) {
                keyStore.load(fis, (!Utils.isNullOrEmpty(pwd) ? pwd.toCharArray() : new char[] {}));
            }
            return keyStore;
        } catch (NullPointerException | IOException | KeyStoreException | NoSuchAlgorithmException
                | CertificateException ex) {
            throw new SecurityProcessingException("Can not load the keystore [" + path + "]!", ex);
        }
    }

    /**
     * Saves the given keystore to the specified JKS file on disk
     *
     * @param keystore  The keystore to save
     * @param path      The path to the keystore file
     * @param pwd       Password to access the keystore file
     * @throws SecurityProcessingException When the keystore could not be saved to the specified location.
     */
    public static void save(final KeyStore keystore, final String path, final String pwd)
                                                                                    throws SecurityProcessingException {
        try {
            try (FileOutputStream fos = new java.io.FileOutputStream(path)) {
                keystore.store(fos, (!Utils.isNullOrEmpty(pwd) ? pwd.toCharArray() : new char[] {}));
            }
        } catch (NullPointerException | IOException | KeyStoreException | NoSuchAlgorithmException
                | CertificateException ex) {
            throw new SecurityProcessingException("Can not save the keystore to file [" + path + "]!", ex);
        }
    }
}

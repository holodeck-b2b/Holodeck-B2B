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

import java.util.HashMap;
import java.util.Map;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.pmode.IEncryptionConfiguration;
import org.holodeckb2b.interfaces.pmode.IKeyTransport;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.holodeckb2b.interfaces.pmode.ISigningConfiguration;
import org.holodeckb2b.interfaces.pmode.IUsernameTokenConfiguration;
import org.holodeckb2b.interfaces.security.DefaultSecurityAlgorithms;
import org.holodeckb2b.interfaces.security.SecurityHeaderTarget;
import org.holodeckb2b.interfaces.security.X509ReferenceType;

/**
 * Is a container class to collect the security settings that must be applied in the creation/processing of the
 * WS-Security headers of a message. It also acts as "decorator" as it will return the default algorithms if none has
 * been specified in the original configuration.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class SecurityConfig implements ISecurityConfiguration {

    private Map<SecurityHeaderTarget, IUsernameTokenConfiguration> usernameTokenConfig = new HashMap<>(2);
    private ISigningConfiguration signingConfig;
    private IEncryptionConfiguration encryptionConfig;

    public void setUsernameTokenConfiguration(SecurityHeaderTarget target, IUsernameTokenConfiguration utConfig) {
        usernameTokenConfig.put(target, utConfig);
    }

    @Override
    public IUsernameTokenConfiguration getUsernameTokenConfiguration(SecurityHeaderTarget target) {
        return usernameTokenConfig.get(target);
    }

    public void setSignatureConfiguration(ISigningConfiguration signConfig) {
        if (signConfig == null)
            signingConfig = null;
        else
            signingConfig = new SigningConfigWithDefaults(signConfig);
    }

    @Override
    public ISigningConfiguration getSignatureConfiguration() {
        return signingConfig;
    }

    public void setEncryptionConfiguration(IEncryptionConfiguration encConfig) {
        if (encConfig == null)
            encryptionConfig = null;
        else
            encryptionConfig = new EncryptionConfigWithDefaults(encConfig);
    }

    @Override
    public IEncryptionConfiguration getEncryptionConfiguration() {
        return encryptionConfig;
    }

    /**
     * Is a decorator for a {@link ISigningConfiguration} instance to ensure that the default security algorithms are
     * returned if none is specified by the source instance.
     */
    public class SigningConfigWithDefaults implements ISigningConfiguration {

        private ISigningConfiguration   original;

        public SigningConfigWithDefaults(ISigningConfiguration original) {
            this.original = original;
        }

        @Override
        public String getKeystoreAlias() {
            return original.getKeystoreAlias();
        }

        @Override
        public String getCertificatePassword() {
            return original.getCertificatePassword();
        }

        @Override
        public X509ReferenceType getKeyReferenceMethod() {
            if (original.getKeyReferenceMethod() != null)
                return original.getKeyReferenceMethod();
            else
                return DefaultSecurityAlgorithms.KEY_REFERENCE;
        }

        @Override
        public Boolean includeCertificatePath() {
            if (original.includeCertificatePath() != null)
                return original.includeCertificatePath();
            else
                return Boolean.FALSE;
        }

        @Override
        public String getSignatureAlgorithm() {
            if (!Utils.isNullOrEmpty(original.getSignatureAlgorithm()))
                return original.getSignatureAlgorithm();
            else
                return DefaultSecurityAlgorithms.SIGNATURE;
        }

        @Override
        public String getHashFunction() {
            if (!Utils.isNullOrEmpty(original.getHashFunction()))
                return original.getHashFunction();
            else
                return DefaultSecurityAlgorithms.MESSAGE_DIGEST;
        }
    }

    /**
     * Is a decorator for a {@link IEncryptionConfiguration} instance to ensure that the default security algorithms are
     * returned if none is specified by the source instance.
     */
    public class EncryptionConfigWithDefaults implements IEncryptionConfiguration {

        private IEncryptionConfiguration   original;
        private IKeyTransport              keytransportCfg = null;

        public EncryptionConfigWithDefaults(IEncryptionConfiguration original) {
            this.original = original;
            this.keytransportCfg = new KeyTransportConfigWithDefaults(original.getKeyTransport());
        }

        @Override
        public String getKeystoreAlias() {
            return original.getKeystoreAlias();
        }

        @Override
        public String getCertificatePassword() {
            return original.getCertificatePassword();
        }

        @Override
        public String getAlgorithm() {
            if (!Utils.isNullOrEmpty(original.getAlgorithm()))
                return original.getAlgorithm();
            else
                return DefaultSecurityAlgorithms.ENCRYPTION;
        }

        @Override
        public IKeyTransport getKeyTransport() {
            return keytransportCfg;
        }

        /**
         * Is a decorator for a {@link IKeyTransport} instance to ensure that the default security algorithms are
         * returned if none is specified by the source instance.
         */
        public class KeyTransportConfigWithDefaults implements IKeyTransport {
            private IKeyTransport original;

            public KeyTransportConfigWithDefaults(IKeyTransport original) {
                this.original = original;
            }

            @Override
            public String getAlgorithm() {
                if (original != null && !Utils.isNullOrEmpty(original.getAlgorithm()))
                    return original.getAlgorithm();
                else
                    return DefaultSecurityAlgorithms.KEY_TRANSPORT;
            }

            @Override
            public String getMGFAlgorithm() {
                return original != null ? original.getMGFAlgorithm() : null;
            }

            @Override
            public String getDigestAlgorithm() {
                if (original != null && !Utils.isNullOrEmpty(original.getDigestAlgorithm()))
                    return original.getDigestAlgorithm();
                else
                    return DefaultSecurityAlgorithms.MESSAGE_DIGEST;
            }

            @Override
            public X509ReferenceType getKeyReferenceMethod() {
                if (original != null && original.getKeyReferenceMethod() != null)
                    return original.getKeyReferenceMethod();
                else
                    return DefaultSecurityAlgorithms.KEY_REFERENCE;
            }
        }
    }
}

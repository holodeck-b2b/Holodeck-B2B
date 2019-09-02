/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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

import org.apache.axis2.context.ConfigurationContext;
import org.holodeckb2b.core.config.InternalConfiguration;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class TestConfig implements InternalConfiguration {

    public String hb2b_home;
    public String pmodeValidatorClass = null;
    public String pmodeStorageClass = null;
    public String persistencyProviderClass;
    public String securityProviderClass;
    public boolean useStrictHeaderValidation = false;

    public boolean allowSignalBundling  = false;
    public boolean reportErrorOnError   = false;
    public boolean reportErrorOnReceipt = false;

    public TestConfig() {    	
    }
    
    public TestConfig(final String homeDir) {
        hb2b_home = homeDir;
    }

    public TestConfig(final String homeDir, final String pmodeValidatorClass) {
        hb2b_home = homeDir;
        this.pmodeValidatorClass = pmodeValidatorClass;
    }

    public TestConfig(final String homeDir, final String pmodeValidatorClass,
           final String pmodeStorageClass) {
        hb2b_home = homeDir;
        this.pmodeValidatorClass = pmodeValidatorClass;
        this.pmodeStorageClass = pmodeStorageClass;
    }

    public TestConfig(final String homeDir, final boolean useStrictValidation) {
        hb2b_home = homeDir;
        useStrictHeaderValidation = useStrictValidation;
    }

    @Override
    public String getHolodeckB2BHome() {
        return hb2b_home;
    }

    @Override
    @Deprecated
    public String getPublicKeyStorePath() {
        return getHolodeckB2BHome() + "/publickeys.jks";
    }

    @Override
    @Deprecated
    public String getPublicKeyStorePassword() {
        return "nosecrets";
    }

    @Override
    public String getHostName() {
        return "test.holodeckb2b.org";
    }

    @Override
    public ConfigurationContext getAxisConfigurationContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
	public String getWorkerPoolCfgFile() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTempDirectory() {
        return getHolodeckB2BHome() + "/temp/";
    }

    @Override
    public boolean allowSignalBundling() {
        return allowSignalBundling;
    }

    @Override
    public boolean shouldReportErrorOnError() {
        return reportErrorOnError;
    }
   

    @Override
    public boolean shouldReportErrorOnReceipt() {
        return reportErrorOnReceipt;
    }

    @Override
    @Deprecated
    public boolean useStrictErrorRefCheck() {
        return false;
    }

    @Override
    @Deprecated
    public String getPrivateKeyStorePath() {
        return getHolodeckB2BHome() + "/privatekeys.jks";
    }

    @Override
    @Deprecated
    public String getPrivateKeyStorePassword() {
        return "secrets";
    }

    @Override
    @Deprecated
    public String getTrustKeyStorePath() {
        return getHolodeckB2BHome() + "/trustedcerts.jks";
    }

    @Override
    @Deprecated
    public String getTrustKeyStorePassword() {
        return "trusted";
    }

    @Override
    public String getMessageProcessingEventProcessor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPModeValidatorImplClass() {
        return pmodeValidatorClass;
    }

    @Override
    public String getPModeStorageImplClass() {
        return pmodeStorageClass;
    }

    @Override
    public String getPersistencyProviderClass() {
    	return persistencyProviderClass;
    }

    @Override
    public boolean useStrictHeaderValidation() {
        return useStrictHeaderValidation;
    }

    @Override
    public String getSecurityProviderClass() {
        return securityProviderClass;
    }
}
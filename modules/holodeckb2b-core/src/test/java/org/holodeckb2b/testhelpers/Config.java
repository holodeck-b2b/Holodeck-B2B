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
package org.holodeckb2b.testhelpers;

import org.apache.axis2.context.ConfigurationContext;
import org.holodeckb2b.common.config.InternalConfiguration;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Config implements InternalConfiguration {

    private String  hb2b_home;
    private String  pmodeValidatorClass = null;
    private String  pmodeStorageClass = null;

    Config(final String homeDir) {
        hb2b_home = homeDir;
    }

    Config(final String homeDir, final String pmodeValidatorClass) {
        hb2b_home = homeDir;
        this.pmodeValidatorClass = pmodeValidatorClass;
    }

    Config(final String homeDir, final String pmodeValidatorClass, final String pmodeStorageClass) {
        hb2b_home = homeDir;
        this.pmodeValidatorClass = pmodeValidatorClass;
        this.pmodeStorageClass = pmodeStorageClass;
    }

    @Override
    public String getHolodeckB2BHome() {
        return hb2b_home;
    }

    @Override
    public String getPublicKeyStorePath() {
        return getHolodeckB2BHome() + "/publickeys.jks";
    }

    @Override
    public String getPublicKeyStorePassword() {
        return "nosecrets";
    }

    @Override
    public String getHostName() {
        return "test.holodeckb2b.org";
    }

    @Override
    public ConfigurationContext getAxisConfigurationContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getWorkerPoolCfgFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getTempDirectory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean allowSignalBundling() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean shouldReportErrorOnError() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean shouldReportErrorOnReceipt() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean useStrictErrorRefCheck() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getPrivateKeyStorePath() {
        return getHolodeckB2BHome() + "/privatekeys.jks";
    }

    @Override
    public String getPrivateKeyStorePassword() {
        return "secrets";
    }

    @Override
    public boolean shouldCheckCertificateRevocation() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getTrustKeyStorePath() {
        return getHolodeckB2BHome() + "/trustedcerts.jks";
    }

    @Override
    public String getTrustKeyStorePassword() {
        return "trusted";
    }

    @Override
    public String getMessageProcessingEventProcessor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

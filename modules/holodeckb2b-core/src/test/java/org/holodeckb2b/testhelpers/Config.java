/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.testhelpers;

import org.apache.axis2.context.ConfigurationContext;
import org.holodeckb2b.common.config.InternalConfiguration;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Config implements InternalConfiguration {

    private final String  hb2b_home;

    Config(final String homeDir) {
        hb2b_home = homeDir;
    }

    @Override
    public String getHolodeckB2BHome() {
        return hb2b_home;
    }

    @Override
    public String getPublicKeyStorePath() {
        return getHolodeckB2BHome() + "/publickeystore.jks";
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
    public String getPersistencyUnit() {
        return "holodeckb2b-core-test";
    }

    @Override
    public String getMessageProcessingEventProcessor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.common.testhelpers.pmode;

import org.holodeckb2b.interfaces.pmode.IBusinessInfo;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.IPayloadProfile;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class UserMessageFlow implements IUserMessageFlow {

    private BusinessInfo       businessInfo;
    private PayloadProfile      payloadProfile;
    private ErrorHandlingConfig errorHandlingCfg;

    @Override
    public IBusinessInfo getBusinessInfo() {
        return businessInfo;
    }

    public void setBusinnessInfo(final BusinessInfo busInfo) {
        this.businessInfo = busInfo;
    }

    @Override
    public IPayloadProfile getPayloadProfile() {
        return payloadProfile;
    }

    public void setPayloadProfile(final PayloadProfile payloadProfile) {
        this.payloadProfile = payloadProfile;
    }

    @Override
    public IErrorHandling getErrorHandlingConfiguration() {
        return errorHandlingCfg;
    }

    public void setErrorHandlingConfiguration(final ErrorHandlingConfig errorHandlingConfig) {
        this.errorHandlingCfg = errorHandlingConfig;
    }

}

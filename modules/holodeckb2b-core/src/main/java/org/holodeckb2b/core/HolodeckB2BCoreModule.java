/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.modules.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.holodeckb2b.core.config.InternalConfiguration;

/**
 * Axis2 module class for the Holodeck B2B Core module.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public final class HolodeckB2BCoreModule implements Module {
    /**
     * The name of the Axis2 Module that contains the Holodeck B2B Core implementation
     */
    public static final String NAME = "holodeckb2b-core";

//    /**
//     * The singleton Axis2 Module instance of the Holodeck B2B Core
//     */
//    public static final HolodeckB2BCoreModule INSTANCE = new HolodeckB2BCoreModule();

//    @Override
//    public Module getModule() {
//    	return INSTANCE;
//    }

    /**
     * Initializes the Holodeck B2B Core module.
     *
     * @param cc
     * @param am
     * @throws AxisFault
     */
    @Override
    public void init(final ConfigurationContext cc, final AxisModule am) throws AxisFault {
    	AxisConfiguration axisConfiguration = cc.getAxisConfiguration();
//    	if (am.getModule() != this || !(axisConfiguration instanceof InternalConfiguration)) {
        if (!(axisConfiguration instanceof InternalConfiguration)) {
            LogManager.getLogger().fatal("Invalid Holodeck B2B Core module configuration!");
            throw new AxisFault("Invalid configuration found for module: " + am.getName());
        }
//        setModuleClassLoader(axisConfiguration.getModuleClassLoader());
        HolodeckB2BCore.init((InternalConfiguration) axisConfiguration);
    }

    @Override
    public void engageNotify(final AxisDescription ad) throws AxisFault {
    }

    @Override
    public boolean canSupportAssertion(final Assertion asrtn) {
        return false;
    }

    @Override
    public void applyPolicy(final Policy policy, final AxisDescription ad) throws AxisFault {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void shutdown(final ConfigurationContext cc) throws AxisFault {
    	HolodeckB2BCore.shutdown();
    }
}

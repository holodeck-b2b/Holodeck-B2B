/*
 * Copyright (C) 2026 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.axis2;

import java.util.ArrayList;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.ServiceLifeCycle;
import org.apache.axis2.modules.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;

/**
 * A facade to the Axis2 configuration context that uses a implements a different shutdown sequence where first all
 * services are stopped and then the modules in reverse order of being started.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.1.1
 */
public class ConfigurationContext extends org.apache.axis2.context.ConfigurationContext {
	private static final Logger log = LogManager.getLogger(org.apache.axis2.context.ConfigurationContext.class);

	private final String contextPath;
	/**
	 * Creates a facade to a new Axis2 {@link org.apache.axis2.context.ConfigurationContext} based on the given
	 * Axis configuration. Also sets the context root as specified in the configuration file or if not specified to
	 * the default "holodeckb2b".
	 *
	 * @param axisConfig		the Axis2 configuration to use for constructing the context
	 */
	public ConfigurationContext(AxisConfiguration axisConfig) {
		super(axisConfig);
		Parameter ctxPathParameter = axisConfig.getParameter(Constants.PARAM_CONTEXT_ROOT);
		String ctxPath = ctxPathParameter != null ? (String) ctxPathParameter.getValue() : null;
        contextPath = Utils.isNullOrEmpty(ctxPath) ? "holodeckb2b" : ctxPath;
    }

	@Override
	public String getContextRoot() {
		return contextPath;
	}

	@Override
	public String getServiceContextPath() {
		return contextPath;
	}

	@Override
	public void shutdownModulesAndServices() throws AxisFault {
		AxisConfiguration axisConfiguration = getAxisConfiguration();
		if (axisConfiguration != null) {
			for (AxisService service : axisConfiguration.getServices().values()) {
				ServiceLifeCycle serviceLifeCycle = service.getServiceLifeCycle();
				if (serviceLifeCycle != null) {
					try {
						serviceLifeCycle.shutDown(this, service);
					} catch (Exception e) {
						log.warn("Could not shutdown service " + service.getName(), e);
					}
				}
			}
			ArrayList<AxisModule> allModules = new ArrayList<>(axisConfiguration.getModules().values());
			for (int i = allModules.size() - 1; i >= 0; i--) {
				Module m = allModules.get(i).getModule();
				if (m != null) {
					try {
						m.shutdown(this);
					} catch (Exception e) {
						log.warn("Could not shutdown module " + allModules.get(i).getName(), e);
					}
				}
			}
		}
		cleanupContexts();
	}
}

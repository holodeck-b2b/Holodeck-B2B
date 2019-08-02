/*
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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

import java.util.concurrent.TimeUnit;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;

/**
 * Is a customised {@link org.apache.axis2.transport.http.server.HttpFactory} that uses the actual {@link 
 * TransportInDescription} to configure this server and uses Holodeck B2B's own HTTP worker factory to support the use
 * of Service specified messsge processing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class HTTPFactory extends org.apache.axis2.transport.http.server.HttpFactory {
	
	private TransportInDescription	httpConfiguration;
	
	public HTTPFactory(ConfigurationContext axisConf, TransportInDescription transprtIn) throws AxisFault {
        super(axisConf, 0, new HTTPWorkerFactory());
        this.httpConfiguration = transprtIn;
        
        setPort(getIntParam(PARAMETER_PORT, 8080));
        setHostAddress(getStringParam(PARAMETER_HOST_ADDRESS, null));
        setOriginServer(Axis2Utils.HTTP_PRODID_HEADER);
        setRequestSocketTimeout(getIntParam(PARAMETER_REQUEST_SOCKET_TIMEOUT, 20000));
        setRequestTcpNoDelay(getBooleanParam(PARAMETER_REQUEST_TCP_NO_DELAY, true));
        setRequestCoreThreadPoolSize(getIntParam(PARAMETER_REQUEST_CORE_THREAD_POOL_SIZE, 100));
        setRequestMaxThreadPoolSize(getIntParam(PARAMETER_REQUEST_MAX_THREAD_POOL_SIZE, 150));
        setThreadKeepAliveTime(getLongParam(PARAMETER_THREAD_KEEP_ALIVE_TIME, 180L));
        setThreadKeepAliveTimeUnit(getTimeUnitParam(PARAMETER_THREAD_KEEP_ALIVE_TIME_UNIT, TimeUnit.SECONDS));        
	}
	
	/*
	 * Because everything related to the HTTP configuration is private in the Axis2 parent class we have just copied
	 * relevant methods to this class. 
	 */
	
    @Override
	public TransportInDescription getHttpConfiguration() {
        return httpConfiguration;
    }	
	
    private int getIntParam(String name, int def) {
        String config = getStringParam(name, null);
        if (config != null) {
            return Integer.parseInt(config);
        } else {
            return def;
        }
    }

    private long getLongParam(String name, long def) {
        String config = getStringParam(name, null);
        if (config != null) {
            return Long.parseLong(config);
        } else {
            return def;
        }
    }

    private boolean getBooleanParam(String name, boolean def) throws AxisFault {
        String config = getStringParam(name, null);
        if (config != null) {
            if (config.equals("yes") || config.equals("true")) {
                return true;
            } else if (config.equals("no") || config.equals("false")) {
                return false;
            } else {
                throw new AxisFault("Boolean value must be yes, true, no or false for parameter " +
                        name + ":  " + config);
            }
        }
        return def;
    }

    private TimeUnit getTimeUnitParam(String name, TimeUnit def) throws AxisFault {
        String config = getStringParam(name, null);
        if (config != null) {
            try {
                return (TimeUnit) TimeUnit.class.getField(config).get(null);
            } catch (Exception e) {
                throw AxisFault.makeFault(e);
            }
        }
        return def;
    }

    private String getStringParam(String name, String def) {
        Parameter param = this.httpConfiguration.getParameter(name);
        if (param != null) {
            String config = (String) param.getValue();
            if (config != null) {
                return config;
            }
        }
        return def;
    }	
}

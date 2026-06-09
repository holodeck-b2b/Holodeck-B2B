/**
 * Copyright (C) 2025 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.transport.http;

import org.apache.axis2.transport.http.server.ConnectionListenerFailureHandler;
import org.apache.axis2.transport.http.server.IOProcessor;

/**
 * Handles failures during the start of the Axis2 Http server and notifies the Holodeck B2B component of the failure.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.1.1
 */
public class HttpListenerFailureHandler implements ConnectionListenerFailureHandler {

	private HTTPListener httpListener;

	HttpListenerFailureHandler(HTTPListener httpListener) {
		this.httpListener = httpListener;
	}

	@Override
	public boolean failed(IOProcessor connectionListener, Throwable cause) {
		httpListener.notifyStartupFailure(cause);
		return false;
	}

	@Override
	public void notifyAbnormalTermination(IOProcessor connectionListener, String message, Throwable cause) {
		httpListener.notifyStartupFailure(cause);
	}

}

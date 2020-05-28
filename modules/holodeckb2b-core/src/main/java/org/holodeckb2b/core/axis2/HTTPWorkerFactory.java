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

import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.http.server.Worker;
import org.apache.axis2.transport.http.server.WorkerFactory;

/**
 * Is the {@link WorkerFactory} implementation that creates the {@link HTTPWorker}s that are capable of using a Service 
 * specified Builder implementation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class HTTPWorkerFactory implements WorkerFactory {
	private TransportInDescription	httpConfiguration;
	
	public HTTPWorkerFactory(TransportInDescription transprtIn) {
		this.httpConfiguration = transprtIn;			
	}

	@Override
	public Worker newWorker() {
		return new HTTPWorker(httpConfiguration);
	}

}

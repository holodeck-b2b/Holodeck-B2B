/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.core;

import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.modules.Module;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.delivery.IDeliveryManager;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventProcessor;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;
import org.holodeckb2b.interfaces.general.IVersionInfo;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.security.trust.ICertificateManager;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.StorageException;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.IWorkerPool;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;

/**
 * Defines the interface the Holodeck B2B Core implementation has to provide to the outside world, like submitters,
 * delivery methods and extensions for dynamic configuration.
 * <p>
 * NOTE: This interface is for <b>internal use only</b> to allow a loose coupling of the Core and interfaces modules. It
 * SHOULD NOT be used by other code. Use the static methods of the {@link HolodeckB2BCoreInterface} class instead.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see HolodeckB2BCoreInterface
 */
public interface IHolodeckB2BCore {

	/**
	 * See {@link HolodeckB2BCoreInterface#getConfiguration()}
	 */
    IConfiguration getConfiguration();

	/**
	 * See {@link HolodeckB2BCoreInterface#getModule()}
	 */
    Module getModule(final String name);

	/**
	 * See {@link HolodeckB2BCoreInterface#getService(String)}
	 */
    AxisService getService(final String name);

	/**
	 * See {@link HolodeckB2BCoreInterface#getTransport(String)}
	 */
    TransportOutDescription getTransport(final String name);

	/**
	 * See {@link HolodeckB2BCoreInterface#getMessageSubmitter()}
	 */
    IMessageSubmitter getMessageSubmitter();

	/**
	 * See {@link HolodeckB2BCoreInterface#getPModeSet()}
	 */
    IPModeSet getPModeSet();

	/**
	 * See {@link HolodeckB2BCoreInterface#getEventProcessor()}
	 */
    IMessageProcessingEventProcessor getEventProcessor();

	/**
	 * See {@link HolodeckB2BCoreInterface#getQueryManager()}
	 */
    IQueryManager getQueryManager();

	/**
	 * See {@link HolodeckB2BCoreInterface#getCertificateManager()}
	 */
    ICertificateManager getCertificateManager();

	/**
	 * See {@link HolodeckB2BCoreInterface#registerEventHandler(IMessageProcessingEventConfiguration)}
	 */
    boolean registerEventHandler(IMessageProcessingEventConfiguration eventConfiguration)
    																	throws MessageProccesingEventHandlingException;

	/**
	 * See {@link HolodeckB2BCoreInterface#removeEventHandler(String)}
	 */
    void removeEventHandler(String id);

	/**
	 * See {@link HolodeckB2BCoreInterface#getEventHandlerConfiguration()}
	 */
    List<IMessageProcessingEventConfiguration> getEventHandlerConfiguration();

	/**
	 * See {@link HolodeckB2BCoreInterface#getVersion()}
	 */
    IVersionInfo getVersion();

	/**
	 * See {@link HolodeckB2BCoreInterface#createWorkerPool(String, IWorkerPoolConfiguration)}
	 */
    IWorkerPool createWorkerPool(final String name, final IWorkerPoolConfiguration configuration)
																							throws WorkerPoolException;
	/**
	 * See {@link HolodeckB2BCoreInterface#getWorkerPool(String)}
	 */
    IWorkerPool getWorkerPool(final String name);

	/**
	 * See {@link HolodeckB2BCoreInterface#resumeProcessing(IUserMessageEntity)}
	 */
    void resumeProcessing(IUserMessageEntity userMessage) throws StorageException, IllegalArgumentException;

	/**
	 * See {@link HolodeckB2BCoreInterface#resend(IUserMessageEntity)}
	 */
    void resend(IUserMessageEntity userMessage) throws StorageException, IllegalArgumentException;

	/**
	 * See {@link HolodeckB2BCoreInterface#getDeliveryManager()}
	 */
    IDeliveryManager getDeliveryManager();

	/**
	 * See {@link HolodeckB2BCoreInterface#executeSendProcess(MessageContext, AxisService)}
	 */
    MessageContext executeSendProcess(MessageContext msgContext, AxisService service) throws AxisFault;
}

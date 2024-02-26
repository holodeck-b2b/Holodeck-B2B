/**
 * Copyright (C) 2022 The Holodeck B2B Team, Sander Fieten
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.axis2.description.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.UserManagedCache;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.builders.UserManagedCacheBuilder;
import org.ehcache.event.EventType;
import org.ehcache.impl.events.CacheEventAdapter;
import org.holodeckb2b.common.events.impl.MessageDelivered;
import org.holodeckb2b.common.events.impl.MessageDeliveryFailure;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.delivery.IDeliveryCallback;
import org.holodeckb2b.interfaces.delivery.IDeliveryManager;
import org.holodeckb2b.interfaces.delivery.IDeliveryMethod;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;


/**
 * Is the default implementation of {@link IDeliveryManager}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 6.0.0
 */
class DeliveryManager implements IDeliveryManager {
	private static final Logger log = LogManager.getLogger();

	/**
	 * Cache containing already instantiated Delivery Methods.
	 */
	private UserManagedCache<String, IDeliveryMethod> 	dmCache;

	/**
	 * The collection of pre-registered Delivery Specifications
	 */
	private Map<String, IDeliverySpecification> registeredSpecs;

	/**
	 * The HolodeckB2B Core StorageManager to use for updating the processing state of message units
	 */
	private StorageManager mdManager;

	/**
	 * Initialises the Delivery Manager.
	 *
	 * @param sm		the {@link StorageManager} to use for updating the meta-data of message units
	 * @param config	the Holodeck B2B configuration which may contain settings for the delivery method cache
	 */
	DeliveryManager(final StorageManager sm, final IConfiguration config) {
		this.mdManager = sm;
		registeredSpecs = new HashMap<>();
		int maxCacheSize = 0;
		Parameter cacheParam = config.getParameter("MaxDeliveryMethodCacheSize");
		if (cacheParam != null && cacheParam.getParameterType() == Parameter.TEXT_PARAMETER) {
			try { maxCacheSize = Integer.parseInt((String) cacheParam.getValue()); }
			catch (NumberFormatException nan) {}
		}
		if (maxCacheSize > 0) {
			log.trace("Initialise Delivery Method cache with max size = {}", maxCacheSize);
			dmCache = UserManagedCacheBuilder.newUserManagedCacheBuilder(String.class, IDeliveryMethod.class)
		    				.withEventExecutors(Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(5))
		    				.withEventListeners(
		    					CacheEventListenerConfigurationBuilder
		    						.newEventListenerConfiguration(DMRemovalListener.class, EventType.EVICTED).build(),
		    					CacheEventListenerConfigurationBuilder
				    				.newEventListenerConfiguration(DMRemovalListener.class, EventType.REMOVED)
				    				.synchronous().build())
		    				.withResourcePools(ResourcePoolsBuilder.heap(maxCacheSize))
		    				.build(true);
		}
	}

	@Override
	public void deliver(IMessageUnitEntity messageUnit) throws IllegalStateException, MessageDeliveryException {
		log.trace("Delivery requested for {} (msgId={})", MessageUnitUtils.getMessageUnitName(messageUnit),
					messageUnit.getMessageId());
		// Make sure the P-Mode of the message unit is available
		if (Utils.isNullOrEmpty(messageUnit.getPModeId())
			|| !HolodeckB2BCoreInterface.getPModeSet().containsId(messageUnit.getPModeId())) {
			log.error("Unable to deliver message unit (msgId={}) as no P-Mode is available!",
					messageUnit.getMessageId());
			throw new MessageDeliveryException("No P-Mode available for message unit");
		}
		if (!messageUnit.getProcessingStates().stream()
											  .anyMatch(s -> s.getState() == ProcessingState.READY_FOR_DELIVERY)) {
			log.error("Message unit (msgId={}) is not ready for delivery!", messageUnit.getMessageId());
			throw new IllegalStateException("Message unit not ready for delivery");
		}

		try {
			IDeliverySpecification delSpec;
			delSpec = getDeliverySpec(messageUnit);
			if (delSpec != null) {
				if (!mdManager.setProcessingState(messageUnit, ProcessingState.OUT_FOR_DELIVERY)) {
					log.error("Cannot start delivery of message unit (msgId={})", messageUnit.getMessageId());
					throw new MessageDeliveryException("Message unit already in process");
				}
				try {
					IDeliveryMethod deliveryMethod = getDeliveryMethod(delSpec);
					if (delSpec.performAsyncDelivery()) {
						log.debug("Start async delivery of message unit (msgId={}) with delivery method: {}",
								messageUnit.getMessageId(), deliveryMethod.getClass().getName());
						deliveryMethod.deliver(messageUnit, new DeliveryResultHandler(messageUnit));
					} else {
						log.debug("Delivering message unit (msgId={}) with delivery method: {}",
								messageUnit.getMessageId(), deliveryMethod.getClass().getName());
						deliveryMethod.deliver(messageUnit);
						handleResult(messageUnit, null);
					}
				} catch (MessageDeliveryException deliveryError) {
					handleResult(messageUnit, deliveryError);
					throw deliveryError;
				} catch (Throwable deliveryError) {
					log.error("Unexpected error during delivery of message unit (msgId={}): {}",
							messageUnit.getMessageId(), Utils.getExceptionTrace(deliveryError));
					MessageDeliveryException mde = new MessageDeliveryException(deliveryError.getMessage(),
																				deliveryError);
					handleResult(messageUnit, mde);
					throw mde;
				}
			} else {
				boolean updated = false;
				if (messageUnit instanceof IUserMessageEntity) {
					log.warn("No delivery specified in P-Mode ({}) used for User Message (msgId={})",
							messageUnit.getPModeId(), messageUnit.getMessageId());
					updated = mdManager.setProcessingState(messageUnit, ProcessingState.DONE, "No delivery specified");
				} else {
					log.info("{} (msgId={}) does not need to be delivered to back-end",
							MessageUnitUtils.getMessageUnitName(messageUnit), messageUnit.getMessageId());
					updated = mdManager.setProcessingState(messageUnit, ProcessingState.DONE);
				}
				if (!updated) {
					log.error("Message unit (msgId={}) is already in process", messageUnit.getMessageId());
					throw new MessageDeliveryException("Message unit already in process");
				}
			}
		} catch (StorageException dbError) {
			log.error("Error updating processing state of message unit (msgId={}) : {}", messageUnit.getMessageId(),
					dbError.getMessage());
			throw new MessageDeliveryException("Error updating processing state", dbError);
		}
	}

	/**
	 * Determines if and how the message unit should be delivered to the back-end application.
	 *
	 * @param messageUnit	the received message unit
	 * @return	the {@link IDeliverySpecification} how the message unit should be delivered to the back-end,<br/>
	 * 			<code>null</code> if the message does not need to be delivered to the back-end
	 */
	private IDeliverySpecification getDeliverySpec(IMessageUnitEntity messageUnit) {
		IDeliverySpecification deliverySpec = null;
		if (messageUnit instanceof IUserMessageEntity)
            deliverySpec = PModeUtils.getLeg(messageUnit).getDefaultDelivery();
		else if (messageUnit instanceof IReceiptEntity) {
			ILeg leg = PModeUtils.getLeg(messageUnit);
	        IReceiptConfiguration rcptConfig = leg != null ? leg.getReceiptConfiguration() : null;
	        if (rcptConfig != null && rcptConfig.shouldNotifyReceiptToBusinessApplication()) {
	            deliverySpec = rcptConfig.getReceiptDelivery();
	            if (deliverySpec == null)
	                deliverySpec = leg.getDefaultDelivery();
	        }
		} else { // messageUnit instanceof IErrorMessageEntity
			/*
			 * For Error Messages the delivery configuration can be specified in three places, depending on whether
			 * error refers to a Pull Request. In that case the Pull Request flow can contain specific error handling
			 * configuration including a delivery specification. If it does not, or the error does not refer to a
			 * Pull Request the delivery specification in the user message flow will be used, first checking if there
			 * is one specific for errors.
			 */
			IErrorHandling errHandlingCfg = null;
	        IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(messageUnit.getPModeId());
	        ILeg leg = pmode.getLeg(((IErrorMessageEntity) messageUnit).getLeg());
	        ILeg pullLeg = PModeUtils.getOutPullLeg(pmode);
	        if (pullLeg != null && leg.getLabel() == pullLeg.getLabel()) {
	        	// There error occurred on a Pull Request
	        	IPullRequestFlow prFlow = PModeUtils.getOutPullRequestFlow(leg);
	        	errHandlingCfg = prFlow != null ? prFlow.getErrorHandlingConfiguration() : null;
	        } else {
	        	IUserMessageFlow umFlow = leg.getUserMessageFlow();
	        	errHandlingCfg = umFlow != null ? umFlow.getErrorHandlingConfiguration() : null;
	        }

	        if (errHandlingCfg != null && errHandlingCfg.shouldNotifyErrorToBusinessApplication()) {
	        	deliverySpec = errHandlingCfg.getErrorDelivery();
	        	if (deliverySpec == null)
	        		// No specific error delivery specified, fall back to default
	        		deliverySpec = leg.getDefaultDelivery();
	        }
		}
		return deliverySpec;
	}

	/**
	 * Gets the Delivery Method for the given Delivery Specification.
	 * <p>
	 * If the cache does not a Delivery Method for the given specification a new instance will be created and added to
	 * the cache (only if the spec has a unique id).
	 *
	 * @param spec	the Delivery Specification
	 * @return		a Delivery Method configured according to the provided specification
	 * @throws MessageDeliveryException	when there is an issue creating the Delivery Method
	 */
	private IDeliveryMethod getDeliveryMethod(IDeliverySpecification spec) throws MessageDeliveryException {
		IDeliveryMethod deliveryMethod = null;
		final String specId = spec.getId();
		if (dmCache != null && !Utils.isNullOrEmpty(specId)) {
			log.trace("Check if Delivery Method for spec ({}) is cached", specId);
			deliveryMethod = dmCache.get(specId);
		}
		if (deliveryMethod == null) {
			IDeliverySpecification spec2use = spec;
			if (registeredSpecs.containsKey(specId)) {
				log.debug("Using registered spec to create delivery method");
				spec2use = registeredSpecs.get(specId);
			} else
				log.debug("Using provided spec to create delivery method");
			try {
				log.trace("Create Delivery Method ({})", spec2use.getDeliveryMethod().getName());
				deliveryMethod = spec2use.getDeliveryMethod().newInstance();
				log.trace("Initialise Delivery Method");
				Map<String, ?> settings = spec2use.getSettings();
				deliveryMethod.init(settings != null ? settings : new HashMap<>());
				log.debug("Created Delivery Method {}", spec2use.getDeliveryMethod().getName());
			} catch (Throwable dmError) {
				log.error("Error creating Delivery Method ({}) for spec ({}): {}", spec2use.getDeliveryMethod().getName(),
						specId, Utils.getExceptionTrace(dmError));
				if (dmError instanceof MessageDeliveryException)
					throw (MessageDeliveryException) dmError;
				else
					throw new MessageDeliveryException("Could not create Delivery Method");
			}
			if (dmCache != null && !Utils.isNullOrEmpty(specId)) {
				log.trace("Add new Delivery Method to cache");
				dmCache.putIfAbsent(specId, deliveryMethod);
			}
		}
		return deliveryMethod;
	}

	/**
	 * Handles the result of the delivery attempt to the back-end application by updating the processing state of the
	 * message unit and raising the applicable message processing event.
	 *
	 * @param msgUnit			message unit being delivered
	 * @param deliveryError		the exception that caused the delivery attempt to fail, <code>null</code> if the
	 * 							delivery was successful
	 * @throws StorageException when an error occurs updating the processing state of the message unit
	 */
	private void handleResult(IMessageUnitEntity msgUnit, MessageDeliveryException deliveryError)
																						throws StorageException {
		if (deliveryError == null) {
			log.info("Successfully delivered {} (msgId={})", MessageUnitUtils.getMessageUnitName(msgUnit),
					 msgUnit.getMessageId());
			mdManager.setProcessingState(msgUnit,
									msgUnit instanceof IUserMessage ? ProcessingState.DELIVERED : ProcessingState.DONE);
		} else {
			log.error("An error occurred during delivery of {} (msgId={}): {}",
					MessageUnitUtils.getMessageUnitName(msgUnit), msgUnit.getMessageId(),
					Utils.getExceptionTrace(deliveryError));
	        // Indicate failure in processing state
	        mdManager.setProcessingState(msgUnit, ProcessingState.DELIVERY_FAILED, deliveryError.getMessage());
	        // If the problem that occurred is indicated as permanent, we can set the state to FAILURE and return
	        //  an ebMS Error to the sender
	        if (deliveryError.isPermanent())
	            mdManager.setProcessingState(msgUnit, ProcessingState.FAILURE);
		}
        // Raise delivery event to inform external components
        HolodeckB2BCore.getEventProcessor().raiseEvent(deliveryError == null ? new MessageDelivered(msgUnit)
        											  			: new MessageDeliveryFailure(msgUnit, deliveryError));


	}

	@Override
	public void registerDeliverySpec(IDeliverySpecification spec) {
		if (registeredSpecs.put(spec.getId(), spec) != null)
			log.debug("Replaced Delivery Specification ({})", spec.getId());
		else
			log.debug("Added Delivery Specification ({})", spec.getId());

		if (dmCache != null)
			dmCache.remove(spec.getId());
	}

	@Override
	public boolean isSpecIdUsed(String id) {
		return registeredSpecs.containsKey(id) || dmCache.containsKey(id);
	}

	@Override
	public IDeliverySpecification getDeliverySpecification(String id) {
		return registeredSpecs.get(id);
	}

	@Override
	public Collection<IDeliverySpecification> getAllRegisteredSpecs() {
		return registeredSpecs.values();
	}

	@Override
	public void removeDeliverySpec(String id) {
		registeredSpecs.remove(id);
		if (dmCache != null)
			dmCache.remove(id);
	}

	/**
	 * Shuts down the Delivery Manager, removing all cached Delivery Methods which automatically results in shutting
	 * them down.
	 */
	public void shutdown() {
		if (dmCache != null) {
			log.trace("Clearing Delivery Method cache");
			dmCache.forEach(e -> dmCache.remove(e.getKey()));
			dmCache.close();
		}
	}

	/**
	 * Callback handler for processing the result of the async delivery process.
	 */
	class DeliveryResultHandler implements IDeliveryCallback {
		private final IMessageUnitEntity msgUnit;

		private DeliveryResultHandler(IMessageUnitEntity m) {
			this.msgUnit = m;
		}

		@Override
		public void success() {
			try {
				handleResult(msgUnit, null);
			} catch (StorageException dbError) {
				log.error("Error updating processing state of message unit (msgId={}) : {}", msgUnit.getMessageId(),
						dbError.getMessage());
			}
		}

		@Override
		public void failed(MessageDeliveryException failure) {
			try {
				handleResult(msgUnit, failure);
			} catch (StorageException dbError) {
				log.error("Error updating processing state of message unit (msgId={}) : {}", msgUnit.getMessageId(),
						dbError.getMessage());
			}
		}
	}

	/**
	 * Is a cache event listener which handles the removal, either explicit or implicit through eviction of Delivery
	 * Methods from the cache. It {@link IDeliveryMethod#shutdown() shuts down} the Delivery Method to release the
	 * resources it may hold.
	 */
	public static class DMRemovalListener extends CacheEventAdapter<String, IDeliveryMethod> {
		@Override
		protected void onEviction(String key, IDeliveryMethod dm) {
			log.trace("Delivery Method (specId={}) evicted from cache", key);
			try {
				dm.shutdown();
			} catch (Throwable t) {
				log.error("An error occurred during Delivery Method (specId={}) shutdown:", key,
						Utils.getExceptionTrace(t));
			}
		}
		@Override
		protected void onRemoval(String key, IDeliveryMethod dm) {
			log.trace("Delivery Method (specId={}) removed from cache", key);
			try {
				dm.shutdown();
			} catch (Throwable t) {
				log.error("An error occurred during Delivery Method (specId={}) shutdown:", key,
						Utils.getExceptionTrace(t));
			}
		}
	}
}

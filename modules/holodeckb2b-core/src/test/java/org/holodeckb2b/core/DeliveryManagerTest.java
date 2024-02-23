package org.holodeckb2b.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.UUID;

import org.ehcache.UserManagedCache;
import org.ehcache.config.ResourceType;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.DeliveryConfiguration;
import org.holodeckb2b.common.pmode.ErrorHandlingConfig;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.pmode.PullRequestFlow;
import org.holodeckb2b.common.pmode.ReceiptConfiguration;
import org.holodeckb2b.common.pmode.UserMessageFlow;
import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestEventProcessor;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.core.storage.StorageManager;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.delivery.IDeliveryMethod;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.events.IMessageDelivered;
import org.holodeckb2b.interfaces.events.IMessageDeliveryFailure;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DeliveryManagerTest {
	
	static TestEventProcessor eventProc;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		HolodeckB2BTestCore testCore = new HolodeckB2BTestCore();
		testCore.setDeliveryManager(new DeliveryManager(testCore.getStorageManager(), testCore.getConfiguration()));
		HolodeckB2BCoreInterface.setImplementation(testCore);
		eventProc = (TestEventProcessor) HolodeckB2BCoreInterface.getEventProcessor();
	}

	@Before
	public void cleanup() {
		TestDeliveryMethod.instances.clear();
		eventProc.reset();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testInitWithCaching() throws Exception {
		
		InternalConfiguration config = new InternalConfiguration(org.holodeckb2b.commons.testing.TestUtils.getTestClassBasePath());
		config.addParameter("MaxDeliveryMethodCacheSize", "10");
		
		DeliveryManager dm = new DeliveryManager(null, config);		
		
		Field cacheFld = DeliveryManager.class.getDeclaredField("dmCache");
		cacheFld.setAccessible(true);
		UserManagedCache<String, IDeliveryMethod> dmCache = (UserManagedCache<String, IDeliveryMethod>) cacheFld.get(dm);
		
		assertNotNull(dmCache);		
		assertEquals(10, 
			dmCache.getRuntimeConfiguration().getResourcePools().getPoolForResource(ResourceType.Core.HEAP).getSize());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testInitNoCaching() throws Exception {
		
		InternalConfiguration config = new InternalConfiguration(org.holodeckb2b.commons.testing.TestUtils.getTestClassBasePath());
		
		DeliveryManager dm = new DeliveryManager(null, config);		
		
		Field cacheFld = DeliveryManager.class.getDeclaredField("dmCache");
		cacheFld.setAccessible(true);
		UserManagedCache<String, IDeliveryMethod> dmCache = (UserManagedCache<String, IDeliveryMethod>) cacheFld.get(dm);
		
		assertNull(dmCache);		
	}
		
	@Test
	public void testRejectNotReady() throws Exception {
		PMode pmode = HB2BTestUtils.create1WayReceivePMode();        
        HolodeckB2BCore.getPModeSet().add(pmode);

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		userMessage.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IUserMessageEntity umEntity = storageManager.storeReceivedMessageUnit(userMessage);
		storageManager.setProcessingState(umEntity, ProcessingState.RECEIVED);
		storageManager.setProcessingState(umEntity, ProcessingState.PROCESSING);
		storageManager.setProcessingState(umEntity, ProcessingState.FAILURE);

		try {
			HolodeckB2BCoreInterface.getDeliveryManager().deliver(umEntity);
			fail();
		} catch (IllegalStateException expected) {}
	}
	
	@Test
	public void testUseRegisteredSpec() throws Exception {
		DeliveryConfiguration delSpec = new DeliveryConfiguration();
		delSpec.setId(UUID.randomUUID().toString());
		delSpec.setDeliveryMethod(TestDeliveryMethod.class);

		HolodeckB2BCoreInterface.getDeliveryManager().registerDeliverySpec(delSpec);
		
		PMode pmode = HB2BTestUtils.create1WayReceivePMode();
		DeliveryConfiguration pmodeSpec = new DeliveryConfiguration();
		pmodeSpec.setId(delSpec.getId());
		pmode.getLeg(Label.REQUEST).setDefaultDelivery(pmodeSpec);
		
        HolodeckB2BCore.getPModeSet().add(pmode);

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		userMessage.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IUserMessageEntity umEntity = storageManager.storeReceivedMessageUnit(userMessage);
		storageManager.setProcessingState(umEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(umEntity);
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertEquals(1, dm.deliveredMsgUnits.size());
		assertEquals(umEntity, dm.deliveredMsgUnits.get(0));		
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testReplaceRegisteredSpec() throws Exception {
		DeliveryConfiguration delSpec = new DeliveryConfiguration();
		delSpec.setId(UUID.randomUUID().toString());
		delSpec.setDeliveryMethod(TestDeliveryMethod.class);
		delSpec.addSetting("changingP", "old value");
		
		InternalConfiguration config = new InternalConfiguration(org.holodeckb2b.commons.testing.TestUtils.getTestClassBasePath());
		config.addParameter("MaxDeliveryMethodCacheSize", "10");
		DeliveryManager delman = new DeliveryManager(HolodeckB2BCore.getStorageManager(), config);
		
		delman.registerDeliverySpec(delSpec);
		
		PMode pmode = HB2BTestUtils.create1WayReceivePMode();
		DeliveryConfiguration pmodeSpec = new DeliveryConfiguration();
		pmodeSpec.setId(delSpec.getId());
		pmode.getLeg(Label.REQUEST).setDefaultDelivery(pmodeSpec);
		
        HolodeckB2BCore.getPModeSet().add(pmode);

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		userMessage.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IUserMessageEntity umEntity = storageManager.storeReceivedMessageUnit(userMessage);
		storageManager.setProcessingState(umEntity, ProcessingState.READY_FOR_DELIVERY);
		
		delman.deliver(umEntity);

		Field cacheFld = DeliveryManager.class.getDeclaredField("dmCache");
		cacheFld.setAccessible(true);
		IDeliveryMethod cdm = ((UserManagedCache<String, IDeliveryMethod>) cacheFld.get(delman)).get(delSpec.getId());

		assertNotNull(cdm);
		assertEquals("old value", ((TestDeliveryMethod) cdm).settings.get("changingP"));
		
		DeliveryConfiguration newDelSpec = new DeliveryConfiguration();
		newDelSpec.setId(delSpec.getId());
		newDelSpec.setDeliveryMethod(TestDeliveryMethod.class);
		newDelSpec.addSetting("changingP", "new value");
		delman.registerDeliverySpec(newDelSpec);
		
		delman.deliver(umEntity);

		assertEquals(1, TestDeliveryMethod.instances.size());		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertEquals(1, dm.deliveredMsgUnits.size());
		assertEquals(umEntity, dm.deliveredMsgUnits.get(0));
		
		cdm = ((UserManagedCache<String, IDeliveryMethod>) cacheFld.get(delman)).get(delSpec.getId());
		assertNotNull(cdm);
		assertEquals("new value", ((TestDeliveryMethod) cdm).settings.get("changingP"));		
	}
	
	
	@Test
	public void testDMReuse() throws Exception {
		InternalConfiguration config = new InternalConfiguration(org.holodeckb2b.commons.testing.TestUtils.getTestClassBasePath());
		config.addParameter("MaxDeliveryMethodCacheSize", "10");		
		DeliveryManager dm = new DeliveryManager(HolodeckB2BCore.getStorageManager(), config);		
		
		IUserMessageEntity umEntity = createUserMessage(false);
		
		dm.deliver(umEntity);
		assertEquals(1, TestDeliveryMethod.instances.size());			
		dm.deliver(umEntity);
		assertEquals(1, TestDeliveryMethod.instances.size());			
	}
	
	@Test
	public void testUserMessage() throws Exception {
		IUserMessageEntity umEntity = createUserMessage(false);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(umEntity);
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		
		assertEquals(1, dm.deliveredMsgUnits.size());
		assertEquals(umEntity, dm.deliveredMsgUnits.get(0));
		
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
						umEntity.getProcessingStates().get(umEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DELIVERED, umEntity.getCurrentProcessingState().getState());
				
		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDelivered);
	}
	
	@Test
	public void testAsync() throws Exception {
		IUserMessageEntity umEntity = createUserMessage(true);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(umEntity);
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertEquals(1, dm.async);
		assertEquals(1, dm.deliveredMsgUnits.size());
		assertEquals(umEntity, dm.deliveredMsgUnits.get(0));		
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				umEntity.getProcessingStates().get(umEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DELIVERED, umEntity.getCurrentProcessingState().getState());

		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDelivered);
	}
	
	@Test
	public void testRejection() throws Exception {
		IUserMessageEntity umEntity = createUserMessage(false);		
		PMode pmode = (PMode) HolodeckB2BCore.getPModeSet().get(umEntity.getPModeId());
		DeliveryConfiguration delSpec = pmode.getLeg(Label.REQUEST).getDefaultDelivery();
		
		delSpec.addSetting("reject", "warning");
		try {
			HolodeckB2BCoreInterface.getDeliveryManager().deliver(umEntity);
			fail();
		} catch (MessageDeliveryException mde) {}
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertEquals(0, dm.deliveredMsgUnits.size());
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				umEntity.getProcessingStates().get(umEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DELIVERY_FAILED, umEntity.getCurrentProcessingState().getState());

		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDeliveryFailure);
		assertNotNull(((IMessageDeliveryFailure) eventProc.events.get(0)).getFailureReason());
		assertFalse(((IMessageDeliveryFailure) eventProc.events.get(0)).getFailureReason().isPermanent());

		delSpec.addSetting("reject", "failure");
		try {
			HolodeckB2BCoreInterface.getDeliveryManager().deliver(umEntity);
			fail();
		} catch (MessageDeliveryException mde) {}
		
		dm = TestDeliveryMethod.instances.get(1);
		assertEquals(0, dm.deliveredMsgUnits.size());
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				umEntity.getProcessingStates().get(umEntity.getProcessingStates().size() - 3).getState());
		assertEquals(ProcessingState.DELIVERY_FAILED, 
				umEntity.getProcessingStates().get(umEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.FAILURE, umEntity.getCurrentProcessingState().getState());

		assertEquals(2, eventProc.events.size());
		assertTrue(eventProc.events.get(1) instanceof IMessageDeliveryFailure);
		assertNotNull(((IMessageDeliveryFailure) eventProc.events.get(1)).getFailureReason());
		assertTrue(((IMessageDeliveryFailure) eventProc.events.get(1)).getFailureReason().isPermanent());
	}
	
	@Test
	public void testRejectionAsync() throws Exception {
		IUserMessageEntity umEntity = createUserMessage(true);		
		PMode pmode = (PMode) HolodeckB2BCore.getPModeSet().get(umEntity.getPModeId());
		DeliveryConfiguration delSpec = pmode.getLeg(Label.REQUEST).getDefaultDelivery();
		
		delSpec.addSetting("reject", "warning");
		try {
			HolodeckB2BCoreInterface.getDeliveryManager().deliver(umEntity);
		} catch (MessageDeliveryException mde) {			
			fail();
		}
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertEquals(0, dm.deliveredMsgUnits.size());
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				umEntity.getProcessingStates().get(umEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DELIVERY_FAILED, umEntity.getCurrentProcessingState().getState());
		
		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDeliveryFailure);
		assertNotNull(((IMessageDeliveryFailure) eventProc.events.get(0)).getFailureReason());
		assertFalse(((IMessageDeliveryFailure) eventProc.events.get(0)).getFailureReason().isPermanent());	
	}
	
	@Test
	public void testIgnoreReceipt() throws Exception {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();		
		ReceiptConfiguration rcptCfg = new ReceiptConfiguration();
		rcptCfg.setNotifyReceiptToBusinessApplication(false);
		pmode.getLeg(Label.REQUEST).setReceiptConfiguration(rcptCfg);
		
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		Receipt receipt = new Receipt();
		receipt.setMessageId(MessageIdUtils.createMessageId());
		receipt.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IReceiptEntity rEntity = storageManager.storeReceivedMessageUnit(receipt);
		storageManager.setProcessingState(rEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(rEntity);
		
		assertEquals(0, TestDeliveryMethod.instances.size());
		assertEquals(ProcessingState.READY_FOR_DELIVERY, 
						rEntity.getProcessingStates().get(rEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DONE, rEntity.getCurrentProcessingState().getState());
		
		assertEquals(0, eventProc.events.size());		
	}
	
	@Test
	public void testReceiptDefault() throws Exception {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();		
		ReceiptConfiguration rcptCfg = new ReceiptConfiguration();
		rcptCfg.setNotifyReceiptToBusinessApplication(true);
		pmode.getLeg(Label.REQUEST).setReceiptConfiguration(rcptCfg);
		DeliveryConfiguration delSpec = new DeliveryConfiguration();
		delSpec.setId(pmode.getId() + "-DefaultDelivery");
		delSpec.setDeliveryMethod(TestDeliveryMethod.class);
		delSpec.addSetting("specId", delSpec.getId());
		pmode.getLeg(Label.REQUEST).setDefaultDelivery(delSpec);
		
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		Receipt receipt = new Receipt();
		receipt.setMessageId(MessageIdUtils.createMessageId());
		receipt.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IReceiptEntity rEntity = storageManager.storeReceivedMessageUnit(receipt);
		storageManager.setProcessingState(rEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(rEntity);
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertNotNull(dm.settings);
		assertEquals(delSpec.getId(), dm.settings.get("specId"));
		assertEquals(1, dm.deliveredMsgUnits.size());
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				rEntity.getProcessingStates().get(rEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DONE, rEntity.getCurrentProcessingState().getState());
		
		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDelivered);
	}
	
	@Test
	public void testReceiptSpecific() throws Exception {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();		
		ReceiptConfiguration rcptCfg = new ReceiptConfiguration();
		rcptCfg.setNotifyReceiptToBusinessApplication(true);
		DeliveryConfiguration rcptDelSpec = new DeliveryConfiguration();
		rcptDelSpec.setId(pmode.getId() + "-ReceiptDelivery");
		rcptDelSpec.setDeliveryMethod(TestDeliveryMethod.class);
		rcptDelSpec.addSetting("specId", rcptDelSpec.getId());
		rcptCfg.setReceiptDelivery(rcptDelSpec);		
		pmode.getLeg(Label.REQUEST).setReceiptConfiguration(rcptCfg);
		DeliveryConfiguration delSpec = new DeliveryConfiguration();
		delSpec.setId(pmode.getId() + "-DefaultDelivery");
		delSpec.setDeliveryMethod(TestDeliveryMethod.class);
		delSpec.addSetting("specId", delSpec.getId());
		pmode.getLeg(Label.REQUEST).setDefaultDelivery(delSpec);
		
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		Receipt receipt = new Receipt();
		receipt.setMessageId(MessageIdUtils.createMessageId());
		receipt.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IReceiptEntity rEntity = storageManager.storeReceivedMessageUnit(receipt);
		storageManager.setProcessingState(rEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(rEntity);
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertNotNull(dm.settings);
		assertEquals(rcptDelSpec.getId(), dm.settings.get("specId"));
		assertEquals(1, dm.deliveredMsgUnits.size());
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				rEntity.getProcessingStates().get(rEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DONE, rEntity.getCurrentProcessingState().getState());
		
		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDelivered);		
	}
	
	@Test
	public void testIgnoreError() throws Exception {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		ErrorHandlingConfig errCfg = new ErrorHandlingConfig();
		errCfg.setNotifyErrorToBusinessApplication(false);
		UserMessageFlow umFlow = new UserMessageFlow();
		umFlow.setErrorHandlingConfiguration(errCfg);
		pmode.getLeg(Label.REQUEST).setUserMessageFlow(umFlow);
				
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		ErrorMessage error = new ErrorMessage();
		error.setMessageId(MessageIdUtils.createMessageId());
		error.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IErrorMessageEntity errEntity = storageManager.storeReceivedMessageUnit(error);
		storageManager.setPModeAndLeg(errEntity, new Pair<>(pmode, Label.REQUEST));
		storageManager.setProcessingState(errEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(errEntity);
				
		assertEquals(0, TestDeliveryMethod.instances.size());
		assertEquals(ProcessingState.READY_FOR_DELIVERY, 
				errEntity.getProcessingStates().get(errEntity.getProcessingStates().size() - 2).getState());			
		assertEquals(ProcessingState.DONE, errEntity.getCurrentProcessingState().getState());
		
		assertEquals(0, eventProc.events.size());
	}	
	
	@Test
	public void testErrorDefault() throws Exception {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();		
		ErrorHandlingConfig errCfg = new ErrorHandlingConfig();
		errCfg.setNotifyErrorToBusinessApplication(true);
		UserMessageFlow umFlow = new UserMessageFlow();
		umFlow.setErrorHandlingConfiguration(errCfg);
		pmode.getLeg(Label.REQUEST).setUserMessageFlow(umFlow);
		DeliveryConfiguration delSpec = new DeliveryConfiguration();
		delSpec.setId(pmode.getId() + "-DefaultDelivery");
		delSpec.setDeliveryMethod(TestDeliveryMethod.class);
		delSpec.addSetting("specId", delSpec.getId());
		pmode.getLeg(Label.REQUEST).setDefaultDelivery(delSpec);
		
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		ErrorMessage error = new ErrorMessage();
		error.setMessageId(MessageIdUtils.createMessageId());
		error.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IErrorMessageEntity errEntity = storageManager.storeReceivedMessageUnit(error);
		storageManager.setPModeAndLeg(errEntity, new Pair<>(pmode, Label.REQUEST));
		storageManager.setProcessingState(errEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(errEntity);
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertNotNull(dm.settings);
		assertEquals(delSpec.getId(), dm.settings.get("specId"));
		assertEquals(1, dm.deliveredMsgUnits.size());
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				errEntity.getProcessingStates().get(errEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DONE, errEntity.getCurrentProcessingState().getState());
		
		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDelivered);		
	}
	
	@Test
	public void testErrorSpecific() throws Exception {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();		
		ErrorHandlingConfig errCfg = new ErrorHandlingConfig();
		errCfg.setNotifyErrorToBusinessApplication(true);
		DeliveryConfiguration errDelSpec = new DeliveryConfiguration();
		errDelSpec.setId(pmode.getId() + "-ErrorDelivery");
		errDelSpec.setDeliveryMethod(TestDeliveryMethod.class);
		errDelSpec.addSetting("specId", errDelSpec.getId());
		errCfg.setErrorDelivery(errDelSpec);
		UserMessageFlow umFlow = new UserMessageFlow();
		umFlow.setErrorHandlingConfiguration(errCfg);
		pmode.getLeg(Label.REQUEST).setUserMessageFlow(umFlow);
		DeliveryConfiguration delSpec = new DeliveryConfiguration();
		delSpec.setId(pmode.getId() + "-DefaultDelivery");
		delSpec.setDeliveryMethod(TestDeliveryMethod.class);
		delSpec.addSetting("specId", delSpec.getId());
		pmode.getLeg(Label.REQUEST).setDefaultDelivery(delSpec);
		
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		ErrorMessage error = new ErrorMessage();
		error.setMessageId(MessageIdUtils.createMessageId());
		error.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IErrorMessageEntity errEntity = storageManager.storeReceivedMessageUnit(error);
		storageManager.setPModeAndLeg(errEntity, new Pair<>(pmode, Label.REQUEST));
		storageManager.setProcessingState(errEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(errEntity);
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertNotNull(dm.settings);
		assertEquals(errDelSpec.getId(), dm.settings.get("specId"));
		assertEquals(1, dm.deliveredMsgUnits.size());
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				errEntity.getProcessingStates().get(errEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DONE, errEntity.getCurrentProcessingState().getState());
		
		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDelivered);		
	}
	
	@Test
	public void testIgnorePullError() throws Exception {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		pmode.setMepBinding(EbMSConstants.ONE_WAY_PULL);
		ErrorHandlingConfig errCfg = new ErrorHandlingConfig();
		errCfg.setNotifyErrorToBusinessApplication(false);
		PullRequestFlow prFlow = new PullRequestFlow();
		prFlow.setErrorHandlingConfiguration(errCfg);
		pmode.getLeg(Label.REQUEST).addPullRequestFlow(prFlow);
				
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		ErrorMessage error = new ErrorMessage();
		error.setMessageId(MessageIdUtils.createMessageId());
		error.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IErrorMessageEntity errEntity = storageManager.storeReceivedMessageUnit(error);
		storageManager.setPModeAndLeg(errEntity, new Pair<>(pmode, Label.REQUEST));
		storageManager.setProcessingState(errEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(errEntity);
				
		assertEquals(0, TestDeliveryMethod.instances.size());
		assertEquals(ProcessingState.READY_FOR_DELIVERY, 
				errEntity.getProcessingStates().get(errEntity.getProcessingStates().size() - 2).getState());			
		assertEquals(ProcessingState.DONE, errEntity.getCurrentProcessingState().getState());
		
		assertEquals(0, eventProc.events.size());
	}	
	
	@Test
	public void testPullErrorWithDefault() throws Exception {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		pmode.setMepBinding(EbMSConstants.ONE_WAY_PULL);
		ErrorHandlingConfig errCfg = new ErrorHandlingConfig();
		errCfg.setNotifyErrorToBusinessApplication(true);
		PullRequestFlow prFlow = new PullRequestFlow();
		prFlow.setErrorHandlingConfiguration(errCfg);
		pmode.getLeg(Label.REQUEST).addPullRequestFlow(prFlow);
		
		DeliveryConfiguration delSpec = new DeliveryConfiguration();
		delSpec.setId(pmode.getId() + "-DefaultDelivery");
		delSpec.setDeliveryMethod(TestDeliveryMethod.class);
		delSpec.addSetting("specId", delSpec.getId());
		pmode.getLeg(Label.REQUEST).setDefaultDelivery(delSpec);
		
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		ErrorMessage error = new ErrorMessage();
		error.setMessageId(MessageIdUtils.createMessageId());
		error.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IErrorMessageEntity errEntity = storageManager.storeReceivedMessageUnit(error);
		storageManager.setPModeAndLeg(errEntity, new Pair<>(pmode, Label.REQUEST));
		storageManager.setProcessingState(errEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(errEntity);
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertNotNull(dm.settings);
		assertEquals(delSpec.getId(), dm.settings.get("specId"));
		assertEquals(1, dm.deliveredMsgUnits.size());			
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				errEntity.getProcessingStates().get(errEntity.getProcessingStates().size() - 2).getState());		
		assertEquals(ProcessingState.DONE, errEntity.getCurrentProcessingState().getState());
		
		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDelivered);		
	}	
	
	@Test
	public void testPullErrorSpecific() throws Exception {
		PMode pmode = HB2BTestUtils.create1WaySendPushPMode();
		pmode.setMepBinding(EbMSConstants.ONE_WAY_PULL);
		ErrorHandlingConfig errCfg = new ErrorHandlingConfig();
		errCfg.setNotifyErrorToBusinessApplication(true);
		DeliveryConfiguration delSpec = new DeliveryConfiguration();
		delSpec.setId(pmode.getId() + "-ErrorDelivery");
		delSpec.setDeliveryMethod(TestDeliveryMethod.class);
		delSpec.addSetting("specId", delSpec.getId());
		errCfg.setErrorDelivery(delSpec);
		PullRequestFlow prFlow = new PullRequestFlow();
		prFlow.setErrorHandlingConfiguration(errCfg);
		pmode.getLeg(Label.REQUEST).addPullRequestFlow(prFlow);
				
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		ErrorMessage error = new ErrorMessage();
		error.setMessageId(MessageIdUtils.createMessageId());
		error.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IErrorMessageEntity errEntity = storageManager.storeReceivedMessageUnit(error);
		storageManager.setPModeAndLeg(errEntity, new Pair<>(pmode, Label.REQUEST));
		storageManager.setProcessingState(errEntity, ProcessingState.READY_FOR_DELIVERY);
		
		HolodeckB2BCoreInterface.getDeliveryManager().deliver(errEntity);
		
		TestDeliveryMethod dm = TestDeliveryMethod.instances.get(0);
		assertNotNull(dm.settings);
		assertEquals(delSpec.getId(), dm.settings.get("specId"));
		assertEquals(1, dm.deliveredMsgUnits.size());			
		assertEquals(ProcessingState.OUT_FOR_DELIVERY, 
				errEntity.getProcessingStates().get(errEntity.getProcessingStates().size() - 2).getState());
		assertEquals(ProcessingState.DONE, errEntity.getCurrentProcessingState().getState());
		
		assertEquals(1, eventProc.events.size());
		assertTrue(eventProc.events.get(0) instanceof IMessageDelivered);		
	}	
	
	
	private IUserMessageEntity createUserMessage(boolean async) throws Exception {
		PMode pmode = HB2BTestUtils.create1WayReceivePMode();		
		DeliveryConfiguration delSpec = new DeliveryConfiguration();
		delSpec.setId(pmode.getId() + "-DefaultDelivery");
		delSpec.setDeliveryMethod(TestDeliveryMethod.class);
		delSpec.setAsyncDelivery(async);
		pmode.getLeg(Label.REQUEST).setDefaultDelivery(delSpec);
		
        HolodeckB2BCore.getPModeSet().add(pmode);

		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		userMessage.setPModeId(pmode.getId());
		StorageManager storageManager = HolodeckB2BCore.getStorageManager();
		IUserMessageEntity umEntity = storageManager.storeReceivedMessageUnit(userMessage);
		storageManager.setProcessingState(umEntity, ProcessingState.READY_FOR_DELIVERY);
		
		return umEntity;
	}	
}

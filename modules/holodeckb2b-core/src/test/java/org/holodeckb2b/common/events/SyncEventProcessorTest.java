package org.holodeckb2b.common.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.holodeckb2b.common.events.impl.MessageDeliveryFailure;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.pmode.EventHandlerConfig;
import org.holodeckb2b.common.pmode.Leg;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.MessageProccesingEventHandlingException;
import org.holodeckb2b.interfaces.events.IMessageDeliveryFailure;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class SyncEventProcessorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
	}
	
	@After
	public void cleanUp() {
		List<IMessageProcessingEventConfiguration> evtCfgs = HolodeckB2BCoreInterface
																			.getMessageProcessingEventConfiguration();
		List<String> ids = evtCfgs.stream().map(c -> c.getId()).collect(Collectors.toList());
		ids.stream().forEach(id -> HolodeckB2BCoreInterface.removeEventHandler(id));
	}
	
	@Test
	public void testPModeConfiguredEvent() throws PModeSetException, MessageProccesingEventHandlingException {

		ArrayList<IMessageProcessingEvent> pModeEvents = new ArrayList<IMessageProcessingEvent>();
		ArrayList<IMessageProcessingEvent> globalEvents = new ArrayList<IMessageProcessingEvent>();
		
		Map settings = new HashMap();		
		
		TestEventConfig globalConfig = new TestEventConfig();
		globalConfig.addEvent(IMessageDeliveryFailure.class);
		globalConfig.setFactoryClass(NullEventHandler.class.getName());
		settings.put("1", globalEvents);
		globalConfig.setHandlerSettings(settings);
		
		
		HolodeckB2BCoreInterface.registerEventHandler(globalConfig);
		
		PMode pmode = TestUtils.create1WaySendPushPMode();
		Leg leg = pmode.getLeg(Label.REQUEST);
		EventHandlerConfig config = new EventHandlerConfig();
		List<Class<? extends IMessageProcessingEvent>> list = new ArrayList<>();
		list.add(IMessageDeliveryFailure.class);
		config.setHandledEvents(list);
		config.setFactoryClass(NullEventHandler.class.getName());
		settings.put("1", pModeEvents);
		config.setHandlerSettings(settings);
		leg.addMessageProcessingEventConfiguration(config);
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		userMessage.setPModeId(pmode.getId());

		try {
			new SyncEventProcessor().raiseEvent(new MessageDeliveryFailure(userMessage, null));
		} catch (Throwable t) {
			t.printStackTrace();
			fail();			
		}
		
		assertEquals(1, pModeEvents.size());
		assertTrue(pModeEvents.get(0) instanceof IMessageDeliveryFailure);
		assertEquals(userMessage.getMessageId(), pModeEvents.get(0).getSubject().getMessageId());

		assertEquals(0, globalEvents.size());			
	}

	@Test
	public void testGlobalConfiguredEvent() throws PModeSetException, MessageProccesingEventHandlingException {
		
		ArrayList<IMessageProcessingEvent> pModeEvents = new ArrayList<IMessageProcessingEvent>();
		ArrayList<IMessageProcessingEvent> globalEvents = new ArrayList<IMessageProcessingEvent>();
		
		Map settings = new HashMap();		
		
		TestEventConfig globalConfig = new TestEventConfig();
		globalConfig.addEvent(IMessageDeliveryFailure.class);
		globalConfig.setFactoryClass(NullEventHandler.class.getName());
		settings.put("1", globalEvents);
		globalConfig.setHandlerSettings(settings);
				
		HolodeckB2BCoreInterface.registerEventHandler(globalConfig);
		
		PMode pmode = TestUtils.create1WaySendPushPMode();
		Leg leg = pmode.getLeg(Label.REQUEST);
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		userMessage.setPModeId(pmode.getId());
		
		try {
			new SyncEventProcessor().raiseEvent(new MessageDeliveryFailure(userMessage, null));
		} catch (Throwable t) {
			t.printStackTrace();
			fail();			
		}
		
		assertEquals(0, pModeEvents.size());
		
		assertEquals(1, globalEvents.size());
		assertTrue(globalEvents.get(0) instanceof IMessageDeliveryFailure);
		assertEquals(userMessage.getMessageId(), globalEvents.get(0).getSubject().getMessageId());
	}

	@Test
	public void testIgnoreExceptions() throws PModeSetException, MessageProccesingEventHandlingException {
		
		Map settings = new HashMap();		
		TestEventConfig globalConfig = new TestEventConfig();
		globalConfig.addEvent(IMessageDeliveryFailure.class);
		globalConfig.setFactoryClass(NullEventHandler.class.getName());
		settings.put("1", new MessageDeliveryException());
		globalConfig.setHandlerSettings(settings);
				
		HolodeckB2BCoreInterface.registerEventHandler(globalConfig);
		
		PMode pmode = TestUtils.create1WaySendPushPMode();
		Leg leg = pmode.getLeg(Label.REQUEST);
		HolodeckB2BCore.getPModeSet().add(pmode);
		
		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(MessageIdUtils.createMessageId());
		userMessage.setPModeId(pmode.getId());
		
		try {
			new SyncEventProcessor().raiseEvent(new MessageDeliveryFailure(userMessage, null));
		} catch (Throwable t) {
			t.printStackTrace();
			fail();			
		}
	}
}

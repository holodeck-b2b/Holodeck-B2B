package org.holodeckb2b.common.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

public class TestEventConfig implements IMessageProcessingEventConfiguration {

	private String id = UUID.randomUUID().toString();
	private List<Class<? extends IMessageProcessingEvent>> events = new ArrayList<>();
	private List<Class<? extends IMessageUnit>> msgUnits = new ArrayList<>();
	private String factoryClass;
	private Map<String, ?> settings;
	
	@Override
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public List<Class<? extends IMessageProcessingEvent>> getHandledEvents() {
		return events;
	}
	
	public void addEvent(Class<? extends IMessageProcessingEvent> eventClass) {
		events.add(eventClass);
	}

	@Override
	public List<Class<? extends IMessageUnit>> appliesTo() {
		return msgUnits;
	}
	
	public void addMsgUnit(Class<? extends IMessageUnit> msgUnit) {
		msgUnits.add(msgUnit);
	}
	
	@Override
	public String getFactoryClass() {
		return factoryClass;
	}
	
	public void setFactoryClass(String factory) {
		factoryClass = factory;
	}
	
	@Override
	public Map<String, ?> getHandlerSettings() {
		return settings;
	}

	public void setHandlerSettings(Map<String, ?> settings) {
		this.settings = settings;
	}

}

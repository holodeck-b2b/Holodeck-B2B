/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.common.testhelpers.pmode;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class EventHandlerConfig implements IMessageProcessingEventConfiguration {

    private String  id;
    private List<Class<? extends IMessageProcessingEvent>> handledEvents;
    private List<Class<? extends IMessageUnit>> forMessageUnits;
    private String  factoryClass;
    private Map<String, ?>  settings;

    @Override
    public String getId() {
        return id;
    }

    public void setId(final String newId) {
        this.id = newId;
    }

    @Override
    public List<Class<? extends IMessageProcessingEvent>> getHandledEvents() {
        return handledEvents;
    }

    public void setHandledEvents(final List<Class<? extends IMessageProcessingEvent>> newHandledEvents) {
        if (!Utils.isNullOrEmpty(handledEvents))
            this.handledEvents = new ArrayList<>(handledEvents);
        else
            this.handledEvents = null;
    }

    @Override
    public List<Class<? extends IMessageUnit>> appliesTo() {
        return forMessageUnits;
    }

    public void setAppliesTo(final List<Class<? extends IMessageUnit>> newAppliesTo) {
        if (!Utils.isNullOrEmpty(newAppliesTo))
            this.forMessageUnits = new ArrayList<>(newAppliesTo);
        else
            this.forMessageUnits = null;
    }

    @Override
    public String getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(final String newFactoryClass) {
        this.factoryClass = newFactoryClass;
    }

    @Override
    public Map<String, ?> getHandlerSettings() {
        return settings;
    }

    public void setHandlerSettings(Map<String, ?> newSettings) {
        if (!Utils.isNullOrEmpty(newSettings))
            this.settings = new HashMap<>(newSettings);
        else
            this.settings = null;
    }
}

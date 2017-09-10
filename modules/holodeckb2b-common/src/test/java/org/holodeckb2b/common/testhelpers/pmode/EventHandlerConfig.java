/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.common.testhelpers.pmode;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.processing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
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
        if (!Utils.isNullOrEmpty(newHandledEvents))
            this.handledEvents = new ArrayList<>(newHandledEvents);
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

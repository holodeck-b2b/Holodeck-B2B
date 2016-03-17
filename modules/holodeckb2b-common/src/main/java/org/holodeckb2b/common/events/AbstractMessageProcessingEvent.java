/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.events;

import java.util.Date;
import java.util.UUID;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is an abstract base class for implementations of {@link IMessageProcessingEvent}. Although all methods from the 
 * interface are implemented by this class it is defined as abstract as each event type should have its own class.
 * <p>In addition to the required <i>getters</i> this class adds <i>setters</i> for the event message and subject. The 
 * identifier and timestamp are fixed and set when the event instance is created. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */
public abstract class AbstractMessageProcessingEvent implements IMessageProcessingEvent {

    /**
     * The identifier of this event
     */
    private String        id;
    /**
     * The timestamp when the event was created
     */
    private Date          timestamp;
    /**
     * A short description about what happened
     */
    private String        message;
    /**
     * The message unit in which processing the event occurred
     */
    private IMessageUnit  subject;
    
    /**
     * Creates a new event for the given message unit. Initializes the identifier and timestamp.
     * 
     * @param subject   The message unit in which processing the event occurred.
     */
    public AbstractMessageProcessingEvent(final IMessageUnit subject) {
        this.id = "EV-" + UUID.randomUUID().toString();
        this.timestamp = new Date();
        this.subject = subject;
    }
    
    /**
     * Creates a new event for the given message unit. Initializes the identifier and timestamp and sets the event 
     * description to the provided text.
     * 
     * @param subject       The message unit in which processing the event occurred.
     * @param eventMessage  The short description of the event
     */
    public AbstractMessageProcessingEvent(final IMessageUnit subject, final String eventMessage) {
       this(subject);
       this.message = eventMessage;
    }
    
    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public IMessageUnit getSubject() {
        return subject;
    }
    
    public void setSubject(IMessageUnit subject) {
        this.subject = subject;
    }
}

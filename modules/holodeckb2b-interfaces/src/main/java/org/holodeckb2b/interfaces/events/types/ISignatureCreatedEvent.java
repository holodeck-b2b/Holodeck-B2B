/*
 * Copyright (C) 2016 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published 
 * by the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.events.types;

import java.util.Collection;
import org.holodeckb2b.interfaces.events.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.security.IPayloadDigest;

/**
 * Is the <i>message processing event</i> that indicates that a signature is created for a <b>User Message Unit</b> that 
 * is being sent. This event is to inform the business application (or extensions) about the digests that have been 
 * created as part of the message level signature so it can check data integrity both for the sent message but also of
 * a resulting <i>NRR</i>.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since 2.1.0
 */
public interface ISignatureCreatedEvent extends IMessageProcessingEvent {
    
    /**
     * Gets the information on the digests that were calculated for the payloads in the User Message that is the <i>
     * subject</i> of this event. 
     * 
     * @return  A <code>Collection</code> of {@link IPayloadDigest} objects with information on the calculated digests.
     */
    public Collection<IPayloadDigest>   getPayloadDigests();    
}

/*
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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

package org.holodeckb2b.common.delivery;

import java.util.Map;

/**
 * Is used to specify how a message unit should be delivered to the business application. The specification consists of
 * the {@link IMessageDelivererFactory} that should be used for the delivery and the settings needed to configure the
 * delivery method. The delivery specification is part of the P-Mode that governs the message exchange.
 * <p>For each delivery specification Holodeck B2B will create a factory with the given settings. To allow reuse of a
 * delivery method instance (i.e. a configured delivered method) the [optional] id of a delivery specification will be
 * used by Holodeck B2B to determine if it can reuse an existing factory.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IMessageDelivererFactory
 * @see IMessageDeliverer
 */
public interface IDeliverySpecification {

    /**
     * Returns the id of this delivery specification. This id is used by Holodeck B2B to determine whether it must 
     * create and configure a new {@link IMessageDelivererFactory}. If no id is returned Holodeck B2B will always create
     * a new factory.
     * <p><b>NOTE: </b>Configurations should take care of uniquely identifying delivery specifications as Holodeck B2B
     * will only use the id to check for equivalence.
     * 
     * @return  The id of this delivery specification or <code>null</code> if this specification has no id.
     */
    String getId();
    
    /**
     * Gets the class name of the {@link IMessageDelivererFactory} implementation that should be used to create actual 
     * {@link IMessageDeliverer} objects.
     * 
     * @return  Class name of the {@link IMessageDelivererFactory} implementation to use for creating actual message
     *          deliverers
     */
    String getFactory();
    
    /**
     * Returns the settings that should be used to configure the message delivery.
     * Holodeck B2B will not process the settings and just pass them to the factory class.
     * 
     * @return The settings to use for this specific delivery
     */
    Map<String, ?> getSettings();
}

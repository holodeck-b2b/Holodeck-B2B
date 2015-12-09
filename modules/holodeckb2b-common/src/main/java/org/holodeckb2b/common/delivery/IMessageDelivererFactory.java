/**
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
import org.holodeckb2b.common.pmode.IPMode;

/**
 * Defines the interface for factoring pre-configured {@link IMessageDeliverer} implementations.   
 * <p>Message delivery to the business application is not done by the Holodeck B2B core but left to special <i>message 
 * deliverers</i>. This makes it possible to support different protocols by adding new deliverers. To allow for 
 * optimization of resources Holodeck B2B uses a factory to create the deliverers. 
 * <p>Because the delivery may need some configuration the factory is initialized with a set of parameters before 
 * deliverers are requested. The parameters to use for an actual delivery are specified by an {@link IDeliverySpecification}
 * which should be configured in the P-Mode of the message unit to be delivered. As the factory is specific for each
 * set of parameters Holodeck B2B will create a factory per {@link IDeliverySpecification}.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IMessageDeliverer
 * @see IDeliverySpecification
 * @see IPMode
 */
public interface IMessageDelivererFactory {
   
    /**
     * Initializes the factory with the specific settings to use for instantiating {@link IMessageDeliverer} objects.
     * <p>This method is called once by Holodeck B2B before requesting message delivers. 
     * 
     * @param settings    The settings to use for initialization of the factory
     * @throws MessageDeliveryException When the factory is unable to successfully initialize itself and therefor can 
     *                                  not create message delivers.
     */
    void init(Map<String, ?> settings) throws MessageDeliveryException;
    
    /**
     * Creates a {@link IMessageDeliverer} that can be used to deliver a message unit to
     * the consuming business application.
     * 
     * @return  The {@link IMessageDeliverer} to use for the delivery of the message unit
     * @throws  MessageDeliveryException 
     */
    IMessageDeliverer createMessageDeliverer() throws MessageDeliveryException;
}

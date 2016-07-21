/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.delivery;

import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * Defines the interface of the <i>"message deliverer"</i> that is used by the Holodeck B2B core to deliver areceived
 * message unit to the business application.
 * <p>It is the responsibility of the <i>message deliverer</i> to correctly deliver the message unit to the business
 * application. Holodeck B2B has no knowledge of the protocol being used to communicate with the business application.
 * The assumption is that that a successful execution of the {@link #deliver(org.holodeckb2b.common.messagemodel.IMessageUnit)}
 * method means that the message unit is successfully delivered to the business application.
 * <p>This decoupling of internal and external interface makes it easy to implement different protocols for message
 * delivery. Each protocol probably has its own set of configuration parameters and optimal connection usage. Therefor
 * Holodeck B2B uses a factory class to get instances of a pre-configured deliverer.
 * <p>This means that implementing a <i>delivery method</i> requires two classes, one implementing this
 * interface to do the actual delivery of the messages and one implementing {@link IMessageDelivererFactory} that will
 * create the pre-configured deliverers.
 * <p>Which <i>delivery method</i> and with which settings should be used for a message unit is configured in the P-Mode.
 *
 * @see IMessageDelivererFactory
 * @see IPMode
 * @see IDeliverySpecification
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IMessageDeliverer {

    /**
     * Delivers the message unit to the business application. Delivery is not limited to user messages, depending on
     * configuration also signal messages may be delivered.
     * <p>When this method finishes successfully, i.e. without throwing an exception, Holodeck B2B assumes the message
     * is or certainly will be delivered to the business application. This means that it is valid to send out a
     * <i>Receipt</i> to the sending MSH. It also implies that all payload files can safely be deleted, so this method
     * is responsible for copying all content.
     *
     * @param rcvdMsgUnit                   The {@link IMessageUnit} to be delivered to the business application
     * @throws MessageDeliveryException     When delivery of the message unit to the business application fails
     */
    void deliver(IMessageUnit rcvdMsgUnit) throws MessageDeliveryException;

}

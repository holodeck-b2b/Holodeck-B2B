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

package org.holodeckb2b.common.pmode;

import org.holodeckb2b.common.delivery.IDeliverySpecification;
import org.holodeckb2b.common.general.ReplyPattern;

/**
 * Describes the general P-Mode parameters for sending receipt signals. The ebMS V3 Core Specification does not specify 
 * in detail when and how receipt signals should be used. It only specifies P-Mode parameters for whether the receipt
 * should be send, the reply pattern to use and the address where to sent the receipts to. 
 * <br>These parameters are defined in appendix D of the Core Specification as part of the P-Mode parameter group 
 * Security. As they are not directly related to security settings we moved them to a separate <i>"Receipt"</i> group. 
 * <p>The content of the signal are not strictly defined and are left open for further specification in profiles (like 
 * AS4). Currently Holodeck B2B will always generate a receipt signal as specified in the AS4 profile. Future version of 
 * this interface may introduce the option to specify the <i>"receipt content generator"</i>.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IReceiptConfiguration {

    /**
     * Gets the indication how the Receipt signal is to be sent: as a callback, or synchronously in the back-channel 
     * response. Note that when the receipt should be sent via a callback the address where to deliver the receipt must
     * also be specified.
     * <p>NOTE: When the message to acknowledge is pulled the Receipt can only be sent using a callback. 
     * 
     * @return  {@link ReplyPattern#CALLBACK} when the receipt should be sent as a callback,<br>
     *          {@link ReplyPattern#RESPONSE} when the receipt should be sent as a response.
     */
    public ReplyPattern getPattern();
    
    /**
     * Gets the URI where receipt signals should be sent to in case the <i>callback</i> reply pattern is used.
     * 
     * @return The URI where to sent receipt signals to
     */
    public String getTo();
    
    /**
     * Gets the indication whether the connected business application should be notified in case a Receipt signal is 
     * received for a sent message. 
     * 
     * @return  <code>true</code> when the business application should be notified on receipts,<br>
     *          <code>false</code> when the business application does not need to be notified on receipts.
     */
    public boolean shouldNotifyReceiptToBusinessApplication();
    
    /**
     * Gets the <i>delivery specification</i> specific for the delivery of Receipt signal message units to the connected 
     * business application. Whether receipt should be delivered to the business application is configured through a
     * {@link #shouldNotifyReceiptToBusinessApplication()}
     * <p>If no specific delivery is, this method should return <code>null</code> so the default delivery mechanism of
     * the leg will be used (see {@link ILeg#getDefaultDelivery()}). 
     * 
     * @return An {@link IDeliverySpecification} object containing the configuration of the message delivery specific
     *         for Receipt signals
     */
    public IDeliverySpecification getReceiptDelivery();
}

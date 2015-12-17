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
package org.holodeckb2b.interfaces.pmode;



import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.general.ReplyPattern;

/**
 * Describes the general P-Mode parameters for processing receipt signals. 
 * <p>The ebMS V3 Core Specification does not specify in detail when and how receipt signals should be used. It only 
 * specifies P-Mode parameters for whether the receipt should be send, the reply pattern to use and the address where 
 * to sent the receipts to if to sent as a response. 
 * <p>These parameters are defined in appendix D of the Core Specification as part of the P-Mode parameter group 
 * <b>Security</b>. As they are not directly related to security settings we moved them to a separate <i>"Receipt"</i> 
 * group.<br> 
 * The ebMS specification defines the <b>PMode[1].Security.SendReceipt</b> parameter that indicate whether a Receipt
 * should be sent for a user message. This parameter is not directly implemented in Holodeck B2B but its value derived
 * from the existence of a receipt configuration for a leg, i.e. <b>PMode[1].Security.Receipt</b> := 
 * <code>{@link ILeg#getReceiptConfiguration()} != null</code>.
 * <p>The content of the signal are not strictly defined and are left open for further specification in profiles (like 
 * AS4). Currently Holodeck B2B will always generate a receipt signal as specified in the AS4 profile (see section 5.1.8
 * of the profile). Future version of this interface may introduce the option to specify the <i>"receipt content 
 * generator"</i>.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IReceiptConfiguration {

    /**
     * Gets the indication how the Receipt signal is to be sent: as a callback, or synchronously on the back-channel,
     * i.e. as response.
     * <p>Note that when the receipt should be sent via a callback the address where to deliver the receipt must
     * also be specified, i.e. {@link #getTo()} MUST return a URL. 
     * <p>NOTE: When the user message to acknowledge is pulled the Receipt can only be sent using a callback because 
     * the user message is already received on the back channel of the pull request. The value of this method is ignored
     * 
     * @return  {@link ReplyPattern#CALLBACK} when the receipt should be sent as a callback,<br>
     *          {@link ReplyPattern#RESPONSE} when the receipt should be sent as a response.
     */
    public ReplyPattern getPattern();
    
    /**
     * Gets the URL where receipt signals should be sent to in case the <i>callback</i> reply pattern is used.
     * 
     * @return The URL where to sent the receipt signals to
     */
    public String getTo();
    
    /**
     * Gets the indication whether the connected business application should be notified in case a Receipt signal is 
     * received or is missing for a sent message. 
     * <p>Note that the ebMS specifications do not specify a specific parameter for this. There is only the parameter 
     * <b>PMode[1].ErrorHandling.Report.MissingReceiptNotifyProducer</b> defined in the AS4 Profile to indicate whether 
     * the business application should be notified about a missing receipt, but not about receiving one.<br>
     * This method applies to both situations, receiving or missing an expected receipt. It is not possible to configure
     * these situations separately.
     * <p>Also note that for detecting a missing receipt an interval to wait for a Receipt has to be specified. This
     * is however part of the AS4 Reception Awareness feature and therefore also part of its configuration ({@link
     * IReceptionAwareness}).
     * <p>Notifying the business application about receipts is done using a standard <i>message delivery mechanism</i> 
     * which is configured by a {@link IDeliverySpecification}. If needed a specific delivery can be configured for 
     * receipt notifications by providing it through {@link #getReceiptDelivery()}. 
     * 
     * @return  <code>true</code> when the business application should be notified on receipts,<br>
     *          <code>false</code> when the business application does not need to be notified on receipts.
     */
    public boolean shouldNotifyReceiptToBusinessApplication();
    
    /**
     * Gets the <i>delivery specification</i> specific for the delivery of notifications related to the Receipt signal 
     * message. 
     * <p>If no specific delivery is needed this method should return <code>null</code>, in which case the default 
     * delivery mechanism of the leg will be used ({@link ILeg#getDefaultDelivery()}). 
     * 
     * @return An {@link IDeliverySpecification} object containing the configuration of the message delivery specific
     *         for Receipt signals
     */
    public IDeliverySpecification getReceiptDelivery();
}

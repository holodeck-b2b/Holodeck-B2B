/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import java.io.Serializable;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;
import org.simpleframework.xml.Element;

/**
 * Contains the parameters related to the configuration of Receipt handling.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class ReceiptConfiguration implements IReceiptConfiguration, Serializable {
	private static final long serialVersionUID = -7063330308596279624L;

    @Element (name = "ReplyPattern", required = false)
    private String replyPattern;

    @Element (name = "To", required = false)
    private String replyTo;

    @Element (name = "NotifyReceiptToBusinessApplication", required = false)
    private Boolean notifyReceiptToBusinessApp;

    @Element (name = "ReceiptDelivery", required = false)
    private DeliveryConfiguration receiptDelivery;

    /**
     * Default constructor creates a new and empty <code>ReceiptConfiguration</code> instance.
     */
    public ReceiptConfiguration() {}

    /**
     * Creates a new <code>ReceiptConfiguration</code> instance using the parameters from the provided {@link
     * IReceiptConfiguration} object.
     *
     * @param source The source object to copy the parameters from
     */
    public ReceiptConfiguration(final IReceiptConfiguration source) {
        this.replyPattern = source.getPattern() != null ? source.getPattern().name() : null;
        this.replyTo = source.getTo();
        this.notifyReceiptToBusinessApp = source.shouldNotifyReceiptToBusinessApplication() ? true : null;	        
        this.receiptDelivery = source.getReceiptDelivery() != null ?
                                    					new DeliveryConfiguration(source.getReceiptDelivery()) : null;
    }

    @Override
    public ReplyPattern getPattern() {
        return Utils.isNullOrEmpty(replyPattern) ? null : ReplyPattern.valueOf(replyPattern.toUpperCase());
    }

    public void setPattern(final ReplyPattern pattern) {
        this.replyPattern = pattern.toString();
    }

    @Override
    public String getTo() {
        return replyTo;
    }

    public void setTo(final String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public boolean shouldNotifyReceiptToBusinessApplication() {
        return notifyReceiptToBusinessApp != null ? notifyReceiptToBusinessApp.booleanValue() : false;
    }

    public void setNotifyReceiptToBusinessApplication(final boolean notify) {
        this.notifyReceiptToBusinessApp = notify;
    }

    @Override
    public IDeliverySpecification getReceiptDelivery() {
        return receiptDelivery;
    }

    public void setReceiptDelivery(final DeliveryConfiguration deliveryConfig) {
        this.receiptDelivery = deliveryConfig;
    }

}

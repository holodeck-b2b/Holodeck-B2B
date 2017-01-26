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

import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.pmode.IReceiptConfiguration;

/**
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ReceiptConfiguration implements IReceiptConfiguration {

    private ReplyPattern            pattern;
    private String                  replyTo;
    private boolean                 notify;
    private DeliverySpecification   rcptDeliverySpec;

    @Override
    public ReplyPattern getPattern() {
        return pattern;
    }

    public void setPattern(final ReplyPattern pattern) {
        this.pattern = pattern;
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
        return notify;
    }

    public void setNotifyReceiptToBusinessApplication(final boolean notify) {
        this.notify = notify;
    }

    @Override
    public IDeliverySpecification getReceiptDelivery() {
        return rcptDeliverySpec;
    }

    public void setReceiptDelivery(final DeliverySpecification deliverySpec) {
        this.rcptDeliverySpec = deliverySpec;
    }

}

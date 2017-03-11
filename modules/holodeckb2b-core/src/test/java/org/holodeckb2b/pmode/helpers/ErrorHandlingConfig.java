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
package org.holodeckb2b.pmode.helpers;

import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ErrorHandlingConfig implements IErrorHandling {

    private ReplyPattern            pattern;
    private String                  errorsTo;
    private Boolean                 addSOAPFault;
    private Boolean                 reportOnError;
    private Boolean                 reportOnReceipt;
    private boolean                 notify;
    private DeliverySpecification   errorDelivery;

    @Override
    public ReplyPattern getPattern() {
        return pattern;
    }

    public void setPattern(final ReplyPattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getReceiverErrorsTo() {
        return errorsTo;
    }

    public void setReceiverErrorsTo(final String to) {
        this.errorsTo = to;
    }

    @Override
    public Boolean shouldAddSOAPFault() {
        return addSOAPFault;
    }

    public void setAddSOAPFault(final Boolean shouldAddSOAPFault) {
        this.addSOAPFault = shouldAddSOAPFault;
    }

    @Override
    public Boolean shouldReportErrorOnError() {
        return reportOnError;
    }

    public void setReportErrorOnError(final Boolean reportOnError) {
        this.reportOnError = reportOnError;
    }

    @Override
    public Boolean shouldReportErrorOnReceipt() {
        return reportOnReceipt;
    }

    public void setReportErrorOnReceipt(final Boolean reportOnReceipt) {
        this.reportOnReceipt = reportOnReceipt;
    }

    @Override
    public boolean shouldNotifyErrorToBusinessApplication() {
        return notify;
    }

    public void setNotifyErrorToBusinessApplication(final boolean notify) {
        this.notify = notify;
    }

    @Override
    public IDeliverySpecification getErrorDelivery() {
        return errorDelivery;
    }

    public void setErrorDelivery(final DeliverySpecification deliverySpec) {
        this.errorDelivery = deliverySpec;
    }
}

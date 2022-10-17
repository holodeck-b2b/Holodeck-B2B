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
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.general.ReplyPattern;
import org.holodeckb2b.interfaces.pmode.IErrorHandling;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

/**
 * Contains the parameters related to the handling of ebMS Errors.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class ErrorHandlingConfig implements IErrorHandling, Serializable {
	private static final long serialVersionUID = -3278272895696757426L;

    @Element (name = "ReplyPattern", required = false)
    private String replyPattern = "";

    @Element (name = "ReceiverErrorsTo", required = false)
    private String errorsTo = null;

    @Element (name = "AddSOAPFault", required = false)
    private Boolean addSOAPFault;

    @Element (name = "ReportErrorOnError", required = false)
    private Boolean reportOnError = null;

    @Element (name = "ReportErrorOnReceipt", required = false)
    private Boolean reportOnReceipt = null;

    @Element (name = "NotifyErrorToBusinessApplication", required = false)
    private Boolean notifyBusinessApplication;

    @Element ( name = "ErrorDelivery", required = false)
    private DeliveryConfiguration errorDelivery;

    /**
     * This method ensures that the {@link DeliveryConfiguration} for the error delivery method gets an unique id
     * based on the P-Mode id. Because we do not know the P-Mode id here we use the <i>commit</i> functionality of the
     * Simple framework (see <a href="http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state">
     * http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state</a>). We put the <code>
     * errorDelivery</code> object in the deserialization session so {@link PMode#solveDepencies(java.util.Map)} can
     * set the id using the P-Mode id.
     *
     * @param dependencies The Simple session object.
     */
    @Commit
    public void setDepency(final Map dependencies) {
        if (errorDelivery != null) {
            // Because multiple ErrorDelivery elements can exist in the P-Mode document we make sure it get a unique id
            int i = 0;
            while (dependencies.containsKey("ErrorDelivery-" + i)) i++;
            dependencies.put("ErrorDelivery-"+i, errorDelivery);
        }
    }

    /**
     * Default constructor creates a new and empty <code>ErrorHandlingConfig</code> instance.
     */
    public ErrorHandlingConfig() {}

    /**
     * Creates a new <code>ErrorHandlingConfig</code> instance using the parameters from the provided {@link
     * IErrorHandling}  object.
     *
     * @param source The source object to copy the parameters from
     */
    public ErrorHandlingConfig(final IErrorHandling source) {
        this.replyPattern = source.getPattern() != null ? source.getPattern().toString() : null;
        this.errorsTo = source.getReceiverErrorsTo();
        this.addSOAPFault = source.shouldAddSOAPFault();
        this.reportOnError = source.shouldReportErrorOnError();
        this.reportOnReceipt = source.shouldReportErrorOnReceipt();
        this.notifyBusinessApplication = source.shouldNotifyErrorToBusinessApplication();
        this.errorDelivery = source.getErrorDelivery()!= null ? 
        											new DeliveryConfiguration(source.getErrorDelivery()) : null;
    }

    @Override
    public ReplyPattern getPattern() {
        return Utils.isNullOrEmpty(replyPattern) ? ReplyPattern.RESPONSE 
        										 : ReplyPattern.valueOf(replyPattern.toUpperCase());
    }

    public void setPattern(final ReplyPattern pattern) {
        this.replyPattern = pattern.toString();
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
        return notifyBusinessApplication != null ? notifyBusinessApplication.booleanValue() : false;
    }

    public void setNotifyErrorToBusinessApplication(final boolean notify) {
        this.notifyBusinessApplication = notify;
    }

    @Override
    public IDeliverySpecification getErrorDelivery() {
        return errorDelivery;
    }

    public void setErrorDelivery(final IDeliverySpecification deliverySpec) {
        this.errorDelivery = new DeliveryConfiguration(deliverySpec);
    }
}

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

package org.holodeckb2b.pmode.impl;

import java.util.Map;
import org.holodeckb2b.common.delivery.IDeliverySpecification;
import org.holodeckb2b.common.general.ReplyPattern;
import org.holodeckb2b.common.pmode.IErrorHandling;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

/**
 * Represent the <code>ErrorHandling</code> elements from the P-Mode XML document as defined in the P-Mode XML schema 
 * (namespace=http://holodeck-b2b.org/schemas/2014/06/pmode). Although the error handling configuration for the pull
 * request flow is more limited this object is used for both elements as the pull request ones is a restriction of the
 * one in the user message flow.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IErrorHandling
 */
public class ErrorHandling implements IErrorHandling {

    @Element (name = "ReplyPattern", required = false)
    private String replyPattern = "";
    
    @Element (name = "ReceiverErrorsTo", required = false)
    private String to = null;

    @Element (name = "ReportErrorOnError", required = false)
    private Boolean reportOnError = null;
    
    @Element (name = "ReportErrorOnReceipt", required = false)
    private Boolean reportOnReceipt = null;
    
    @Element (name = "NotifyErrorToBusinessApplication", required = false)
    private Boolean notifyBusinessApplication = Boolean.FALSE;
    
    @Element ( name = "ErrorDelivery", required = false)
    private DeliverySpecification errorDelivery;
    
    /**
     * This method ensures that the {@link DeliverySpecification} for the error delivery method gets an unique id
     * based on the P-Mode id. Because we do not know the P-Mode id here we use the <i>commit</i> functionality of the
     * Simple framework (see <a href="http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state">
     * http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state</a>). We put the <code>
     * errorDelivery</code> object in the deserialization session so {@link PMode#solveDepencies(java.util.Map)} can
     * set the id using the P-Mode id.
     * 
     * @param dependencies The Simple session object.
     */
    @Commit
    public void setDepency(Map dependencies) {
        if (errorDelivery != null) {
            // Because multiple ErrorDelivery elements can exist in the P-Mode document we make sure it get a unique id
            int i = 0;
            while (dependencies.containsKey("ErrorDelivery-" + i)) i++;
            dependencies.put("ErrorDelivery-"+i, errorDelivery); 
        }
    }
    
    @Override
    public ReplyPattern getPattern() {
        
        ReplyPattern r = null;
        
        if (this.replyPattern != null) {
        
            r = ReplyPattern.valueOf(this.replyPattern.toUpperCase());
            
        }

        return r != null ? r : ReplyPattern.RESPONSE;
    }


    @Override
    public String getReceiverErrorsTo() {
        return to;
    }

    @Override
    public Boolean shouldReportErrorOnError() {
        return reportOnError;
    }
    
    @Override
    public Boolean shouldReportErrorOnReceipt() {
        return reportOnReceipt;
    }
    
    @Override
    public boolean shouldNotifyErrorToBusinessApplication() {
        return notifyBusinessApplication;
    }

    @Override
    public IDeliverySpecification getErrorDelivery() {
        return errorDelivery;
    }
    
}

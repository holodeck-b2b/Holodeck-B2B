/*
 * Copyright (C) 2014 The Holodeck B2B Team, Bram Bakx
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
package org.holodeckb2b.pmode.xml;

import java.util.Map;
import org.holodeckb2b.common.delivery.IDeliverySpecification;
import org.holodeckb2b.common.general.ReplyPattern;
import org.holodeckb2b.common.pmode.IReceiptConfiguration;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

/**
 *
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class ReceiptConfiguration implements IReceiptConfiguration{
  
    @Element (name = "ReplyPattern", required = false)
    private String replyPattern;
    
    @Element (name = "To", required = false)
    private String to;
    
    @Element (name = "NotifyReceiptToBusinessApplication", required = false)
    private Boolean notifyReceiptToBusinessApp = Boolean.FALSE;

    @Element (name = "ReceiptDelivery", required = false)
    private DeliverySpecification receiptDelivery;
    
    /**
     * This method ensures that the {@link DeliverySpecification} for the receipt delivery method gets an unique id
     * based on the P-Mode id. Because we do not know the P-Mode id here we use the <i>commit</i> functionality of the
     * Simple framework (see <a href="http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state">
     * http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state</a>). We put the <code>
     * receiptDelivery</code> object in the deserialization session so {@link PMode#solveDepencies(java.util.Map)} can
     * set the id using the P-Mode id.
     * 
     * @param dependencies The Simple session object.
     */
    @Commit
    public void setDepency(Map dependencies) {
        if (receiptDelivery != null) {
            // Because multiple ReceiptDelivery elements can exist in the P-Mode document when we enable Two-Way MEPs,
            // we make sure it get a unique id
            int i = 0;
            while (dependencies.containsKey("ReceiptDelivery-" + i)) i++;
            dependencies.put("ReceiptDelivery-"+i, receiptDelivery); 
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
    public String getTo() {
        return this.to;
    }
    
    @Override
    public boolean shouldNotifyReceiptToBusinessApplication() {
        return notifyReceiptToBusinessApp;
    }
    
    
    @Override
    public IDeliverySpecification getReceiptDelivery() {
        return receiptDelivery;
    }
}

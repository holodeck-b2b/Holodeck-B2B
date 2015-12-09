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
package org.holodeckb2b.ebms3.mmd.xml;

import org.holodeckb2b.common.general.ITradingPartner;
import org.simpleframework.xml.Element;

/**
 * Represents the <code>PartyInfo</code> element in the MMD document. This element
 * contains information on the sender and [intended] receiver of the message.
 * <p>The <code>To</code> and <code>From</code> elements share the same datatype,
 * so there is also one class ({@see TradingPartner}) used to represents these elements.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PartyInfo {
    
    @Element(name = "From", required = false)
    private TradingPartner      sender;
    
    @Element(name = "To", required = false)
    private TradingPartner      receiver;

    /**
     * @return the sender
     */
    public ITradingPartner getSender() {
        return sender;
    }

    /**
     * @param sender the sender to set
     */
    public void setSender(ITradingPartner sender) {
        if (sender != null)
            this.sender = new TradingPartner(sender);
        else 
            this.sender = null;
    }

    /**
     * @return the receiver
     */
    public ITradingPartner getReceiver() {
        return receiver;
    }

    /**
     * @param receiver the receiver to set
     */
    public void setReceiver(ITradingPartner receiver) {
        if (receiver != null)
            this.receiver = new TradingPartner(receiver);
        else
            receiver = null;
    }
    
}

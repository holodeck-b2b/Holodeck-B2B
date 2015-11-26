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
package org.holodeckb2b.pmode.xml;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.holodeckb2b.common.as4.pmode.IAS4Leg;
import org.holodeckb2b.common.as4.pmode.IReceptionAwareness;
import org.holodeckb2b.common.delivery.IDeliverySpecification;
import org.holodeckb2b.common.pmode.IProtocol;
import org.holodeckb2b.common.pmode.IPullRequestFlow;
import org.holodeckb2b.common.pmode.IReceiptConfiguration;
import org.holodeckb2b.common.pmode.IUserMessageFlow;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;


/**
 * @author Bram Bakx <bram at holodeck-b2b.org>
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */

public class Leg implements IAS4Leg {
     
    @Element (name = "Protocol", required = false)
    private Protocol protocol;
    
    @Element (name = "Receipt", required = false)
    private ReceiptConfiguration receipt;
    
    @Element (name = "ReceptionAwareness", required = false)
    private ReceptionAwareness rcptAwareness;
    
    @Element ( name = "DefaultDelivery", required = false)
    private DeliverySpecification defaultDelivery;
    
    @ElementList (entry = "PullRequestFlow", type = PullRequestFlow.class, inline = true, required = false)
    private ArrayList<IPullRequestFlow> pullRequestFlows;
    
    @Element (name = "UserMessageFlow", required = false)
    private UserMessageFlow userMessageFlow;
    
    @Attribute (name = "label", required = false)
    private Label label;
    
    /**
     * This method ensures that the {@link DeliverySpecification} for the default delivery method gets an unique id
     * based on the P-Mode id. Because we do not know the P-Mode id here we use the <i>commit</i> functionality of the
     * Simple framework (see <a href="http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state">
     * http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state</a>). We put the <code>
     * defaultDelivery</code> object in the deserialization session so {@link PMode#solveDepencies(java.util.Map)} can
     * set the id using the P-Mode id.
     * 
     * @param dependencies The Simple session object.
     */
    @Commit
    public void setDepency(Map dependencies) {
        if (defaultDelivery != null) {
            // Because multiple DefaultDelivery elements can exist in the P-Mode document when we enable Two-Way MEPs,
            // we make sure it get a unique id
            int i = 0;
            while (dependencies.containsKey("DefaultDelivery-" + i)) i++;
            dependencies.put("DefaultDelivery-"+i, defaultDelivery); 
        }
    }
    
    /**
     * Returns the leg label
     * 
     * @return The leg <code>label</code>
     */
    @Override
    public Label getLabel() {
        return this.label;
    }
    
    /**
     * Returns the leg protocol
     * 
     * @return The leg <code>protocol</code>
     */
    @Override
    public IProtocol getProtocol() {
        return this.protocol;
    }
    
    /**
     * Returns the leg default delivery
     * 
     * @return The leg <code>default delivery</code>
     */
    @Override
    public IDeliverySpecification getDefaultDelivery() {
        return defaultDelivery;
    }
    
    /**
     * Returns the leg pull request flow.
     * 
     * @return The leg pull request <code>flow</code>
     */
    @Override
    public List<IPullRequestFlow> getPullRequestFlows() {
        return this.pullRequestFlows;
    }
    
    /**
     * Returns the leg user message flow.
     * 
     * @return The leg user message <code>flow</code> 
     */
    @Override
    public IUserMessageFlow getUserMessageFlow() {
        return this.userMessageFlow;
    }
    
    /**
     * Returns the leg receipt.
     * 
     * @return The leg <code>receipt</code> 
     */
    @Override
    public IReceiptConfiguration getReceiptConfiguration() {
        return this.receipt;
    }

    /**
     * Returns the leg reception awareness.
     * 
     * @return  The leg <code>reception awareness</code>
     */    
    @Override
    public IReceptionAwareness getReceptionAwareness() {
        return rcptAwareness;
    }
}
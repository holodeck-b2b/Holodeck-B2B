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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

/**
 * Contains the parameters related to a specific leg of the P-Mode.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class Leg implements ILeg, Serializable {
	private static final long serialVersionUID = 322239336695896781L;

    @Element (name = "Protocol", required = false)
    private Protocol protocolConfig;

    @Element (name = "Receipt", required = false)
    private ReceiptConfiguration receiptConfig;

    @Element (name = "ReceptionAwareness", required = false)
    private ReceptionAwarenessConfig rcptAwareness;

    @Element ( name = "DefaultDelivery", required = false)
    private DeliveryConfiguration defaultDelivery;

    @ElementList (entry = "PullRequestFlow", type = PullRequestFlow.class, inline = true, required = false)
    private ArrayList<IPullRequestFlow> pullRequestFlows;

    @Element (name = "UserMessageFlow", required = false)
    private UserMessageFlow userMessageFlow;

    @ElementList (name = "EventHandlers", entry = "EventHandler", type = EventHandlerConfig.class, required = false)
    private ArrayList<IMessageProcessingEventConfiguration> eventHandlers;

    @Attribute (name = "label", required = false)
    private Label label;

    /**
     * Default constructor creates a new and empty <code>Leg</code> instance.
     */
    public Leg() {}

    /**
     * Creates a new <code>Leg</code> instance using the parameters from the provided {@link ILeg} object.
     *
     * @param source The source object to copy the parameters from
     */
    public Leg(final ILeg source) {
        this.label = source.getLabel();
        this.protocolConfig = source.getProtocol() != null ? new Protocol(source.getProtocol()) : null;
        this.receiptConfig = source.getReceiptConfiguration() != null ?
                                    new ReceiptConfiguration(source.getReceiptConfiguration()) : null;
        if (source.getReceptionAwareness() != null)
            this.rcptAwareness = new ReceptionAwarenessConfig(source.getReceptionAwareness());
        this.defaultDelivery = source.getDefaultDelivery() != null ?
                                    new DeliveryConfiguration(source.getDefaultDelivery()) : null;
        this.userMessageFlow = source.getUserMessageFlow() != null ?
                                    new UserMessageFlow(source.getUserMessageFlow()) : null;
        Collection<IPullRequestFlow> srcPullFlows = source.getPullRequestFlows();
        if (!Utils.isNullOrEmpty(srcPullFlows)) {
            this.pullRequestFlows = new ArrayList<>(srcPullFlows.size());
            srcPullFlows.forEach(prFlow -> this.pullRequestFlows.add(new PullRequestFlow(prFlow)));
        }
        List<IMessageProcessingEventConfiguration> srcEventCfgs = source.getMessageProcessingEventConfiguration();
        if (!Utils.isNullOrEmpty(srcEventCfgs)) {
            this.eventHandlers = new ArrayList<>(srcEventCfgs.size());
            srcEventCfgs.forEach(ec -> this.eventHandlers.add(new EventHandlerConfig(ec)));
        }
    }

    /**
     * This method ensures that the {@link DeliveryConfiguration} for the default delivery method gets an unique id
     * based on the P-Mode id. Because we do not know the P-Mode id here we use the <i>commit</i> functionality of the
     * Simple framework (see <a href="http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state">
     * http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php#state</a>). We put the <code>
     * defaultDelivery</code> object in the deserialization session so {@link PMode#solveDepencies(java.util.Map)} can
     * set the id using the P-Mode id.
     *
     * @param dependencies The Simple session object.
     */
    @Commit
    public void setDepency(final Map dependencies) {
        if (defaultDelivery != null) {
            // Because multiple DefaultDelivery elements can exist in the P-Mode document when we enable Two-Way MEPs,
            // we make sure it get a unique id
            int i = 0;
            while (dependencies.containsKey("DefaultDelivery-" + i)) i++;
            dependencies.put("DefaultDelivery-"+i, defaultDelivery);
        }
    }
    
    @Override
    public Label getLabel() {
        return label;
    }

    public void setLabel(final Label label) {
        this.label = label;
    }

    @Override
    public Protocol getProtocol() {
        return protocolConfig;
    }

    public void setProtocol(final Protocol protocolConfig) {
        this.protocolConfig = protocolConfig;
    }

    @Override
    public ReceiptConfiguration getReceiptConfiguration() {
        return receiptConfig;
    }

    public void setReceiptConfiguration(final ReceiptConfiguration rcptConfig) {
        this.receiptConfig = rcptConfig;
    }

    @Override
    public ReceptionAwarenessConfig getReceptionAwareness() {
        return rcptAwareness;
    }

    public void setReceptionAwareness(final ReceptionAwarenessConfig raConfig) {
        this.rcptAwareness = raConfig;
    }

    @Override
    public DeliveryConfiguration getDefaultDelivery() {
        return defaultDelivery;
    }

    public void setDefaultDelivery(final DeliveryConfiguration deliveryConfig) {
        this.defaultDelivery = deliveryConfig;
    }

    @Override
    public Collection<IPullRequestFlow> getPullRequestFlows() {
        return pullRequestFlows;
    }

    public void setPullRequestFlows(final Collection<IPullRequestFlow> prFlows) {
        this.pullRequestFlows = new ArrayList<>();
        if (prFlows != null)
            prFlows.forEach(prFlow -> this.pullRequestFlows.add(new PullRequestFlow(prFlow)));
    }

    public void addPullRequestFlow(final PullRequestFlow prFlow) {
        if (this.pullRequestFlows == null)
            this.pullRequestFlows = new ArrayList<>();
        this.pullRequestFlows.add(new PullRequestFlow(prFlow));
    }

    @Override
    public UserMessageFlow getUserMessageFlow() {
        return userMessageFlow;
    }

    public void setUserMessageFlow(final UserMessageFlow usrMsgFlow) {
        this.userMessageFlow = usrMsgFlow;
    }

    @Override
    public List<IMessageProcessingEventConfiguration> getMessageProcessingEventConfiguration() {
        return eventHandlers;
    }

    public void addMessageProcessingEventConfiguration(EventHandlerConfig eventConfig) {
        if (this.eventHandlers == null)
            this.eventHandlers = new ArrayList<>();

        this.eventHandlers.add(eventConfig);
    }

    public void setMessageProcessingEventConfiguration(Collection<EventHandlerConfig> eventConfigs) {
        if (!Utils.isNullOrEmpty(eventConfigs)) {
            this.eventHandlers = new ArrayList<>(eventConfigs.size());
            eventConfigs.forEach(eh -> this.eventHandlers.add(eh));
        } else
            this.eventHandlers = null;
    }
}

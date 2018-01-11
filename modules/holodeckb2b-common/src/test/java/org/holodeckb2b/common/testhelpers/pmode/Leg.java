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

import org.holodeckb2b.interfaces.as4.pmode.IAS4Leg;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class Leg implements IAS4Leg {

    private Label                    label;
    private Protocol                 protocolConfig;
    private ReceiptConfiguration     receiptConfig;
    private ReceptionAwarenessConfig raConfig;
    private DeliverySpecification    defDeliverySpec;

    private List<IPullRequestFlow>   pullRequestFlows;
    private List<IMessageProcessingEventConfiguration> eventConfigurations;

    private UserMessageFlow          userMessageConfig;

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
        return raConfig;
    }

    public void setReceptionAwareness(final ReceptionAwarenessConfig raConfig) {
        this.raConfig = raConfig;
    }

    @Override
    public DeliverySpecification getDefaultDelivery() {
        return defDeliverySpec;
    }

    public void setDefaultDelivery(final DeliverySpecification deliverySpec) {
        this.defDeliverySpec = deliverySpec;
    }

    @Override
    public Collection<IPullRequestFlow> getPullRequestFlows() {
        return pullRequestFlows;
    }

    public void addPullRequestFlow(PullRequestFlow prFlow) {
        if (this.pullRequestFlows == null)
            this.pullRequestFlows = new ArrayList<>();

        if (prFlow != null)
            this.pullRequestFlows.add(prFlow);
    }

    @Override
    public UserMessageFlow getUserMessageFlow() {
        return userMessageConfig;
    }

    public void setUserMessageFlow(final UserMessageFlow usrMsgFlow) {
        this.userMessageConfig = usrMsgFlow;
    }

    @Override
    public List<IMessageProcessingEventConfiguration> getMessageProcessingEventConfiguration() {
        return eventConfigurations;
    }

    public void addMessageProcessingEventConfiguration(EventHandlerConfig eventConfig) {
        if (this.eventConfigurations == null)
            this.eventConfigurations = new ArrayList<>();

        if (eventConfig != null)
            this.eventConfigurations.add(eventConfig);
    }
}

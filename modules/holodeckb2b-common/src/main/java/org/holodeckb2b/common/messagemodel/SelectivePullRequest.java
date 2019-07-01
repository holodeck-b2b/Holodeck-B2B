/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.messagemodel;

import java.io.Serializable;

import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;

/**
 * Is an in memory only implementation of {@link IPullRequest} to temporarily store the meta-data information on a
 * selective Pull Request Signal message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  4.1.0
 */
public class SelectivePullRequest extends PullRequest implements ISelectivePullRequest, Serializable {
	private static final long serialVersionUID = 9075656384699164939L;

	private String              refdMessageId;
    private String              conversationId;
    private AgreementReference  agreementRef;
    private Service             service;
    private String              action;

    /**
     * Default constructor creates a new empty <code>SelectivePullRequest</code> object
     */
    public SelectivePullRequest() {}

    /**
     * Creates a new <code>SelectivePullRequest</code> object using the mpc from the given source pull request. The
     * source can also be a normal {@link IPullRequest} to which the criteria data is added later.
     *
     * @param source    The Pull Request data to use
     */
    public SelectivePullRequest(final IPullRequest source) {
        super(source);

        if (source instanceof ISelectivePullRequest) {
            ISelectivePullRequest selective = (ISelectivePullRequest) source;
            this.refdMessageId = selective.getReferencedMessageId();
            this.conversationId = selective.getConversationId();
            this.action = selective.getAction();
            setAgreementRef(selective.getAgreementRef());
            setService(selective.getService());
        }
    }

    @Override
    public String getReferencedMessageId() { return refdMessageId; }

    public void setReferencedMessageId(final String messageId) { refdMessageId = messageId; }

    @Override
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(final String convid) {
        conversationId = convid;
    }

    @Override
    public IAgreementReference getAgreementRef() {
        return agreementRef;
    }

    public void setAgreementRef(final IAgreementReference ref) {
        this.agreementRef = ref != null ? new AgreementReference(ref) : null;
    }

    @Override
    public IService getService() {
        return service;
    }

    public void setService(final IService service) {
        this.service = service != null ? new Service(service) : null;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

}

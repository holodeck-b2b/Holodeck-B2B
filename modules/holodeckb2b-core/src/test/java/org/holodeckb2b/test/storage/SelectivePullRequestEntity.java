/*
 * Copyright (C) 2019 The Holodeck B2B Team
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
package org.holodeckb2b.test.storage;

import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.storage.ISelectivePullRequestEntity;

/**
 * Is the {@link ISelectivePullRequestEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SelectivePullRequestEntity extends PullRequestEntity implements ISelectivePullRequestEntity {
	private String              refdMessageId;
    private String              conversationId;
    private AgreementReference  agreementRef;
    private Service             service;
    private String              action;

    public SelectivePullRequestEntity() {
    	super();
    }
    
	public SelectivePullRequestEntity(ISelectivePullRequest source) {
		super(source);
		copyFrom(source);
	} 
	
	public void copyFrom(ISelectivePullRequest source) {
		if (source == null)
			return;
		super.copyFrom(source);
		if (source instanceof ISelectivePullRequest) {
            ISelectivePullRequest selective = (ISelectivePullRequest) source;
            this.refdMessageId = selective.getReferencedMessageId();
            this.conversationId = selective.getConversationId();
            this.action = selective.getAction();
            this.agreementRef = selective.getAgreementRef() == null ? null :
            														new AgreementReference(selective.getAgreementRef());
            this.service = selective.getService() == null ? null : new Service(selective.getService());
        }
	}
	
	@Override
	public MessageUnitEntity clone() {
		return new SelectivePullRequestEntity(this);
	}
	
    @Override
    public String getReferencedMessageId() { return refdMessageId; }

    @Override
    public String getConversationId() {
        return conversationId;
    }

    @Override
    public IAgreementReference getAgreementRef() {
        return agreementRef;
    }

    @Override
    public IService getService() {
        return service;
    }

    @Override
    public String getAction() {
        return action;
    }
}

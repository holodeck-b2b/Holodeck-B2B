package org.holodeckb2b.persistency.inmemory.dto;

import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.persistency.entities.ISelectivePullRequestEntity;

/**
 * Is the {@link ISelectivePullRequestEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SelectivePullRequestDTO extends PullRequestDTO implements ISelectivePullRequestEntity {
	private String              refdMessageId;
    private String              conversationId;
    private AgreementReference  agreementRef;
    private Service             service;
    private String              action;

    public SelectivePullRequestDTO() {
    	super();
    }
    
	public SelectivePullRequestDTO(ISelectivePullRequest source) {
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
	public MessageUnitDTO clone() {
		return new SelectivePullRequestDTO(this);
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

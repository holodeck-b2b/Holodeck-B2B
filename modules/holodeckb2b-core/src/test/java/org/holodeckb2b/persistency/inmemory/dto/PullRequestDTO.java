package org.holodeckb2b.persistency.inmemory.dto;

import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;

/**
 * Is the {@link IPullRequestEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PullRequestDTO extends MessageUnitDTO implements IPullRequestEntity {
	private String mpc;
	
	public PullRequestDTO() {}
	
	public PullRequestDTO(IPullRequest source) {
		super(source);
		copyFrom(source);
	}
	
	public void copyFrom(IPullRequest source) {
		if (source == null)
			return;
		
		super.copyFrom(source);
		this.mpc = source.getMPC();
	}	
	
	@Override
	public MessageUnitDTO clone() {
		return new PullRequestDTO(this);
	}
	
	@Override
	public String getMPC() {
		return mpc;
	}
	
}

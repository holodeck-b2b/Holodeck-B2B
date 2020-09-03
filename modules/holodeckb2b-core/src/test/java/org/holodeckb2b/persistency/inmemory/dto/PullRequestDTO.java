package org.holodeckb2b.persistency.inmemory.dto;

import org.holodeckb2b.common.messagemodel.PullRequest;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;

/**
 * Is the {@link IPullRequestEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PullRequestDTO extends PullRequest implements IPullRequestEntity {

	private boolean isMultiHop = false;
	
	public PullRequestDTO(IPullRequest source) {
		super(source);
	}
	
	@Override
	public boolean isLoadedCompletely() {
		return true;
	}


	@Override
	public boolean usesMultiHop() {
		return isMultiHop;
	}

	public void setIsMultiHop(boolean usesMultiHop) {
		isMultiHop = usesMultiHop;
	}
}

package org.holodeckb2b.persistency.inmemory.dto;

import org.holodeckb2b.common.messagemodel.SelectivePullRequest;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.persistency.entities.ISelectivePullRequestEntity;

/**
 * Is the {@link ISelectivePullRequestEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SelectivePullRequestDTO extends SelectivePullRequest implements ISelectivePullRequestEntity {

	private boolean isMultiHop = false;
	
	public SelectivePullRequestDTO(ISelectivePullRequest source) {
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

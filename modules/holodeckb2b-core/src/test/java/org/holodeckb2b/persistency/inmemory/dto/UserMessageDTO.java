package org.holodeckb2b.persistency.inmemory.dto;

import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;

/**
 * Is the {@link IUserMessageEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class UserMessageDTO extends UserMessage implements IUserMessageEntity {

	private boolean isMultiHop = false;
	
	public UserMessageDTO(IUserMessage source) {
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

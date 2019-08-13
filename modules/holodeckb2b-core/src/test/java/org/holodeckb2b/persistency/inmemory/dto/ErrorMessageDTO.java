package org.holodeckb2b.persistency.inmemory.dto;

import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;

public class ErrorMessageDTO extends ErrorMessage implements IErrorMessageEntity {

	private boolean isMultiHop = false;
	private boolean useSOAPFault = false;
	
	public ErrorMessageDTO(IErrorMessage source) {
		super(source);
	}
	
	@Override
	public boolean isLoadedCompletely() {
		return true;
	}

	@Override
	@Deprecated
	public Label getLeg() {
		return null;
	}

	@Override
	public boolean usesMultiHop() {
		return isMultiHop;
	}

	public void setIsMultiHop(boolean usesMultiHop) {
		isMultiHop = usesMultiHop;
	}

	@Override
	public boolean shouldHaveSOAPFault() {
		return useSOAPFault;
	}

	public void setUseSOAPFault(boolean addSOAPFault) {
		useSOAPFault = addSOAPFault;
	}
}

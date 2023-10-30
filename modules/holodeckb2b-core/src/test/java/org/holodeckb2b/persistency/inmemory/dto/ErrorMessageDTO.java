package org.holodeckb2b.persistency.inmemory.dto;

import java.util.ArrayList;
import java.util.Collection;

import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;

public class ErrorMessageDTO extends MessageUnitDTO implements IErrorMessageEntity {
	private ArrayList<IEbmsError>    errors = new ArrayList<>();
	private boolean useSOAPFault = false;
	private ILeg.Label leg = null;

	public ErrorMessageDTO() {
		super();
	}
	
	public ErrorMessageDTO(IErrorMessage source) {
		super(source);
		copyFrom(source);
	}
	
	@Override
	public MessageUnitDTO clone() {
		return new ErrorMessageDTO(this);
	}
	
	public void copyFrom(IErrorMessage source) {
		if (source == null)
			return;
		super.copyFrom(source);
		if (!Utils.isNullOrEmpty(source.getErrors()))
			source.getErrors().forEach(e -> this.errors.add(new EbmsError(e)));
		if (source instanceof IErrorMessageEntity) {
			IErrorMessageEntity e = (IErrorMessageEntity) source;
			this.useSOAPFault = e.shouldHaveSOAPFault();
			this.leg = e.getLeg();
		}		
	}
	
	@Override
	public boolean shouldHaveSOAPFault() {
		return useSOAPFault;
	}

	@Override
	public void setAddSOAPFault(boolean addSOAPFault) {
		useSOAPFault = addSOAPFault;
	}

	@Override
	public Label getLeg() {
		return leg;
	}
	
	public void setLeg(ILeg.Label leg) {
		this.leg = leg;
	}

	@Override
	public Collection<IEbmsError> getErrors() {
		return errors;
	}

}

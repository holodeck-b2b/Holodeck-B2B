package org.holodeckb2b.persistency.inmemory;

import java.util.Collection;
import java.util.Set;

import org.holodeckb2b.common.messagemodel.MessageProcessingState;
import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.dao.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.inmemory.dto.ErrorMessageDTO;
import org.holodeckb2b.persistency.inmemory.dto.PullRequestDTO;
import org.holodeckb2b.persistency.inmemory.dto.ReceiptDTO;
import org.holodeckb2b.persistency.inmemory.dto.SelectivePullRequestDTO;
import org.holodeckb2b.persistency.inmemory.dto.UserMessageDTO;

public class UpdateManager implements IUpdateManager {

	private Set<IMessageUnitEntity>	msgUnits;
	
	UpdateManager(Set<IMessageUnitEntity> data) {
		msgUnits = data;
	}
	
	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> V storeMessageUnit(T messageUnit)
			throws PersistenceException {
		IMessageUnitEntity dto;
		if (messageUnit instanceof IUserMessage)
			dto = new UserMessageDTO((IUserMessage) messageUnit);
		else if (messageUnit instanceof ISelectivePullRequest)
			dto = new PullRequestDTO((IPullRequest) messageUnit);
		else if (messageUnit instanceof IPullRequest)
			dto = new PullRequestDTO((IPullRequest) messageUnit);		
		else if (messageUnit instanceof IReceipt)
			dto = new ReceiptDTO((IReceipt) messageUnit);
		else if (messageUnit instanceof IErrorMessage)
			dto = new ErrorMessageDTO((IErrorMessage) messageUnit);
		else
			throw new IllegalArgumentException("Unknown message unit type");
		
		msgUnits.add(dto);
		
		return (V) dto;
	}

	@Override
	public void setPModeId(IMessageUnitEntity msgUnit, String pmodeId) throws PersistenceException {
		if (msgUnit instanceof MessageUnit)
			synchronized (msgUnit) {
				((MessageUnit) msgUnit).setPModeId(pmodeId);				
			}
		else
			throw new IllegalArgumentException("Unmanaged message unit");
	}

	@Override
	public boolean setProcessingState(IMessageUnitEntity msgUnit, ProcessingState currentProcState,
			ProcessingState newProcState) throws PersistenceException {
		return this.setProcessingState(msgUnit, currentProcState, newProcState, null);
	}

	@Override
	public boolean setProcessingState(IMessageUnitEntity msgUnit, ProcessingState currentProcState,
			ProcessingState newProcState, String description) throws PersistenceException {
		if (msgUnit instanceof MessageUnit)
			synchronized(msgUnit) {
				if (currentProcState != null && msgUnit.getCurrentProcessingState().getState() != currentProcState)
					return false;
				
				((MessageUnit) msgUnit).setProcessingState(new MessageProcessingState(newProcState, description));
				return true;
			}
		else
			throw new IllegalArgumentException("Unmanaged message unit");
	}

	@Override
	public void setMultiHop(IMessageUnitEntity messageUnit, boolean isMultihop) throws PersistenceException {
		if (messageUnit instanceof IUserMessage)
			((UserMessageDTO) messageUnit).setIsMultiHop(isMultihop);
		else if (messageUnit instanceof ISelectivePullRequest)
			((SelectivePullRequestDTO) messageUnit).setIsMultiHop(isMultihop);
		else if (messageUnit instanceof IPullRequest)
			((PullRequestDTO) messageUnit).setIsMultiHop(isMultihop);
		else if (messageUnit instanceof IReceipt)
			((ReceiptDTO) messageUnit).setIsMultiHop(isMultihop);
		else if (messageUnit instanceof IErrorMessage)
			((ErrorMessageDTO) messageUnit).setIsMultiHop(isMultihop);
		else
			throw new IllegalArgumentException("Unknown message unit type");
	}

	@Override
	public void setLeg(IMessageUnit msgUnit, Label legLabel) throws PersistenceException {		
	}

	@Override
	public void setPayloadInformation(IUserMessageEntity userMessage, Collection<IPayload> payloadInfo)
			throws PersistenceException {
		if (userMessage instanceof UserMessage)
			synchronized (userMessage) {
				((UserMessage) userMessage).setPayloads(payloadInfo);				
			}
		else
			throw new IllegalArgumentException("Unmanaged message unit");
	}

	@Override
	public void setAddSOAPFault(IErrorMessageEntity errorMessage, boolean addSOAPFault) throws PersistenceException {
		if (errorMessage instanceof ErrorMessageDTO)
			synchronized (errorMessage) {
				((ErrorMessageDTO) errorMessage).setUseSOAPFault(addSOAPFault);				
			}
		else
			throw new IllegalArgumentException("Unmanaged message unit");
	}

	@Override
	public void deleteMessageUnit(IMessageUnitEntity messageUnit) throws PersistenceException {
		msgUnits.remove(messageUnit);
	}

}

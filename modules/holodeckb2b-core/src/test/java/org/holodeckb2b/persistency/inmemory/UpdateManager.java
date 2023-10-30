package org.holodeckb2b.persistency.inmemory;

import java.util.Date;
import java.util.Set;

import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.AlreadyChangedException;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPayloadEntity;
import org.holodeckb2b.persistency.inmemory.dto.ErrorMessageDTO;
import org.holodeckb2b.persistency.inmemory.dto.MessageUnitDTO;
import org.holodeckb2b.persistency.inmemory.dto.PayloadDTO;
import org.holodeckb2b.persistency.inmemory.dto.PullRequestDTO;
import org.holodeckb2b.persistency.inmemory.dto.ReceiptDTO;
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
		
		return (V) ((MessageUnitDTO) dto).clone();
	}

	@Override
	public void updateMessageUnit(IMessageUnitEntity messageUnit) throws PersistenceException {
		if (!(messageUnit instanceof MessageUnitDTO))
			throw new PersistenceException("Unsupported entity class");
		
		MessageUnitDTO update = ((MessageUnitDTO) messageUnit);
		MessageUnitDTO stored = (MessageUnitDTO) msgUnits.stream().filter(m -> messageUnit.getCoreId().equals(m.getCoreId())).findFirst().orElse(null);
		if (stored == null)
			throw new PersistenceException("Entity not managed");
		
		if (stored.getLastChanged().after(update.getLastChanged())) {
			update.copyFrom(stored);
			throw new AlreadyChangedException(); 
		}
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		update.setChanged(new Date());
		if (stored != messageUnit)
			stored.copyFrom(update);
	}
	
	@Override
	public void updatePayload(IPayloadEntity payload) throws PersistenceException {
		if (!(payload instanceof PayloadDTO))
			throw new PersistenceException("Unsupported entity class");
		
		if (msgUnits.stream().noneMatch(m -> 
									((PayloadDTO) payload).getParentUserMessage().getCoreId().equals(m.getCoreId())))
			throw new PersistenceException("Entity not managed");
	}	

	@Override
	public void deleteMessageUnit(IMessageUnitEntity messageUnit) throws PersistenceException {
		msgUnits.removeIf(m -> messageUnit.getCoreId().equals(m.getCoreId()));
	}
}

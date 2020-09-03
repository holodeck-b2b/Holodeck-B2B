package org.holodeckb2b.persistency.inmemory.dto;

import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;

/**
 * Is the {@link IReceiptEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ReceiptDTO extends Receipt implements IReceiptEntity {

	private boolean isMultiHop = false;

	public ReceiptDTO(IReceipt source) {
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

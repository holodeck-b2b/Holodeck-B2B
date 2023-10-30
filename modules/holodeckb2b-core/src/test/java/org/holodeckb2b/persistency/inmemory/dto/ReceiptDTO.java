package org.holodeckb2b.persistency.inmemory.dto;

import java.util.List;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;

/**
 * Is the {@link IReceiptEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ReceiptDTO extends MessageUnitDTO implements IReceiptEntity {
	private List<OMElement>    content;
	
	public ReceiptDTO() {
		super();
	}

	@Override
	public MessageUnitDTO clone() {
		return new ReceiptDTO(this);
	}
	
    public ReceiptDTO(final IReceipt source) {
        super(source);
        copyFrom(source);
    }
    
    public void copyFrom(IReceipt source) {
    	if (source == null)
    		return;
    	
    	super.copyFrom(source);
    	content = source.getContent();        	
    }
    
	@Override
	public List<OMElement> getContent() {
		return content;
	}	
}

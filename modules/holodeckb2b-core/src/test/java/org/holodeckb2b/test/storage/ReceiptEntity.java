/*
 * Copyright (C) 2019 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.test.storage;

import java.util.List;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;

/**
 * Is the {@link IReceiptEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ReceiptEntity extends MessageUnitEntity implements IReceiptEntity {
	private List<OMElement>    content;
	
	public ReceiptEntity() {
		super();
	}

	@Override
	public MessageUnitEntity clone() {
		return new ReceiptEntity(this);
	}
	
    public ReceiptEntity(final IReceipt source) {
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

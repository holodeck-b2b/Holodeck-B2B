/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.storage;

import java.util.UUID;

import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.MessageProcessingState;
import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;

/**
 * Is a helper class to allow sending of an Error Message to the sender of the message even if the persistency layer is
 * down.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class NonPersistedErrorMessage extends ErrorMessage implements IErrorMessageEntity {
	private static final long serialVersionUID = -6816284794434753373L;

	private String  coreId;
	private MessageProcessingState currentState;
	private boolean addSOAPFault = false;
	private boolean isMultiHop = false;
	private Label 	leg;

	public NonPersistedErrorMessage(final ErrorMessage source) {
		super(source);
		coreId = UUID.randomUUID().toString();
		setMessageId(MessageIdUtils.createMessageId());
	}

	@Override
	public IMessageUnitProcessingState getCurrentProcessingState() {
		return currentState;
	}

	@Override
	public boolean usesMultiHop() {
		return isMultiHop;
	}

	@Override
	public boolean shouldHaveSOAPFault() {
		return addSOAPFault;
	}

	@Override
	public String getCoreId() {
		return coreId;
	}

	@Override
	public void setMultiHop(boolean usingMultiHop) {
		isMultiHop = usingMultiHop;
	}

	@Override
	public void setAddSOAPFault(boolean addSOAPFault) {
		this.addSOAPFault = addSOAPFault;
	}

	@Override
	public Label getLeg() {
		return leg;
	}

	@Override
	public void setLeg(Label leg) {
		this.leg = leg;
	}

	@Override
	public void setProcessingState(ProcessingState newState, String description) {
		this.currentState = new MessageProcessingState(newState, description);
	}
}
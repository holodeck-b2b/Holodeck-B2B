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

import java.util.ArrayList;
import java.util.Collection;

import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;

public class ErrorMessageEntity extends MessageUnitEntity implements IErrorMessageEntity {
	private ArrayList<IEbmsError>    errors = new ArrayList<>();
	private boolean useSOAPFault = false;
	private ILeg.Label leg = null;

	public ErrorMessageEntity() {
		super();
	}
	
	public ErrorMessageEntity(IErrorMessage source) {
		super(source);
		copyFrom(source);
	}
	
	@Override
	public MessageUnitEntity clone() {
		return new ErrorMessageEntity(this);
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

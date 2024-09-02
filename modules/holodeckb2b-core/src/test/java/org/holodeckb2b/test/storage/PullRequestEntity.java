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

import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.storage.IPullRequestEntity;

/**
 * Is the {@link IPullRequestEntity} implementation of the in-memory persistency provider used for testing.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PullRequestEntity extends MessageUnitEntity implements IPullRequestEntity {
	private String mpc;
	
	public PullRequestEntity() {}
	
	public PullRequestEntity(IPullRequest source) {
		super(source);
		copyFrom(source);
	}
	
	public void copyFrom(IPullRequest source) {
		if (source == null)
			return;
		
		super.copyFrom(source);
		this.mpc = source.getMPC();
	}	
	
	@Override
	public MessageUnitEntity clone() {
		return new PullRequestEntity(this);
	}
	
	@Override
	public String getMPC() {
		return mpc;
	}
	
}

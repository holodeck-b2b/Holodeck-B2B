/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.messagemodel;

import org.holodeckb2b.interfaces.messagemodel.IPullRequest;

/**
 * Is an in memory only implementation of {@link IPullRequest} to temporarily store the meta-data information on a Pull
 * Request Signal message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class PullRequest extends MessageUnit implements IPullRequest {
	private String  mpc;

    /**
     * Default constructor creates a new empty <code>PullRequest</code> object
     */
    public PullRequest() {}

    /**
     * Creates a new <code>PullRequest</code> object using the mpc from the given source pull request.
     *
     * @param source    The Pull Request data to use
     */
    public PullRequest(final IPullRequest source) {
        super(source);

        this.mpc = source != null ? source.getMPC() : null;
    }

    /**
     * Creates a new <code>PullRequest</code> object for the given P-Mode id and MPC value.
     *
     * @param pmodeId   The P-Mode id
     * @param mpc       The MPC
     */
    public PullRequest(final String pmodeId, final String mpc) {
        setPModeId(pmodeId);
        this.mpc = mpc;
    }

    @Override
    public String getMPC() {
        return mpc;
    }

    public void setMPC(final String mpc) {
        this.mpc = mpc;
    }
}

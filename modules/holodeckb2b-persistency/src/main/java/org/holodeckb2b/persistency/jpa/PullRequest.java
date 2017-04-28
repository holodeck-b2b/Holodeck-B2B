/**
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency.jpa;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;

/**
 * Is the JPA entity class to store the meta-data of a <b>PullRequest Signal</b> message unit as described by the {@link
 * IPullRequest} interface in the Holodeck B2B messaging model. The maximum length of the MPC URL is 1024 characters.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
@Entity
@Table(name="PULLREQUEST")
@DiscriminatorValue("PULLREQ")
public class PullRequest extends MessageUnit implements IPullRequest, Serializable {

    /*
     * Getters and setters
     */
    @Override
    public String getMPC() {
        return MPC;
    }

    public void setMPC(final String newMPC) {
        this.MPC = newMPC;
    }
    /*
     * Constructors
     */
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

        this.MPC = source != null ? source.getMPC() : null;
    }

    /**
     * Creates a new <code>PullRequest</code> object for the given P-Mode id and MPC value.
     *
     * @param pmodeId   The P-Mode id
     * @param mpc       The MPC
     */
    public PullRequest(final String pmodeId, final String mpc) {
        setPModeId(pmodeId);
        this.MPC = mpc;
    }

    /*
     * Fields
     *
     * NOTES:
     * 1) The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     * 2) The primary key field is inherited from super class
     */
    @Lob
    @Column(length = 1024)
    private String          MPC;
}


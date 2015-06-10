/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.persistent.message;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.holodeckb2b.common.messagemodel.IPullRequest;

/**
 * Is a persistency class representing an ebMS PullRequest messaage unit that 
 * is processed by Holodeck B2B. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Entity
@Table(name="PULLREQUEST")
@DiscriminatorValue("PULLREQ")
public class PullRequest extends SignalMessage implements IPullRequest {

    /*
     * Getters and setters
     */
    @Override
    public String getMPC() {
        return MPC;
    }

    public void setMPC(String newMPC) {
        this.MPC = newMPC;
    }
    
    /*
     * Fields
     * 
     * NOTES: 
     * 1) The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     * 2) The primary key field is inherited from super class
     */
    
    private String          MPC;
}


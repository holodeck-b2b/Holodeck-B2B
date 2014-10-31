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
package org.holodeckb2b.ebms3.persistent.processing;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;

/**
 * Represents a state during the processing of a {@see MessageUnit}. A state consists
 * of a name, the timestamp when the state was entered/started and optionally a 
 * more detailed description.
 * 
 * <p>Process flows and related states are not modeled in Holodeck B2B but coded 
 * into the handlers that execute the process. 
 * 
 * <p>A <code>MessageUnit</code> can be associated with several states, but can
 * have only one <i>current</i> state. This is the <code>ProcessingState</code>
 * with the most recent <code>start</code> timestamp.
 * 
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@Entity
@Table(name="MSG_STATE")
public class ProcessingState implements Serializable {
   
    /*
     * Getters and setters
     */
    public void setName(String name) {
        NAME = name;
    }
    
    public String getName() {
        return  NAME;
    }
    
    public void setStartTime(Date timestamp) {
        START = timestamp;
    }
    
    public Date getStartTime() {
        return  START;
    } 
    
    
    /*
     * Constructors
     */
    public ProcessingState() {}
    
    
    /**
     * Creates a ProcessingState with the given name and a start time with the 
     * current time.
     * 
     * @param name  The name of the new state
     */
    public ProcessingState(String name) {
        this.NAME = name;
        this.START = new Date();
    }
    
    /*
     * Fields
     * 
     * NOTE: The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    
    /*
     * Technical object id acting as the primary key
     */
    @Id
    @GeneratedValue
    private long        OID;
    
    private String      NAME;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date        START;
}

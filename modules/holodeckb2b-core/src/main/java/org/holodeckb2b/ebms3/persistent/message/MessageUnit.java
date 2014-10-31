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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.holodeckb2b.ebms3.persistent.processing.ProcessingState;

/**
 * Is a persistency class representing an ebMS messaage unit that is processed 
 * by Holodeck B2B. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Entity
@Table(name = "MSG_UNIT")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "UM_TYPE")
@NamedQueries({
        @NamedQuery(name="MessageUnit.findWithMessageIdInDirection",
            query = "SELECT mu " +
                    "FROM MessageUnit mu " +
                    "WHERE mu.MESSAGE_ID = :msgId " +
                    "AND mu.DIRECTION = :direction"
            ),
        @NamedQuery(name="MessageUnit.findInState",
            query = "SELECT mu " +
                    "FROM MessageUnit mu JOIN mu.states s1 " +
                    "WHERE s1.START = (SELECT MAX(s2.START) FROM mu.states s2) " +
                    "AND s1.NAME = :state"  
            )}
)
public abstract class MessageUnit implements Serializable, org.holodeckb2b.common.messagemodel.IMessageUnit {

    /**
     * Indicates whether the message unit was sent (OUT) or received (IN) by Holodeck B2B
     */
    public enum Direction { IN, OUT }
    
    /*
     * Getters and setters
     */
    public long getOID() {
        return OID;
    }
    
    @Override
    public Date getTimestamp() {
        return MU_TIMESTAMP;
    }

    public void setTimestamp(Date timestamp) {
        MU_TIMESTAMP = timestamp;
    }

    @Override
    public String getMessageId() {
        return MESSAGE_ID;
    }
    
    public void setMessageId(String messageId) {
        MESSAGE_ID = messageId;
    }

    @Override
    public String getRefToMessageId() {
        return REF_TO_MSG_ID;
    }

    public void setRefToMessageId(String refToMsgId) {
        REF_TO_MSG_ID = refToMsgId;
    }
    
    /**
     * Add a processing state to the list of states this message unit has been in.
     * <p>Because the {@see ProcessingState} object includes the time since the state
     * was active it does not need to be the latest (=current) state. 
     * 
     * @param state 
     */
    public void addProcessingState(ProcessingState state) {
        if (states == null) 
            states = new ArrayList<ProcessingState>();
        
        states.add(state);
    }
    
    /**
     * Sets the current state of processing to the given state.
     * <p>
     * 
     * @param state 
     */
    public void setProcessingState(ProcessingState state) {
        if (states == null) 
            states = new ArrayList<ProcessingState>();
        
        states.add(0, state);
    }
    
    public ProcessingState getCurrentProcessingState() {
        if( states == null || states.size() == 0)
            return null;
        else 
            return states.get(0);
    }
    
    public List<ProcessingState> getProcessingStates() {
            return states;
    }
    
    /**
     * Associate this message unit with the given P-Mode
     * 
     * @param pmodeId   The id of the P-Mode that contains the processing parameters 
     *                  for this message unit
     */
    public void setPMode(String pmodeId) {
        this.PMODE_ID = pmodeId;
    }
            
    /**
     * Gets the id of the P-Mode for this message unit.
     * <p>The P-Mode contains the parameters for processing the message unit.
     * 
     * @return The id of the P-Mode 
     */
    public String getPMode() {
        return PMODE_ID;
    }
    
    /**
     * Sets the direction the message unit flows.
     * 
     * @param direction     The direction of the message unit
     */
    public void setDirection(Direction direction) {
        DIRECTION = direction;
    }
    
    /**
     * Gets the direction the message unit flows.
     * 
     * @returns     The direction of the message unit
     */
    public Direction getDirection() {
        return DIRECTION;
    }
    
    
    
    
    /**
     * Default constructor creates an empty object
     */
    public MessageUnit() {};
    
    /**
     * Constructor to create a new persistent <code>IMessageUnit</code> object 
     * based on another IMessageUnit object.
     * 
     * @deprecated 
     */
    public MessageUnit(org.holodeckb2b.common.messagemodel.IMessageUnit src) {
        MU_TIMESTAMP = src.getTimestamp();
        MESSAGE_ID = src.getMessageId();
        REF_TO_MSG_ID = src.getRefToMessageId();
        
        states = new ArrayList<ProcessingState>();
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
    private long    OID;
    
    private String  MESSAGE_ID;
    
    private String  REF_TO_MSG_ID;

    private String  PMODE_ID;

    private Direction   DIRECTION;
    
    /*
     * Because timestamp is a reserved SQL-99 word it is prefixed
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date    MU_TIMESTAMP;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("START DESC")
    private List<ProcessingState>       states;
}

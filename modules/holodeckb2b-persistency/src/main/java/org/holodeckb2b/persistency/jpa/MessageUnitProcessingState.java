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
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is the JPA entity class that represent a state during the processing of a message unit as described by the {@link
 * IMessageUnitProcessingState} interface from the Holodeck B2B messaging model.
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 * @since HB2B_NEXT_VERSION
 */
@Entity
@Table(name="MSG_STATE")
public class MessageUnitProcessingState implements IMessageUnitProcessingState, Serializable {

    @Override
    public ProcessingState getState() {
        return STATE;
    }

    public void setStartTime(final Date timestamp) {
        START = timestamp;
    }

    @Override
    public Date getStartTime() {
        return  START;
    }

    public void setSeqNumber(final int n) {
        this.PROC_STATE_NUM = n;
    }

    public int getSeqNumber() {
        return PROC_STATE_NUM;
    }

    public void setMessageUnit(final MessageUnit mu) {
        this.msgUnit = mu;
    }

    /*
     * Constructors
     */
    public MessageUnitProcessingState() {}

    /**
     * Creates a ProcessingState using the information from the provided {@link IMessageUnitProcessingState} object.
     *
     * @param state  The source object
     */
    public MessageUnitProcessingState(final IMessageUnitProcessingState state) {
        this.STATE = state.getState();
        this.START = state.getStartTime();
    }

    /*
     * Fields
     *
     * NOTE 1: The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     *
     * NOTE 2: Because the timestamp of processing states is not accurate enough
     * to determine the order of the states a extra PROC_STATE_NUM field is used. The
     * order is only relevant in relation to a specific message unit, not for all
     * processing states in general.
     */

    /*
     * Technical object id acting as the primary key
     */
    @Id
    @GeneratedValue
    private long        OID;

    @ManyToOne
    private MessageUnit msgUnit;

    @Column(nullable = false)
    private int         PROC_STATE_NUM;

    @Enumerated(EnumType.STRING)
    private ProcessingState      STATE;

    @Temporal(TemporalType.TIMESTAMP)
    private Date        START;
}

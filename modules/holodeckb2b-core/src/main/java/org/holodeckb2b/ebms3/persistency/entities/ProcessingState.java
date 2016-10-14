/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.persistency.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Is the JPA entity class that represent a state during the processing of a message unit. A state consists of a name,
 * the timestamp when the state was entered/started and a sequence number that defines the order of the processing
 * states. The sequence number is needed because the timestamp may not be fine grained enough to guarantee correct
 * ordering of states.
 * <p>A {@link MessageUnit} can be associated with several states, but can have only one <i>current</i> state. This is
 * the <code>ProcessingState</code> with the highest <i>sequence number</i>.
 * <p>Process flows and related states are not modeled in Holodeck B2B but coded into the handlers that execute the
 * process.
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@Entity
@Table(name="MSG_STATE")
public class ProcessingState implements Serializable {

    /*
     * Getters and setters
     */
    public void setName(final String name) {
        NAME = name;
    }

    public String getName() {
        return  NAME;
    }

    public void setStartTime(final Date timestamp) {
        START = timestamp;
    }

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
    public ProcessingState() {}

    /**
     * Creates a ProcessingState with the given name and a start time with the
     * current time.
     *
     * @param name  The name of the new state
     */
    public ProcessingState(final String name) {
        this.NAME = name;
        this.START = new Date();
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

    private String      NAME;

    @Temporal(TemporalType.TIMESTAMP)
    private Date        START;
}

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;

/**
 * Is the JPA persistency class to store the generic information that applies to all ebMS message unit types as
 * described by the {@link IMessageUnit} interface in the Holodeck B2B messaging model.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
@Entity
@Table(name = "MSG_UNIT")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class MessageUnit implements IMessageUnit, Serializable {

    /*
     * Getters and setters
     */

    public long getOID() {
        return OID;
    }

    @Override
    public Direction getDirection() {
        return DIRECTION;
    }

    public void setDirection(final Direction direction) {
        DIRECTION = direction;
    }

    @Override
    public Date getTimestamp() {
        return MU_TIMESTAMP;
    }

    public void setTimestamp(final Date timestamp) {
        MU_TIMESTAMP = timestamp;
    }

    @Override
    public String getMessageId() {
        return MESSAGE_ID;
    }

    public void setMessageId(final String messageId) {
        MESSAGE_ID = messageId;
    }

    @Override
    public String getRefToMessageId() {
        return REF_TO_MSG_ID;
    }

    public void setRefToMessageId(final String refToMsgId) {
        REF_TO_MSG_ID = refToMsgId;
    }

    @Override
    public List<IMessageUnitProcessingState> getProcessingStates() {
        return states;
    }

    @Override
    public IMessageUnitProcessingState getCurrentProcessingState() {
        return Utils.isNullOrEmpty(states) ? null : states.get(states.size() - 1);
    }

    public void setProcessingState(final IMessageUnitProcessingState state) {
        if (states == null)
            states = new ArrayList<>();

        MessageUnitProcessingState newState = new MessageUnitProcessingState(state);
        newState.setSeqNumber(states.size());
        newState.setMessageUnit(this);
        states.add(newState);
    }

    @Override
    public String getPModeId() {
        return PMODE_ID;
    }

    public void setPModeId(final String pmodeId) {
        PMODE_ID = pmodeId;
    }

    public Label getLeg() {
        return LEG_LABEL;
    }

    public void setLeg(final Label label) {
        LEG_LABEL = label;
    }

    public boolean usesMultiHop() {
        return USES_MULTI_HOP;
    }

    public void setMultiHop(final boolean usesMultiHop) {
        USES_MULTI_HOP = usesMultiHop;
    }

    /*
     * Constructors
     */
    /**
     * Default constructor to initialize as empty meta-data object
     */
    public MessageUnit() {}

   /**
     * Create a new <code>M</code> object for the user message unit described by the given
     * {@link IUserMessage} object.
     *
     * @param sourceMessageUnit   The meta data of the message unit to copy to the new object
     */
    public MessageUnit(final IMessageUnit sourceMessageUnit) {
        if (sourceMessageUnit == null)
            return;

        this.DIRECTION = sourceMessageUnit.getDirection();
        this.MESSAGE_ID = sourceMessageUnit.getMessageId();
        this.MU_TIMESTAMP = sourceMessageUnit.getTimestamp();
        this.REF_TO_MSG_ID = sourceMessageUnit.getRefToMessageId();
        this.PMODE_ID = sourceMessageUnit.getPModeId();

        if (!Utils.isNullOrEmpty(sourceMessageUnit.getProcessingStates())) {
            for (IMessageUnitProcessingState state : sourceMessageUnit.getProcessingStates())
                setProcessingState(state);
        }
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
    /*
     * Field to use for JPA optimistic locking
     */
    @Version
    private long    VERSION;

    private String  MESSAGE_ID;

    private String  REF_TO_MSG_ID;

    private String  PMODE_ID;

    private Label   LEG_LABEL;

    private Direction   DIRECTION;

    private boolean     USES_MULTI_HOP = false;

    /*
     * Because timestamp is a reserved SQL-99 word it is prefixed
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date    MU_TIMESTAMP;

    @OneToMany(mappedBy = "msgUnit", targetEntity = MessageUnitProcessingState.class,
                cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("PROC_STATE_NUM")
    private List<IMessageUnitProcessingState>       states;
}

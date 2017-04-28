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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is an in memory only implementation of {@link IMessageUnit} to temporarily store the generic information on message
 * unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public abstract class MessageUnit implements IMessageUnit {

    private IMessageUnit.Direction  direction;
    private String  messageId;
    private Date    timestamp;
    private String  refToMessageId;
    private ArrayList<IMessageUnitProcessingState>  states;
    private String  pmodeId;
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

        this.direction = sourceMessageUnit.getDirection();
        this.messageId = sourceMessageUnit.getMessageId();
        this.timestamp = sourceMessageUnit.getTimestamp();
        this.refToMessageId = sourceMessageUnit.getRefToMessageId();
        this.pmodeId = sourceMessageUnit.getPModeId();

        if (!Utils.isNullOrEmpty(sourceMessageUnit.getProcessingStates())) {
            for (IMessageUnitProcessingState state : sourceMessageUnit.getProcessingStates())
                setProcessingState(state);
        }
    }

    /**
     * Gets the direction this message unit is flowing, i.e. whether it is sent by Holodeck B2B or received.
     *
     * @return The direction of the message unit
     */
    @Override
    public Direction getDirection() {
        return direction;
    }

    /**
     * Sets the direction this message unit is flowing, i.e. whether it is sent by Holodeck B2B or received.
     *
     * @param direction The direction of the message unit
     */
    public void setDirection(final Direction direction) {
        this.direction = direction;
    }

    /**
     * Gets the message id of this message unit
     *
     * @return  The message id
     */
    @Override
    public String getMessageId() {
        return messageId;
    }

    /**
     * Sets the message id of this message unit
     *
     * @param msgId The new message id
     */
    public void setMessageId(final String msgId) {
        this.messageId = msgId;
    }

    /**
     * Gets the time stamp of this message unit
     *
     * @return  The message id
     */
    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the time stamp of this message unit
     *
     * @param timestamp The new timestamp
     */
    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the message id of the message unit that is referenced by this message unit
     *
     * @return  The referenced message id
     */
    @Override
    public String getRefToMessageId() {
        return refToMessageId;
    }

    /**
     * Sets the message id of the message unit that is referenced by this message unit
     *
     * @param refToMessageId The new message id
     */
    public void setRefToMessageId(final String refToMessageId) {
        this.refToMessageId = refToMessageId;
    }

    /**
     * Gets the id of the P-Mode that governs the processing of this message unit
     *
     * @return  The P-Mode id
     */
    @Override
    public String getPModeId() {
        return pmodeId;
    }

    /**
     * Sets the id of the P-Mode that governs the processing of this message unit
     *
     * @param pmodeId The new message id
     */
    public void setPModeId(final String pmodeId) {
        this.pmodeId = pmodeId;
    }

    /**
     * Gets the list of processing states this message unit was or is in.
     * <p>The order of the processing states as they occur in the list is the same as they applied to the message unit
     * with the last processing state in the list  (i.e. with the highest index) being the current processing state.
     *
     * @return  List of {@link IMessageUnitProcessingState} in the order they applied to this message unit
     */
    @Override
    public List<IMessageUnitProcessingState> getProcessingStates() {
        return states;
    }

    /**
     * Gets the current processing state the message unit is in.
     *
     * @return  The {@link IMessageUnitProcessingState} the message unit is currently in
     */
    public IMessageUnitProcessingState getCurrentProcessingState() {
        return states.get(states.size() - 1);
    }

    /**
     * Sets a new list of processing states this message unit was or is in.
     *
     * @param states The new list of processing states in the order they applied to this message unit
     */
    public void setProcessingStates(final List<IMessageUnitProcessingState> states) {
        if (Utils.isNullOrEmpty(states))
            this.states = null;
        else {
            this.states = new ArrayList<>(states.size());
            for (IMessageUnitProcessingState s : states)
                this.states.add(new MessageProcessingState(s));
        }
    }

    /**
     * Sets a new current processing state for this message unit.
     *
     * @param state The new current processing state
     */
    public void setProcessingState(final ProcessingState state) {
        if (state != null) {
            if (states == null)
                states = new ArrayList<>();
            states.add(new MessageProcessingState(state));
        }
    }

    /**
     * Sets a new current processing source for this message unit based on the provided source.
     *
     * @param source The new current processing source
     */
    public void setProcessingState(final IMessageUnitProcessingState source) {
        if (source != null) {
            if (states == null)
                states = new ArrayList<>();
            states.add(new MessageProcessingState(source));
        }
    }

}

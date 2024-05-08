/*
 * Copyright (C) 2016 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.storage;

import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Defines the interface of the stored object that is used by the Holodeck B2B to store the general message unit
 * meta-data.
 * <p>It is based on the {@link IMessageUnit} interface from the generic message model and adds setter methods for the
 * meta-data that can be changed during the processing of a message unit. The <i>Meta-data Storage Provider</i> may use
 * this to optimise the storage of the meta-data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public interface IMessageUnitEntity extends IMessageUnit {

	/**
	 * Gets the <i>CoreId</i> which uniquely identifies the message unit object in the Holodeck B2B instance. Although
	 * the MessageId of a message unit should be globally unique this is not enforced for received message. Assigning
	 * each message unit instance a unique internal identifier therefore helps to make unambiguous references.
	 *
	 * @return	internal unique identifier assigned to this message unit
	 * @since 7.0.0
	 */
	String getCoreId();

    /**
     * Gets the indication whether this message unit is send using a multi-hop exchange
     *
     * @return  <code>true</code> if multi-hop is used for exchange of this message unit,<br>
     *          <code>false</code> otherwise
     */
    boolean usesMultiHop();

    /**
     * Sets the indication whether this message unit is send using a multi-hop exchange
     *
     * @param usingMultiHop		<code>true</code> if multi-hop is used for exchange of this message unit,<br>
     *          				<code>false</code> otherwise
     * @since 7.0.0
     */
    void setMultiHop(boolean usingMultiHop);

    /**
     * Sets the identifier of the P-Mode that governs the message exchange of the message unit.
     *
     * @param pmodeId	the P-Mode.id
     * @since 7.0.0
     */
    void setPModeId(String pmodeId);

    /**
     * Sets the new processing state of the message unit and additional description on it. The start time of the new
     * state should be set to the current time.
     *
     * @param newState		the new processing state
     * @param description	additional description on the new state
     * @since 7.0.0
     */
    void setProcessingState(ProcessingState newState, String description);

    /**
     * Gets the current processing state the message unit is in.
     * <p>Although the current state is the last item in the list that is returned by the {@link #getProcessingStates()}
     * method this method is simpler to use and it also allows implements to optimise the handling of the current
     * processing state.
     *
     * @return  The {@link IMessageUnitProcessingState} the message unit is currently in
     * @since 7.0.0
     */
    IMessageUnitProcessingState getCurrentProcessingState();
}

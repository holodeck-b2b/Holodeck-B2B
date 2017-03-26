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
package org.holodeckb2b.interfaces.persistency.entities;

import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.pmode.ILeg;

/**
 * Defines the interface of the persistent entity object that is used by the Holodeck B2B to store the general message
 * unit meta-data.
 * <p>It is based on the {@link IMessageUnit} interface from the message model. It however does not include
 * <i>setter</i> methods as changes need to be done through the DAO objects. This to ensure that changes in the
 * meta-data of the message unit are saved correctly.
 * <p>As persistency implementations may optimize the loading of collections it is not required to load collection
 * valued fields immediately but perform <i>lazy loading</i>. Therefore this interface adds the
 * {@link #isLoadedCompletely()} method to the getter methods already defined in the {@link IMessageUnit} interface
 * which indicates whether all information is loaded from storage. If a getter method is used for a field that has not
 * been loaded a {@link PersistenceException} will be thrown.<br>
 * Lazy loading SHOULD ONLY be used by persistency implementations when multiple entity objects are retrieved from
 * storage and it is not clear if all meta-data will be used in further processing.
 * <p>In case of the general meta-data that applies to all message units only the list of processing states may be
 * lazily loaded, i.e. {@link #getProcessingStates()} should only be called when the data is completely loaded. <b>
 * NOTE</b> that implementations MUST return the current processing state even if the object is not completely loaded.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.2
 */
public interface IMessageUnitEntity extends IMessageUnit {

    /**
     * Indicates whether all meta-data of the object have been loaded. See the class documentation which fields may be
     * loaded lazily.
     *
     * @return  <code>true</code> if all data has been retrieved from storage,<br>
     *          <code>false</code> otherwise
     */
    boolean isLoadedCompletely();

    /**
     * Gets the label of the leg within the P-Mode on which this message unit is exchanged.
     *
     * @return  The leg label
     */
    ILeg.Label getLeg();

    /**
     * Gets the indication whether this message unit is send using a multi-hop exchange
     *
     * @return  <code>true</code> if multi-hop is used for exchange of this message unit,<br>
     *          <code>false</code> otherwise
     */
    boolean usesMultiHop();
}

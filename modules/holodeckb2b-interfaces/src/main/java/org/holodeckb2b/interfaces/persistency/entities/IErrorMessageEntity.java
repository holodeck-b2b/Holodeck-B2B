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

import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.pmode.ILeg;

/**
 * This interface is used to indicate that the <i>Error Signal Message</i> message unit meta-data is stored by the
 * persistency layer.
 * <p>Beside the generic meta-data fields that may be <i>lazily loaded</i> persistency implementations MAY load the
 * information on the <b>individual errors <i>lazily</i></b>, i.e. before  calling {@link #getErrors()} to get this info
 * the {@link #isLoadedCompletely()} should be executed to check if all  information is loaded.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 * @see   IMessageUnitEntity
 */
public interface IErrorMessageEntity extends IMessageUnitEntity, IErrorMessage {

    /**
     * Gets indicator whether this Error Signal should be combined with a SOAP Fault.
     * <p>Note that the decision whether the SOAP Fault will be added to the message is taken when the actual message
     * is constructed and depends also on the other message units that are included in the same message.
     *
     * @return  <code>true</code> if this Error Signal should be combined with SOAP Fault,<br>
     *          <code>false</code> when not
     */
    boolean shouldHaveSOAPFault();
    
    /**
     * Gets the label of the leg within the P-Mode on which this Error Message is exchanged. Although the Leg can in
     * most cases be calculated there can be an issue when there is no explicit reference to the message unit in error 
     * and there are more than one sent message units in the message the Error Message is a reply to. In that case the
     * P-Mode and Leg of the primary message unit from the sent message are used. But this information is not persisted
     * and therefore the leg is stored with the Error Message.    
     * 
     * @return  The leg label
     * @since 6.0.0
     */
    ILeg.Label getLeg();
}

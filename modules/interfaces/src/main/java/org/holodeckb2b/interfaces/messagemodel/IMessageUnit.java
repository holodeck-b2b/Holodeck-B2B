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
package org.holodeckb2b.interfaces.messagemodel;

/*
 * #%L
 * Holodeck B2B - Interfaces
 * %%
 * Copyright (C) 2015 The Holodeck B2B Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.Date;

/**
 * Is a general representation for all types of ebMS message units and defines methods to access the information
 * available to all ebMS message units. This is the information contained in the <code>eb:MessageInfo</code> and child
 * elements of the ebMS messaging header. See ebMS Core specification, section 5 for more information on the message
 * model. Extension of this base interface define how information specific for a type of message unit can be accessed.
 * <p>This interface and its "<i>subclasses</i>" are used in Holodeck B2B to define the interfaces between the core 
 * modules and the external <i>business</i> applications. This decoupling allows for more easy extension of both the core 
 * as the external functionality.
 * <p><b>NOTE:</b> The information that is available at  depends on the context of processing! If for example a user 
 * message is submitted to Holodeck B2B as a response of a Two-Way MEP, only the <i>RefToMessageId</i> might be known.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IUserMessage
 * @see ISignalMessage
 * @see IErrorMessage
 * @see IPullRequest
 * @see IReceipt
 */
public interface IMessageUnit {
    
    /**
     * Gets the timestamp when the message unit was created. 
     * <p>Corresponds to the <code>MessageInfo/Timestamp</code> element. See section 5.2.2.1 of the ebMS Core 
     * specification.
     * 
     * @return  The timestamp when the message unit was created as a {@link Date}
     */
    public Date getTimestamp();
    
    /**
     * Gets the message id of the message unit.
     * <p>Corresponds to the <code>MessageInfo/MessageId</code> element. See section 5.2.2.1 of the ebMS Core 
     * specification.
     *
     * @return  The message id as a globally unique identifier conforming to RFC2822. 
     */
    public String getMessageId();
    
    /**
     * Get the message id of the message unit to which this message unit is a response.
     * <p>Corresponds to the <code>MessageInfo/RefToMessageId</code> element. See section 5.2.2.1 of the ebMS Core 
     * specification.
     * 
     * @return  The message id of the message this message unit is a response to
     */
    public String getRefToMessageId();
}

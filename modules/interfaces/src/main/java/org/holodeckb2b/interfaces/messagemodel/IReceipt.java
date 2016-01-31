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

import java.util.List;
import org.apache.axiom.om.OMElement;

/**
 * Represents the information available of the Receipt type of signal message. 
 * <p>The Receipt signal message unit essentially is very simple as there is no specific content required for it in the
 * specification the ebMS Core Specification (see section 5.2.3.3). It may contain any kind of XML element. For 
 * notification to the <i>Producer</i> business application the most important info of a Receipt signal therefor is the
 * <i>RefToMessageId</i> which indicates the User Message being acknowledged.<br>
 * The contents of the Receipt, i.e. the child element of <code>eb:Receipt</code> can be retrieved as 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IMessageUnit
 */
public interface IReceipt extends ISignalMessage {

    /**
     * Gets the contents of the Receipt. The ebMS V3 Core Specification only requires that the Receipt element contains
     * at least on child element, but it doesn't specify anything about the child elements. Therefore this methods just
     * returns all elements in the Receipt.
     * 
     * @return A {@link List} of {@link OMElement} objects representing the child elements of <code>eb:Receipt</code>
     */
    public List<OMElement> getContent(); 
}

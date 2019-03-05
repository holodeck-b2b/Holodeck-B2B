/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.messagemodel;

import org.holodeckb2b.interfaces.general.IService;

/**
 * Represents the information available of the PullRequest type of signal message that uses <b>selective pulling</b> as
 * described in <a href="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/part2/201004/cs01/ebms-v3.0-part2-cs01.html#__RefHeading__435723_822242408">
 * section 5.1 of ebMS V3 part 2 (Advanced Features)</a>
 * <p>Besides the <i>MPC</i> (message partition channel) included in the default Pull Request the selective Pull Request
 * can also contain the following <i>selection items</i>:<ol>
 * <li>Simple selection items:<ul>
 *     <li>RefToMessageId</li>
 *     <li>ConversationId</li>
 *     <li>Agreement Reference</li>
 *     <li>Service</li>
 *     <li>Action</li>
 *     </ul>
 *     The selection semantics of such items is: messages from the targeted MPC will be pulled only if they contain a
 *     header element matching exactly the selection item.</li>
 * <li>Complex selection items:<ul>
 *     <li>Sending party: this item may contain one or more <i>PartyId</i>s and at most one <i>Role</i>. The selection
 *     semantics is that only messages with <code>eb3:PartyInfo/eb3:From</code> containing the same elements (including
 *     attributes values if any) as those under this selection item - or a superset of these - will be pulled.</li>
 *     <li>Receiving party: same as for the sending party selection item this may contain one or more <i>PartyId</i>s
 *     and at most one <i>Role</i>. The selection semantics is also the same except it now matches to <code>
 *     eb3:PartyInfo/eb3:To</code></li>
 *     <li>Message Properties: this item may contain one or more message properties. The selection semantics is that
 *     only messages that have a property set <code>eb3:MessageProperties</code> containing the set of given properties
 *     in this item, with specified values, will be pulled. Note that both <code>@name</code> attribute value and
 *     element value must be matched.</li>
 *     </ul></li></ol>
 * <p><b>NOTE:</b> The current version only supports selective pulling as client using the <b>simple</b> selection items
 * listed under 1.</p>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IMessageUnit
 * @since 4.1.0
 */
public interface ISelectivePullRequest extends IPullRequest {

    /**
     * Gets the <i>RefToMessageId</i> selection criterion that is included in the PullRequest.
     * <p>NOTE: Because the methode <code>getRefToMessageId</code> already exists this method is renamed to
     * <code>getReferencedMessageId</code></p>
     *
     * @return      The <i>RefToMessageId</i> of the message to be pulled
     */
    String getReferencedMessageId();

    /**
     * Gets the <i>ConversationId</i> selection criterion that is included in the PullRequest.
     *
     * @return      The <i>ConversationId</i> of the message to be pulled
     */
    String getConversationId();

    /**
     * Gets the <i>Agreement Reference</i> selection criterion that is included in the PullRequest.
     *
     * @return      The <i>Agreement Reference</i> of the message to be pulled
     */
    IAgreementReference getAgreementRef();

    /**
     * Gets the <i>Service</i> selection criterion that is included in the PullRequest.
     *
     * @return      The <i>Service</i> of the message to be pulled
     */
    IService getService();

    /**
     * Gets the <i>Action</i> selection criterion that is included in the PullRequest.
     *
     * @return      The <i>Action</i> of the message to be pulled
     */
    String getAction();
}

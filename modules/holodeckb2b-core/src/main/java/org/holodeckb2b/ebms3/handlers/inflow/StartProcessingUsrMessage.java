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
package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;

/**
 * Is the <i>in flow</i> handler that starts the process of delivering the user message message unit to the business
 * application by changing the message units processing state to {@link ProcessingStates#PROCESSING}. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class StartProcessingUsrMessage extends AbstractUserMessageHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(MessageContext mc, UserMessage um) throws DatabaseException {
        String msgId = um.getMessageId();
        log.debug("Change processing state to indicate start of processing of message [" + msgId + "]" );
        um = MessageUnitDAO.startProcessingMessageUnit(um);
        if (um == null) {
            // Because the returned null is directly assigned to mu the user message unit is automaticly removed
            // from the context and not processed further
            log.warn("User message [msgId= " + msgId + "] is already being processed");
        }
        
        return InvocationResponse.CONTINUE;
    }
}

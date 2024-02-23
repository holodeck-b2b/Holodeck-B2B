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
package org.holodeckb2b.ebms3.handlers.outflow;

import org.holodeckb2b.common.handlers.AbstractConfigureHTTPTransport;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;

/**
 * Is the <i>OUT_FLOW</i> handler that configures the actual message transport over the HTTP protocol and sets the
 * target URL where the message should be send.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see AbstractConfigureHTTPTransport
 */
public class ConfigureHTTPTransportHandler extends AbstractConfigureHTTPTransport {

	@Override
	protected String getDestinationURL(IMessageUnitEntity msgToSend, ILeg leg, IMessageProcessingContext procCtx) {
        String destURL = null;
        try {
            // If the message to send is a Receipt or Error signal we first check if they have a specific URL defined,
        	// otherwise we will use the default target URL
            try {
               if (msgToSend instanceof IReceipt)
                    destURL = leg.getReceiptConfiguration().getTo();
                else if (msgToSend instanceof IErrorMessage)
                    destURL = leg.getUserMessageFlow().getErrorHandlingConfiguration().getReceiverErrorsTo();
            } catch (NullPointerException npe) {}
            // If not we use the URL defined on the leg level which is also the one to use for UserMessage and
            // PullRequest
            if (destURL == null)
                destURL = leg.getProtocol().getAddress();
        } catch (final NullPointerException npe) {
        }

        return destURL;
    }
}

/*
 * Copyright (C) 2017 The Holodeck B2B Team.
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
package org.holodeckb2b.interfaces.security;

import java.util.Collection;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * Defines the interface of the class responsible for the processing of the relevant WS-Security headers in a received
 * ebMS message. This includes the signature and encryption in the <i>"default"</i> header and the username tokens in
 * both the <i>"default"</i> and <i>"ebms"</i> targeted header.
 * <p>When invoked by the Holodeck B2B Core the security processor must complete the signature verification and
 * symmetric key decryption process as well as reading the data from the available username tokens in the message. Note
 * that the processor doesn't need to decrypt the payload data immediately but can do this on the fly when the payloads
 * are saved.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface ISecurityHeaderProcessor {

    /**
     * Processes the WS-Security header(s) in the message. Should at least complete the signature verification and
     * symmetric key decryption process for the <i>"default"</i> security header and read the data from username tokens
     * in both the <i>"default"</i> and <i>"ebms"</i> security headers. If payload data is not immediately
     * decrypted, the method must ensure that decryption will take place when payload is saved (and decryption errors
     * are handled correctly).
     *
     * @param msgContext    The Axis2 message context which should be used to get the SOAP Envelope and access to the
     *                      attachments
     * @param userMsgs      The collection of meta-data on the User Message message units contained in the message
     * @param pmode         The P-Mode of the primary message unit which should be used to configure the security
     *                      processing
     * @return              The result of the processing each part of the WS-Security header.
     * @throws SecurityProcessingException  When an error occurs in the <b>internal</b> processing of the processor,i.e.
     *                                      the error is not directly related to problems in the security headers
     *                                      contained in the message.
     *
     */
    Collection<ISecurityProcessingResult> processHeaders(MessageContext msgContext, Collection<IUserMessage> userMsgs,
                                                         IPMode pmode) throws SecurityProcessingException;
}

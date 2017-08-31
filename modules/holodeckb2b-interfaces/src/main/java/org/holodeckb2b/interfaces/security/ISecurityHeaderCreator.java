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
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;

/**
 * Defines the interface of the class responsible for creating the relevant WS-Security headers in an ebMS message to be
 * send. This includes the signature and encryption in the <i>"default"</i> header and the username tokens in both the
 * <i>"default"</i> and <i>"ebms"</i> targeted header.
 * <p>When invoked by the Holodeck B2B Core the security header creator must complete the signature creation and
 * symmetric key encryption process as well as creating the username tokens in the message. Note however that it is
 * not required to immediately encrypt the payload data and that the can be done on the fly when the message is send.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface ISecurityHeaderCreator {

    /**
     * Creates the WS-Security header(s) in the message. Should at least complete the signature creation and symmetric
     * key encryption process for the <i>"default"</i> security header and create the username tokens in both the
     * <i>"default"</i> and <i>"ebms"</i> security headers. If payload data is not immediately encrypted, the method
     * must ensure that encryption will take place when the message is sent.
     *
     * @param msgContext    The Axis2 message context which should be used to get the SOAP Envelope and access to the
     *                      attachments
     * @param userMsgs      The collection of meta-data on the User Message message units contained in the message
     * @param config        The security configuration to apply as copied from the P-Mode of the primary message unit
     * @return              The result of the creating each part of the WS-Security header.
     * @throws SecurityProcessingException  When an error occurs in the <b>internal</b> processing of the creator,i.e.
     *                                      the error is not directly related to problems in the security headers to be
     *                                      created in the message.
     *
     */
    Collection<ISecurityProcessingResult> createHeaders(MessageContext msgContext, Collection<IUserMessage> userMsgs,
                                                        ISecurityConfiguration config)
                                                                                     throws SecurityProcessingException;
}

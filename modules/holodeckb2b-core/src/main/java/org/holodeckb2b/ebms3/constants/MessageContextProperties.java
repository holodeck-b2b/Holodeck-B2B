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
package org.holodeckb2b.ebms3.constants;

import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.security.IEncryptionProcessingResult;
import org.holodeckb2b.interfaces.security.ISignatureProcessingResult;
import org.holodeckb2b.interfaces.security.IUsernameTokenProcessingResult;

/**
 * Defines constants for <code>MessageContext</code> properties used by the Holodeck B2B Core to store information about
 * the processed ebMS message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION     Converted to interface
 */
public interface MessageContextProperties {

    /**
     * Prefix for all keys used to avoid conflicts
     */
    static String PREFIX = "org:hb2b:";

    /**
     * Holds the <i>primary</i> outgoing user message. The P-Mode of this message unit determines the configuration that
     * is used for sending the complete message.
     */
    static final String OUT_USER_MESSAGE = PREFIX + "out-user-msg";

    /**
     * Holds the received user message.
     */
    static final String IN_USER_MESSAGE = PREFIX + "in-user-msg";

    /**
     * Holds information on the received pull request
     */
    static final String IN_PULL_REQUEST = PREFIX + "in-pullrequest";

    /**
     * Holds the meta-data on the received Receipts
     */
    static final String IN_RECEIPTS = PREFIX + "in-receipts";

    /**
     * Holds the meta-data on the received Errors
     */
    static final String IN_ERRORS = PREFIX + "in-errors";

    /**
     * Holds the list of PModes a received PullRequest is authorized for
     */
    static final String PULL_AUTH_PMODES = PREFIX + "pull-from-pmodes";

    /**
     * Holds the indication whether a synchronous response should be sent back
     */
    static final String RESPONSE_REQUIRED = PREFIX + "response-req";

    /**
     * Holds an array of ebMS Errors that where generated during message processing
     */
    static final String GENERATED_ERRORS = PREFIX + "gen-errors";

    /**
     * Holds an array of Error Signals that should be sent.
     */
    static final String OUT_ERRORS = PREFIX + "send-errors";

    /**
     * Holds the Receipt signal data of the receipt that must be sent as response.
     */
    static final String RESPONSE_RECEIPT = PREFIX + "resp-receipt";

    /**
     * Holds the Receipt signal data of the receipts that must be sent.
     */
    static final String OUT_RECEIPTS = PREFIX + "out-receipts";

    /**
     * Holds the PullRequest signal data that must be sent.
     */
    static final String OUT_PULL_REQUEST = PREFIX + "out-pullrequest";

    /**
     * Holds the indicator whether the received user message message unit was successfully delivered to the business
     * application, i.e. the {@link IMessageDeliverer} did throw an exception.
     */
    static final String DELIVERED_USER_MSG = PREFIX + "usrmsg-delivered";

    /**
     * Holds the {@link ISignatureProcessingResult} object with the results of the signature verification
     * @since HB2B_NEXT_VERSION
     */
    static final String SIG_VERIFICATION_RESULT = PREFIX + "signcheck-result";

    /**
     * Holds the {@link IEncryptionProcessingResult} object with the results of the processing of the encryption headers
     * @since HB2B_NEXT_VERSION
     */
    static final String DECRYPTION_RESULT = PREFIX + "decryption-result";

    /**
     * Holds the {@link IUsernamTokenProcessingResult} object with the results of the processing of the username tokene
     * header targeted to the <i>default</i> role/actor.
     * @since HB2B_NEXT_VERSION
     */
    static final String DEFAULT_UT_RESULT = PREFIX + "def-ut-result";

    /**
     * Holds the {@link IUsernamTokenProcessingResult} object with the results of the processing of the username tokene
     * header targeted to the <i>ebms</i> role/actor.
     * @since HB2B_NEXT_VERSION
     */
    static final String EBMS_UT_RESULT = PREFIX + "ebms-ut-result";
}

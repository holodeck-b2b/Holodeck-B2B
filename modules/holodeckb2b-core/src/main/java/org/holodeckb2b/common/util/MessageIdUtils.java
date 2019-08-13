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
package org.holodeckb2b.common.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;

/**
 * Is a utility class to generate and the message and MIME content-id identifiers that are use in the message
 * processing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 4.0.0     Replaces the old <code>org.holodeckb2b.common.util.MessageIdGenerator</code> class
 */
public class MessageIdUtils {

    /**
     * Generates an unique message id as specified in the ebMS V3 Core Specification.
     *
     * @return A message id conforming to RFC2822
     */
    public static String createMessageId() {
        // Generate a UUID as left part of the msg-id
        final String leftPart = UUID.randomUUID().toString();

        // Right part of the msg-id is the host name which can be either specified,
        //  retrieved or randomly generated
        final String rightPart = HolodeckB2BCoreInterface.getConfiguration().getHostName();

        return leftPart + '@' + rightPart;
    }

    /**
     * Generates a unique [MIME] content id based on the given message id.
     * <p><b>NOTE:</b> If the given message id is <code>null</code> or empty a random
     * content id will be generated.
     *
     * @param msgId     The message id to use as base for the content id
     * @return          A unique content id
     */
    public static String createContentId(final String msgId) {
        String leftPart, rightPart;

        if (msgId == null || msgId.isEmpty())
            // Because there is no message id to base the cid on, just create
            // a completely new id which is equivalent to a msg id
            return createMessageId();
        else {
            // Split msgId in left and right part (including the '@'). When msg
            // id does not contain '@' use empty right part
            final int i = msgId.indexOf("@");
            if (i > 0) {
                leftPart = msgId.substring(0, i);
                rightPart = msgId.substring(i);
            } else {
                leftPart = msgId;
                rightPart = "";
            }

            // Add random number to left part
            leftPart += "-" + String.valueOf(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));

            // And return with rightPart added again
            return leftPart + rightPart;
        }
    }

    /**
     * Contains the regular expression to use for checking on conformance to RFC2822. The regular expression is based
     * on the ABNF definition of messageId given in the RFC.
     */
    private static final String   RFC2822_MESSAGE_ID;
    static {
        String  ALPHA = "[a-zA-Z]";
        String  DIGIT = "\\d";
        String  atext = "[" + ALPHA + DIGIT + "\\Q" +
                            "!" + "#" +
                            "$" + "%" +
                            "&" + "'" +
                            "*" + "+" +
                            "-" + "/" +
                            "=" + "?" +
                            "^" + "_" +
                            "`" + "{" +
                            "|" + "}" +
                            "~" + "\\E" + "]";

        String   dot_atom_text   =   atext + "+" +  "(\\." + atext + "+)*";

        String   id_left         =   dot_atom_text;
        String   id_right        =   dot_atom_text;

        RFC2822_MESSAGE_ID = id_left + "@" + id_right;
    }

    /**
     * Checks whether the given messageId is correctly formatted as specified in <a href=
     * "https://tools.ietf.org/html/rfc2822">RFC2822</a>.
     *
     * @param messageId     The message id to check for correct syntax
     * @return              <code>true</code> if the given messageId is in correct format,<br>
     *                      <code>false></code> otherwise
     * @since 4.0.0
     */
    public static boolean isCorrectFormat(final String messageId) {
        if (Utils.isNullOrEmpty(messageId))
            return false;
        else
            return messageId.matches(RFC2822_MESSAGE_ID);
    }
    
    /**
     * Strips the brackets (&lt; and &gt;) from the given MessageId string. 
     * 
     * @param messageId	The MessageId string
     * @return	The messageId value without brackets
     * @since 4.0.0
     */
    public static String stripBrackets(final String messageId) {
    	if (messageId == null)
    		return null;
    	
    	String msgIdOnly = messageId;
        if (msgIdOnly.startsWith("<"))
        	msgIdOnly = msgIdOnly.substring(1);
        if (msgIdOnly.endsWith(">"))
        	msgIdOnly = msgIdOnly.substring(0, msgIdOnly.length() - 1);
        return msgIdOnly;
    }
}

/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
import org.holodeckb2b.common.config.Config;

/**
 * Generates unique identifiers for use in message processing.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageIdGenerator {
   
    /**
     * Generates an unique message id as specified in the ebMS V3 Core Specification.
     * 
     * @return A message id conforming to RFC2822
     */
    public static String createMessageId() {
        // Generate a UUID as left part of the msg-id
        String leftPart = UUID.randomUUID().toString();
        
        // Right part of the msg-id is the host name which can be either specified,
        //  retrieved or randomly generated
        String rightPart = Config.getHostName();
        
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
    public static String createContentId(String msgId) {
        String leftPart, rightPart;
        
        if (msgId == null || msgId.isEmpty())
            // Because there is no message id to base the cid on, just create
            // a completely new id which is equivalent to a msg id
            return createMessageId();
        else {
            // Split msgId in left and right part (including the '@'). When msg 
            // id does not contain '@' use empty right part 
            int i = msgId.indexOf("@");
            if (i > 0) {
                leftPart = msgId.substring(0, i - 1);
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
}

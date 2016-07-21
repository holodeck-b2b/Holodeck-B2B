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
package org.holodeckb2b.ebms3.mmd.xml;

import java.text.ParseException;
import java.util.Date;

import org.holodeckb2b.common.util.Utils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * Represents the <code>MessageInfo</code> element from a MMD document.
 * <p><code>MessageInfo</code> contains some technical details that are used to
 * identify the user message unit and its possible relation to another user
 * message unit.
 * <p>When submitting a message the timestamp <b>SHOULD NOT</b> be used as the
 * timestamp is set by Holodeck B2B (as required by ebMS spec, see 5.2.2.1).<br>
 * If the submitted message relates to another message the RefToMessageId <b>MUST</b>
 * be set by the submitter, Holodeck B2B will not try to detect message relations.<br>
 * The submitter <b>MAY</b> set the MessageId if needed and <b>MUST</b> ensure that the
 * supplied value is a globally unique identifier conforming to <i>MessageId</i>
 * as defined in [RFC2822].
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageInfo {

    /*
     * NOTE: The schema for the MMD document (see <code>/src/main/resources/xsd/messagemetadata.xsd</code>)
     * defines the <code>Timestamp</code> as <i>xs:dateTime</i>. Simple XML however does not parse these
     * to <code>java.util.Date</code>. Therefor the variable is defined as <code>String</code> and the
     * tranformation to <code>Date</code> is done in the getter and setter methods.
     */
    @Element(name = "Timestamp", required = false)
    private String  timestamp;

    @Element(name = "MessageId", required = false)
    private String  messageId;

    @Element(name = "RefToMessageId", required = false)
    private String  refToMessageId;

    @Attribute(name = "mpc", required = false)
    private String  mpc;

    /**
     * @return the timestamp
     */
    public Date getTimestamp() {
        Date result = null;

        try {
            result = Utils.fromXMLDateTime(timestamp);
        } catch (final ParseException ex) {
            // If date could not be parsed, no date will be available
         }

        return result;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(final Date timestamp) {
        this.timestamp = Utils.toXMLDateTime(timestamp);
    }

    /**
     * @return the messageId
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * @param messageId the messageId to set
     */
    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    /**
     * @return the refToMessageId
     */
    public String getRefToMessageId() {
        return refToMessageId;
    }

    /**
     * @param refToMessageId the refToMessageId to set
     */
    public void setRefToMessageId(final String refToMessageId) {
        this.refToMessageId = refToMessageId;
    }

    /**
     * @return the mpc
     */
    public String getMpc() {
        return mpc;
    }

    /**
     * @param mpc the mpc to set
     */
    public void setMpc(final String mpc) {
        this.mpc = mpc;
    }

}

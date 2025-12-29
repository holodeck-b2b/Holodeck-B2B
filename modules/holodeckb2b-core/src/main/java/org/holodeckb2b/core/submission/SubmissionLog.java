/**
 * Copyright (C) 2023 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.submission;

import java.util.Collection;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventHandler;
import org.holodeckb2b.interfaces.events.IMessageSubmission;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Provides a basic logging of submitted messages using the default Log4j logging system. When more extensive logging is
 * required, it is recommended to implement a {@link IMessageProcessingEventHandler} for the {@link IMessageSubmission}
 * event.
 * <p>For each submission a log line containing the meta-data of the submitted message unit is written to the
 * <i>org.holodeckb2b.core.submission.SubmissionLog</i> log. Which meta-data is logged depends on the log level and is
 * described below. The different data elements are separated by a ';' and when the meta-data element has a <i>type</i>
 * attribute set, it is formatted as "[" + «type» + "]" + «value». For example for a <code>PartyId</code> with
 * <i>type</i> "urn:org:holodeckb2b:example:pid" and <i>value</i> "PartyA" the logged text would
 * be "[urn:org:holodeckb2b:example:pid]PartyA".<br>
 * When a element can contain multiple values, for example the message properties, the formatting is "{" +
 * «value<sub>0</sub>» + ("," + «value<sub>i</sub>»)* + "}".
 * <p><u>Logged data at level INFO</u>
 * <p>For both Pull Request and User Message message units:<ol>
 * <li>Message Unit name, i.e. "[Selective]PullRequest" or "UserMessage"</li>
 * <li>PMode.id of the P-Mode that handles the processing of the message unit</li>
 * <li>MessageId</li>
 * <li>RefToMessageId</li>
 * <li>Timestamp</li>
 * </ol>
 * For Pull Request message units:<ol start="6">
 * <li>MPC</li>
 * </ol>
 * For User Message message units:<ol start="6">
 * <li>ConversationId</li>
 * </ol>
 *
 * <p><u>Additional data logged at level DEBUG</u>
 * <p>No additional data is logged for Pull Requests.<br/>
 * For User Message message units:<ol start="7">
 * <li>First PartyId of the Sender of the message as contained in the <code>//eb3:From/eb3:PartyId[0]</li> element of
 * the ebMS messaging header</li>
 * <li>Role of the Sender</li>
 * <li>First PartyId of the Receiver of the message as contained in the <code>//eb3:To/eb3:PartyId[0]</li> element of
 * the ebMS messaging header</li>
 * <li>Role of the Receiver</li>
 * <li>Service</li>
 * <li>Action</li>
 * </ol>
 * NOTE: Although only a single PartyId is logged for both Sender and Receiver they are formatted as a collection, i.e.
 * surrounded by '{' and '}', to ensure consistent formatting on all log levels.
 *
 * <p><u>Additional data logged at level TRACE</u>
 * <p>No additional data is logged for "normal" Pull Requests.<br>
 * For Selective Pull Request message units the selection criteria are logged:<ol start="7">
 * <li>RefToMessageId</li>
 * <li>ConversationId</li>
 * <li>AgreementRef</li>
 * <li>Service</li>
 * <li>Action</li>
 * </ol>
 * <p>For User Message message units:<ol start="11">
 * <li>All PartyIds of the Sender or the message</li>
 * <li>MPC, only included when different from the {@link EbMSConstants#DEFAULT_MPC default MPC}. When the default MPC
 * is used the field stays empty</li>
 * <li>AgreementRef</i>
 * <li>Message Properties. The properties are formatted as a collection of name value pairs that are format as
 * 	«name« + "=" + «value». Because the value itself can be typed it is formatted as explained above.</li>
 * <li>The number of payloads contained in the User Message</li>
 * </ol>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 6.1.0
 */
class SubmissionLog {

	private static final Logger log = LogManager.getLogger();

	/**
	 * Adds a log entry to the submission log for the given message unit.
	 *
	 * @param m	the submitted message unit
	 */
	static void logSubmission(IMessageUnit m) {
		if (!log.isInfoEnabled())
			return;

		StringBuilder entry = new StringBuilder();

		if (m instanceof IUserMessage)
			entry.append("UserMessage");
		else if (m instanceof ISelectivePullRequest)
			entry.append("SelectivePullRequest");
		else
			entry.append("PullRequest");
		entry.append(';');

		entry.append(m.getPModeId()).append(';')
			 .append(m.getMessageId()).append(';')
			 .append(formatString(m.getRefToMessageId())).append(';')
			 .append(Utils.toXMLDateTime(m.getTimestamp())).append(';');

		if (m instanceof IUserMessage)
			entry.append(((IUserMessage) m).getCollaborationInfo().getConversationId());
		else
			entry.append(((IPullRequest) m).getMPC());

		if (log.isDebugEnabled() && m instanceof IUserMessage) {
			IUserMessage um = (IUserMessage) m;
			entry.append(';')
				 // listPartyIds() will take care of logging one or all PartyIds depending on log level
				 .append(listPartyIds(um.getSender().getPartyIds())).append(';')
				 .append(formatString(um.getSender().getRole())).append(';')
				 .append(listPartyIds(um.getReceiver().getPartyIds())).append(';')
				 .append(formatString(um.getReceiver().getRole())).append(';')
				 .append(formatService(um.getCollaborationInfo().getService())).append(';');
			entry.append(formatString(um.getCollaborationInfo().getAction()));
			if (log.isTraceEnabled()) {
				entry.append(';');
				String mpc = um.getMPC();
				if (!Utils.isNullOrEmpty(mpc) && !mpc.equals(EbMSConstants.DEFAULT_MPC))
					entry.append(mpc);
				entry.append(';')
					 .append(formatAgreementRef(um.getCollaborationInfo().getAgreement())).append(';')
					 .append(listProperties(um.getMessageProperties())).append(';')
					 .append(Utils.isNullOrEmpty(um.getPayloads()) ? 0 : um.getPayloads().size());
			}
		}

		if (log.isTraceEnabled() && m instanceof ISelectivePullRequest) {
			ISelectivePullRequest pr = (ISelectivePullRequest) m;
			entry.append(formatString(pr.getReferencedMessageId())).append(';')
				 .append(formatString(pr.getConversationId())).append(';')
				 .append(formatAgreementRef(pr.getAgreementRef())).append(';')
				 .append(formatService(pr.getService())).append(';')
				 .append(formatString(pr.getAction()));
		}

		log.log(log.getLevel(), entry.toString());
	}

	/**
	 * Creates a string representation for the collection of PartyIds. Depending on log level it will contain only the
	 * first (for DEBUG) or all ids (for TRACE).
	 *
	 * @param pids	collection to PartyIds
	 * @return	String representation of the PartyIds to include in log message
	 */
	private static StringBuffer listPartyIds(Collection<IPartyId> pids) {
		StringBuffer sb = new StringBuffer();
		Iterator<IPartyId> it = pids.iterator();
		IPartyId pid = null;
		do {
			if (pid == null)
				sb.append('{');
			else
				sb.append(',');
			pid = it.next();
			if (!Utils.isNullOrEmpty(pid.getType()))
				sb.append('[').append(pid.getType()).append(']');
			sb.append(pid.getId());
		} while (it.hasNext() && log.isTraceEnabled());
		sb.append('}');

		return sb;
	}

	/**
	 * Formats the Service meta-data.
	 *
	 * @param svc	Service meta-data of the message
	 * @return	formatted text representing the Service
	 */
	private static StringBuffer formatService(IService svc) {
		StringBuffer sb = new StringBuffer();
		if (svc == null)
			return sb;

		if (!Utils.isNullOrEmpty(svc.getType()))
			sb.append('[').append(svc.getType()).append(']');
		sb.append(svc.getName());
		return sb;
	}

	/**
	 * Formats the Agreement Reference meta-data.
	 *
	 * @param ar	Agreement Reference meta-data of the message
	 * @return	formatted text representing the Agreement Reference
	 */
	private static StringBuffer formatAgreementRef(IAgreementReference ar) {
		StringBuffer sb = new StringBuffer();
		if (ar == null)
			return sb;

		if (!Utils.isNullOrEmpty(ar.getType()))
			sb.append('[').append(ar.getType()).append(']');
		sb.append(ar.getName());
		return sb;
	}

	/**
	 * Creates a string representation for the collection of Properties.
	 *
	 * @param props	the collection to properties
	 * @return	String representation of the properties to include in log message
	 */
	private static StringBuffer listProperties(Collection<IProperty> props) {
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		if (!Utils.isNullOrEmpty(props)) {
			Iterator<IProperty> it = props.iterator();
			IProperty p = null;
			do {
				if (p != null)
					sb.append(',');
				p = it.next();
				sb.append(p.getName()).append('=');
				if (!Utils.isNullOrEmpty(p.getType()))
					sb.append('[').append(p.getType()).append(']');
				sb.append(p.getValue());
			} while (it.hasNext());
		}
		sb.append('}');

		return sb;
	}

	/**
	 * Formats the String value of a meta-data element, returning a empty String if the value is <code>null</code>
	 *
	 * @param s		the string value to format
	 * @return	the value if not <code>null</code> or the empty string if <code>null</code>
	 */
	private static String formatString(String s) {
		return s != null ? s : "";
	}
}
/*
 * Copyright (C) 2022 The Holodeck B2B Team, Sander Fieten
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.delivery.IDeliveryMethod;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is a <i>delivery method</i> that will only log some meta-data of the received message. Such a delivery method is
 * useful when implementing a test service where there is no need to actually deliver the message to a back-end system.
 * By the default the meta-data is logged to <i>org.holodeckb2b.common.util.LogOnlyDeliveryMethod</i>. The log name can
 * be further specified by providing the <i>LOG_NAME</i> parameter which will be added as postfix to the default name.
 * <p>The meta-data is logged on one line and parts are separated by a ';'. The amount of meta-data that is logged
 * depends on the configured log level. At <i>INFO</i> the type of message unit, the MessageId, time stamp and
 * RefToMessageId. For User Messages the first PartyId of the Sender and Receiver can be added by setting the log level
 * to <i>DEBUG</i>. At <i>TRACE</i> all PartyIds are logged together with the MPC, Agreement info, Service and Action.
 * A complete log line, i.e. for a UserMessage at log level <i>TRACE</i> will look like this:
 * <pre>
 * UserMessage;0002-test-msg@example.holodeck-b2b.org;2022-09-23T14:37:22.373Z;0001-test-msg@example.holodeck-b2b.org;http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC;{[examples]partya};{[examples]partyb,sub-partyb2};ConnectivityTestAgreement;TestService;Ping
 * </pre>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 6.0.0
 */
public class LogOnlyDeliveryMethod implements IDeliveryMethod {
	/**
	 * The log to write the meta-data of the received User Messages to
	 */
	private Logger log;

	@Override
	public void init(Map<String, ?> settings) throws MessageDeliveryException {
		String specificName = null;
		try {
			specificName = (String) settings.get("LOG_NAME");
		} catch (ClassCastException nas) {
			LogManager.getLogger().warn("An invalid value was provided as the log name");
		}
		log = LogManager.getLogger(LogOnlyDeliveryMethod.class.getName()
									+ (!Utils.isNullOrEmpty(specificName) ? "." + specificName : ""));
	}

	@Override
	public boolean supportsAsyncDelivery() {
		return false;
	}

	@Override
	public void deliver(IMessageUnit rcvdMsgUnit) throws MessageDeliveryException {
		if (log.isInfoEnabled()) {
			StringBuilder msg = new StringBuilder();
			msg.append(MessageUnitUtils.getMessageUnitName(rcvdMsgUnit)).append(';');
			msg.append(rcvdMsgUnit.getMessageId()).append(';');
			msg.append(Utils.toXMLDateTime(rcvdMsgUnit.getTimestamp())).append(';');
			if (!Utils.isNullOrEmpty(rcvdMsgUnit.getRefToMessageId()))
				msg.append(rcvdMsgUnit.getRefToMessageId());
			msg.append(';');
			if (rcvdMsgUnit instanceof IUserMessage && log.isDebugEnabled()) {
				IUserMessage um = (IUserMessage) rcvdMsgUnit;
				if (log.isTraceEnabled())
					msg.append(um.getMPC()).append(';');
				msg.append(listPartyIds(um.getSender().getPartyIds())).append(';');
				msg.append(listPartyIds(um.getReceiver().getPartyIds())).append(';');
				if (log.isTraceEnabled()) {
					IAgreementReference agreement = um.getCollaborationInfo().getAgreement();
					if (agreement != null) {
						if (!Utils.isNullOrEmpty(agreement.getType()))
							msg.append('[').append(agreement.getType()).append(']');
						msg.append(agreement.getName());
					}
					msg.append(';');
					IService service = um.getCollaborationInfo().getService();
					if (!Utils.isNullOrEmpty(service.getType()))
						msg.append('[').append(service.getType()).append(']');
					msg.append(service.getName()).append(';');
					msg.append(um.getCollaborationInfo().getAction());
				}
			}
			log.log(log.getLevel(), msg.toString());
		}
	}

	/**
	 * Creates a string representation for the collection of PartyIds. Depending on log level it will contain only the
	 * first (for DEBUG) or all ids (for TRACE).
	 *
	 * @param pids	collection to PartyIds
	 * @return	String representation of the PartyIds to include in log message
	 */
	private StringBuffer listPartyIds(Collection<IPartyId> pids) {
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
}

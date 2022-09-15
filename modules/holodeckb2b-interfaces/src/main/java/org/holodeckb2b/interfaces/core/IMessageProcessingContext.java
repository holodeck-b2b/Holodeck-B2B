/*
 * Copyright (C) 2020 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.core;

import java.util.Collection;
import java.util.Map;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;

/**
 * Is the interface that represents the Holodeck B2B <i>message processing context</i> during the execution of the 
 * message processing pipe line. It contains the entity objects of the received and sent message units which are used by 
 * the message processing handlers.
 * <p>Note that "sent" and "received" are not equal to [HTTP] request and response as message units are also received 
 * in the request and sent in responses. Message units of the same type can also occur both as sent and received, except
 * for the <i>Pull Request</i> which can always occur just once.
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  5.0.0
 */
public interface IMessageProcessingContext {
	/**
	 * Special key to identify the generated errors which do not reference a message in error.
	 */
	String	UNREFD_ERRORS = "unrefd";

	/**
	 * Gets the current parent Axis2 message context. 
	 * 
	 * @return	The current Axis2 {@link MessageContext}
	 */
	MessageContext getParentContext();

	/**
	 * Indicates if the current message processing is for a request initiated by Holodeck B2B or is for responding
	 * to a received request.
	 * 
	 * @return 	<code>true</code> if this context is for a request initiated by Holodeck B2B,<br>
	 * 			<code>false</code> if it is for a request handled by Holodeck B2B
	 */
	boolean isHB2BInitiated();

	/**
	 * Clears the message processing context by removing all messages from it.
	 */
	void removeAllMessages();

	/**
	 * Sets the <i>User Message</i> that is processed in the current flow of this message processing context. Based on
	 * the flow the message unit is set as either received (in flow) or sending (out_flow). 
	 * 
	 * @param userMessage	The received User Message
	 */
	void setUserMessage(IUserMessageEntity userMessage);

	/**
	 * Sets the <i>User Message</i> that should be send as a response [to a Pull Request]. This will automatically set
	 * the response indicator to true. 
	 * 
	 * @param userMessage	The User Message to be send as response
	 */
	void setResponseUserMessage(IUserMessageEntity userMessage);

	/**
	 * @return the received <i>User Message</i>.	
	 */
	IUserMessageEntity getReceivedUserMessage();

	/**
	 * @return the <i>User Message</i> that is being send.	
	 */
	IUserMessageEntity getSendingUserMessage();

	/**
	 * Sets the <i>Pull Request</i> that is being processed in this message processing context.
	 * 
	 * @param pullRequest	The Pull Request being processed 
	 */
	void setPullRequest(IPullRequestEntity pullRequest);

	/**
	 * @return the received <i>Pull Request</i>.	
	 */
	IPullRequestEntity getReceivedPullRequest();

	/**
	 * @return 	the <i>Pull Request</i> that is being send.
	 */
	IPullRequestEntity getSendingPullRequest();

	/**
	 * Adds a received <i>Receipt Signal</i> to the message processing context. 
	 * 
	 * @param receipt	The received Receipt
	 */
	void addReceivedReceipt(IReceiptEntity receipt);

	/**
	 * @return the collection of received <i>Receipt</i>s
	 */
	Collection<IReceiptEntity> getReceivedReceipts();

	/**
	 * Sets the <i>Receipt Signals</i> to be send.
	 * 
	 * @param receiptsToSend	The collection of Receipts to send
	 */
	void setSendingReceipts(Collection<IReceiptEntity> receiptsToSend);

	/**
	 * Adds a <i>Receipt Signal</i> to be send to the message processing context. 
	 * 
	 * @param receipt	The Receipt to be send
	 */
	void addSendingReceipt(IReceiptEntity receipt);

	/**
	 * @return the collection of <i>Receipt</i>s to be send
	 */
	Collection<IReceiptEntity> getSendingReceipts();

	/**
	 * Adds a received <i>Error Signal</i> to the message processing context. 
	 * 
	 * @param error	The received Error message
	 */
	void addReceivedError(IErrorMessageEntity error);

	/**
	 * @return the collection of received <i>Error Signal</i>s
	 */
	Collection<IErrorMessageEntity> getReceivedErrors();

	/**
	 * Sets the <i>Error Signals</i> to be send.
	 * 
	 * @param errorsToSend	The collection of Error Signals to send
	 */
	void setSendingErrors(Collection<IErrorMessageEntity> errorsToSend);

	/**
	 * Adds a <i>Error Signal</i> to be send to the message processing context. 
	 * 
	 * @param error	The Error Signal to be send
	 */
	void addSendingError(IErrorMessageEntity error);

	/**
	 * @return the collection of <i>Error Signals</i>s to be send
	 */
	Collection<IErrorMessageEntity> getSendingErrors();

	/**
	 * Retrieves all entity objects of message units that are received in the current operation.
	 *
	 * @return      {@link Collection} of {@link EntityProxy} objects for the message units in the received message.
	 */
	Collection<IMessageUnitEntity> getReceivedMessageUnits();

	/**
	 * Checks if a received message unit exists with the given messageId and if it does returns it.
	 * 
	 * @param messageId		The messageId to search for
	 * @return				Received message unit with the given messageId if one exists,<br><code>null</code> otherwise 
	 */
	IMessageUnitEntity getReceivedMessageUnit(String messageId);

	/**
	 * Retrieves the collection of entity objects for all message units that are (to be) or were previously sent in the 
	 * current operation.
	 *
	 * @return      {@link Collection} of {@link IMessageUnitEntity} objects for the message units that were sent.
	 */
	Collection<IMessageUnitEntity> getSendingMessageUnits();

	/**
	 * Checks if a outgoing message unit exists with the given messageId and if it does returns it.
	 * 
	 * @param messageId		The messageId to search for
	 * @return				Outgoing message unit with the given messageId if one exists,<br><code>null</code> otherwise 
	 */
	IMessageUnitEntity getSendingMessageUnit(String messageId);

	/**
	 * Adds an {@link IEbmsError} that has been generated during message processing to the context.
	 *
	 * @param error     The generated {@link EbmsError} 
	 */
	void addGeneratedError(IEbmsError error);

	/**
	 * @return	all generated errors during the processing of the received message mapped on the referenced messageId  
	 */
	Map<String, Collection<IEbmsError>> getGeneratedErrors();

	/**
	 * Adds a entry to the map that registers the relationship between received Error Signals and the message units they
	 * reference.  
	 * 
	 * @param error			The received Error Signal
	 * @param refdMsgUnits	The referenced message units
	 */
	void addRefdMsgUnitByError(IErrorMessage error, Collection<IMessageUnitEntity> refdMsgUnits);

	/**
	 * Gets the message units that are referenced by the given Error Signal as registered using the {@link 
	 * IMessageProcessingContext#addRefdMsgUnitByError(IErrorMessage, Collection)}
	 * 
	 * @param error		The received Error Signal
	 * @return 			The referenced message units if the given Error Signal is registered in this context,<br>
	 * 					<code>null</code> otherwise
	 */
	Collection<IMessageUnitEntity> getRefdMsgUnitByError(IErrorMessage error);

	/**
	 * Gets the primary message unit from a message. The primary message unit determines which settings must be used for
	 * message wide P-Mode parameters, i.e. parameters that do not relate to the content of a specific message unit.
	 * Examples are the destination URL for a message and the WS-Security settings.
	 * <p>The primary message unit is determined by the type of message unit, but differs depending on whether the
	 * message is sent or received by Holodeck B2B. The following table lists the priority of message unit types for
	 * each direction, the first message unit with the highest classified type is considered to be the primary message
	 * unit of the message:
	 * <table border="1">
	 * <tr><th>Prio</th><th>Received</th><th>Sent</th></tr>
	 * <tr><td>1</td><td>User message</td><td>Pull request</td></tr>
	 * <tr><td>2</td><td>Receipt</td><td>User message</td></tr>
	 * <tr><td>3</td><td>Error</td><td>Receipt</td></tr>
	 * <tr><td>4</td><td>Pull request</td><td>Error</td></tr>
	 * </table>
	 *
	 * @return      The entity object of the primary message unit if one was found, or
	 *              <code>null</code> if no message unit could be found in the message context
	 */
	IMessageUnitEntity getPrimaryMessageUnit();

	/**
	 * Adds the result of processing a security token in the received message to the context.
	 * 
	 * @param result	the processing result
	 */
	void addSecurityProcessingResult(ISecurityProcessingResult result);

	/**
	 * Gets the processing results for a specific type of security tokens (identified by the results class). 
	 * 
	 * @param type	The class of processing results to retrieve, MUST NOT be <code>null</code>
	 * @return	All results of processing of the given class 
	 */
	<T extends ISecurityProcessingResult> Collection<T> getSecurityProcessingResults(Class<T> type);

	/**
	 * @return all results of processing the security tokens in the received message.
	 */
	Collection<ISecurityProcessingResult> getSecurityProcessingResults();

	/**
	 * Sets the indicator whether a <b>synchronous</b> response should be send or not. 
	 * 
	 * @param responseNeeded	Indicates whether a response should be send. 
	 * @throws IllegalStateException when this method is called from the out flow
	 */
	void setNeedsResponse(boolean responseNeeded);

	/**
	 * @return indicator if a <b>synchronous</b> response is requested.	
	 */
	boolean responseNeeded();

	/**
	 * Sets the indicator whether the Core should check for and eliminate duplicates. Using this indicator messaging
	 * protocol specific handlers can trigger the duplicate elimination function based on protocol specific conditions.
	 * 
	 * @param useDupElimination the indication whether the Core should check for and eliminate duplicates
	 */
	void setDuplicateElimination(boolean useDupElimination);
	
	/**
	 * Gets the indicator whether the Core should check for and eliminate duplicates. Using this indicator messaging
	 * protocol specific handlers can trigger the duplicate elimination function based on protocol specific conditions.
	 * 
	 * @return	indication whether the Core should check for and eliminate duplicates
	 */
	boolean eliminateDuplicates();	
	
	/**
	 * Sets a property in this message processing context.
	 *  
	 * @param name		the propety's name
	 * @param value		the property's value
	 */
	void setProperty(String name, Object value);

	/**
	 * Gets the value of a property in this message processing context or in the Axis2 message context if no property
	 * with the given name exists in this context.
	 *  
	 * @param name		the propety's name
	 * @return 			the property's value if available or <code>null</code> if no property with the given name is
	 * 					available in either this or the Axis2 message context.
	 */
	Object getProperty(String name);
}
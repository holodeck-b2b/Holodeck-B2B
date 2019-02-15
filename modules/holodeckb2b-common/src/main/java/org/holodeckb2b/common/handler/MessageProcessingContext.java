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
package org.holodeckb2b.common.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.messagemodel.EbmsError;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;


/**
 * Represents the Holodeck B2B <i>message processing context</i> during the execution of the message processing pipe
 * line. It contains the entity objects of the received and sent message units which are used by the message processing 
 * handlers. Note that sent and received are not equal to [HTTP] request and response as message units are also received 
 * in the request and sent in responses. Message units of the same type can also occur both as sent and received, except
 * for the <i>Pull Request</i> which can always occur just once.
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  4.1.0
 * @see AbstractBaseHandler
 */
public class MessageProcessingContext {
	/**
	 * The name of the Axis2 message context property used to store this Holodeck B2B message processing context
	 */
	static final String	AXIS_MSG_CTX_PROP = "hb2b-msgprocctx";
        
	private MessageContext						axisParentCtx;
	
	private IUserMessageEntity					receivedUserMessage;
	private IUserMessageEntity					sendingUserMessage;
	
	private IPullRequestEntity					receivedPullRequest;
	private IPullRequestEntity					sendingPullRequest;
	
	private ArrayList<IReceiptEntity>			receivedReceipts = new ArrayList<>();
	private ArrayList<IReceiptEntity>			sendingReceipts = new ArrayList<>();
	
	private ArrayList<IErrorMessageEntity>		receivedErrors = new ArrayList<>();
	private ArrayList<IErrorMessageEntity>		sendingErrors = new ArrayList<>();
	
	/**
	 * The errors generated during the processing of a received message, mapped to the messageId of the message in 
	 * error. If an error does not reference a message it will be added to the special key {@link #UNREFD_ERRORS}.
	 * <p>NOTE: As errors that occur during the processing of an outgoing message are reported using {@link 
	 * IMessageProcessingEvent}s there is no need to store these in the context.
	 */
	private Map<String, Collection<IEbmsError>>		generatedErrors  = new HashMap<>();
	/**
	 * Special key to identify the generated errors which do not reference a message in error.
	 */
	public static final String	UNREFD_ERRORS = "unrefd";
	
	/**
	 * Holds a map of received errors to the message units they reference. This mapping is maintained in the context
	 * so the handlers responsible for processing and delivery of the errors do not need to both query the database. 
	 */
	private Map<IErrorMessage, Collection<IMessageUnitEntity>>	refdMessagesByError = new HashMap<>();
	
	private boolean	needsResponse;
	
	/**
	 * Stores the results of processing the security tokens available in the incoming message. These may be needed by
	 * multiple handlers in the pipeline, for example to authorise messages or find a matching P-Mode.  
	 */
	private Collection<ISecurityProcessingResult>	securityResults = new ArrayList<>();
	
	/**
	 * Handlers or extension can add custom properties to the processing context so they can pass information to
	 * other components.
	 */
	private Map<String, Object>		properties = new HashMap<>();
	

	/**
	 * Creates a new instance based on the given Axis2 message context.
	 * 
	 * @param axisMsgCtx	The Axis2 message context
	 */
	public MessageProcessingContext(final MessageContext axisMsgCtx) {
		this.setParentContext(axisMsgCtx);		
	}
	
	/**
	 * Sets the parent Axis2 context.
	 * 
	 * @param axisMsgCtx	The new Axis2 parent context
	 */
	public void setParentContext(final MessageContext axisMsgCtx) {
		this.axisParentCtx = axisMsgCtx;
		axisMsgCtx.setProperty(AXIS_MSG_CTX_PROP, this);
	}
	
	/**
	 * Gets the current parent Axis2 message context. 
	 * 
	 * @return	The current Axis2 {@link MessageContext}
	 */
	public MessageContext getParentContext() {
		return axisParentCtx;
	}
	
	/**
	 * Indicates if the current message processing is for a request initiated by Holodeck B2B or is for responding
	 * to a received request.
	 * 
	 * @return 	<code>true</code> if this context is for a request initiated by Holodeck B2B,<br>
	 * 			<code>false</code> if it is for a request handled by Holodeck B2B
	 *  
	 */
	public boolean isHB2BInitiated() {
		return !axisParentCtx.isServerSide();
	}
	
	/**
	 * Clears the message processing context by removing all messages from it.
	 */
	public void removeAllMessages() {
		receivedUserMessage = null;
		sendingUserMessage 	= null;		
		receivedPullRequest = null;
		sendingPullRequest 	= null;		
		receivedReceipts 	= new ArrayList<>();
		sendingReceipts 	= new ArrayList<>();		
		receivedErrors 		= new ArrayList<>();
		sendingErrors 		= new ArrayList<>();
	}
	
	/**
	 * Sets the <i>User Message</i> that is processed in the current flow of this message processing context. Based on
	 * the flow the message unit is set as either received (in flow) or sending (out_flow). 
	 * 
	 * @param userMessage	The received User Message
	 */
	public void setUserMessage(final IUserMessageEntity userMessage) {
		if (currentFlowIsIn()) 	
			this.receivedUserMessage = userMessage;
		else 
			this.sendingUserMessage = userMessage;				
	}
	
	/**
	 * Sets the <i>User Message</i> that should be send as a response [to a Pull Request]. This will automatically set
	 * the response indicator to true. 
	 * 
	 * @param userMessage	The User Message to be send as response
	 */
	public void setResponseUserMessage(final IUserMessageEntity userMessage) {
		this.sendingUserMessage = userMessage;				
		this.needsResponse = true;
	}
	
	/**
	 * @return the received <i>User Message</i>.	
	 */
	public IUserMessageEntity getReceivedUserMessage() {
		return receivedUserMessage;		
	}
	
	/**
	 * @return the <i>User Message</i> that is being send.	
	 */
	public IUserMessageEntity getSendingUserMessage() {
		return sendingUserMessage;		
	}
	
	/**
	 * Sets the <i>Pull Request</i> that is being processed in this message processing context.
	 * 
	 * @param pullRequest	The Pull Request being processed 
	 */
	public void setPullRequest(final IPullRequestEntity pullRequest) {
		if (currentFlowIsIn()) 
			this.receivedPullRequest = pullRequest;		
		else 
			this.sendingPullRequest = pullRequest;
	}
	
	/**
	 * @return the received <i>Pull Request</i>.	
	 */
	public IPullRequestEntity getReceivedPullRequest() {
		return receivedPullRequest;		
	}
	
	/**
	 * @return 	the <i>Pull Request</i> that is being send.
	 */
	public IPullRequestEntity getSendingPullRequest() {
		return sendingPullRequest;		
	}
	
	/**
     * Adds a received <i>Receipt Signal</i> to the message processing context. 
     * 
     * @param receipt	The received Receipt
     */
    public void addReceivedReceipt(final IReceiptEntity receipt) {
        receivedReceipts.add(receipt);
    }

    /**
     * @return the collection of received <i>Receipt</i>s
     */
    public Collection<IReceiptEntity> getReceivedReceipts() {
    	return receivedReceipts;
    }
    
    /**
     * Sets the <i>Receipt Signals</i> to be send.
     * 
     * @param receiptsToSend	The collection of Receipts to send
     */
    public void setSendingReceipts(final Collection<IReceiptEntity> receiptsToSend) {
    	this.sendingReceipts = new ArrayList<IReceiptEntity>(receiptsToSend);
    }
    
    /**
     * Adds a <i>Receipt Signal</i> to be send to the message processing context. 
     * 
     * @param receipt	The Receipt to be send
     */
    public void addSendingReceipt(final IReceiptEntity receipt) {
    	sendingReceipts.add(receipt);
    }
    
    /**
     * @return the collection of <i>Receipt</i>s to be send
     */
    public Collection<IReceiptEntity> getSendingReceipts() {
    	return sendingReceipts;
    }
    
    /**
     * Adds a received <i>Error Signal</i> to the message processing context. 
     * 
     * @param error	The received Error message
     */
    public void addReceivedError(final IErrorMessageEntity error) {
    	receivedErrors.add(error);
    }
    
    /**
     * @return the collection of received <i>Error Signal</i>s
     */
    public Collection<IErrorMessageEntity> getReceivedErrors() {
    	return receivedErrors;
    }

    /**
     * Sets the <i>Error Signals</i> to be send.
     * 
     * @param errorsToSend	The collection of Error Signals to send
     */
    public void setSendingErrors(final Collection<IErrorMessageEntity> errorsToSend) {
    	this.sendingErrors = new ArrayList<IErrorMessageEntity>(errorsToSend);
    }
    
    /**
     * Adds a <i>Error Signal</i> to be send to the message processing context. 
     * 
     * @param error	The Error Signal to be send
     */
    public void addSendingError(final IErrorMessageEntity error) {
    	sendingErrors.add(error);
    }
    
    /**
     * @return the collection of <i>Error Signals</i>s to be send
     */
    public Collection<IErrorMessageEntity> getSendingErrors() {
    	return sendingErrors;
    }

    /**
     * Retrieves all entity objects of message units that are received in the current operation.
     *
     * @return      {@link Collection} of {@link EntityProxy} objects for the message units in the received message.
     */
    public Collection<IMessageUnitEntity> getReceivedMessageUnits() {
        final Collection<IMessageUnitEntity>   messageUnits = new ArrayList<>();

        if (receivedUserMessage != null)
        	messageUnits.add(receivedUserMessage);
        if (receivedPullRequest != null)
            messageUnits.add(receivedPullRequest);
        messageUnits.addAll(receivedReceipts);
        messageUnits.addAll(receivedErrors);
        
        return messageUnits;
    }  

    /**
     * Checks if a received message unit exists with the given messageId and if it does returns it.
     * 
     * @param messageId		The messageId to search for
     * @return				Received message unit with the given messageId if one exists,<br><code>null</code> otherwise 
     */
    public IMessageUnitEntity getReceivedMessageUnit(final String messageId) {
    	if (messageId == null)
    		throw new IllegalArgumentException("MessageId to search for must not be null");
    	
    	Optional<IMessageUnitEntity> refdMsgUnit = getReceivedMessageUnits().parallelStream()
    																.filter(mu -> messageId.equals(mu.getMessageId()))
    																.findFirst();
    	return refdMsgUnit.isPresent() ? refdMsgUnit.get() : null;
    }
    
    /**
     * Retrieves the collection of entity objects for all message units that are (to be) or were previously sent in the 
     * current operation.
     *
     * @return      {@link Collection} of {@link IMessageUnitEntity} objects for the message units that were sent.
     */
    public Collection<IMessageUnitEntity> getSendingMessageUnits() {
        final Collection<IMessageUnitEntity>   messageUnits = new ArrayList<>();
        
        if (sendingUserMessage != null)
        	messageUnits.add(sendingUserMessage);
        if (sendingPullRequest != null)
            messageUnits.add(sendingPullRequest);
        messageUnits.addAll(sendingReceipts);
        messageUnits.addAll(sendingErrors);
        
        return messageUnits;
    }
    
    /**
     * Checks if a outgoing message unit exists with the given messageId and if it does returns it.
     * 
     * @param messageId		The messageId to search for
     * @return				Outgoing message unit with the given messageId if one exists,<br><code>null</code> otherwise 
     */
    public IMessageUnitEntity getSendingMessageUnit(final String messageId) {
    	if (messageId == null)
    		throw new IllegalArgumentException("MessageId to search for must not be null");
    	
    	Optional<IMessageUnitEntity> refdMsgUnit = getSendingMessageUnits().parallelStream()
    																.filter(mu -> messageId.equals(mu.getMessageId()))
    																.findFirst();
    	return refdMsgUnit.isPresent() ? refdMsgUnit.get() : null;
    }
    
    /**
     * Adds an {@link IEbmsError} that has been generated during message processing to the context.
     *
     * @param error     The generated {@link EbmsError} 
     */
    public void addGeneratedError(final IEbmsError error) {
    	String refToMsgId = error.getRefToMessageInError();
    	if (Utils.isNullOrEmpty(refToMsgId))
    		refToMsgId = UNREFD_ERRORS;
    	Collection<IEbmsError> errorsForRefdMsg = generatedErrors.get(refToMsgId);
    	if (errorsForRefdMsg == null) {
    		errorsForRefdMsg = new ArrayList<>();
    		generatedErrors.put(refToMsgId, errorsForRefdMsg);
    	}
    	errorsForRefdMsg.add(error);
    }
    
    /**
     * @return	all generated errors during the processing of the received message mapped on the referenced messageId  
     */
    public Map<String, Collection<IEbmsError>> getGeneratedErrors() {    	
    	return generatedErrors;
    }    
    
    /**
     * Adds a entry to the map that registers the relationship between received Error Signals and the message units they
     * reference.  
     * 
     * @param error			The received Error Signal
     * @param refdMsgUnits	The referenced message units
     */
    public void addRefdMsgUnitByError(final IErrorMessage error, final Collection<IMessageUnitEntity> refdMsgUnits) {
    	refdMessagesByError.put(error, refdMsgUnits);
    }
  
    /**
     * Gets the message units that are referenced by the given Error Signal.
     * 
     * @param error		The received Error Signal
     * @return 			The referenced message units if the given Error Signal is registered in this context,<br>
     * 					<code>null</code> otherwise
     */
    public Collection<IMessageUnitEntity> getRefdMsgUnitByError(final IErrorMessage error) {
    	return refdMessagesByError.get(error);
    }
    
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
    public IMessageUnitEntity getPrimaryMessageUnit() {
        if (currentFlowIsIn()) {
            if (receivedUserMessage != null)
                return receivedUserMessage;
            else if (!Utils.isNullOrEmpty(receivedReceipts))
                return receivedReceipts.get(0);
            else if (!Utils.isNullOrEmpty(receivedErrors))
                return receivedErrors.get(0);
            else 
            	return receivedPullRequest;
        } else {
        	if (sendingPullRequest != null)
        		return sendingPullRequest;
        	else if (sendingUserMessage != null)
    			return sendingUserMessage;
        	else if (!Utils.isNullOrEmpty(sendingReceipts))
                return sendingReceipts.get(0);
            else if (!Utils.isNullOrEmpty(sendingErrors))
                return sendingErrors.get(0);
            else
            	return null;
        }
    }
    
    /**
     * Adds the result of processing a security token in the received message to the context.
     * 
     * @param result	the processing result
     */
    public void addSecurityProcessingResult(final ISecurityProcessingResult result) {
    	securityResults.add(result);
    }
    
    /**
     * Gets the processing results for a specific type of security tokens (identified by the results class). 
     * 
     * @param type	The class of processing results to retrieve, MUST NOT be <code>null</code>
     * @return	All results of processing of the given class 
     */
	@SuppressWarnings("unchecked")
	public <T extends ISecurityProcessingResult> Collection<T> getSecurityProcessingResults(final Class<T> type) {
		if (type == null)
			throw new IllegalArgumentException("A specific type of result must be specified");
		
		return (Collection<T>) securityResults.parallelStream().filter(r -> type.isAssignableFrom(r.getClass()))
															   .collect(Collectors.toList());
	}

	/**
	 * @return all results of processing the security tokens in the received message.
	 */
    public Collection<ISecurityProcessingResult> getSecurityProcessingResults() {
    	return securityResults;
    }
    
    /**
     * Sets the indicator whether a <b>synchronous</b> response should be send or not. 
     * 
     * @param responseNeeded	Indicates whether a response should be send. 
     * @throws IllegalStateException when this method is called from the out flow
     */
    public void setNeedsResponse(final boolean responseNeeded) {
    	if (!currentFlowIsIn() && axisParentCtx.isServerSide())
    		throw new IllegalStateException("Already responding");
    	this.needsResponse = responseNeeded;
    }
    
    /**
     * @return indicator if a <b>synchronous</b> response is requested.	
     */
    public boolean responseNeeded() {
    	return needsResponse;
    }
    
    /**
     * Sets a property in this message processing context.
     *  
     * @param name		the propety's name
     * @param value		the property's value
     */
    public void setProperty(final String name, Object value) {
    	properties.put(name, value);
    }

    /**
     * Gets the value of a property in this message processing context or in the Axis2 message context if no property
     * with the given name exists in this context.
     *  
     * @param name		the propety's name
     * @return 			the property's value if available or <code>null</code> if no property with the given name is
     * 					available in either this or the Axis2 message context.
     */
    public Object getProperty(final String name) {
    	Object v = properties.get(name);
    	if (v == null)
    		v = axisParentCtx.getProperty(name);
    	return v;
    }
    
    /**
     * Checks if the current flow is incoming or outgoing. 
     * 
     * @return	<code>true</code> if the current flow is incoming,<br><code>false</code> if outgoing
     */
    private boolean currentFlowIsIn() {
    	return axisParentCtx.getFLOW() == MessageContext.IN_FLOW 
    			|| axisParentCtx.getFLOW() == MessageContext.IN_FAULT_FLOW;
    }
}

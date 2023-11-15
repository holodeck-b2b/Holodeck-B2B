/**
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.security.ISecurityProcessingResult;

/**
 * Is the implementation of {@link IMessageProcessingContext} that represents the Holodeck B2B <i>message processing 
 * context</i> during the execution of the message processing pipe line.
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  4.1.0
 * @since  5.0.0 Now implements interface extracted from earlier version
 * @see AbstractBaseHandler
 */
public class MessageProcessingContext implements IMessageProcessingContext {
	/**
	 * The name of the Axis2 message context property used to store this Holodeck B2B message processing context
	 */
	private static final String	AXIS_MSG_CTX_PROP = "hb2b-msgprocctx";
        
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
			
	private boolean	needsResponse = false;
	
	private boolean execDupElimination = false;
	
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
	 * Gets the message processing associated with the given Axis2 message context. If the context does not contain a
	 * processing context, a new one will be created and linked to it. 
	 * 
	 * @param mc	The Axis2 message context
	 * @return		The Holodeck B2B processing context associated with it
	 */
	public static MessageProcessingContext getFromMessageContext(final MessageContext mc) {
		MessageProcessingContext procCtx = (MessageProcessingContext) mc.getProperty(AXIS_MSG_CTX_PROP);
		if (procCtx == null) 
			procCtx = new MessageProcessingContext(mc);
		
		return procCtx;
	}

	/**
	 * Creates a new instance based on the given Axis2 message context.
	 * 
	 * @param axisMsgCtx	The Axis2 message context
	 */
	private MessageProcessingContext(final MessageContext axisMsgCtx) {
		this.setParentContext(axisMsgCtx);		
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#setParentContext(org.apache.axis2.context.MessageContext)
	 */
	public void setParentContext(final MessageContext axisMsgCtx) {
		this.axisParentCtx = axisMsgCtx;
		axisMsgCtx.setProperty(AXIS_MSG_CTX_PROP, this);
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getParentContext()
	 */
	@Override
	public MessageContext getParentContext() {
		return axisParentCtx;
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#isHB2BInitiated()
	 */
	@Override
	public boolean isHB2BInitiated() {
		return !axisParentCtx.isServerSide();
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#removeAllMessages()
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#setUserMessage(org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity)
	 */
	@Override
	public void setUserMessage(final IUserMessageEntity userMessage) {
		if (currentFlowIsIn()) 	
			this.receivedUserMessage = userMessage;
		else 
			this.sendingUserMessage = userMessage;				
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#setResponseUserMessage(org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity)
	 */
	@Override
	public void setResponseUserMessage(final IUserMessageEntity userMessage) {
		this.sendingUserMessage = userMessage;				
		this.needsResponse = true;
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getReceivedUserMessage()
	 */
	@Override
	public IUserMessageEntity getReceivedUserMessage() {
		return receivedUserMessage;		
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getSendingUserMessage()
	 */
	@Override
	public IUserMessageEntity getSendingUserMessage() {
		return sendingUserMessage;		
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#setPullRequest(org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity)
	 */
	@Override
	public void setPullRequest(final IPullRequestEntity pullRequest) {
		if (currentFlowIsIn()) 
			this.receivedPullRequest = pullRequest;		
		else 
			this.sendingPullRequest = pullRequest;
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getReceivedPullRequest()
	 */
	@Override
	public IPullRequestEntity getReceivedPullRequest() {
		return receivedPullRequest;		
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getSendingPullRequest()
	 */
	@Override
	public IPullRequestEntity getSendingPullRequest() {
		return sendingPullRequest;		
	}
	
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#addReceivedReceipt(org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity)
	 */
    @Override
	public void addReceivedReceipt(final IReceiptEntity receipt) {
        receivedReceipts.add(receipt);
    }

    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getReceivedReceipts()
	 */
    @Override
	public Collection<IReceiptEntity> getReceivedReceipts() {
    	return receivedReceipts;
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#setSendingReceipts(java.util.Collection)
	 */
    @Override
	public void setSendingReceipts(final Collection<IReceiptEntity> receiptsToSend) {
    	this.sendingReceipts = new ArrayList<IReceiptEntity>(receiptsToSend);
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#addSendingReceipt(org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity)
	 */
    @Override
	public void addSendingReceipt(final IReceiptEntity receipt) {
    	sendingReceipts.add(receipt);
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getSendingReceipts()
	 */
    @Override
	public Collection<IReceiptEntity> getSendingReceipts() {
    	return sendingReceipts;
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#addReceivedError(org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity)
	 */
    @Override
	public void addReceivedError(final IErrorMessageEntity error) {
    	receivedErrors.add(error);
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getReceivedErrors()
	 */
    @Override
	public Collection<IErrorMessageEntity> getReceivedErrors() {
    	return receivedErrors;
    }

    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#setSendingErrors(java.util.Collection)
	 */
    @Override
	public void setSendingErrors(final Collection<IErrorMessageEntity> errorsToSend) {
    	this.sendingErrors = new ArrayList<IErrorMessageEntity>(errorsToSend);
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#addSendingError(org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity)
	 */
    @Override
	public void addSendingError(final IErrorMessageEntity error) {
    	sendingErrors.add(error);
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getSendingErrors()
	 */
    @Override
	public Collection<IErrorMessageEntity> getSendingErrors() {
    	return sendingErrors;
    }

    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getReceivedMessageUnits()
	 */
    @Override
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

    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getReceivedMessageUnit(java.lang.String)
	 */
    @Override
	public IMessageUnitEntity getReceivedMessageUnit(final String messageId) {
    	if (messageId == null)
    		throw new IllegalArgumentException("MessageId to search for must not be null");
    	
    	Optional<IMessageUnitEntity> refdMsgUnit = getReceivedMessageUnits().parallelStream()
    																.filter(mu -> messageId.equals(mu.getMessageId()))
    																.findFirst();
    	return refdMsgUnit.isPresent() ? refdMsgUnit.get() : null;
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getSendingMessageUnits()
	 */
    @Override
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
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getSendingMessageUnit(java.lang.String)
	 */
    @Override
	public IMessageUnitEntity getSendingMessageUnit(final String messageId) {
    	if (messageId == null)
    		throw new IllegalArgumentException("MessageId to search for must not be null");
    	
    	Optional<IMessageUnitEntity> refdMsgUnit = getSendingMessageUnits().parallelStream()
    																.filter(mu -> messageId.equals(mu.getMessageId()))
    																.findFirst();
    	return refdMsgUnit.isPresent() ? refdMsgUnit.get() : null;
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#addGeneratedError(org.holodeckb2b.interfaces.messagemodel.IEbmsError)
	 */
    @Override
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
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getGeneratedErrors()
	 */
    @Override
	public Map<String, Collection<IEbmsError>> getGeneratedErrors() {    	
    	return generatedErrors;
    }    
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getPrimaryMessageUnit()
	 */
    @Override
	public IMessageUnitEntity getPrimaryMessageUnit() {
    	if (currentFlowIsIn()) 
    		return getPrimaryReceivedMessageUnit(); 
    	else 
    		return getPrimarySentMessageUnit();    	
    }
    
	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getPrimaryMessageUnit()
	 * @since 6.0.0
	 */
	@Override
	public IMessageUnitEntity getPrimaryReceivedMessageUnit() {
        if (receivedUserMessage != null)
            return receivedUserMessage;
        else if (!Utils.isNullOrEmpty(receivedReceipts))
            return receivedReceipts.get(0);
        else if (!Utils.isNullOrEmpty(receivedErrors))
            return receivedErrors.get(0);
        else 
        	return receivedPullRequest;
    }
    
    /* (non-Javadoc)
  	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getPrimarySentMessageUnit()
  	 * @since 6.0.0
  	 */
    @Override
  	public IMessageUnitEntity getPrimarySentMessageUnit() {
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
 
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#addSecurityProcessingResult(org.holodeckb2b.interfaces.security.ISecurityProcessingResult)
	 */
    @Override
	public void addSecurityProcessingResult(final ISecurityProcessingResult result) {
    	securityResults.add(result);
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getSecurityProcessingResults(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends ISecurityProcessingResult> Collection<T> getSecurityProcessingResults(final Class<T> type) {
		if (type == null)
			throw new IllegalArgumentException("A specific type of result must be specified");
		
		return (Collection<T>) securityResults.parallelStream().filter(r -> type.isAssignableFrom(r.getClass()))
															   .collect(Collectors.toList());
	}

	/* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getSecurityProcessingResults()
	 */
    @Override
	public Collection<ISecurityProcessingResult> getSecurityProcessingResults() {
    	return securityResults;
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#setNeedsResponse(boolean)
	 */
    @Override
	public void setNeedsResponse(final boolean responseNeeded) {
    	if (!currentFlowIsIn() && axisParentCtx.isServerSide())
    		throw new IllegalStateException("Already responding");
    	this.needsResponse = responseNeeded;
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#responseNeeded()
	 */
    @Override
	public boolean responseNeeded() {
    	return needsResponse;
    }
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#setDuplicateElimination(boolean)
	 */
	@Override
	public void setDuplicateElimination(boolean useDupElimination) {
		execDupElimination = useDupElimination;		
	}
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#eliminateDuplicates()
	 */
	@Override
	public boolean eliminateDuplicates() {
		return execDupElimination;
	}    
    
    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#setProperty(java.lang.String, java.lang.Object)
	 */
    @Override
	public void setProperty(final String name, Object value) {
    	properties.put(name, value);
    }

    /* (non-Javadoc)
	 * @see org.holodeckb2b.core.handlers.IMessageProcessingContext#getProperty(java.lang.String)
	 */
    @Override
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

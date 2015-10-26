/*
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

package org.holodeckb2b.axis2;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.wsdl.WSDLConstants;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.ebms3.persistent.message.EbmsError;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.persistent.message.SignalMessage;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;

/**
 * Contains some utility methods related to the {@link MessageContext}.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageContextUtils {
    
    /**
     * Adds a {@link Receipt} to the list of received receipt signals in the given {@link MessageContext}.
     * 
     * @param mc        The {@link MessageContext} to which the receipt should be added
     * @param receipt   The {@link Receipt} to add
     */
    public static void addRcvdReceipt(MessageContext mc, Receipt receipt) {
        ArrayList<Receipt> rcptList = null;
        
        try {
            rcptList = (ArrayList<Receipt>) mc.getProperty(MessageContextProperties.IN_RECEIPTS);
        } catch (Exception e) {}
        
        // If the message context does not contain a list of receipts already or if
        // the returned object is not a list, create new list
        if (rcptList == null) {
            rcptList = new ArrayList<Receipt>();
            mc.setProperty(MessageContextProperties.IN_RECEIPTS, rcptList);
        }
        
        // Add the receipt to the list
        rcptList.add(receipt);
    }

    /**
     * Adds a {@link ErrorMessage} to the list of received error signals in the given {@link MessageContext}.
     * 
     * @param mc        The {@link MessageContext} to which the receipt should be added
     * @param receipt   The {@link ErrorMessage} to add
     */
    public static void addRcvdError(MessageContext mc, ErrorMessage error) {
        ArrayList<ErrorMessage> errList = null;
        
        try {
            errList = (ArrayList<ErrorMessage>) mc.getProperty(MessageContextProperties.IN_RECEIPTS);
        } catch (Exception e) {}
        
        // If the message context does not contain a list of errors already or if
        // the returned object is not a list, create new list
        if (errList == null) {
            errList = new ArrayList<ErrorMessage>();
            mc.setProperty(MessageContextProperties.IN_ERRORS, errList);
        }
        
        // Add the error to the list
        errList.add(error);
    }
    
    /**
     * Adds a {@link Receipt} to the list of receipt signals that should be sent with the current message.
     * 
     * @param mc        The {@link MessageContext} representing the message to which the receipt should be added
     * @param receipt   The {@link Receipt} to add
     */
    public static void addReceiptToSend(MessageContext mc, Receipt receipt) {
        ArrayList<Receipt> rcptList = null;
        
        try {
            rcptList = (ArrayList<Receipt>) mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
        } catch (Exception e) {}
        
        // If the message context does not contain a list of errors already or if
        // the returned object is not a list, create new list
        if (rcptList == null) {
            rcptList = new ArrayList<Receipt>();
            mc.setProperty(MessageContextProperties.OUT_RECEIPTS, rcptList);
        }
        
        // Add the error to the list
        rcptList.add(receipt);
    }
    
    /**
     * Adds an error that has been generated during message processing to the
     * {@link MessageContext}.
     * 
     * @param mc        The {@link MessageContext} to which the error should be added
     * @param error     The {@link EbmsError} to add
     */
    public static void addGeneratedError(MessageContext mc, EbmsError error) {
        ArrayList<EbmsError> errList = null;
        
        try {
            errList = (ArrayList<EbmsError>) mc.getProperty(MessageContextProperties.GENERATED_ERRORS);
        } catch (Exception e) {}
        
        // If the message context does not contain a list of errors already or if
        // the returned object is not a list, create new list
        if (errList == null) {
            errList = new ArrayList<EbmsError>();
            mc.setProperty(MessageContextProperties.GENERATED_ERRORS, errList);
        }
        
        // Add the error to the list
        errList.add(error);
    }

    /**
     * Adds an error signal that has to be sent as a response to the current 
     * message to the {@link MessageContext} so it can be retrieved in the out 
     * flow.
     * 
     * @param mc        The {@link MessageContext} to which the error should be added
     * @param errorMU    The {@link ErrorMessage} to add
     * @deprecated Use {@link #addErrorSignalToSend(org.apache.axis2.context.MessageContext, org.holodeckb2b.ebms3.persistent.message.ErrorMessage)} and
     * set {@link MessageContextProperties#RESPONSE_REQUIRED} separately
     */
    public static void addErrorSignalToRespond(MessageContext mc, ErrorMessage errorMU) {
        addErrorSignalToSend(mc, errorMU);
        
        // And require that a response is sent
        mc.setProperty(MessageContextProperties.RESPONSE_REQUIRED, true);
    }

    /**
     * Adds an error signal message unit to the current message. 
     * 
     * @param mc        The {@link MessageContext} to which the error should be added
     * @param errorMU   The {@link ErrorMessage} to add
     */
    public static void addErrorSignalToSend(MessageContext mc, ErrorMessage errorMU) {
        ArrayList<ErrorMessage> errList = null;
        
        try {
            errList = (ArrayList<ErrorMessage>) mc.getProperty(MessageContextProperties.OUT_ERROR_SIGNALS);
        } catch (Exception e) {}
        
        // If the message context does not contain a list of errors already or if
        // the returned object is not a list, create new list
        if (errList == null) {
            errList = new ArrayList<ErrorMessage>();
            mc.setProperty(MessageContextProperties.OUT_ERROR_SIGNALS, errList);
        }
        
        // Add the error to the list
        errList.add(errorMU);
    }
   
    /**
     * Gets a property from the in flow message context to which the given message context is a response.
     * 
     * @param outMsgCtx     The response {@link MessageContext}
     * @param key           The name of the property to get the value for
     * @return              The value of the requested property if it exists
     *                      in the in flow message context, <code>null</code>
     *                      otherwise.
     */
    public static Object getPropertyFromInMsgCtx(MessageContext outMsgCtx, String key) {
        if (outMsgCtx == null || key == null) 
            return null;
        
        try {
            OperationContext opContext = outMsgCtx.getOperationContext();
            MessageContext inMsgContext = opContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            
            return inMsgContext.getProperty(key);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Gets a property from the out flow message context of the current message exchange.
     * 
     * @param msgCtx     The current {@link MessageContext}
     * @param key        The name of the property to get the value for
     * @return           The value of the requested property if it exists in the out flow message context,or <br>
     *                   <code>null</code> otherwise.
     */
    public static Object getPropertyFromOutMsgCtx(MessageContext msgCtx, String key) {
        if (msgCtx == null || key == null) 
            return null;
        
        try {
            OperationContext opContext = msgCtx.getOperationContext();
            MessageContext outMsgContext = opContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            
            return outMsgContext.getProperty(key);
        } catch (Exception ex) {
            return null;
        }
    }   
    
    /**
     * Retrieves all message units that were previously (or are to be) sent. 
     * 
     * @param mc    The current message context
     * @return      {@link Collection} of {@link MessageUnit} objects for the message units that were sent. 
     */
    public static Collection<MessageUnit> getSentMessageUnits(MessageContext mc) {
        Collection<MessageUnit>   reqMUs = new ArrayList<MessageUnit>();
        
        UserMessage userMsg = (UserMessage)
                getPropertyFromOutMsgCtx(mc, MessageContextProperties.OUT_USER_MESSAGE);
        if (userMsg != null) 
            reqMUs.add(userMsg);
        PullRequest pullReq = (PullRequest) 
                getPropertyFromOutMsgCtx(mc, MessageContextProperties.OUT_PULL_REQUEST);
        if (pullReq != null)
            reqMUs.add(pullReq);
        Collection<Receipt> receipts = (ArrayList<Receipt>)
                getPropertyFromOutMsgCtx(mc, MessageContextProperties.OUT_RECEIPTS);
        if (receipts != null && !receipts.isEmpty())
            reqMUs.addAll(receipts);
        Collection<ErrorMessage> errors = (ArrayList<ErrorMessage>) 
                getPropertyFromOutMsgCtx(mc, MessageContextProperties.OUT_ERROR_SIGNALS);
        if (errors != null && !errors.isEmpty())
            reqMUs.addAll(errors);
        return reqMUs;
    }    

    /**
     * Retrieves all message units in the received message. 
     * 
     * @param mc    The in flow message context
     * @return      {@link Collection} of {@link MessageUnit} objects for the message units in the received message. 
     */
    public static Collection<MessageUnit> getRcvdMessageUnits(MessageContext mc) {
        Collection<MessageUnit>   reqMUs = new ArrayList<MessageUnit>();
        
        UserMessage userMsg = (UserMessage) mc.getProperty(MessageContextProperties.IN_USER_MESSAGE);
        if (userMsg != null) 
            reqMUs.add(userMsg);
        PullRequest pullReq = (PullRequest) mc.getProperty(MessageContextProperties.IN_PULL_REQUEST);
        if (pullReq != null)
            reqMUs.add(pullReq);
        Collection<Receipt> receipts = (ArrayList<Receipt>) mc.getProperty(MessageContextProperties.IN_RECEIPTS);
        if (receipts != null && !receipts.isEmpty())
            reqMUs.addAll(receipts);
        Collection<ErrorMessage> errors = (ArrayList<ErrorMessage>) mc.getProperty(MessageContextProperties.IN_ERRORS);
        if (errors != null && !errors.isEmpty())
            reqMUs.addAll(errors);
        return reqMUs;
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
     * @param mc    The {@link MessageContext} of the message
     * @return      The primary message unit if one was found or <code>null</code> if no message unit could be found 
     *              in the message context
     */
    public static MessageUnit getPrimaryMessageUnit(final MessageContext mc) {
        if (mc.getFLOW() == MessageContext.IN_FLOW || mc.getFLOW() == MessageContext.IN_FAULT_FLOW)
            return getPrimaryMessageUnitFromInFlow(mc);
        else 
            return getPrimaryMessageUnitFromOutFlow(mc);
    }
    
    /**
     * Gets the primary message unit from a received message.
     * 
     * @param mc    The {@link MessageContext} of the message
     * @return      The primary message unit if one was found or <code>null</code> if no message unit could be found 
     *              in the message context
     * @see         #getPrimaryMessageUnit(org.apache.axis2.context.MessageContext)
     */
    protected static MessageUnit getPrimaryMessageUnitFromInFlow(final MessageContext mc) {
        //
        // Class cast exceptions are ignored, the requested message unit type is considered to not be available
        MessageUnit pMU = null;
        try {
            pMU = (MessageUnit) mc.getProperty(MessageContextProperties.IN_USER_MESSAGE);
        } catch (ClassCastException cce) {}
        
        if (pMU != null)
            // Message contains UserMessage, so this is the primary message unit
            return pMU;
        
        // No user message, check for Receipt
        try {
            Collection<SignalMessage> rcpts = (Collection<SignalMessage>) 
                                                        mc.getProperty(MessageContextProperties.IN_RECEIPTS);
            pMU = rcpts.iterator().next();
        } catch (Exception ex) {}
        
        if (pMU != null)
            // Message does contain receipt, so this becomes the primary message unit
            return pMU;
        
        // No receipts either, maybe errors?
        try {
            Collection<SignalMessage> errs = (Collection<SignalMessage>) 
                                                        mc.getProperty(MessageContextProperties.IN_ERRORS);
            pMU = errs.iterator().next();
        } catch (Exception ex) {}
        
        if (pMU != null)
            // Message does contain error, so this becomes the primary message unit
            return pMU;
        
        // No errors, maybe a PullRequest
        try {
            pMU = (MessageUnit) mc.getProperty(MessageContextProperties.IN_PULL_REQUEST);
        } catch (ClassCastException cce) {}
        
        if (pMU != null)
            // Message does contain error, so this becomes the primary message unit
            return pMU;
        else // no message unit in this context
            return null;
    }
    
    /**
     * Gets the primary message unit from a message to be sent.
     * 
     * @param mc    The {@link MessageContext} of the message
     * @return      The primary message unit if one was found or <code>null</code> if no message unit could be found 
     *              in the message context
     * @see         #getPrimaryMessageUnit(org.apache.axis2.context.MessageContext)
     */
    protected static MessageUnit getPrimaryMessageUnitFromOutFlow(final MessageContext mc) {
        //
        // Class cast exceptions are ignored, the requested message unit type is considered to not be available
        MessageUnit pMU = null;
        try {
            pMU = (MessageUnit) mc.getProperty(MessageContextProperties.OUT_PULL_REQUEST);
        } catch (ClassCastException cce) {}
        if (pMU != null)
            // Message contains PullRequest, so this is the primary message unit
            return pMU;
        
        // No PullRequest, check for User message
        try {
            pMU = (MessageUnit) mc.getProperty(MessageContextProperties.OUT_USER_MESSAGE);
        } catch (ClassCastException cce) {}
        if (pMU != null)
            // Message does contains User Message, so this becomes the primary message unit
            return pMU;

        // No pull request message, check for Receipt
        try {
            Collection<SignalMessage> rcpts = (Collection<SignalMessage>) 
                                                        mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
            pMU = rcpts.iterator().next();
        } catch (Exception ex) {}
        if (pMU != null)
            // Message does contain receipt, so this becomes the primary message unit
            return pMU;
        
        // No receipts either, maybe errors?
        try {
            Collection<SignalMessage> errs = (Collection<SignalMessage>) 
                                                        mc.getProperty(MessageContextProperties.OUT_ERROR_SIGNALS);
            pMU = errs.iterator().next();
        } catch (Exception ex) {}        
        if (pMU != null)
            // Message does contain error, so this becomes the primary message unit
            return pMU;
        else // no message unit in this context
            return null;
    }    
}

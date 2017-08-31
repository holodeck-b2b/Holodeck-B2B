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
package org.holodeckb2b.ebms3.axis2;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.wsdl.WSDLConstants;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;


/**
 * Contains some utility methods related to handling the message units available in the Axis2 {@link MessageContext}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class MessageContextUtils {

    /**
     * Adds a {@link IReceiptEntity} to the list of received receipt signals in the given {@link MessageContext}.
     *
     * @param mc        The {@link MessageContext} to which the receipt should be added
     * @param receipt   The {@link IReceiptEntity} to add
     */
    public static void addRcvdReceipt(final MessageContext mc, final IReceiptEntity receipt) {
        ArrayList<IReceiptEntity> rcptList = null;

        try {
            rcptList = (ArrayList<IReceiptEntity>) mc.getProperty(MessageContextProperties.IN_RECEIPTS);
        } catch (final Exception e) {}

        // If the message context does not contain a list of receipts already or if
        // the returned object is not a list, create new list
        if (rcptList == null) {
            rcptList = new ArrayList<>();
            mc.setProperty(MessageContextProperties.IN_RECEIPTS, rcptList);
        }

        // Add the receipt to the list
        rcptList.add(receipt);
    }

    /**
     * Adds a {@link IErrorMessageEntity} to the list of received error signals in the given {@link MessageContext}.
     *
     * @param mc        The {@link MessageContext} to which the receipt should be added
     * @param error     The {@link IErrorMessageEntity} to add
     */
    public static void addRcvdError(final MessageContext mc, final IErrorMessageEntity error) {
        ArrayList<IErrorMessageEntity> errList = null;

        try {
            errList = (ArrayList<IErrorMessageEntity>) mc.getProperty(MessageContextProperties.IN_ERRORS);
        } catch (final Exception e) {}

        // If the message context does not contain a list of errors already or if
        // the returned object is not a list, create new list
        if (errList == null) {
            errList = new ArrayList<>();
            mc.setProperty(MessageContextProperties.IN_ERRORS, errList);
        }

        // Add the error to the list
        errList.add(error);
    }

    /**
     * Adds a {@link IReceiptEntity} to the list of receipt signals that should be sent with the current message.
     *
     * @param mc        The {@link MessageContext} representing the message to which the receipt should be added
     * @param receipt   The {@link Receipt} to add
     */
    public static void addReceiptToSend(final MessageContext mc, final IReceiptEntity receipt) {
        ArrayList<IReceiptEntity> rcptList = null;

        try {
            rcptList = (ArrayList<IReceiptEntity>) mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
        } catch (final Exception e) {}

        // If the message context does not contain a list of errors already or if
        // the returned object is not a list, create new list
        if (rcptList == null) {
            rcptList = new ArrayList<>();
            mc.setProperty(MessageContextProperties.OUT_RECEIPTS, rcptList);
        }

        // Add the error to the list
        rcptList.add(receipt);
    }

    /**
     * Adds an {@link IEbmsError} that has been generated during message processing to the {@link MessageContext}.
     *
     * @param mc        The {@link MessageContext} to which the error should be added
     * @param error     The {@link EbmsError} to add
     */
    public static void addGeneratedError(final MessageContext mc, final IEbmsError error) {
        ArrayList<IEbmsError> errList = null;

        try {
            errList = (ArrayList<IEbmsError>) mc.getProperty(MessageContextProperties.GENERATED_ERRORS);
        } catch (final Exception e) {}

        // If the message context does not contain a list of errors already or if
        // the returned object is not a list, create new list
        if (errList == null) {
            errList = new ArrayList<>();
            mc.setProperty(MessageContextProperties.GENERATED_ERRORS, errList);
        }

        // Add the error to the list
        errList.add(error);
    }

    /**
     * Adds an {@link IErrorMessageEntity} to the list of error signals that have to be sent as part of the current
     * message.
     *
     * @param mc        The {@link MessageContext} to which the error should be added
     * @param error     The {@link IErrorMessageEntity} to add
     */
    public static void addErrorSignalToSend(final MessageContext mc, final IErrorMessageEntity error) {
        ArrayList<IErrorMessageEntity> errList = null;

        try {
            errList = (ArrayList<IErrorMessageEntity>) mc.getProperty(MessageContextProperties.OUT_ERRORS);
        } catch (final Exception e) {}

        // If the message context does not contain a list of errors already or if
        // the returned object is not a list, create new list
        if (errList == null) {
            errList = new ArrayList<>();
            mc.setProperty(MessageContextProperties.OUT_ERRORS, errList);
        }

        // Add the error to the list
        errList.add(error);
    }

    /**
     * Gets a property from the in flow message context of the operation the given message context is part of. Note that
     * this can retrieve the property from the given if it already is the message context of the in flow.
     *
     * @param currentMsgCtx     The current {@link MessageContext}
     * @param key               The name of the property to get the value for
     * @return              The value of the requested property if it exists in the in flow message context, or<br>
     *                      <code>null</code> otherwise.
     */
    public static Object getPropertyFromInMsgCtx(final MessageContext currentMsgCtx, final String key) {
        return getPropertyFromMsgCtx(currentMsgCtx, key, MessageContext.IN_FLOW);
    }

    /**
     * Gets a property from the out flow message context of the operation the given message context is part of. Note
     * that this can retrieve the property from the given if it already is the message context of the out flow.
     *
     * @param currentMsgCtx     The current {@link MessageContext}
     * @param key               The name of the property to get the value for
     * @return           The value of the requested property if it exists in the out flow message context,or <br>
     *                   <code>null</code> otherwise.
     */
    public static Object getPropertyFromOutMsgCtx(final MessageContext currentMsgCtx, final String key) {
        return getPropertyFromMsgCtx(currentMsgCtx, key, MessageContext.OUT_FLOW);
    }

    /**
     * Helper method to retrieve a property from a specific message context of the operation the given message context
     * is part of.
     *
     * @param currentMsgCtx     The current {@link MessageContext}
     * @param key               The name of the property to get the value for
     * @param flow              The flow from which the property should be retrieved as integer represented using the
     *                          {@link MessageContext#IN_FLOW} and {@link MessageContext#OUT_FLOW} constants
     * @return          The value of the requested property if it exists in the out flow message context,or <br>
     *                   <code>null</code> otherwise.
     */
    private static Object getPropertyFromMsgCtx(final MessageContext currentMsgCtx, final String key,
                                                final int flow) {
        if (currentMsgCtx == null || key == null)
            return null;

        try {
            final OperationContext opContext = currentMsgCtx.getOperationContext();
            final MessageContext targetMsgContext = currentMsgCtx.getFLOW() == flow ? currentMsgCtx :
                                                    opContext.getMessageContext(flow == MessageContext.IN_FLOW ?
                                                                                WSDLConstants.MESSAGE_LABEL_IN_VALUE :
                                                                                WSDLConstants.MESSAGE_LABEL_OUT_VALUE);

            return targetMsgContext.getProperty(key);
        } catch (final Exception ex) {
            return null;
        }
    }

    /**
     * Retrieves all entity objects of message units that are (to be) or were previously sent in the current operation.
     *
     * @param mc    The current message context
     * @return      {@link Collection} of {@link IMessageUnitEntity} objects for the message units that were sent.
     */
    public static Collection<IMessageUnitEntity> getSentMessageUnits(final MessageContext mc) {
        final Collection<IMessageUnitEntity>   messageUnits = new ArrayList<>();

        final IMessageUnitEntity userMsg = (IMessageUnitEntity)
                                                getPropertyFromOutMsgCtx(mc, MessageContextProperties.OUT_USER_MESSAGE);
        if (userMsg != null)
            messageUnits.add(userMsg);
        final IMessageUnitEntity pullReq = (IMessageUnitEntity)
                                                getPropertyFromOutMsgCtx(mc, MessageContextProperties.OUT_PULL_REQUEST);
        if (pullReq != null)
            messageUnits.add(pullReq);
        final Collection<IMessageUnitEntity> receipts = (Collection<IMessageUnitEntity>)
                                                getPropertyFromOutMsgCtx(mc, MessageContextProperties.OUT_RECEIPTS);
        if (receipts != null && !receipts.isEmpty())
            messageUnits.addAll(receipts);
        final Collection<IMessageUnitEntity> errors = (Collection<IMessageUnitEntity>)
                                               getPropertyFromOutMsgCtx(mc, MessageContextProperties.OUT_ERRORS);
        if (errors != null && !errors.isEmpty())
            messageUnits.addAll(errors);
        return messageUnits;
    }

    /**
     * Retrieves all entity objects of message units that are received in the current operation.
     *
     * @param mc    The current message context
     * @return      {@link Collection} of {@link EntityProxy} objects for the message units in the received message.
     */
    public static Collection<IMessageUnitEntity> getReceivedMessageUnits(final MessageContext mc) {
        final Collection<IMessageUnitEntity>   messageUnits = new ArrayList<>();

        final IMessageUnitEntity userMsg = (IMessageUnitEntity)
                                                getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_USER_MESSAGE);
        if (userMsg != null)
            messageUnits.add(userMsg);
        final IMessageUnitEntity pullReq = (IMessageUnitEntity)
                                                getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_PULL_REQUEST);
        if (pullReq != null)
            messageUnits.add(pullReq);
        final Collection<IMessageUnitEntity> receipts = (Collection<IMessageUnitEntity>)
                                                getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_RECEIPTS);
        if (receipts != null && !receipts.isEmpty())
            messageUnits.addAll(receipts);
        final Collection<IMessageUnitEntity> errors = (Collection<IMessageUnitEntity>)
                                               getPropertyFromInMsgCtx(mc, MessageContextProperties.IN_ERRORS);
        if (errors != null && !errors.isEmpty())
            messageUnits.addAll(errors);
        return messageUnits;
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
     * @return      The entity object of the primary message unit if one was found, or
     *              <code>null</code> if no message unit could be found in the message context
     */
    public static IMessageUnitEntity getPrimaryMessageUnit(final MessageContext mc) {
        if (mc.getFLOW() == MessageContext.IN_FLOW || mc.getFLOW() == MessageContext.IN_FAULT_FLOW)
            return getPrimaryMessageUnitFromInFlow(mc);
        else
            return getPrimaryMessageUnitFromOutFlow(mc);
    }

    /**
     * Gets the primary message unit from a received message.
     *
     * @param mc    The {@link MessageContext} of the message
     * @return      The entity object of the primary message unit if one was found, or<br>
     *              <code>null</code> if no message unit could be found in the message context
     * @see         #getPrimaryMessageUnit(org.apache.axis2.context.MessageContext)
     */
    private static IMessageUnitEntity getPrimaryMessageUnitFromInFlow(final MessageContext mc) {
        //
        // Class cast exceptions are ignored, the requested message unit type is considered to not be available
        IMessageUnitEntity primaryMsgUnit = null;
        try {
            primaryMsgUnit = (IUserMessageEntity) mc.getProperty(MessageContextProperties.IN_USER_MESSAGE);
        } catch (final ClassCastException cce) {}

        if (primaryMsgUnit != null)
            // Message contains User Message, so this is the primary message unit
            return primaryMsgUnit;

        // No user message, check for Receipt
        try {
            final Collection<IUserMessageEntity> rcpts = (Collection<IUserMessageEntity>)
                                                        mc.getProperty(MessageContextProperties.IN_RECEIPTS);
            primaryMsgUnit = rcpts.iterator().next();
        } catch (final Exception ex) {}

        if (primaryMsgUnit != null)
            // Message does contain Receipt, so this becomes the primary message unit
            return primaryMsgUnit;

        // No receipts either, maybe errors?
        try {
            final Collection<IUserMessageEntity> errors = (Collection<IUserMessageEntity>)
                                                        mc.getProperty(MessageContextProperties.IN_ERRORS);
            primaryMsgUnit = errors.iterator().next();
        } catch (final Exception ex) {}

        if (primaryMsgUnit != null)
            // Message does contain error, so this becomes the primary message unit
            return primaryMsgUnit;

        // No errors, maybe a PullRequest?
        primaryMsgUnit = (IMessageUnitEntity) mc.getProperty(MessageContextProperties.IN_PULL_REQUEST);

        return primaryMsgUnit;
    }

    /**
     * Gets the primary message unit from a message to be sent.
     *
     * @param mc    The {@link MessageContext} of the message
     * @return      The entity object of the primary message unit if one was found, or<br>
     *              <code>null</code> if no message unit could be found in the message context
     * @see         #getPrimaryMessageUnit(org.apache.axis2.context.MessageContext)
     */
    protected static IMessageUnitEntity getPrimaryMessageUnitFromOutFlow(final MessageContext mc) {
        //
        // Class cast exceptions are ignored, the requested message unit type is considered to not be available
        IMessageUnitEntity primaryMsgUnit = null;
        try {
            primaryMsgUnit = (IMessageUnitEntity) mc.getProperty(MessageContextProperties.OUT_PULL_REQUEST);
        } catch (final ClassCastException cce) {}
        if (primaryMsgUnit != null)
            // Message contains PullRequest, so this is the primary message unit
            return primaryMsgUnit;

        // No PullRequest, check for User message
        try {
            primaryMsgUnit = (IMessageUnitEntity) mc.getProperty(MessageContextProperties.OUT_USER_MESSAGE);
        } catch (final ClassCastException cce) {}
        if (primaryMsgUnit != null)
            // Message does contains User Message, so this becomes the primary message unit
            return primaryMsgUnit;

        // No pull request message, check for Receipt
        try {
            final Collection<IMessageUnitEntity> rcpts = (Collection<IMessageUnitEntity>)
                                                        mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
            primaryMsgUnit = rcpts.iterator().next();
        } catch (final Exception ex) {}
        if (primaryMsgUnit != null)
            // Message does contain receipt, so this becomes the primary message unit
            return primaryMsgUnit;

        // No receipts either, maybe errors?
        try {
            final Collection<IMessageUnitEntity> errs = (Collection<IMessageUnitEntity>)
                                                        mc.getProperty(MessageContextProperties.OUT_ERRORS);
            primaryMsgUnit = errs.iterator().next();
        } catch (final Exception ex) {}
        if (primaryMsgUnit != null)
            // Message does contain error, so this becomes the primary message unit
            return primaryMsgUnit;
        else // no message unit in this context
            return null;
    }

    /**
     * Gets all User Message message units from the message.
     *
     * @param mc    The current message context
     * @return      All User Message message units in the message
     * @since HB2B_NEXT_VERSION
     */
    public static Collection<IUserMessage> getUserMessagesFromMessage(final MessageContext mc) {
        Collection<IUserMessage> userMessages = new ArrayList<>();
        Collection<IMessageUnitEntity> allMsgUnits;
        if (mc.getFLOW() == MessageContext.IN_FLOW || mc.getFLOW() == MessageContext.IN_FAULT_FLOW)
            allMsgUnits = getReceivedMessageUnits(mc);
        else
            allMsgUnits = getSentMessageUnits(mc);
        allMsgUnits.stream().filter(mu -> (mu instanceof IUserMessage))
                            .forEachOrdered(mu -> userMessages.add((IUserMessage) mu));
        return userMessages;
    }
}

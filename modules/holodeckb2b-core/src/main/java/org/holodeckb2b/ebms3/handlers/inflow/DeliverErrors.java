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
package org.holodeckb2b.ebms3.handlers.inflow;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.common.messagemodel.ErrorMessage;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.axis2.MessageContextUtils;
import org.holodeckb2b.ebms3.constants.MessageContextProperties;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.*;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.module.HolodeckB2BCore;
import org.holodeckb2b.persistency.dao.StorageManager;
import org.holodeckb2b.pmode.PModeUtils;

import java.util.Collection;

/**
 * Is the <i>IN_FLOW</i> handler responsible for checking if error message should be delivered to the business
 * application and if so to hand them over to the responsible {@link IMessageDeliverer}.
 * <p>To prevent that errors in the error message unit are delivered twice in parallel delivery only takes place when
 * the processing state of the unit can be successfully changed from {@link ProcessingState#READY_FOR_DELIVERY} to
 * {@link ProcessingState#OUT_FOR_DELIVERY}.
 * <p>To enable easy monitoring all received error signals will always be logged to a separate log
 * (<code>org.holodeckb2b.msgproc.errors.received</code>).
 * <p>NOTE: The actual delivery to the business application is done through a {@link IMessageDeliverer} which is
 * specified in the P-Mode for this message unit. That P-Mode is the same as the P-Mode of the referenced message or
 * if no message is referenced and this message unit is received as a response the primary message unit in the request.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class DeliverErrors extends BaseHandler {

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {
        // Check if this message contains error signals
        final Collection<IErrorMessageEntity> errorSignals = (Collection<IErrorMessageEntity>)
                                                                    mc.getProperty(MessageContextProperties.IN_ERRORS);

        if (Utils.isNullOrEmpty(errorSignals))
            // No errors to deliver
            return InvocationResponse.CONTINUE;

        log.debug("Message contains " + errorSignals.size() + " Error Signals");
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        // Process each signal
        for(final IErrorMessageEntity errorSignal : errorSignals) {
            // Prepare message for delivery by checking it is still ready for delivery and then
            // change its processing state to "out for delivery"
            log.debug("Prepare Error Signal [" + errorSignal.getMessageId() + "] for delivery");

            if(updateManager.setProcessingState(errorSignal, ProcessingState.READY_FOR_DELIVERY,
                                                             ProcessingState.OUT_FOR_DELIVERY)) {
                // Errors in this signal can be delivered to business application
                log.debug("Start delivery of Error Signal [" + errorSignal.getMessageId() + "]");
                // We deliver each error in the signal separately because they can reference different
                // messages and therefor have different delivery specs
                try {
                    checkAndDeliver(errorSignal, mc);
                    // All errors in signal processed, change the processing state to done
                    updateManager.setProcessingState(errorSignal, ProcessingState.DONE);
                } catch (final MessageDeliveryException ex) {
                    log.warn("Could not deliver Error Signal (msgId=" + errorSignal.getMessageId()
                                    + "]) to application! Error details: " + ex.getMessage());
                    // Although the error could not be delivered it was processed completely on the ebMS level,
                    //  so processing state is set to warning instead of failure
                    updateManager.setProcessingState(errorSignal, ProcessingState.WARNING);
                }
            } else {
                log.info("Error signal [" + errorSignal.getMessageId() + "] is already processed for delivery");
            }
        }

        log.debug("Processed all Error signals in message");
        return InvocationResponse.CONTINUE;
    }

    /**
     * Is a helper method responsible for checking whether and if so delivering an Error Signal to the business
     * application. Delivery to the business application is done through a {@link IMessageDeliverer}.
     *
     * @param errorSignal   The Error signal that must be delivered
     * @param mc            The current message context which is needed when the error does not reference a message
     *                      directly
     * @throws MessageDeliveryException When the error should be delivered to the business application but an error
     *                                  prevented successful delivery
     * @throws PersistenceException    When an error occurs retrieving the message unit referenced by the Error Signal
     */
    private void checkAndDeliver(final IErrorMessageEntity errorSignal, final MessageContext mc)
            throws MessageDeliveryException, PersistenceException {
        IDeliverySpecification deliverySpec = null;

        log.debug("Determine P-Mode for error");
        // Does the error reference another message unit?
        String refToMsgId = MessageUnitUtils.getRefToMessageId(errorSignal);

        if (!Utils.isNullOrEmpty(refToMsgId)) {
            log.debug("The error references message unit with msgId=" + refToMsgId);
            // Get the referenced message unit. There may be more than one MU with the given id, we assume they
            // all use the same P-Mode
            final Collection<IMessageUnitEntity> refdMsgUnits = HolodeckB2BCore.getQueryManager()
                                                                               .getMessageUnitsWithId(refToMsgId,
                                                                            		   	 			  Direction.OUT);

            if (!Utils.isNullOrEmpty(refdMsgUnits))
                // Found referenced message unit (should be one), use its P-Mode to determine if and how to deliver
                deliverySpec = getErrorDelivery(refdMsgUnits.iterator().next());
            else
                // No messsage units found for refToMsgId. This should not occur here as this is already checked in
                // previous handler!
                log.error("No referenced message unit found! Probably there is a configuration error!");
        } else {
            log.debug("Error does not directly reference a message unit");
            // If the error is a direct response and there was just on outgoing message unit we still have a
            // reference
            final Collection<IMessageUnitEntity>  reqMUs = MessageContextUtils.getSentMessageUnits(mc);
            if (reqMUs != null && reqMUs.size() == 1) {
                log.debug("Request contained one message unit, assuming error applies to it");
                final IMessageUnitEntity refdMU = reqMUs.iterator().next();
                refToMsgId = refdMU.getMessageId();
                deliverySpec = getErrorDelivery(refdMU);
            }
        }

        // If a delivery specification was found the error should be delivered, else no reporting is needed
        if (deliverySpec != null) {
            log.debug("Error Signal should be delivered using delivery specification with id:" + deliverySpec.getId());
            log.debug("Get deliverer from Core");
            final IMessageDeliverer deliverer = HolodeckB2BCore.getMessageDeliverer(deliverySpec);
            log.debug("Delivering the error using deliverer");
            // Because the reference to the message in error may be derived, set it explicitly on signal meta-data
            // See issue #12
            try {
                ErrorMessage deliverySignal = new ErrorMessage(errorSignal);
                deliverySignal.setRefToMessageId(refToMsgId);
                deliverer.deliver(deliverySignal);
                log.debug("Error successfully delivered!");
            } catch (final MessageDeliveryException ex) {
                // There was an "normal/expected" issue during delivery, continue as normal
                throw ex;
            } catch (final Throwable t) {
                // Catch of Throwable used for extra safety in case the DeliveryMethod implementation does not
                // handle all exceptions correctly
                log.warn(deliverer.getClass().getSimpleName() + " threw " + t.getClass().getSimpleName()
                         + " instead of MessageDeliveryException!");
                throw new MessageDeliveryException("Unhandled exception during message delivery", t);
            }
        } else
            log.debug("Error does not need to (or can not) be delivered");
    }

    /**
     * Is a helper method to determine if and how an Error Signal should be delivered to the business application. The
     * P-Mode of the referenced message unit (which is also the P-Mode for the error) is used for this check. It depends
     * on the type of the referenced message unit which P-Mode setting determines whether the error should be delivered
     * and how:<ul>
     * <li>For User message and Receipt signal : <code>PMode.Leg.usermessage.ErrorHandling</code></li>
     * <li>Pull Request : First <code>PMode.leg.pullrequest.ErrorHandling</code> and if that is not specified <code>
     * PMode.Leg.usermessage.ErrorHandling</code></li>
     * <li>Error signal : Here also <code>PMode.Leg.usermessage.ErrorHandling</code> is used. But this can only be done
     * if the sent error was assigned to a P-Mode. This is not guaranteed, so it possible that no error handling
     * configuration can be retrieved. In which case the error will not be delivered.
     * </ul>
     * How the error is delivered is defined by the delivery specification linked to the error handling configuration.
     * If that is not set the default delivery specification [of the Leg] will be used.
     *
     * @param refdMU    The message unit referenced by the error
     * @return          When the error should be delivered to the business application, the {@link
     *                  IDeliverySpecification} that should be used for the delivery,<br>
     *                  <code>null</code> otherwise
     */
    private IDeliverySpecification getErrorDelivery(final IMessageUnitEntity refdMU) {
        IDeliverySpecification deliverySpec = null;

        if (Utils.isNullOrEmpty(refdMU.getPModeId()))
            return null; // Referenced message unit without P-Mode, can not determine delivery

        final IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(refdMU.getPModeId());
        if (pmode == null) {
            log.warn("Sent message unit [" + refdMU.getMessageId() +"] does not reference valid P-Mode ["
                        + refdMU.getPModeId() + "]!");
            return null;
        }
        // First get the delivery specification for errors related to the user message as this will also be the fall
        // back for errors related to pull request if nothing is specified specifically for pull requests
        final ILeg leg = pmode.getLeg(refdMU.getLeg());
        final IUserMessageFlow umFlow = leg.getUserMessageFlow();
        IErrorHandling errHandling = umFlow != null ? umFlow.getErrorHandlingConfiguration() : null;

        if (refdMU instanceof IPullRequest) {
            // Check if the pull request have their own error handling
            final IPullRequestFlow prFlow = PModeUtils.getOutPullRequestFlow(pmode);
            errHandling = prFlow != null && prFlow.getErrorHandlingConfiguration() != null ?
                                                                   prFlow.getErrorHandlingConfiguration() : errHandling;
        }

        if (errHandling != null)
            deliverySpec = errHandling.getErrorDelivery();
        if (deliverySpec == null)
            deliverySpec = leg.getDefaultDelivery();

        if (errHandling != null && errHandling.shouldNotifyErrorToBusinessApplication())
            return deliverySpec;
        else
            return null;
    }
}

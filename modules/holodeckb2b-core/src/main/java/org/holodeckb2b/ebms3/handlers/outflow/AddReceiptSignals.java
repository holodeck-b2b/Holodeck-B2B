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
package org.holodeckb2b.ebms3.handlers.outflow;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.handler.BaseHandler;
import org.holodeckb2b.interfaces.persistency.PersistenceException;

/**
 * <b>THIS HANDLER IS DEPRECATED</b>
 * <p><b>Bundling should be handled by specific P-Mode parameters because in a multi-hop context the destination URL
 * does not guarantee that the ultimate receiver is the same for all message units!
 * <br>============================</b>
 * <p>Is the <i>OUT_FLOW</i> handler responsible for adding <i>Receipt signals</i> that are waiting to be sent to the
 * outgoing message.
 * <p>Currently receipt signals will only be added to messages that initiate a message exchange, i.e. that are sent
 * as a request. This allows for an easy bundling rule as the destination URL can be used as selector. Adding receipts
 * to a response would require additional P-Mode configuration as possible bundling options must be indicated.
 * <p>NOTE: There exists some ambiguity in both the ebMS Core Specification and AS4 Profile about bundling of message
 * units (see issue https://tools.oasis-open.org/issues/browse/EBXMLMSG-50?jql=project%20%3D%20EBXMLMSG). This is also
 * a reason to bundle only if the URL is the same.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Deprecated
public class AddReceiptSignals extends BaseHandler {

    /**
     * Response message are ignored as receipts are not added to responses
     *
     * @return Indication that the handler only runs in the response out flow, i.e. <code>OUT_FLOW | INITIATOR </code>
     */
    @Override
    protected byte inFlows() {
        return OUT_FLOW | INITIATOR;
    }

    @Override
    protected InvocationResponse doProcessing(final MessageContext mc) throws PersistenceException {

//        log.debug("Check if this message already contains Receipt signals to send");
//        Collection<EntityProxy<Receipt>> rcptSigs = null;
//        try {
//            rcptSigs = (Collection<EntityProxy<Receipt>>) mc.getProperty(MessageContextProperties.OUT_RECEIPTS);
//        } catch(final ClassCastException cce) {
//            log.fatal("Illegal state of processing! MessageContext contained a "
//                        + mc.getProperty(MessageContextProperties.OUT_RECEIPTS).getClass().getName() + " object as collection of error signals!");
//            return InvocationResponse.ABORT;
//        }
//
//        if(!Utils.isNullOrEmpty(rcptSigs)) {
//            log.debug("Message already contains Receipt signals, can not add additional ones");
//            return InvocationResponse.CONTINUE;
//        }
//
//        // Whether Receipts can be bundled is determined by the primary message unit
//        log.debug("Get primary message unit already in the message");
//        final EntityProxy<MessageUnit> primaryMU = MessageContextUtils.getPrimaryMessageUnit(mc);
//
//        if(primaryMU == null) {
//            // This message does not have any other message unit
//            // => it is not an ebMS message (strange as Holodeck B2B module was engaged)
//            log.warn("No ebMS message units in message!");
//            return InvocationResponse.CONTINUE;
//        }
//
//        log.debug("Get receipts that can be bundled with current message unit");
//        final Collection<EntityProxy<Receipt>> rcptsToAdd = getBundableReceipts(primaryMU);
//        if (Utils.isNullOrEmpty(rcptsToAdd)) {
//            log.debug("No receipt signal(s) found to add to the message");
//            return InvocationResponse.CONTINUE;
//        } else
//            log.debug(rcptsToAdd.size() + " receipt signal(s) will be added to the message");
//
//        // Change the processing state of the rcpts that are included
//        for(final EntityProxy<Receipt> r : rcptsToAdd) {
//            log.debug("Change processing state of receipt signal [" + r.entity.getMessageId()
//                        + "] to indicate it is included");
//            // Change processing state to indicate Receipt is going to be processed
//            if (MessageUnitDAO.startProcessingMessageUnit(r)) {
//                log.debug("Processing state changed for Receipt signal with msgId=" + r.entity.getMessageId());
//                MessageContextUtils.addReceiptToSend(mc, r);
//            }
//        }
        return InvocationResponse.CONTINUE;
    }

//    /**
//     * Retrieves the {@link Receipt}s waiting to be sent and that can be bundled with the  included message
//     * units.
//     * <p>An <i>receipt signal</i> can be included in this message if the URL the receipt should be sent to equals the
//     * destination URL of the <i>primary</i> message unit.
//     * <p>An receipt signal is waiting to be sent if its processing state is either {@link ProcessingStates#CREATED} or
//     * {@link ProcessingStates#TRANSPORT_FAILURE}.
//     *
//     * @param primaryMU     The primary message unit contained in the message
//     * @return              A collection of Error signal that can be bundled with the given primary message unit
//     */
//    private Collection<EntityProxy<Receipt>> getBundableReceipts(final EntityProxy<MessageUnit> primaryMU) {
//        final ArrayList<EntityProxy<Receipt>> rcpts = new ArrayList<>();
//        Collection<IPMode>       pmodes = null;
//
//        log.debug("Get the destination URL of the message");
//        String destURL = null;
//        final IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(primaryMU.entity.getPModeId());
//
//        if (primaryMU.entity instanceof UserMessage ||  primaryMU.entity instanceof PullRequest) {
//            destURL = pmode.getLegs().iterator().next().getProtocol().getAddress();
//        } else { // MessageUnit instanceof Error
//            try {
//                destURL = pmode.getLegs().iterator().next().getUserMessageFlow()
//                                                            .getErrorHandlingConfiguration().getReceiverErrorsTo();
//            } catch (final NullPointerException npe) {
//                // Unable to determine URL, no receipts to bundle
//                return null;
//            }
//        }
//
//        log.debug("Get P-Modes of Receipts that can be bundled");
//        pmodes = PModeFinder.getPModesWithReceiptsTo(destURL);
//
//        if(Utils.isNullOrEmpty(pmodes)) {
//            log.debug("No P-Modes found that allow bundling of receipts to this message");
//            return null;
//        }
//
//        log.debug("Receipts from " + pmodes.size() + " P-Modes can be bundled to message.");
//        log.debug("Retrieve Receipts waiting to send");
//        try {
//            Collection<EntityProxy<Receipt>> createdRcpts =
//                                         MessageUnitDAO.getMessageUnitsForPModesInState(Receipt.class,
//                                                                                        pmodes,
//                                                                                        ProcessingStates.CREATED);
//            if (!Utils.isNullOrEmpty(createdRcpts))
//                rcpts.addAll(createdRcpts);
//            createdRcpts = MessageUnitDAO.getMessageUnitsForPModesInState(Receipt.class, pmodes,
//                                                                                        ProcessingStates.READY_TO_PUSH);
//            if (!Utils.isNullOrEmpty(createdRcpts))
//                rcpts.addAll(createdRcpts);
//            final Collection<EntityProxy<Receipt>> failedRcpts =
//                                    MessageUnitDAO.getMessageUnitsForPModesInState(Receipt.class,
//                                                                                   pmodes,
//                                                                                   ProcessingStates.TRANSPORT_FAILURE);
//            if (!Utils.isNullOrEmpty(failedRcpts))
//                rcpts.addAll(failedRcpts);
//        } catch (final PersistenceException dbe) {
//            log.error("An error occurred while retrieving receipts signals from the database! Details: "
//                        + dbe.getMessage());
//            return null;
//        }
//
//        // As we only allow one error signal in the message we select the oldest one if more are available
//        if (rcpts.size() > 1) {
//            log.debug("More than one Receipt available, select oldest as bundling not allowed");
//            EntityProxy<Receipt> oldestRcpt = rcpts.get(0);
//            for(final EntityProxy<Receipt> r : rcpts)
//                if (r.entity.getTimestamp().before(oldestRcpt.entity.getTimestamp()))
//                    oldestRcpt = r;
//            rcpts.clear();
//            rcpts.add(oldestRcpt);
//        }
//
//        return rcpts;
//    }
}

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

import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.errors.ProcessingModeMismatch;
import org.holodeckb2b.common.handlers.AbstractBaseHandler;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.StorageManager;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.ebms3.pmode.PModeFinder;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is the <i>IN_FLOW</i> handler responsible for determining the P-Modes that define how the received message units
 * should be processed.
 * <p>Without a P-Mode message units can not be processed by Holodeck B2B, so when no P-Mode is found message processing
 * is stopped (for that messsage unit, it may continue for others); an ebMS Error is generated and the processing state
 * of the message unit is set to <i>FAILURE</i> (which should stop the processing of the message unit).
 * <p>The determination of the P-Mode is easy for Error and Receipt Signals as they must include a reference to a
 * message unit that was sent earlier by Holodeck B2B. This means the P-Mode for the received signal is the same as the
 * P-Mode of the sent message (note that there can exist situations that the sent message unit has no P-Mode if it was
 * an Error signal for a message unit for which the P-Mode could not be determined).
 * <p>For Pull Request message units finding the P-Mode is dependent on the information supplied in the WS-Security
 * header. As we have not read the WSS header at this point the P-Mode can not be determined for Pull Request signals.
 * <p>Finding the P-Mode for a User Message is done by the {@link PModeFinder} utility class by matching the meta-data 
 * from the received message to the P-Mode configuration data. Since version 4.1.0 this handler will check if the User 
 * Message was received as result of a Pull Request and if no P-Mode was found, assign the P-Mode used by the
 * Pull Request to the User Message.
 * <p><b>NOTE:</b> The Error generated in case the P-Mode can not be determined is <i>ProcessingModeMismatch</i>. The
 * ebMS specification is not very clear if a specific error must be used. Current choice is based on discussion on <a
 * href="https://issues.oasis-open.org/browse/EBXMLMSG-67">issue 67 of ebMS TC</a>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class FindPModes extends AbstractBaseHandler {


    @Override
    protected InvocationResponse doProcessing(final IMessageProcessingContext procCtx, final Logger log) 
    																					throws PersistenceException {
        StorageManager updateManager = HolodeckB2BCore.getStorageManager();
        final IUserMessageEntity userMsg = procCtx.getReceivedUserMessage();
        if (userMsg != null) {
            log.debug("Finding P-Mode for User Message [" + userMsg.getMessageId() + "]");
            IPMode pmode = PModeFinder.forReceivedUserMessage(userMsg, log);            
            if (pmode == null && procCtx.isHB2BInitiated()) {
            	final IPullRequest pullRequest = procCtx.getSendingPullRequest();
            	if (pullRequest != null) {
            		log.debug("Using P-Mode of sent PullRequest");
            		pmode = HolodeckB2BCoreInterface.getPModeSet().get(pullRequest.getPModeId());
            	}
            }
            if (pmode == null) {
                log.warn("No P-Mode found for User Message [" + userMsg.getMessageId() + "]");
                procCtx.addGeneratedError(new ProcessingModeMismatch(
                								"Can not process message because no P-Mode was found for the message!",
                								userMsg.getMessageId()));
                log.trace("Set the processing state of this User Message to failure");
                updateManager.setProcessingState(userMsg, ProcessingState.FAILURE);
            } else {
                log.info("Found P-Mode [" + pmode.getId() + "] for User Message [" + userMsg.getMessageId() + "]");
                updateManager.setPModeId(userMsg, pmode.getId());
            }
        } 

        final Collection<IErrorMessageEntity>  errorSignals = procCtx.getReceivedErrors();
        if (!Utils.isNullOrEmpty(errorSignals)) {
            log.debug("Message contains " + errorSignals.size() + " Error Signals, finding P-Modes");
            for (final IErrorMessageEntity e : errorSignals) {
                final Pair<IPMode, ILeg.Label> pl = findForReceivedErrorSignal(e, procCtx);
                if (pl == null) {
                    log.warn("No P-Mode found for Error Signal [" + e.getMessageId() + "]");
                    procCtx.addGeneratedError(new ProcessingModeMismatch(
                    								"Can not process message because no P-Mode was found for the message!",
                    								e.getMessageId()));
                    log.trace("Set the processing state of this Error Signal to failure");
                    updateManager.setProcessingState(e, ProcessingState.FAILURE);
                } else {
                    log.info("Found P-Mode [" + pl.value1().getId() + "] for Error Signal [" + e.getMessageId() + "]");
                    updateManager.setPModeAndLeg(e, pl);
                }
            }
        } 

        final Collection<IReceiptEntity>  rcptSignals = procCtx.getReceivedReceipts();
        if (!Utils.isNullOrEmpty(rcptSignals)) {
            log.debug("Message contains " + rcptSignals.size() + " Receipt Signals, finding P-Modes");
            for (final IReceiptEntity r : rcptSignals) {
                final Pair<IPMode, ILeg.Label> pl = getPModeAndLegFromRefdMessage(r.getRefToMessageId());
                if (pl == null) {
                    log.warn("No P-Mode found for Receipt Signal [" + r.getMessageId() + "]");
                    procCtx.addGeneratedError(new ProcessingModeMismatch(
                    								"Can not process message because no P-Mode was found for the message!",
                    								r.getMessageId()));
                    log.trace("Set the processing state of this Receipt Signal to failure");
                    updateManager.setProcessingState(r, ProcessingState.FAILURE);
                } else {
                	IPMode pmode = pl.value1();
                    log.info("Found P-Mode [" + pmode.getId() + "] for message [" + r.getMessageId() + "]");
                    updateManager.setPModeId(r, pmode.getId());
                }
            }
        } 

        return InvocationResponse.CONTINUE;
    }

    /**
     * Helper method that finds the P-Mode and Leg for a received Error signal message unit.
     * <p>The P-Mode that handles a received Error signal is the same as the P-Mode that handles the message unit the
     * error is response to. So the referenced message unit is determined and its P-Mode is set on the error signal.
     * <p>If the error signal itself does not contain a reference to the message unit in error but it is received as an
     * HTTP response to an outgoing ebMS message, the primary message unit of that message is used to determine the 
     * P-Mode.
     *
     * @param e     The received Error signal message unit
     * @param mc    The message context of the Error signal
     * @return      Pair consisting of the P-Mode and the label of Leg that handles this Error signal if the referenced 
     * 				message id can be matched to a sent message unit,<br/>
     * 				<code>null</code> otherwise 
     * @throws PersistenceException When an error occurs retrieving the referenced message unit
     */
    private Pair<IPMode, ILeg.Label> findForReceivedErrorSignal(final IErrorMessageEntity e, 
    											final IMessageProcessingContext procCtx) throws PersistenceException {
    	// First get the referenced message id, starting with information from the header
        String refToMessageId = MessageUnitUtils.getRefToMessageId(e);
        // If there is no referenced message unit and the error is received as a response we will use the primary 
        // message unit from the message we sent out
        if (Utils.isNullOrEmpty(refToMessageId) && !procCtx.getParentContext().isServerSide()) {
        	IMessageUnitEntity primarySentMessageUnit = procCtx.getPrimarySentMessageUnit();
        	if (primarySentMessageUnit != null) 
    			return new Pair<>(HolodeckB2BCoreInterface.getPModeSet().get(primarySentMessageUnit.getPModeId()), 
    								PModeUtils.getLeg(primarySentMessageUnit).getLabel());
        	else
        		return null;
        } else
	        // Use the referenced message id to get the P-Mode and Leg
	    	return getPModeAndLegFromRefdMessage(refToMessageId);
    }

    /**
     * Helper method to get the P-Mode from the referenced message unit.
     *
     * @param refToMsgId    The message id of the referenced message unit
     * @return              The PMode of the referenced message unit if it is found, or<br>
     *                      <code>null</code> if no message unit can be found for the given message id
     * @throws PersistenceException When a problem occurs retrieving the meta-data from the database for the referenced
     *                              message unit
     */
    private Pair<IPMode, ILeg.Label> getPModeAndLegFromRefdMessage(final String refToMsgId) throws PersistenceException {
    	Pair<IPMode, ILeg.Label> result = null;
        if (!Utils.isNullOrEmpty(refToMsgId)) {
            Collection<IMessageUnitEntity> refdMsgUnits = HolodeckB2BCore.getQueryManager()
                                                                         .getMessageUnitsWithId(refToMsgId,
                                                                        		 				Direction.OUT);
            if (!Utils.isNullOrEmpty(refdMsgUnits) && refdMsgUnits.size() == 1) {
            	IMessageUnitEntity refdMsg = refdMsgUnits.iterator().next();
        		result = new Pair<>(HolodeckB2BCoreInterface.getPModeSet().get(refdMsg.getPModeId()), 
        							PModeUtils.getLeg(refdMsg).getLabel());
            }
        }
        return result;
    }

}

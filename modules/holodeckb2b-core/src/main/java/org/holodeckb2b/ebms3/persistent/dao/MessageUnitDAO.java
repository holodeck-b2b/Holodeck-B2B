/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.persistent.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.exceptions.DuplicateMessageIdError;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.general.IDescription;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.general.ISchemaReference;
import org.holodeckb2b.common.general.IService;
import org.holodeckb2b.common.messagemodel.IAgreementReference;
import org.holodeckb2b.common.messagemodel.ICollaborationInfo;
import org.holodeckb2b.common.messagemodel.IPayload;
import org.holodeckb2b.common.messagemodel.IPayload.Containment;
import org.holodeckb2b.common.messagemodel.IUserMessage;
import org.holodeckb2b.common.pmode.IPMode;
import org.holodeckb2b.common.util.MessageIdGenerator;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistent.general.Description;
import org.holodeckb2b.ebms3.persistent.general.Property;
import org.holodeckb2b.ebms3.persistent.general.SchemaReference;
import org.holodeckb2b.ebms3.persistent.general.Service;
import org.holodeckb2b.ebms3.persistent.general.TradingPartner;
import org.holodeckb2b.ebms3.persistent.message.AgreementReference;
import org.holodeckb2b.ebms3.persistent.message.CollaborationInfo;
import org.holodeckb2b.ebms3.persistent.message.EbmsError;
import org.holodeckb2b.ebms3.persistent.message.ErrorMessage;
import org.holodeckb2b.ebms3.persistent.message.MessageUnit;
import org.holodeckb2b.ebms3.persistent.message.Payload;
import org.holodeckb2b.ebms3.persistent.message.PullRequest;
import org.holodeckb2b.ebms3.persistent.message.Receipt;
import org.holodeckb2b.ebms3.persistent.message.SignalMessage;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.persistent.processing.ProcessingState;

/**
 * Is a data access object for {@link MessageUnit} objects (including derived classes like {@link UserMessage}.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageUnitDAO {

    /**
     * Creates and stores a new outgoing User Message message unit in the database. Because this is an outgoing message
     * it should have a message id and creation timestamp. If not supplied in the meta-data both will be created now.
     * <p>
     * The {@link ProcessingState} of the new message unit will be set to {@link ProcessingStates#SUBMITTED} to indicate
     * that is a new message.
     *
     * @param um        The meta data on the User Message
     * @param pmodeId   The id of the P-Mode that defines the processing of the message unit
     * @return          The newly created user message
     * @throws DuplicateMessageIdError  If the message data includes a message id which already exists in the message 
     *                                  database for an outgoing message
     * @throws DatabaseException        If an error occurs when saving the new message unit to the database.
     */
    public static UserMessage createOutgoingUserMessage(IUserMessage um, String pmodeId) throws DatabaseException {
        // A submitted user message may already contain an message id, before going further, check that it is unique
        String msgId = um.getMessageId();
        if (msgId != null && !msgId.isEmpty()) {
            // Ensure that message id are unique
            if (getSentMessageUnitWithId(msgId) != null)
                // Message id already exists for an outgoing message, raise error
                throw new DuplicateMessageIdError(msgId);
        } else 
            // No message id specified, generate a unique one now
            msgId = MessageIdGenerator.createMessageId();
        
        EntityManager em = JPAUtil.getEntityManager();

        try {
            UserMessage newUserMU = new UserMessage();

            // Open transaction to ensure integrity
            em.getTransaction().begin();

            copy(um, newUserMU, em); // copy data

            // Set the message id and timestamp when not already done so
            newUserMU.setMessageId(msgId);
            Date timeStamp = newUserMU.getTimestamp();
            newUserMU.setTimestamp((timeStamp == null) ? new Date() : timeStamp);
            newUserMU.setDirection(MessageUnit.Direction.OUT);

            // Set P-Mode id
            newUserMU.setPMode(pmodeId);

            // Set state to SUBMITTED
            ProcessingState procstate = new ProcessingState(ProcessingStates.SUBMITTED);
            newUserMU.setProcessingState(procstate);
            em.persist(newUserMU);

            // Commit changes to DB
            em.getTransaction().commit();

            return newUserMU;
        } finally {
            em.close();
        }
    }

    /**
     * Creates and stores a new error signal message unit for the given set of errors.
     * <p>
     * The {@link ProcessingState} of the new error message unit will be set to {@link ProcessingStates#CREATED} or
     * {@link ProcessingStates#PROCESSING} to indicate an error that has to be processed later or an error that is
     * directly processed as a response to the message in error. This is indicated by the <code>asResponse</code>
     * parameter.
     *
     * @param errors The {@link EbmsError}s to include in the new Error signal message unit
     * @param refToMsgId The message id of the message unit in error. May be <code>null</code> if there is no related
     * message.
     * @param pmodeId The id of the PMode that defines the processing of the new Error signal. May be <code>null</code>
     * if the P-Mode is not known.
     * @param asResponse The error will be reported as a response
     * @return The new Error signal message unit
     * @throws DatabaseException If an error occurs when saving the new message unit to the database
     */
    public static ErrorMessage createOutgoingErrorMessageUnit(Collection<EbmsError> errors, String refToMsgId, String pmodeId, boolean asResponse) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            ErrorMessage newErrorMU = new ErrorMessage();

            // Open transaction to ensure integrity
            em.getTransaction().begin();

            // Generate a message id and timestamp for the new error message unit
            newErrorMU.setMessageId(MessageIdGenerator.createMessageId());
            newErrorMU.setTimestamp(new Date());
            newErrorMU.setDirection(MessageUnit.Direction.OUT);
            
            // Set reference to message in error if available
            if (refToMsgId != null && !refToMsgId.isEmpty()) {
                newErrorMU.setRefToMessageId(refToMsgId);
            }

            // Set P-Mode id
            if (pmodeId != null && !pmodeId.isEmpty()) {
                newErrorMU.setPMode(pmodeId);
            }

            // Add the errors to it
            for (EbmsError e : errors) {
                newErrorMU.addError(e);
            }

            // Set state to CREATED
            ProcessingState procstate = new ProcessingState(ProcessingStates.CREATED);
            newErrorMU.setProcessingState(procstate);

            if (asResponse) {
                // If error is reported as response, directly set state to processing
                procstate = new ProcessingState(ProcessingStates.PROCESSING);
                newErrorMU.setProcessingState(procstate);
            } else {
                // Otherwise it is ready to push
                procstate = new ProcessingState(ProcessingStates.READY_TO_PUSH);
                newErrorMU.setProcessingState(procstate);
            }

            // Persist the new message unit
            em.persist(newErrorMU);

            // Commit changes to DB
            em.getTransaction().commit();

            return newErrorMU;
        } finally {
            em.close();
        }
    }

    /**
     * Creates and stores a new pull request signal message unit.
     * <p>
     * The {@link ProcessingState} of the new pull request message unit will be set to
     * {@link ProcessingStates#PROCESSING} to indicate that it is processed directly.
     *
     * @param pmodeId The id of the PMode that defines the processing of the new Pull Request signal.
     * @param mpc The MPC that the pull operation will apply to
     * @return The new Pull Request signal message unit
     * @throws DatabaseException If an error occurs when saving the new message unit to the database
     */
    public static PullRequest createOutgoingPullRequest(String pmodeId, String mpc) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            PullRequest newPullReqMU = new PullRequest();

            // Open transaction to ensure integrity
            em.getTransaction().begin();

            // Generate a message id and timestamp for the new error message unit
            newPullReqMU.setMessageId(MessageIdGenerator.createMessageId());
            newPullReqMU.setTimestamp(new Date());
            newPullReqMU.setDirection(MessageUnit.Direction.OUT);
            
            // Set P-Mode id
            if (pmodeId != null || !pmodeId.isEmpty()) {
                newPullReqMU.setPMode(pmodeId);
            }
            // Set MPC
            if (mpc != null || !mpc.isEmpty()) {
                newPullReqMU.setMPC(mpc);
            } else {
                newPullReqMU.setMPC(Constants.DEFAULT_MPC);
            }

            // Add CREATED to signal PullRequest was made
            ProcessingState procstate = new ProcessingState(ProcessingStates.CREATED);
            newPullReqMU.setProcessingState(procstate);
            // and immediately change state to PROCESSING to indicate it is being send
            procstate = new ProcessingState(ProcessingStates.PROCESSING);
            newPullReqMU.setProcessingState(procstate);

            // Persist the new message unit
            em.persist(newPullReqMU);

            // Commit changes to DB
            em.getTransaction().commit();

            return newPullReqMU;
        } finally {
            em.close();
        }
    }

    /**
     * Stores the given {@link Receipt} entity object representing a new receipt signal message unit to the database.
     * <p>
     * The {@link ProcessingState} of the new receipt message unit will be set to {@link ProcessingStates#CREATED} or
     * {@link ProcessingStates#PROCESSING} to indicate a receipt that has to be processed later or directly as a
     * response to the received message. This is indicated by the <code>asResponse</code> parameter.
     *
     * @param receipt The {@link Receipt} to store
     * @param asResponse The error will be reported as a response
     * @return The stored entity object
     * @throws DatabaseException If an error occurs when saving the new message unit to the database
     */
    @Deprecated //@todo: Creating a Receipt should use same pattern as other message units!
    public static Receipt storeOutgoingReceiptMessageUnit(Receipt receipt, boolean asResponse) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            // Open transaction to ensure integrity
            em.getTransaction().begin();

            // Generate a message id and timestamp for the new error message unit if not already done
            if (receipt.getMessageId() == null || receipt.getMessageId().isEmpty()) {
                receipt.setMessageId(MessageIdGenerator.createMessageId());
            }
            if (receipt.getTimestamp() == null) {
                receipt.setTimestamp(new Date());
            }
            receipt.setDirection(MessageUnit.Direction.OUT);
            
            // Set state to CREATED
            ProcessingState procstate = new ProcessingState(ProcessingStates.CREATED);
            receipt.setProcessingState(procstate);

            // If error is reported as response, directly set state to processing
            if (asResponse) {
                procstate = new ProcessingState(ProcessingStates.PROCESSING);
                receipt.setProcessingState(procstate);
            }

            // Persist the new message unit
            em.persist(receipt);

            // Commit changes to DB
            em.getTransaction().commit();

            return receipt;
        } finally {
            em.close();
        }
    }

    /**
     * Stores information of a received message unit in the database so it can be processed. The information is stored
     * as-is to correctly reflect the received info.
     * <p>
     * The processing state of the stored message unit will first be set to {@link ProcessingStates#RECEIVED}.
     *
     * @param mu The information on the received message unit as a {@link MessageUnit} entity object.
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static void storeReceivedMessageUnit(MessageUnit mu) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();

        em.getTransaction().begin();

        mu.setDirection(MessageUnit.Direction.IN);
        mu.setProcessingState(new ProcessingState(ProcessingStates.RECEIVED));

        // If the message unit is a UserMessage object, the related entity objects must be persisted first
        if (mu instanceof UserMessage) {
            persistRelatedObjects((UserMessage) mu, em);
        }

        em.persist(mu);

        // Commit changes to DB
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Updates the stored meta-data of a <i>user message message unit</i> with the information about the payloads.
     *
     * @param um The {@link UserMessage} entity object that needs to be updated
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static void updatePayloadMetaData(UserMessage um) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();

        em.getTransaction().begin();

        // Ensure that the Payload entity objects are persisted
        for (IPayload ip : um.getPayloads()) {
            em.merge((Payload) ip);
        }

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Sets the P-Mode that defines how this message unit should be processed.
     *
     * @param mu The {@link MessageUnit}
     * @param pmode The {@link IPMode} that defines how the message unit must be processed
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static void setPMode(MessageUnit mu, IPMode pmode) throws DatabaseException {
        setPModeId(mu, pmode.getId());
    }

    /**
     * Sets the P-Mode Id that defines how this message unit should be processed.
     *
     * @param mu The {@link MessageUnit}
     * @param pmodeId The P-Mode id of the P-Mode that defines how the message unit must be processed
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static void setPModeId(MessageUnit mu, String pmodeId) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();

        em.getTransaction().begin();
        mu.setPMode(pmodeId);
        em.merge(mu);
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Checks whether a {@link UserMessage} with the given <code>MessageId</code> has already been delivered, i.e. the
     * <i>current</i> processing state is {@link ProcessingStates#DELIVERED}.
     *
     * @param messageId The <code>MessageId</code> to check delivery for
     * @return                      <code>true</code> if a {@link UserMessage} with <code>messageId</code> and
     * {@link UserMessage#getCurrentProcessingState()} == {@link ProcessingStates#DELIVERED} exists, <code>false</code>
     * otherwise.
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static boolean isUserMsgDelivered(String messageId) throws DatabaseException {
        boolean result = false;
        EntityManager em = JPAUtil.getEntityManager();

        try {
            result = "true".equals(em.createNamedQuery("UserMessage.isDelivered",
                    String.class).setParameter("msgId", messageId)
                    .getSingleResult()
            );
        } catch (NoResultException notDelivered) {
            result = false;
        }

        return result;
    }

    /**
     * Retrieves all {@link MessageUnit}s in the given state.
     * <p>
     * <b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is going
     * to be processed it must be loaded completely.
     *
     * @param state The name of the state the message units to retrieve should be in
     * @return A list of {@link MessageUnit} objects representing the message units that are in the given state
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static List<MessageUnit> getMessageUnitsInState(String state) throws DatabaseException {
        List<MessageUnit> result = null;
        EntityManager em = JPAUtil.getEntityManager();

        result = em.createNamedQuery("MessageUnit.findInState", MessageUnit.class)
                .setParameter("state", state).getResultList();
        em.close();

        return result;
    }

    /**
     * Gets the number of times the <i>User message</i> has already been sent to the receiver without getting a receipt.
     *
     * @param um The {@link UserMessage} to get the number for
     * @return The number of times the {@link UserMessage} was already sent out.
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static int getNumberOfRetransmits(UserMessage um) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();

        Long result = em.createNamedQuery("UserMessage.numOfRetransmits", Long.class)
                .setParameter("msgId", um.getMessageId()).getSingleResult();

        em.close();

        return result.intValue();
    }

    /**
     * Retrieves all received {@link MessageUnit}s with the given <code>MessageId</code>. Although messageIds should be 
     * unique there can exist multiple <code>MessageUnits</code> with the same messageId due to resending.
     * <p><b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is 
     * going to be processed it must be loaded completely.
     *
     * @param messageId The messageId of the message units to retrieve
     * @return A list of received {@link MessageUnit}s with the given message id
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static List<MessageUnit> getReceivedMessageUnitsWithId(String messageId) throws DatabaseException {
        return getMessageUnitsWithIdInDirection(messageId, MessageUnit.Direction.IN);
    }
    
    /**
     * Retrieves all sent {@link MessageUnit}s with the given <code>MessageId</code>. Although messageIds should be 
     * unique there can exist multiple <code>MessageUnits</code> with the same messageId due to business applications
     * submitting messages with the same id.
     * <p><b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is 
     * going to be processed it must be loaded completely.
     *
     * @param messageId The messageId of the message units to retrieve
     * @return A list of sent {@link MessageUnit}s with the given message id
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    @Deprecated
    public static List<MessageUnit> getSentMessageUnitsWithId(String messageId) throws DatabaseException {
        return getMessageUnitsWithIdInDirection(messageId, MessageUnit.Direction.OUT);
    }

    /**
     * Retrieves the sent {@link MessageUnit} with the given <code>MessageId</code>. 
     * <p><b>NOTE:</b> The returned entity object is not completely loaded! Before processing it, it must be loaded 
     * completely.
     *
     * @param messageId The messageId of the message units to retrieve
     * @return          A {@link MessageUnit} with the given message id, or<br>
     *                  <code>null</code> when no sent message unit exists with the given id
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static MessageUnit getSentMessageUnitWithId(String messageId) throws DatabaseException {
        List<MessageUnit> msgsWithId = getMessageUnitsWithIdInDirection(messageId, MessageUnit.Direction.OUT);
        
        if (msgsWithId.size() > 0) 
            return msgsWithId.get(0);
        else
            return null;
    }

    
    /**
     * Retrieves all {@link MessageUnit}s with the given <code>MessageId</code> that flow in the given direction. 
     * Although messageIds should be unique there can exist multiple <code>MessageUnits</code> with the same messageId 
     * due to resending.
     * <p><b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is 
     * going to be processed it must be loaded completely.
     *
     * @param messageId The messageId of the message units to retrieve
     * @param direction The direction the message units should be in (IN = receiving, OUT=sending)
     * @return A list of {@link MessageUnit} objects with the given message id and flowing in the given direction
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    protected static List<MessageUnit> getMessageUnitsWithIdInDirection(String messageId, MessageUnit.Direction direction)
                    throws DatabaseException {
        List<MessageUnit> result = null;
        EntityManager em = JPAUtil.getEntityManager();

        result = em.createNamedQuery("MessageUnit.findWithMessageIdInDirection", MessageUnit.class)
                .setParameter("msgId", messageId)
                .setParameter("direction", direction)
                .getResultList();
        em.close();

        return result;
    }

    /**
     * Retrieves all {@link MessageUnit}s of the specified type and that are in the given state and which processing is
     * defined by one of the given P-Modes. The result is ordered on the latest processing state change with the oldest
     * changes first.
     * <p>
     * <b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is going
     * to be processed it must be loaded completely.
     *
     * @param type The type of message units to retrieve specified by their Class
     * @param pmodes List of P-Modes.
     * @param state The name of the state the message units to retrieve should be in
     * @return A list of {@link MessageUnit} objects representing the message units that are in the given state
     * @throws DatabaseException
     */
    public static <T extends MessageUnit> List<T> getMessageUnitsForPModesInState(Class<T> type, Collection<IPMode> pmodes, String state) throws DatabaseException {
        // The query parameter for the PModes is a list op P-Mode Ids. 
        // So convert list of PModes to list of strings
        Collection<String> pmodeIds = new ArrayList<String>();
        for (IPMode pmode : pmodes) {
            pmodeIds.add(pmode.getId());
        }

        return getMessageUnitsForPModeIdsInState(type, pmodeIds, state);
    }

    /**
     * Retrieves all {@link MessageUnit}s of the specified type and that are in the given state and which processing is
     * defined by one of the given P-Modes. The result is ordered on the latest processing state change with the oldest
     * changes first.
     * <p>
     * <b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is going
     * to be processed it must be loaded completely.
     *
     * @param type The type of message units to retrieve specified by their Class
     * @param pmodeIds List of P-Mode id's.
     * @param state The name of the state the message units to retrieve should be in
     * @return A list of {@link MessageUnit} objects representing the message units that are in the given state
     * @throws DatabaseException
     */
    public static <T extends MessageUnit> List<T> getMessageUnitsForPModeIdsInState(Class<T> type, Collection<String> pmodeIds, String state) throws DatabaseException {
        List<T> result = null;

        EntityManager em = JPAUtil.getEntityManager();

        try {
            String queryName = type.getSimpleName() + ".findForPModesInState";
            result = em.createNamedQuery(queryName, type)
                    .setParameter("state", state)
                    .setParameter("pmodes", pmodeIds)
                    .getResultList();
        } catch (Exception e) {
            // Something went wrong executing the query. Probably because wrong class was specified
            throw new DatabaseException("An error occurred while executing query to retreive message units!", e);
        } finally {
            em.close();
        }

        return result;
    }

    /**
     * Changes the processing state of the given message unit to {@link ProcessingStates#PROCESSING} to indicate that
     * the message is being processed. To prevent that a single message unit is processed twice the state is only
     * changed if it is not already in <code>ProcessingStates.PROCESSING</code>.
     *
     * @param mu The {@link MessageUnit} going to be processed
     * @return If the processing state could be changed to <code>ProcessingStates.PROCESSING</code>: A new
     * {@link MessageUnit} object representing the message unit in processing. This entity object has all information
     * loaded from database and can be safely detached from the EntityManager. <code>null</code> otherwise.
     *
     * @throws DatabaseException When
     */
    public static <T extends MessageUnit> T startProcessingMessageUnit(T mu) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();

        em.getTransaction().begin();

        // First get a new managed entity object for this message unit. 
        // Also get a lock on the object because we want to prevent simultaneous changes
        // in processing state
        T amu = null;
        try {
            amu = (T) em.find(MessageUnit.class, mu.getOID(), LockModeType.PESSIMISTIC_WRITE);
        } catch (Exception e) {
            // Getting new access to or a lock on the enity failed, so processing state can not be changed
            return null;
        }

        ProcessingState curState = amu.getCurrentProcessingState();
        if (curState != null && !curState.getName().equals(ProcessingStates.PROCESSING)) {
            ProcessingState newState = new ProcessingState(ProcessingStates.PROCESSING);
            amu.setProcessingState(newState);
            // As the message unit can now be processed, secure that all information is
            // loaded into the entity object before it gets detached from the EM
            refreshMU(amu, em);
        } else {
            amu = null;
        }

        em.getTransaction().commit();
        em.close();

        return amu;
    }

    /**
     * Changes the processing state of the given message unit to {@link ProcessingStates#OUT_FOR_DELIVERY} to indicate
     * that the message is being delivered to the business application. To prevent that a single message unit is
     * delivered twice in parallel the state is only changed if its current state is
     * {@link ProcessingStates#READY_FOR_DELIVERY}.
     *
     * @param mu The {@link MessageUnit} going to be delivered
     * @return          <code>true</code> if the processing state could be changed to
     * <code>ProcessingStates.OUT_FOR_DELIVERY</code>: <code>false</code> otherwise.
     *
     * @throws DatabaseException When
     */
    public static boolean startDeliveryOfMessageUnit(MessageUnit mu) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();
        em.getTransaction().begin();

        try {
            // Get a lock on the object because we want to prevent simultaneous changes
            // in processing state
            mu = em.find(MessageUnit.class, mu.getOID(), LockModeType.PESSIMISTIC_WRITE);

            ProcessingState curState = mu.getCurrentProcessingState();
            if (curState != null && curState.getName().equals(ProcessingStates.READY_FOR_DELIVERY)) {
                ProcessingState newState = new ProcessingState(ProcessingStates.OUT_FOR_DELIVERY);
                mu.setProcessingState(newState);
                em.persist(mu);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            // Getting new access to or a lock on the enity failed, so processing state can not be changed
            return false;
        } finally {
            em.getTransaction().commit();
            em.close();
        }
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#READY_TO_PUSH} to indicate the message
     * unit is ready to be pushed to the receiving MSH.
     *
     * @param mu The {@link MessageUnit} that is ready to be pushed
     * @throws DatabaseException
     */
    public static void setReadyToPush(MessageUnit mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.READY_TO_PUSH);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#AWAITING_PULL} to indicate the message
     * unit is ready to be pulled by the receiving MSH.
     *
     * @param mu The {@link MessageUnit} that is ready to be pulled
     * @throws DatabaseException
     */
    public static void setWaitForPull(MessageUnit mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.AWAITING_PULL);
    }

    /**
     * Changes the processing state of a user message unit to {@link ProcessingStates#AWAITING_RECEIPT} to indicate the
     * message unit is sent and now waiting for a Receipt signal.
     *
     * @param um The {@link UserMessage} that is waiting for a receipt
     * @throws DatabaseException
     */
    public static void setWaitForReceipt(UserMessage um) throws DatabaseException {
        setProcessingState(um, ProcessingStates.AWAITING_RECEIPT);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#DELIVERED} to indicate the message unit
     * is successfully delivered.
     *
     * @param mu The {@link MessageUnit} that is delivered successfully
     * @throws DatabaseException
     */
    public static void setDelivered(MessageUnit mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.DELIVERED);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#FAILURE} to indicate that the message
     * unit could not be processed succesfully.
     *
     * @param mu The {@link MessageUnit} that failed to process successfully
     * @throws DatabaseException
     */
    public static void setFailed(MessageUnit mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.FAILURE);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#READY_FOR_DELIVERY} to indicate that
     * the message unit is now ready for delivery to the business application.
     *
     * @param mu The {@link MessageUnit} that is a duplicate
     * @throws DatabaseException
     */
    public static void setReadyForDelivery(MessageUnit mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.READY_FOR_DELIVERY);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#DUPLICATE} to indicate the message unit
     * is a duplicate of an already processed unit.
     *
     * @param mu The {@link MessageUnit} that is a duplicate
     * @throws DatabaseException
     */
    public static void setDuplicate(MessageUnit mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.DUPLICATE);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#DELIVERY_FAILED} to indicate the
     * message unit could not be delivered to the business application.
     *
     * @param mu The {@link MessageUnit} that could not be delivered
     * @throws DatabaseException
     */
    public static void setDeliveryFailure(MessageUnit mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.DELIVERY_FAILED);
    }

    /**
     * Changes the processing state of a signal message unit to {@link ProcessingStates#DONE} to indicate the signal
     * message unit is successfully processed.
     *
     * @param mu The {@link SignalMessage} that is successfully processed
     * @throws DatabaseException
     */
    public static void setDone(SignalMessage mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.DONE);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#TRANSPORT_FAILURE} to indicate that
     * there was a problem sending the message unit out.
     *
     * @param mu The {@link MessageUnit} that could not be sent out successfully
     * @throws DatabaseException
     */
    public static void setTransportFailure(MessageUnit mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.TRANSPORT_FAILURE);
    }

    /*
     * Helper method to change the current processing state of a MessageUnit object.
     */
    private static void setProcessingState(MessageUnit mu, String state) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();
        em.getTransaction().begin();
        ProcessingState newState = new ProcessingState(state);
        mu.setProcessingState(newState);
        em.merge(mu);
        em.getTransaction().commit();
        em.close();
    }

    /*
     * Helper method to copy information from an {@link IUserMessage} object to a 
     * {@link UserMessage} entity object 
     */
    private static void copy(IUserMessage src, UserMessage dest, EntityManager em) {
        // Copy MessageUnit attributes
        //
        dest.setMessageId(src.getMessageId());
        dest.setTimestamp(src.getTimestamp());
        dest.setRefToMessageId(src.getRefToMessageId());

        // Copy MPC
        //
        dest.setMPC(src.getMPC());

        // Copy sender and receiver TradingPartners
        //
        dest.setSender(TradingPartnerDAO.createTradingPartner(src.getSender(), em));
        dest.setReceiver(TradingPartnerDAO.createTradingPartner(src.getReceiver(), em));

        // Copy CollaborationInfo
        //
        ICollaborationInfo sci = src.getCollaborationInfo();
        if (sci != null) {
            CollaborationInfo dci = new CollaborationInfo();

            dci.setAction(sci.getAction());
            dci.setConversationId(sci.getConversationId());
            IService ssvc = sci.getService();
            if (ssvc != null) {
                dci.setService(new Service(ssvc.getName(), ssvc.getType()));
            }

            IAgreementReference sagref = sci.getAgreement();
            if (sagref != null) {
                AgreementReference dagref = new AgreementReference();
                dagref.setName(sagref.getName());
                dagref.setType(sagref.getType());
                dagref.setPModeId(sagref.getPModeId());
                dci.setAgreement(dagref);
            }

            dest.setCollaborationInfo(dci);
        }

        // Copy list of Payload objects
        //
        Collection<IPayload> sPayloads = src.getPayloads();
        if (sPayloads != null) {
            for (IPayload pl : sPayloads) {
                dest.addPayload(createPayload(pl));
            }
        }

        // Copy list of message properties
        //
        Collection<IProperty> smsgProps = src.getMessageProperties();
        if (smsgProps != null) {
            for (IProperty p : smsgProps) {
                dest.addMessageProperty(new Property(p.getName(), p.getValue(), p.getType()));
            }
        }

    }

    /*
     * Helper method to create a Payload entity object
     */
    private static Payload createPayload(IPayload pl) {
        Payload npl = new Payload();

        Containment containment = pl.getContainment();
        npl.setContainment(containment);
        npl.setContentLocation(pl.getContentLocation());
        String plURI = pl.getPayloadURI();
        if (plURI != null && (containment == Containment.ATTACHMENT || containment == Containment.BODY)) {
            if (plURI.startsWith("cid:"))
                plURI = plURI.substring(4);
            else if (plURI.startsWith("#"))
                plURI = plURI.substring(1);                
        }
        npl.setPayloadURI(plURI);
        npl.setMimeType(pl.getMimeType());

        // Schema
        //
        ISchemaReference ssr = pl.getSchemaReference();
        if (ssr != null) {
            npl.setSchemaReference(new SchemaReference(ssr.getLocation(), ssr.getNamespace(), ssr.getVersion()));
        }

        // Description
        //
        IDescription sdsc = pl.getDescription();
        if (sdsc != null) {
            npl.setDescription(new Description(sdsc.getText(), sdsc.getLanguage()));
        }

        // Properties
        //
        Collection<IProperty> sProps = pl.getProperties();
        if (sProps != null) {
            for (IProperty p : sProps) {
                npl.addProperty(new Property(p.getName(), p.getValue(), p.getType()));
            }
        }

        return npl;
    }

    /*
     * Helper method to save all the entity objects related to the {@link UserMessage} entity object.
     * Currently this are only the TradingPartners objects for sender and receiver.
     */
    private static void persistRelatedObjects(UserMessage um, EntityManager em) throws DatabaseException {
        em.persist((TradingPartner) um.getSender());
        em.persist((TradingPartner) um.getReceiver());
    }

    /*
     * Helper method to load all information from database for a given <code>MessageUnit</code>
     * entity.
     * This is needed because the entity object are only managed for a short period and information
     * from related entity objects must be loaded before the managed state ends.
     */
    private static void refreshMU(MessageUnit mu, EntityManager em) {

        // Determine type of message unit
        if (mu instanceof UserMessage) {
            refreshUserMsg((UserMessage) mu, em);
        } else if (mu instanceof ErrorMessage) {
            refreshErrorMsg((ErrorMessage) mu, em);
        }
    }

    /**
     * Helper method to ensure that all information for a specific {@link ErrorMessage} entity object is loaded so the
     * object can be safely detached from the entity manager.
     * <p>
     * For the UserMessage pre-loading all information consists of getting the sender and receiver, payload info and
     * message properties. All other information is already loaded as field or embedded objects.
     *
     * @param um The {@link UserMessage} that must completely loaded
     * @param em The {@link EntityManager} to use for loading data*
     */
    private static void refreshUserMsg(UserMessage um, EntityManager em) {
        // Get the trading partner info
        um.getSender();
        um.getReceiver();

        // Get the message properties
        for (IProperty p : um.getMessageProperties()) {
            p.getName();
        }

        // Get payload info
        for (IPayload pl : um.getPayloads()) {
            for (IProperty p : pl.getProperties()) {
                p.getName();
            }
        }

    }

    /**
     * Helper method to ensure that all information for a specific {@link ErrorMessage} entity object is loaded so the
     * object can be safely detached from the entity manager.
     *
     * @param err The {@link ErrorMessage} that must completely loaded
     * @param em The {@link EntityManager} to use for loading data
     */
    private static void refreshErrorMsg(ErrorMessage err, EntityManager em) {
        // Ensure the collection of contained errors is loaded
        err.getErrors().size();
    }
}

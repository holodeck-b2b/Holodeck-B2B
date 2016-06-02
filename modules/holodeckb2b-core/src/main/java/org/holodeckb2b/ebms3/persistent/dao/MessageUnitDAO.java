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
package org.holodeckb2b.ebms3.persistent.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.exceptions.DuplicateMessageIdError;
import org.holodeckb2b.common.util.MessageIdGenerator;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.errors.OtherContentError;
import org.holodeckb2b.ebms3.persistency.entities.AgreementReference;
import org.holodeckb2b.ebms3.persistency.entities.CollaborationInfo;
import org.holodeckb2b.ebms3.persistency.entities.Description;
import org.holodeckb2b.ebms3.persistency.entities.EbmsError;
import org.holodeckb2b.ebms3.persistency.entities.ErrorMessage;
import org.holodeckb2b.ebms3.persistency.entities.MessageUnit;
import org.holodeckb2b.ebms3.persistency.entities.PartyId;
import org.holodeckb2b.ebms3.persistency.entities.Payload;
import org.holodeckb2b.ebms3.persistency.entities.ProcessingState;
import org.holodeckb2b.ebms3.persistency.entities.Property;
import org.holodeckb2b.ebms3.persistency.entities.PullRequest;
import org.holodeckb2b.ebms3.persistency.entities.Receipt;
import org.holodeckb2b.ebms3.persistency.entities.SchemaReference;
import org.holodeckb2b.ebms3.persistency.entities.Service;
import org.holodeckb2b.ebms3.persistency.entities.TradingPartner;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ISchemaReference;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IPayload.Containment;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * Is the data access object for {@link MessageUnit} objects that manages all database operations. All other classes
 * must use the methods of this class whenever message meta-data needs to be saved to the database.
 * <p>The methods of this class use {@link EntityProxy} objects as parameters and in results. This is done to allow 
 * one object in the {@link MessageContext} when processing messages even if the actual JPA entity object changes. In 
 * the documentation we often just refer to the entity object directly but it will always be wrapped in a <code>
 * EntityProxy</code> object.
 * <p>Note that this means that a <b>side effect</b> of the methods that update the database the entity object enclosed 
 * in the provided {@link EntityProxy} is replaced by the update version when the update is successful.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageUnitDAO {

    /**
     * Creates and stores a new outgoing User Message message unit in the database. Because this is an outgoing message
     * it must have a message id and creation timestamp. If not supplied in the meta-data both will be created now. Also
     * Holodeck B2B needs to know the P-Mode that defines how this message should be processed. 
     * <p>
     * The {@link ProcessingState} of the new message unit will be set to {@link ProcessingStates#SUBMITTED} to indicate
     * that is a new message.
     *
     * @param usrMsgMetadata        The meta data on the User Message
     * @param pmodeId   The id of the P-Mode that defines the processing of the message unit
     * @return          A {@link EntityProxy} to the newly created user message if it could be created and stored in the 
     *                  database
     * @throws DuplicateMessageIdError  If the message data includes a message id which already exists in the message 
     *                                  database for an outgoing message
     * @throws DatabaseException        If an error occurs when saving the new message unit to the database.
     */
    public static EntityProxy<UserMessage> createOutgoingUserMessage(IUserMessage usrMsgMetadata, String pmodeId) 
                                                        throws DatabaseException {
        // A submitted user message may already contain a message id, check that it is unique 
        String msgId = usrMsgMetadata.getMessageId();
        if (!Utils.isNullOrEmpty(msgId)) {
            // Ensure that message id are unique, we only check for messages sent by this MSH as these are the only
            // message under control 
            // @todo (L): This query could be optimized to check only for existence of the id and not load any MUs
            if (getSentMessageUnitWithId(msgId) != null)
                // Message id already exists for an outgoing message, raise error
                throw new DuplicateMessageIdError(msgId);
        } else 
            // No message id specified, generate a unique one now
            msgId = MessageIdGenerator.createMessageId();
        
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Open transaction to ensure integrity
            em.getTransaction().begin();
            UserMessage persistentMU = new UserMessage();
            persistentMU.setDirection(MessageUnit.Direction.OUT);

            // copyFromMetadata data and create related entity objects 
            copyFromMetadata(usrMsgMetadata, persistentMU, em); 

            // Set the message id and timestamp when not already done so
            persistentMU.setMessageId(msgId);
            Date timeStamp = persistentMU.getTimestamp();
            persistentMU.setTimestamp((timeStamp == null) ? new Date() : timeStamp);

            // Set P-Mode id
            persistentMU.setPMode(pmodeId);

            // Set state to SUBMITTED
            ProcessingState procstate = new ProcessingState(ProcessingStates.SUBMITTED);
            persistentMU.setProcessingState(procstate);

            // Persist the new User Message MessageUnit
            em.persist(persistentMU);
            // Commit changes to DB
            em.getTransaction().commit();

            return new EntityProxy<>(persistentMU);
        } catch (Exception e) {
            // Rollback the transaction and rethrow the exception packaged in a DatabaseException
            em.getTransaction().rollback();
            throw new DatabaseException("The User Message could not be saved in the database!", e);
        } finally {
            em.close();
        }
    }

    /**
     * Creates and stores a new Error Signal message unit for the given set of errors that where generated for a 
     * received message unit. 
     * <p>The {@link ProcessingState} of the new error message unit will be set to {@link ProcessingStates#CREATED} or
     * {@link ProcessingStates#PROCESSING} to indicate an error that has to be processed later or an error that is
     * directly processed as a response to the message in error. This is indicated by the <code>asResponse</code>
     * parameter.
     *
     * @param errors        The <code>Collection</code> of {@link EbmsError}s to include in the new Error Signal 
     *                      message unit
     *                      @todo (L): Should be collection of IEbmsError objects not the entity objects
     * @param refToMsgId    The message id of the message unit in error. May be <code>null</code> if there is no related
     *                      message unit.
     * @param pmodeId       The id of the PMode that defines the processing of the new Error Signal. May be 
     *                      <code>null</code> if the P-Mode is not known.
     * @param addSOAPFault  boolean indicating whether the Error signal should be combined with a SOAP Fault
     * @param asResponse    boolean indicating whether the error should be reported directly as a response
     * @return              A new {@link EntityProxy} object containing the new Error signal message unit if it could be 
     *                      created and stored in the database
     * @throws DatabaseException If an error occurs when saving the new message unit to the database
     */
    public static EntityProxy<ErrorMessage> createOutgoingErrorMessageUnit(Collection<EbmsError> errors, 
                                                              String refToMsgId, 
                                                              String pmodeId,
                                                              boolean addSOAPFault,
                                                              boolean asResponse) throws DatabaseException {
        // An Error Signal MUST contain at least one error
        if (Utils.isNullOrEmpty(errors))
            throw new DatabaseException("Error Signal must contain at least one error!");            
        
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Open transaction to ensure integrity
            em.getTransaction().begin();
            ErrorMessage persistentErrorMU = new ErrorMessage();

            // Generate a message id and timestamp for the new error message unit
            persistentErrorMU.setMessageId(MessageIdGenerator.createMessageId());
            persistentErrorMU.setTimestamp(new Date());
            persistentErrorMU.setDirection(MessageUnit.Direction.OUT);
            
            // Set reference to message in error if available
            if (refToMsgId != null && !refToMsgId.isEmpty()) {
                persistentErrorMU.setRefToMessageId(refToMsgId);
            }

            // Set P-Mode id
            if (!Utils.isNullOrEmpty(pmodeId)) {
                persistentErrorMU.setPMode(pmodeId);
            }

            // Add the errors to it
            for (EbmsError e : errors) {
                persistentErrorMU.addError(e);
            }
            
            // Set indicator if SOAP Fault should be added
            persistentErrorMU.setAddSOAPFault(addSOAPFault);
            
            // Set state to CREATED
            ProcessingState procstate = new ProcessingState(ProcessingStates.CREATED);
            persistentErrorMU.setProcessingState(procstate);

            if (asResponse) {
                // If error is reported as response, directly set state to processing
                procstate = new ProcessingState(ProcessingStates.PROCESSING);
            } else {
                // Otherwise it is ready to push
                procstate = new ProcessingState(ProcessingStates.READY_TO_PUSH);
            }
            persistentErrorMU.setProcessingState(procstate);

            // Persist the new message unit
            em.persist(persistentErrorMU);
            // Commit changes to DB
            em.getTransaction().commit();

            return new EntityProxy<>(persistentErrorMU);
        } catch (Exception e) {
            // Rollback the transaction and rethrow the exception packaged in a DatabaseException
            em.getTransaction().rollback();
            throw new DatabaseException("The Error Signal could not be saved in the database!", e);
        } finally {
            em.close();
        }
    }
    
    /**
     * Emergency method to create a new Error Signal message unit without storing it in the database. This method must
     * only be called when the {@link #createOutgoingErrorMessageUnit(java.util.Collection, java.lang.String, java.lang.String, boolean, boolean)}
     * fails. It allows to sent an Error Signal back to the other MSH even if database is unavailable. To make sure the
     * database is really not available this method will always try to save the error message in the database.
     * 
     * @param errMsg    The error message as an {@link OtherContentError}
     * @return          The <code>EntityProxy</i> for the new Error Signal message unit. Although it is an 
     *                  <code>EntityProxy</code> it is not stored in the database!
     */
    public static EntityProxy<ErrorMessage> createTransientOtherError(OtherContentError errMsg) {
        // When no error message, nothing to do
        if (errMsg == null)
            return null;
        
        // Just to make sure, first try again to save the error to the database
        EntityProxy<ErrorMessage> resultMU = null;
        try {
            resultMU = 
                createOutgoingErrorMessageUnit(Collections.singletonList((EbmsError) errMsg) , null, null, true, true);
        } catch (DatabaseException dbe) {
            // Okay, we really can't save it to the database
            ErrorMessage transientErrorMU = new ErrorMessage();
            // Generate a message id and timestamp for the new error message unit
            transientErrorMU.setMessageId(MessageIdGenerator.createMessageId());
            transientErrorMU.setTimestamp(new Date());
            transientErrorMU.setDirection(MessageUnit.Direction.OUT);            
            // Add the error to it
            transientErrorMU.addError(errMsg);            
            // Set indicator to include SOAP Fault 
            transientErrorMU.setAddSOAPFault(true);
            // Create entityproxy
            resultMU = new EntityProxy<>(transientErrorMU);
        }
        
        return resultMU;
    }

    /**
     * Creates and stores a new Pull Request Signal message unit.
     * <p>Because Pull Request messages are only created when they are sent, the processing state of the new pull 
     * request message unit is set to {@link ProcessingStates#PROCESSING} to indicate that it is processed directly.
     *
     * @param pmodeId   The id of the PMode that defines the processing of the new Pull Request signal.
     * @param mpc       The MPC that the pull operation will apply to. If not specified the <i>default MPC</i> will be
     *                  used for the new Pull Request
     * @return          A new {@link EntityProxy} object for the new Pull Request signal message unit if it could be 
     *                  created and stored in the database
     * @throws DatabaseException If an error occurs when saving the new message unit to the database
     */
    public static EntityProxy<PullRequest> createOutgoingPullRequest(String pmodeId, String mpc) 
                                                                                        throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Open transaction to ensure integrity
            em.getTransaction().begin();
            PullRequest persistentPullReqMU = new PullRequest();
            
            // Generate a message id and timestamp for the new error message unit
            persistentPullReqMU.setMessageId(MessageIdGenerator.createMessageId());
            persistentPullReqMU.setTimestamp(new Date());
            persistentPullReqMU.setDirection(MessageUnit.Direction.OUT);
            // Set P-Mode id
            persistentPullReqMU.setPMode(pmodeId);
            // Set MPC, use default if not provided
            if (!Utils.isNullOrEmpty(mpc)) {
                persistentPullReqMU.setMPC(mpc);
            } else {
                persistentPullReqMU.setMPC(EbMSConstants.DEFAULT_MPC);
            }

            // Add CREATED to signal PullRequest was made
            ProcessingState procstate = new ProcessingState(ProcessingStates.CREATED);
            persistentPullReqMU.setProcessingState(procstate);
            // and immediately change state to PROCESSING to indicate it is being send
            procstate = new ProcessingState(ProcessingStates.PROCESSING);
            persistentPullReqMU.setProcessingState(procstate);

            // Persist the new message unit
            em.persist(persistentPullReqMU);
            // Commit changes to DB
            em.getTransaction().commit();

            return new EntityProxy<>(persistentPullReqMU);
        } catch (Exception e) {
            // Rollback the transaction and rethrow the exception packaged in a DatabaseException
            em.getTransaction().rollback();
            throw new DatabaseException("The Pull Request Signal could not be saved in the database!", e);
        } finally {
            em.close();
        }
    }

    /**
     * Stores the given {@link Receipt} entity object representing a new receipt signal message unit to the database.
     * <p>The processing state of the new receipt message unit will be set to {@link ProcessingStates#CREATED} or
     * {@link ProcessingStates#PROCESSING} to indicate the Receipt is to be processed later respectively directly as a
     * response to the received message. This is indicated by the <code>asResponse</code> parameter.
     *
     * @param receipt       The {@link Receipt} to store
     * @param asResponse    Indication whether Receipt will be reported as a response
     * @return A new {@link EntityProxy} for the stored Receipt entity object
     * @throws DatabaseException If an error occurs when saving the new message unit to the database
     */
    @Deprecated //@todo (H): Creating a Receipt should use same pattern as other message units!
    public static EntityProxy<Receipt> storeOutgoingReceiptMessageUnit(Receipt receipt, boolean asResponse) 
                                                                                            throws DatabaseException {        
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Open transaction to ensure integrity
            em.getTransaction().begin();
            // Generate a message id and timestamp for the new error message unit if not already done
            if (Utils.isNullOrEmpty(receipt.getMessageId())) {
                receipt.setMessageId(MessageIdGenerator.createMessageId());
            }
            if (receipt.getTimestamp() == null) {
                receipt.setTimestamp(new Date());
            }
            receipt.setDirection(MessageUnit.Direction.OUT);
            
            // Set state to CREATED
            ProcessingState procstate = new ProcessingState(ProcessingStates.CREATED);
            receipt.setProcessingState(procstate);
            // If Receipt is reported as response, directly set state to processing
            if (asResponse)
                receipt.setProcessingState(new ProcessingState(ProcessingStates.PROCESSING));
            
            // Persist the new message unit
            em.persist(receipt);
            // Commit changes to DB
            em.getTransaction().commit();

            return new EntityProxy<>(receipt);
        } catch (Exception e) {
            // Rollback the transaction and rethrow the exception packaged in a DatabaseException
            em.getTransaction().rollback();
            throw new DatabaseException("The Receipt Signal could not be saved in the database!", e);
        } finally {
            em.close();
        }
    }

    /**
     * Stores information of a received message unit in the database so it can be processed. The information is stored
     * as-is to correctly reflect the received info, there are no checks on constraints set by ebMS or AS4 specs, like 
     * the uniqueness of the messageId.
     * <p>The processing state of the stored message unit will be set to {@link ProcessingStates#RECEIVED}.
     *
     * @param mu    The information on the received message unit as a {@link MessageUnit} entity object.
     * @return      The {@link EntityProxy} for the JPA entity object as it is stored in the database
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static <T extends MessageUnit> EntityProxy<T> storeReceivedMessageUnit(final T mu) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            mu.setDirection(MessageUnit.Direction.IN);
            mu.setProcessingState(new ProcessingState(ProcessingStates.RECEIVED));

            // If the message unit is a UserMessage object, the related TradingPartner entity objects for the Sender
            // and Receiver must be persisted first
            if (mu instanceof UserMessage) {
                em.persist(((UserMessage) mu).getSender());
                em.persist(((UserMessage) mu).getReceiver());
            }
            em.persist(mu);
            // Commit changes to DB
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new DatabaseException("The received message unit could not be stored in the database!", e);
        } finally {
            em.close();
        }
        return new EntityProxy<>(mu);
    }

    /**
     * Updates the meta-data information for an already stored User Message message unit. 
     *
     * @param um    The {@link EntityProxy} to the {@link UserMessage} entity object that needs to be updated
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static void updateMessageUnitInfo(EntityProxy<UserMessage> um) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            um.entity = em.merge(um.entity);            
            em.getTransaction().commit();
        } catch (Exception e) {
            // Updating the meta-data info failed, rollback and rethrow exception (packaged as DatabaseException)
            em.getTransaction().rollback();
            throw new DatabaseException("Update of user message information failed!", e);
        } finally {
            em.close();
        }
    }

    /**
     * Sets the P-Mode that defines how this message unit should be processed. The MessageUnit entity obect only uses 
     * the P-Mode Id, so only this is saved to the database.
     *
     * @param mu    The {@link EntityProxy} for the {@link MessageUnit} which P-Mode should be set
     * @param pmode The {@link IPMode} that defines how the message unit must be processed
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static void setPMode(EntityProxy mu, IPMode pmode) throws DatabaseException {
        setPModeId(mu, pmode.getId());
    }

    /**
     * Sets the P-Mode Id that defines how this message unit should be processed.
     *
     * @param mu        The {@link EntityProxy} for the {@link MessageUnit} which P-Mode should be set
     * @param pmodeId   The P-Mode id of the P-Mode that defines how the message unit must be processed
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static void setPModeId(EntityProxy mu, String pmodeId) throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            MessageUnit actualMU = refreshMessageUnit(mu.entity, em);
            actualMU.setPMode(pmodeId);
            em.merge(actualMU);
            em.getTransaction().commit();
            
            // Update the EntityProxy with new entity
            mu.entity = actualMU;
        } catch (Exception e) {
            // The update somehow failed: rollback and rethrow
            em.getTransaction().rollback();
            throw new DatabaseException("Setting P-Mode.Id for MessageUnit failed!", e);
        } finally {
            em.close();
        }
    }
    
    /**
     * Changes the processing state of the given message unit to <i>PROCESSING</i> to indicate that the message is being 
     * processed. To prevent that a message unit is processed multiple times in parallel the state is only changed if 
     * the current state equals the state of the entity object enclosed in the given <code>EntityProxy</code>.
     *
     * @param mu    The {@link EntityProxy} for the message unit that is going to be processed
     * @return      <code>true</code> when the processing state was changed to <i>PROCESSING</i>,<br>
     *              <code>false</code> otherwise
     * @throws DatabaseException When an the state can be changed but an error occurs while executing the update
     */
    public static <T extends MessageUnit> boolean startProcessingMessageUnit(final EntityProxy<T> mu)
                                                                    throws DatabaseException {
        return setProcessingState(mu, ProcessingStates.PROCESSING, mu.entity.getCurrentProcessingState().getName());
    }    

    /**
     * Changes the processing state of the given message unit to <i>OUT_FOR_DELIVERY</i> to indicate that the message 
     * unit is being delivered to the <i>Consumer</i> business application. To prevent that a single message unit is
     * delivered twice in parallel the state is only changed if its current state (as indicated in the entity object 
     * enclosed in the <code>EntityProxy</code>) equals <i>READY_FOR_DELIVERY</i>
     *
     * @param mu    The {@link EntityProxy} to the message unit going to be delivered
     * @return      <code>true</code> if the processing state could be changed to <i>OUT_FOR_DELIVERY</i>, <br>
     *              <code>false</code> otherwise.
     * @throws DatabaseException When an the state can be changed but an error occurs while executing the update
     */
    public static boolean startDeliveryOfMessageUnit(EntityProxy mu) throws DatabaseException {
        return setProcessingState(mu, ProcessingStates.OUT_FOR_DELIVERY, ProcessingStates.READY_FOR_DELIVERY);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#READY_TO_PUSH} to indicate the message
     * unit is ready to be pushed to the receiving MSH.
     *
     * @param mu    The {@link EntityProxy} to the message unit that is ready to be pushed
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setReadyToPush(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.READY_TO_PUSH, null);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#AWAITING_PULL} to indicate the message
     * unit is ready to be pulled by the receiving MSH.
     *
     * @param mu    The {@link EntityProxy} to the message unit that is ready to be pulled
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setWaitForPull(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.AWAITING_PULL, null);
    }

    /**
     * Changes the processing state of a user message unit to {@link ProcessingStates#AWAITING_RECEIPT} to indicate the
     * message unit is sent and now waiting for a Receipt signal.
     *
     * @param um    The {@link EntityProxy} to the {@link UserMessage} that is waiting for a receipt
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setWaitForReceipt(EntityProxy<UserMessage> um) throws DatabaseException {
        setProcessingState(um, ProcessingStates.AWAITING_RECEIPT, null);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#DELIVERED} to indicate the message unit
     * is successfully delivered.
     *
     * @param mu T  The {@link EntityProxy} to the message unit that is delivered successfully
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setDelivered(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.DELIVERED, null);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#FAILURE} to indicate that the message
     * unit could not be processed succesfully.
     *
     * @param mu    The {@link EntityProxy} to the message unit that failed to process successfully
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setFailed(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.FAILURE, null);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#READY_FOR_DELIVERY} to indicate that
     * the message unit is now ready for delivery to the business application.
     *
     * @param mu    The {@link EntityProxy} to the message unit that is a duplicate
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setReadyForDelivery(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.READY_FOR_DELIVERY, null);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#DUPLICATE} to indicate the message unit
     * is a duplicate of an already processed unit.
     *
     * @param mu    The {@link EntityProxy} to the {@link UserMessage} that is a duplicate
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setDuplicate(EntityProxy<UserMessage> mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.DUPLICATE, null);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#DELIVERY_FAILED} to indicate the
     * message unit could not be delivered to the business application.
     *
     * @param mu The {@link MessageUnit} that could not be delivered
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setDeliveryFailure(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.DELIVERY_FAILED, null);
    }

    /**
     * Changes the processing state of a signal message unit to {@link ProcessingStates#DONE} to indicate the signal
     * message unit is successfully processed.
     *
     * @param mu The {@link EntityProxy} to the message unit that is successfully processed
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setDone(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.DONE, null);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#PROC_WITH_WARNING} to indicate that
     * the message unit was processed but there was an Error reported with severity <i>warning</i>.
     *
     * @param mu The {@link EntityProxy} to the message unit for which the Error was reported
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setWarning(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.PROC_WITH_WARNING, null);
    }
    
    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#TRANSPORT_FAILURE} to indicate that
     * there was a problem sending the message unit out.
     *
     * @param mu The {@link EntityProxy} to the message unit that could not be sent out successfully
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setTransportFailure(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.TRANSPORT_FAILURE, null);
    }

    /**
     * Changes the processing state of a message unit to {@link ProcessingStates#SENDING} to indicate that the message
     * unit is current being sent out to the other MSH
     *
     * @param mu    The {@link EntityProxy} to the message unit that is being sent out
     * @throws DatabaseException When the processing state can not be updated in the database
     */
    public static void setSending(EntityProxy mu) throws DatabaseException {
        setProcessingState(mu, ProcessingStates.SENDING, null);
    }
    
    /**
     * Helper method to change the processing state of the given message unit to the given state. Sometimes it is 
     * required that the message unit is in a certain current state before changing the processing state, for example
     * to prevent parallel processing, so this can be specified.
     * 
     * @param mu        The {@link EntityProxy} to the message unit going to be delivered
     * @param newState  The new processing state for the message unit
     * @param curState  If change should only be done when message unit is in a certain state, the required current 
     *                  state, <code>null</code> if change should be always executed
     * @return          <code>true</code> if the processing state could be changed to <i>OUT_FOR_DELIVERY</i>, <br>
     *                  <code>false</code> otherwise.
     * @throws DatabaseException When an the state can be changed but an error occurs while executing the update
     */
    private static <T extends MessageUnit> boolean setProcessingState(final EntityProxy<T> mu, 
                                                                      final String newState,
                                                                      final String curState) 
                                                                                            throws DatabaseException {
        EntityManager em = JPAUtil.getEntityManager();
        em.getTransaction().begin();
        try {
            T actual = refreshMessageUnit(mu.entity, em);
            
            if (Utils.isNullOrEmpty(curState) 
               || curState.equals(actual.getCurrentProcessingState().getName())) {
                actual.setProcessingState(new ProcessingState(newState));
                em.flush(); //this will trigger the OptimisticLockingException
                em.getTransaction().commit();
                
                mu.entity = (T) actual;
                return true;
            } else
                // Current states differ, nothing changed!
                return false;
        } catch (OptimisticLockException alreadyChanged) { 
            // During transaction the message unit was already updated, so state can not be changed. 
            // Rollback and return false
            em.getTransaction().rollback();
            return false;            
        } catch (Exception e) {
            // Another error occured when updating the processing state. Rollback and rethrow as DatabaseException
            em.getTransaction().rollback();
            throw new DatabaseException("An error occurred while updating the processing state!", e);            
        }finally {
            em.close();
        }
    }
    
    /**
     * Checks whether a {@link UserMessage} with the given <code>MessageId</code> has already been delivered, i.e. the
     * <i>current</i> processing state is {@link ProcessingStates#DELIVERED}.
     *
     * @param messageId   The <code>MessageId</code> to check delivery for
     * @return            <code>true</code> if a {@link UserMessage} with <code>messageId</code> and
     *                    {@link UserMessage#getCurrentProcessingState()} == {@link ProcessingStates#DELIVERED} exists, 
     *                    <br><code>false</code> otherwise.
     * @throws DatabaseException If an error occurs when executing this query
     */
    public static boolean isUserMsgDelivered(String messageId) throws DatabaseException {
        boolean result = false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            result = "true".equals(em.createNamedQuery("UserMessage.isDelivered",
                    String.class).setParameter("msgId", messageId)
                    .getSingleResult()
            );
        } catch (NoResultException nothingFound) {
            result = false;
        } catch (Exception e) {
            // Something went wrong during query execution
            throw new DatabaseException("Could not execute query", e);
        } finally {
            em.close();
        }
        return result;
    }

    /**
     * Retrieves all {@link MessageUnit}s of the specified type and that are in one of the given states. 
     * <p>
     * <b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is going
     * to be processed it must be loaded completely.
     *
     * @param type      The type of message units to retrieve specified by their Class
     * @param states    Array of processing state [names] that the message units to retrieve should be in
     * @return          A list of {@link EntityProxy} for objects of class <code>type</code> representing the message 
     *                  units that are in one of the given states,<br>or <code>null</code> when no such message units 
     *                  are found.
     * @throws DatabaseException When a problem occurs during the retrieval of the message units
     */
    public static <T extends MessageUnit> List<EntityProxy<T>> getMessageUnitsInState(Class<T> type, String[] states) 
                                                                            throws DatabaseException {
        List<T> result = null;
        EntityManager em = JPAUtil.getEntityManager();

        ArrayList<String> pStates = new ArrayList<>(states.length);
        for(String s : states)
            pStates.add(s);
        
        String queryString = "SELECT mu " +
                             "FROM " + type.getSimpleName() + " mu JOIN FETCH mu.states s1 " +
                             "WHERE s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM mu.states s2) " +
                             "AND s1.NAME IN :states";
        try {  
            result = em.createQuery(queryString, type)
                                    .setParameter("states", pStates)
                                    .getResultList();          
        } catch (NoResultException nothingFound) {
            result = null;
        } catch (Exception e) {
            // Something went wrong during query execution
            throw new DatabaseException("Could not execute query", e);
        } finally {
            em.close();
        }        
        
        return createProxyResultList(result);
    }

    /**
     * Gets the number of times the <i>User Message</i> message unit has already been sent to the receiver without 
     * getting a receipt.<br>
     * This method counts the number of times the message was in the {@link ProcessingStates#SENDING} state. This means
     * that also failed sent attempts, i.e. where a transfort failure occurred, count for the result.
     *
     * @param um    The {@link UserMessage} to get the number of transmission for
     * @return      The number of times the {@link UserMessage} was already sent out.
     * @throws DatabaseException If an error occurs when retrieving the number of transmissions
     */
    public static int getNumberOfTransmissions(UserMessage um) throws DatabaseException {
        Long result = null;        
        EntityManager em = JPAUtil.getEntityManager();
        try {
            result = em.createNamedQuery("UserMessage.numOfTransmits", Long.class)
                       .setParameter("msgId", um.getMessageId()).getSingleResult();
        } catch (Exception e) {
            // Something went wrong during query execution
            throw new DatabaseException("Could not execute query", e);
        } finally {
            em.close();
        }
        return result.intValue();
    }

    /**
     * Retrieves all received Message Units with the given <code>MessageId</code>. Although messageIds should be 
     * unique there can exist multiple <code>MessageUnits</code> with the same messageId due to resending (and because
     * other MSH or business applications may not conform to this constraint).
     * <p><b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is 
     * going to be processed it must be loaded completely.
     *
     * @param messageId     The messageId of the message units to retrieve
     * @return              The list of received {@link MessageUnit}s with the given message id or,<br>
     *                      <code>null</code> if no received message units with this message if where found
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static  List<EntityProxy<MessageUnit>> getReceivedMessageUnitsWithId(String messageId) 
                                                                                throws DatabaseException {
        return getMessageUnitsWithIdInDirection(messageId, MessageUnit.Direction.IN);
    }    

    /**
     * Retrieves the sent Message Unit with the given <code>MessageId</code>. 
     * <p><b>NOTE:</b> The returned entity object is not completely loaded! Before processing it, it must be loaded 
     * completely.
     *
     * @param messageId The messageId of the message units to retrieve
     * @return          A new {@link EntityProxy} to the {@link MessageUnit} object with the given message id, or<br>
     *                  <code>null</code> when no sent message unit exists with the given id
     * @throws DatabaseException If an error occurs when saving the object to the database
     */
    public static EntityProxy<MessageUnit> getSentMessageUnitWithId(String messageId) throws DatabaseException {
        List<EntityProxy<MessageUnit>> msgsWithId = getMessageUnitsWithIdInDirection(messageId, MessageUnit.Direction.OUT);   
        if (!Utils.isNullOrEmpty(msgsWithId)) 
            return msgsWithId.get(0);
        else
            return null;
    }
    
    /**
     * Retrieves all Message Units with the given <code>MessageId</code> that flow in the given direction. Although 
     * messageIds should be unique there can exist multiple <code>MessageUnits</code> with the same messageId 
     * due to resending (and because other MSH or business applications may not conform to this constraint).
     * <p><b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is 
     * going to be processed it must be loaded completely.
     *
     * @param messageId The messageId of the message units to retrieve
     * @param direction The direction the message units should be in (IN = receiving, OUT=sending)
     * @return          List of {@link EntityProxy} objects to the {@link MessageUnit} objects with the given message id 
     *                  and flowing in the given direction, or<br><code>null</code> when no such message units are found
     * @throws DatabaseException If an error occurs while executing the query
     */
    protected static List<EntityProxy<MessageUnit>> getMessageUnitsWithIdInDirection(String messageId, 
                                                                                     MessageUnit.Direction direction)
                                                                            throws DatabaseException {
        List<MessageUnit> result = null;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            result = em.createNamedQuery("MessageUnit.findWithMessageIdInDirection", MessageUnit.class)
                    .setParameter("msgId", messageId)
                    .setParameter("direction", direction)
                    .getResultList();
        } catch (NoResultException nothingFound) {
            result = null;
        } catch (Exception e) {
            // Something went wrong during query execution
            throw new DatabaseException("Could not execute query", e);
        } finally {
            em.close();
        }
        return createProxyResultList(result);
    }

    /**
     * Retrieves all MessageUnits of the specified type and that are in the given state and which processing is defined 
     * by one of the given P-Modes. The message units are ordered ascending on the timestamp of the current processing 
     * state, i.e. the messages that are the longest in the current state are at the front of the list.
     * <p><b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is 
     * going to be processed it must be loaded completely.
     *
     * @param type      The type of message units to retrieve specified by their Class
     * @param pmodes    List of P-Modes.
     * @param state     The name of the processing state the message units to retrieve should be in
     * @return          The ordered list of {@link EntityProxy} objects for the message unit objects of the specified 
     *                  type and which are in the specified processing state and have their processing defined by one of 
     *                  the specified P-Modes, or<br>
     *                  <code>null</code> if no such message units where found
     * @throws DatabaseException When an error occurs while executing the query
     */
    public static <T extends MessageUnit> List<EntityProxy<T>> getMessageUnitsForPModesInState(Class<T> type, 
                                                                                  Collection<IPMode> pmodes, 
                                                                                  String state) 
                                                                            throws DatabaseException {
        // The query parameter for the PModes is a list op P-Mode Ids, so convert list of PModes to list of strings
        Collection<String> pmodeIds = new ArrayList<String>();
        for (IPMode pmode : pmodes) {
            pmodeIds.add(pmode.getId());
        }
        return getMessageUnitsForPModeIdsInState(type, pmodeIds, state);
    }

    /**
     * Retrieves all MessageUnits of the specified type and that are in the given state and which processing is defined 
     * by a P-Mode with one of the given P-Mode ids. The message units are ordered ascending on the timestamp of the 
     * current processing state, i.e. the messages that are the longest in the current state are at the front of the 
     * list.
     * <p><b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is 
     * going to be processed it must be loaded completely.
     *
     * @param type      The type of message units to retrieve specified by their Class
     * @param pmodeIds  List of P-Mode ids
     * @param state     The name of the processing state the message units to retrieve should be in
     * @return          The ordered list of {@link EntityProxy} objects for the message unit objects of the specified 
     *                  type and which are in the specified processing state and have their processing defined by a 
     *                  P-Mode with one of the specified ids, 
     *                  or<br> <code>null</code> if no such message units where found
     * @throws DatabaseException When an error occurs while executing the query

     */
    public static <T extends MessageUnit> List<EntityProxy<T>> getMessageUnitsForPModeIdsInState(Class<T> type, 
                                                                                    Collection<String> pmodeIds, 
                                                                                    String state) 
                                                                                throws DatabaseException {
        List<T> result = null;
        EntityManager em = JPAUtil.getEntityManager();

        String queryString = "SELECT mu " +
                             "FROM " + type.getSimpleName() + " mu JOIN FETCH mu.states s1 " +    
                             "WHERE mu.PMODE_ID IN :pmodes " +
                             "AND s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM mu.states s2) " +
                             "AND s1.NAME = :state " +
                             "ORDER BY s1.START";
        try {  
            result = em.createQuery(queryString, type)
                                    .setParameter("state", state)
                                    .setParameter("pmodes", pmodeIds)
                                    .getResultList();
        } catch (NoResultException nothingFound)  {
            result = null;
        } catch (Exception e) {
            // Something went wrong executing the query. Probably because wrong class was specified
            throw new DatabaseException("An error occurred while executing query to retreive message units!", e);
        } finally {
            em.close();
        }
        return createProxyResultList(result);
    }

    /**
     * Retrieves all Message Units of the specified type that are responses to the given message id, i.e. which 
     * <i>refToMessageId</i> equals the given message id.
     * <p><b>NOTE:</b> The entity objects in the resulting list are not completely loaded! Before a message unit is 
     * going to be processed it must be loaded completely.
     *
     * @param type          The type of message units to retrieve specified by their Class
     * @param refToMsgId    The message Id of the message the requested message units should be a response to.
     * @return  A list of {@link EntityProxy} objects to the entity objects representing the message units that are in 
     *          the given state
     * @throws DatabaseException
     */
    public static <T extends MessageUnit> List<EntityProxy<T>> getResponsesTo(Class<T> type, String refToMessageId) 
                                                                                        throws DatabaseException {
        List<T> result = null;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String queryName = type.getSimpleName() + ".findResponsesTo";
            result = em.createNamedQuery(queryName, type)
                    .setParameter("refToMsgId", refToMessageId)
                    .getResultList();
        } catch (NoResultException nothingFound)  {
            result = null;
        } catch (Exception e) {
            // Something went wrong executing the query. Probably because wrong class was specified
            throw new DatabaseException("An error occurred while executing query to retreive message units!", e);
        } finally {
            em.close();
        }
        return createProxyResultList(result);
    }    

    
    /**
     * Helper method to convert a result list of entity objects into a list of {@link EntityProxy} objects.
     * 
     * @param <T>       The type of {@link MessageUnit} objects in the result list
     * @param result    The original result consisting of the entity objects
     * @return          The new result list containing the proxies to the entity objects
     */
    private static <T extends MessageUnit> List<EntityProxy<T>> createProxyResultList(List<T> result) {
        if (Utils.isNullOrEmpty(result))
            return null;
        
        List<EntityProxy<T>> proxies = new ArrayList<EntityProxy<T>>(result.size());
        for(T e : result)
            proxies.add(new EntityProxy<>(e));
        return proxies;
    }

    /*
     * Helper method to copyFromMetadata information from an {@link IUserMessage} object to a 
     * {@link UserMessage} entity object 
     */
    private static void copyFromMetadata(IUserMessage src, UserMessage dest, EntityManager em) {
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
        dest.setSender(createTradingPartner(src.getSender(), em));
        dest.setReceiver(createTradingPartner(src.getReceiver(), em));

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
        if (!Utils.isNullOrEmpty(sPayloads)) {
            for (IPayload pl : sPayloads) {
                dest.addPayload(createPayload(pl));
            }
        }

        // Copy list of message properties
        //
        Collection<IProperty> smsgProps = src.getMessageProperties();
        if (!Utils.isNullOrEmpty(smsgProps)) {
            for (IProperty p : smsgProps) {
                dest.addMessageProperty(new Property(p.getName(), p.getValue(), p.getType()));
            }
        }

    }
    
    /**
     * Helper method to create a new {@link TradingPartner} entity object based on the provided meta-data. 
     * 
     * @param tp    Meta-data on the trading partner
     * @param em    The entity manager to use for accessing the database
     * @return      A new {@link TradingPartner} entity object containing the provided information
     */
    public static TradingPartner createTradingPartner(ITradingPartner tp, EntityManager em) {
        if (tp == null)
            return null; // nothing to create if no information given
        
        TradingPartner  ntp = new TradingPartner();
        
        // Copy info to the entity object
        //   Role
        ntp.setRole(tp.getRole());
        //   PartyIds
        for(IPartyId pid : tp.getPartyIds())
            ntp.addPartyId(new PartyId(pid.getId(), pid.getType()));
        
        return ntp;
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

    /**
     * Helper method to reload all information for a given {@link MessageUnit} from the database. Besides reloading the
     * entity object itself it will also trigger all <i>lazily</i> loaded relations.
     * 
     * @param mu    The message unit to reload from the database
     * @param em    The entity manager to use for accessing the database
     * @return      The completely reloaded message unit
     */
    private static <T extends MessageUnit> T refreshMessageUnit(final T mu, EntityManager em) {
        
        T actual = (T) em.find(mu.getClass(), mu.getOID());
        
        // Trigger lazily loaded relations 
        if (mu instanceof UserMessage) {
            refreshUserMsg((UserMessage) actual, em);
        } else if (mu instanceof ErrorMessage) {
            refreshErrorMsg((ErrorMessage) actual, em);
        }
        
        return actual;
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
        if (!Utils.isNullOrEmpty(um.getMessageProperties()))
            for (IProperty p : um.getMessageProperties()) 
                p.getName();            
        // Get payload info
        if (!Utils.isNullOrEmpty(um.getPayloads()))
            for (IPayload pl : um.getPayloads())
                for (IProperty p : pl.getProperties())
                    p.getName();            
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

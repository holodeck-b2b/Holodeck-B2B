/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency.managers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TemporalType;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.entities.MessageUnitEntity;
import org.holodeckb2b.persistency.jpa.ErrorMessage;
import org.holodeckb2b.persistency.jpa.MessageUnit;
import org.holodeckb2b.persistency.jpa.UserMessage;
import org.holodeckb2b.persistency.util.EntityManagerUtil;
import org.holodeckb2b.persistency.util.JPAEntityHelper;

/**
 * Is the default persistency provider's implementation of the {@link IQueryManager} interface.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class QueryManager implements IQueryManager {

    @Override
    public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsInState(
               Class<T> type, IMessageUnit.Direction direction, ProcessingState[] states) throws PersistenceException {
        List<T> jpaResult = null;
        final EntityManager em = EntityManagerUtil.getEntityManager();

        Class jpaEntityClass = JPAEntityHelper.determineJPAClass(type);
        final String queryString = "SELECT mu "
                                 + "FROM " + jpaEntityClass.getSimpleName()  + " mu JOIN FETCH mu.states s1 "
                                 + "WHERE mu.DIRECTION = :direction "
                                 + "AND s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM mu.states s2) "
                                 + "AND s1.STATE IN :states "
                                 + "ORDER BY mu.MU_TIMESTAMP";
        try {
            em.getTransaction().begin();
            jpaResult = em.createQuery(queryString, jpaEntityClass)
                                    .setParameter("direction", direction)
                                    .setParameter("states", Arrays.asList(states))
                                    .getResultList();
        } catch (final Exception e) {
            // Something went wrong during query execution
            throw new PersistenceException("Could not execute query \"getMessageUnitsInState\"", e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }

        return JPAEntityHelper.wrapInEntity(jpaResult);
    }

    @Override
    public Collection<IMessageUnitEntity> getMessageUnitsWithId(String messageId) throws PersistenceException {
        List<MessageUnit> jpaResult = null;
        final EntityManager em = EntityManagerUtil.getEntityManager();

        final String queryString = "SELECT mu "
                                 + "FROM MessageUnit mu "
                                 + "WHERE mu.MESSAGE_ID = :msgId "
                                 + "ORDER BY mu.MU_TIMESTAMP";
        try {
            em.getTransaction().begin();
            jpaResult = em.createQuery(queryString, MessageUnit.class)
                                        .setParameter("msgId", messageId)
                                        .getResultList();
        } catch (final Exception e) {
            // Something went wrong during query execution
            throw new PersistenceException("Could not execute query \"getMessageUnitsWithId\"", e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }

        return JPAEntityHelper.wrapInEntity(jpaResult);
    }

    @Override
    public Collection<IMessageUnitEntity> getMessageUnitsWithLastStateChangedBefore(Date maxLastChangeDate)
                                                                                        throws PersistenceException {
        List<MessageUnit> jpaResult = null;
        final EntityManager em = EntityManagerUtil.getEntityManager();

        final String queryString = "SELECT mu "
                                 + "FROM MessageUnit mu JOIN FETCH mu.states s1 "
                                 + "WHERE s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM mu.states s2) "
                                 + "AND   s1.START <= :beforeDate";
        try {
            em.getTransaction().begin();
            jpaResult = em.createQuery(queryString, MessageUnit.class)
                                        .setParameter("beforeDate", maxLastChangeDate, TemporalType.TIMESTAMP)
                                        .getResultList();
        } catch (final Exception e) {
            // Something went wrong during query execution
            throw new PersistenceException("Could not execute query \"getMessageUnitsWithLastStateChangedBefore\"", e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }

        return JPAEntityHelper.wrapInEntity(jpaResult);
    }

    @Override
    public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsForPModesInState(Class<T> type,
                                    Collection<String> pmodeIds, ProcessingState state) throws PersistenceException {
        List<T> jpaResult = null;
        final EntityManager em = EntityManagerUtil.getEntityManager();

        Class jpaEntityClass = JPAEntityHelper.determineJPAClass(type);
        final String queryString = "SELECT mu "
                                 + "FROM " + jpaEntityClass.getSimpleName()  + " mu JOIN FETCH mu.states s1 "
                                 + "WHERE mu.PMODE_ID IN :pmodeIds "
                                 + "AND s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM mu.states s2) "
                                 + "AND s1.STATE = :state "
                                 + "ORDER BY mu.MU_TIMESTAMP";
        try {
            em.getTransaction().begin();
            jpaResult = em.createQuery(queryString, jpaEntityClass)
                                    .setParameter("pmodeIds", pmodeIds)
                                    .setParameter("state", state)
                                    .getResultList();
        } catch (final Exception e) {
            // Something went wrong during query execution
            throw new PersistenceException("Could not execute query \"getMessageUnitsForPModesInState\"", e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }

        return JPAEntityHelper.wrapInEntity(jpaResult);
    }

    @Override
    public <V extends IMessageUnitEntity> void ensureCompletelyLoaded(V messageUnit) throws PersistenceException {
        // Check if already loaded, then nothing to do
        if (messageUnit.isLoadedCompletely())
            return;

        MessageUnitEntity providerEntityObject = (MessageUnitEntity) messageUnit;
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            final MessageUnit actual = em.find(MessageUnit.class, providerEntityObject.getOID());
            loadCompletely(actual);

            providerEntityObject.updateJPAObject(actual);
            providerEntityObject.setMetadataLoaded(true);
        } catch (final Exception e) {
            // Something went wrong during query execution
            throw new PersistenceException("Could not load the object from the database", e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }
    }

    public static void loadCompletely(MessageUnit jpaMessageUnit) {
        if (jpaMessageUnit instanceof IUserMessage) {
            // Ensure all meta-data of the User Message is loaded
            UserMessage userMsg = (UserMessage) jpaMessageUnit;
            // Get sender and receiver info
            userMsg.getSender(); userMsg.getReceiver();
            // Get the message properties
            if (!Utils.isNullOrEmpty(userMsg.getMessageProperties()))
                for (final IProperty p : userMsg.getMessageProperties())
                    p.getName();
            // Get payload info
            Utils.isNullOrEmpty(userMsg.getPayloads());
        } else if (jpaMessageUnit instanceof IErrorMessage) {
            // Ensure all meta-data of the Error Message is loaded by loading all individual error details
            Utils.isNullOrEmpty(((ErrorMessage) jpaMessageUnit).getErrors());
        } // else Other message units are already completely loaded, so nothing to do
    }

    @Override
    public int getNumberOfTransmissions(IUserMessageEntity userMessage) throws PersistenceException {
        int result = 0;
        final EntityManager em = EntityManagerUtil.getEntityManager();

        final String query = "SELECT COUNT(s1.STATE) "
                           + "FROM UserMessage um JOIN um.states s1 "
                           + "WHERE um.MESSAGE_ID = :msgId "
                           + "AND s1.STATE = :state";
        try {
            em.getTransaction().begin();
            result = em.createQuery(query, Long.class)
                                    .setParameter("msgId", userMessage.getMessageId())
                                    .setParameter("state", ProcessingState.SENDING)
                                    .getSingleResult().intValue();
        } catch (final NoResultException nothingFound) {
            result = 0;
        } catch (final Exception e) {
            // Something went wrong during query execution
            throw new PersistenceException("Could not execute query \"getNumberOfTransmissions\"", e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }
        return result;
    }

    @Override
    public boolean isAlreadyDelivered(String messageId) throws PersistenceException {
        boolean result = false;
        final EntityManager em = EntityManagerUtil.getEntityManager();

        final String query = "SELECT 'true' "
                           + "FROM UserMessage um JOIN um.states s1 "
                           + "WHERE um.DIRECTION = :direction AND um.MESSAGE_ID = :msgId "
                           + "AND s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM um.states s2) "
                           + "AND s1.STATE = :state";
        try {
            em.getTransaction().begin();
            result = "true".equals(em.createQuery(query)
                                     .setParameter("direction", IMessageUnit.Direction.IN)
                                     .setParameter("msgId", messageId)
                                     .setParameter("state", ProcessingState.DELIVERED)
                                     .getSingleResult());
        } catch (final NoResultException nothingFound) {
            result = false;
        } catch (final Exception e) {
            // Something went wrong during query execution
            throw new PersistenceException("Could not execute query \"isAlreadyDelivered\"", e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }
        return result;
    }
}

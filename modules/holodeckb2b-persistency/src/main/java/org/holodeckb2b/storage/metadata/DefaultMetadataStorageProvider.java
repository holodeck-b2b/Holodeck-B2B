/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.storage.metadata;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IPayloadEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.AlreadyChangedException;
import org.holodeckb2b.interfaces.storage.providers.IMetadataStorageProvider;
import org.holodeckb2b.interfaces.storage.providers.StorageException;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;
import org.holodeckb2b.storage.metadata.jpa.JPAEntityObject;
import org.holodeckb2b.storage.metadata.jpa.MessageUnit;
import org.holodeckb2b.storage.metadata.jpa.PayloadInfo;
import org.holodeckb2b.storage.metadata.jpa.UserMessage;

/**
 * Is the default implementation of the Holodeck B2B <i>Metadata Storage Provider</i>. This provider uses the Java
 * Persistence API with an integrated Derby database for storing all the message meta-data. It is suitable for smaller
 * gateway deployments. For larger gateways that have additional requirements on performance and high availability a
 * different provider should be used.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 7.0.0
 */
@SuppressWarnings("unchecked")
public class DefaultMetadataStorageProvider implements IMetadataStorageProvider {
	private EntityManagerFactory emf;
	/**
	 * The running instance of the provider is used by the default UI to retrieve the message meta-data.
	 */
	private static DefaultMetadataStorageProvider instance;

	@Override
	public String getName() {
		return "HB2B Default Metadata Storage Provider/" + VersionInfo.fullVersion;
	}

	@Override
	public void init(final IConfiguration config) throws StorageException {
		emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory(DatabaseConfiguration.INSTANCE,
				Collections.emptyMap());
		instance = this;
	}

	/**
	 * @return	the running instance of the provider
	 */
	public static DefaultMetadataStorageProvider getInstance() {
		return instance;
	}

	@Override
	public void shutdown() {
		if (emf != null && emf.isOpen())
			emf.close();
	}

	@Override
	public <T extends IMessageUnit, E extends IMessageUnitEntity> E storeMessageUnit(T messageUnit)
			throws DuplicateMessageIdException, StorageException {

		// If this is an outgoing message check its messageId is unique
		if (messageUnit.getDirection() == Direction.OUT
				&& !Utils.isNullOrEmpty(getMessageUnitsWithId(messageUnit.getMessageId(), Direction.OUT)))
			throw new DuplicateMessageIdException(messageUnit.getMessageId());

		EntityManager em = null;
		MessageUnit jpaMsgUnit = null;
		EntityTransaction tx = null;
		try {
			em = emf.createEntityManager();
			// Determine which JPA class should be created to store the meta-data
			Class<? extends MessageUnit> jpaClass = JPAObjectHelper.getJPAClass(messageUnit);
			Constructor<? extends MessageUnit> cons = jpaClass
					.getConstructor(MessageUnitUtils.getMessageUnitType(messageUnit));
			jpaMsgUnit = cons.newInstance(messageUnit);
			if (messageUnit instanceof IUserMessage && ((IUserMessage) messageUnit).getPayloads() != null) {
				for (IPayload p : ((IUserMessage) messageUnit).getPayloads()) {
					PayloadInfo storedPl = null;
					if ((p instanceof IPayloadEntity) && !Utils.isNullOrEmpty(((IPayloadEntity) p).getPayloadId()))
						try {
							storedPl = em.createNamedQuery("PayloadInfo.findByPayloadId", PayloadInfo.class)
									.setParameter("payloadId", ((IPayloadEntity) p).getPayloadId()).getSingleResult();
							if (storedPl.getParentCoreId() != null)
								throw new StorageException("Payload (payloadId=" + ((IPayloadEntity) p).getPayloadId()
										+ "already linked to other User Message (coreId=" + storedPl.getParentCoreId()
										+ ")");
						} catch (NoResultException notFound) {
							throw new StorageException("Unknown payloadId: " + ((IPayloadEntity) p).getPayloadId());
						}
					else
						storedPl = new PayloadInfo(p);
					storedPl.setParentCoreId(jpaMsgUnit.getCoreId());
					((UserMessage) jpaMsgUnit).addPayload(storedPl);
				}
			}
			tx = em.getTransaction();
			tx.begin();
			em.persist(jpaMsgUnit);
			tx.commit();
			return (E) JPAObjectHelper.proxy(jpaMsgUnit);
		} catch (Exception ex) {
			if (tx != null && tx.isActive())
				tx.rollback();
			throw ex instanceof StorageException ? (StorageException) ex
					: new StorageException("An error occurred while saving the message unit's meta-data!", ex);
		} finally {
			if (em != null && em.isOpen())
				em.close();
		}
	}

	@Override
	public IPayloadEntity storePayloadMetadata(IPayload payload) throws StorageException {
		EntityManager em = null;
		EntityTransaction tx = null;
		try {
			em = emf.createEntityManager();
			// Determine which JPA class should be created to store the meta-data
			PayloadInfo jpaPayload = new PayloadInfo(payload);
			tx = em.getTransaction();
			tx.begin();
			em.persist(jpaPayload);
			tx.commit();
			return JPAObjectHelper.proxy(jpaPayload);
		} catch (Exception ex) {
			if (tx != null && tx.isActive())
				tx.rollback();
			throw new StorageException("An error occurred while saving the payload's meta-data!", ex);
		} finally {
			if (em != null && em.isOpen())
				em.close();
		}
	}

	@Override
	public void updateMessageUnit(IMessageUnitEntity messageUnit) throws AlreadyChangedException, StorageException {
		assertManagedType(messageUnit);
		updateEntity((JPAObjectProxy<MessageUnit>) messageUnit);
	}

	@Override
	public void deleteMessageUnit(IMessageUnitEntity messageUnit) throws StorageException {
		assertManagedType(messageUnit);
		deleteEntity((JPAObjectProxy<MessageUnit>) messageUnit);
	}

	@Override
	public void updatePayloadMetadata(IPayloadEntity payload) throws AlreadyChangedException, StorageException {
		assertManagedType(payload);
		updateEntity((JPAObjectProxy<PayloadInfo>) payload);
	}

	@Override
	public void deletePayloadMetadata(IPayloadEntity payload) throws StorageException {
		assertManagedType(payload);
		if (!Utils.isNullOrEmpty(payload.getParentCoreId())
				&& getMessageUnitWithCoreId(payload.getParentCoreId()) != null)
			throw new StorageException("Cannot delete payload linked to User Message");
		else
			deleteEntity((JPAObjectProxy<PayloadInfo>) payload);
	}

	private <T extends JPAEntityObject> void updateEntity(JPAObjectProxy<T> proxy) throws AlreadyChangedException,
																									StorageException {
		EntityManager em = null;
		EntityTransaction tx = null;
		try {
			em = emf.createEntityManager();
			tx = em.getTransaction();
			tx.begin();
			T updated = em.merge(proxy.getJPAObject());
			// Flushing will trigger the OptimisticLockException
			em.flush();
			proxy.updateJPAObject(updated);
		} catch (OptimisticLockException alreadyChanged) {
			tx.setRollbackOnly();
			throw new AlreadyChangedException();
		} catch (Exception updateFailure) {
			tx.setRollbackOnly();
			throw new StorageException("Failure updating meta-data", updateFailure);
		} finally {
			// Ensure that the object stays completely loaded
			if (tx != null && tx.isActive() && tx.getRollbackOnly())
				tx.rollback();
			else if (tx != null && tx.isActive())
				tx.commit();
			if (em != null && em.isOpen())
				em.close();
		}
	}

	private <T extends JPAEntityObject> void deleteEntity(JPAObjectProxy<T> proxy) throws StorageException {
		T jpaObject = proxy.getJPAObject();
		EntityManager em = null;
		EntityTransaction tx = null;
		try {
			em = emf.createEntityManager();
			tx = em.getTransaction();
			tx.begin();
			em.remove(em.find(jpaObject.getClass(), jpaObject.getOID()));
			tx.commit();
		} catch (final Exception e) {
			// Something went wrong while executing the update, rollback the transaction (if active) and throw exception
			if (tx != null && tx.isActive())
				tx.rollback();
			throw new StorageException("An error occurred removing the meta-data!", e);
		} finally {
			em.close();
		}
	}

	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsForPModesInState(Class<T> type,
			Set<String> pmodeIds, ProcessingState state) throws StorageException {

		return executeMessageUnitQuery(em -> em.createQuery(
										"SELECT mu "
						                + "FROM " + JPAObjectHelper.getJPAClass(type).getSimpleName() + " mu "
						                + "JOIN mu.states s1 "
						                + "WHERE mu.PMODE_ID IN :pmodeIds "
						                + "AND s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM mu.states s2) "
						                + "AND s1.STATE = :state "
						                + "ORDER BY s1.START", JPAObjectHelper.getJPAClass(type))
								      .setParameter("pmodeIds", pmodeIds)
								      .setParameter("state", state));
	}

	@Override
	public <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> getMessageUnitsInState(Class<T> type,
											Direction direction, Set<ProcessingState> states) throws StorageException {

		return executeMessageUnitQuery(em -> em.createQuery(
								"SELECT mu "
				                + "FROM " + JPAObjectHelper.getJPAClass(type).getSimpleName() + " mu "
		                		+ "JOIN mu.states s1 "
				                + "WHERE mu.DIRECTION = :direction "
				                + "AND s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM mu.states s2) "
				                + "AND s1.STATE IN :states "
				                + "ORDER BY mu.MU_TIMESTAMP", JPAObjectHelper.getJPAClass(type))
                                .setParameter("direction", direction)
                                .setParameter("states", states));
	}

	@Override
	public Collection<IMessageUnitEntity> getMessageUnitsWithId(String messageId, Direction... direction)
																							throws StorageException {
		return executeMessageUnitQuery(em -> {
					StringBuilder queryString = new StringBuilder();
			        queryString.append("SELECT mu ")
			        		   .append("FROM MessageUnit mu ")
			        		   .append("WHERE mu.MESSAGE_ID = :msgId ");
			        if (direction.length == 1)
			        	queryString.append("AND mu.DIRECTION = :direction ");

			        queryString.append("ORDER BY mu.MU_TIMESTAMP");
			        TypedQuery<MessageUnit> query = em.createQuery(queryString.toString(),
			        												MessageUnit.class)
	                											.setParameter("msgId", messageId);
		            if (direction.length == 1)
		            	query.setParameter("direction", direction[0]);
		            return query;
			});
	}

	@Override
	public Collection<IMessageUnitEntity> getMessageUnitsWithLastStateChangedBefore(Date maxLastChangeDate)
			throws StorageException {

		return executeMessageUnitQuery(em -> em.createQuery(
								"SELECT mu "
				                + "FROM MessageUnit mu "
				                + "JOIN mu.states s1 "
				                + "WHERE s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM mu.states s2) "
				                + "AND   s1.START <= :beforeDate", MessageUnit.class)
								.setParameter("beforeDate", maxLastChangeDate, TemporalType.TIMESTAMP));
	}

	@Override
	public IMessageUnitEntity getMessageUnitWithCoreId(String coreId)
			throws StorageException {

		return executeMessageUnitQuery(em -> em.createQuery(
								"SELECT mu "
								+ "FROM MessageUnit mu "
								+ "WHERE mu.CORE_ID = :coreId ", MessageUnit.class)
								.setParameter("coreId", coreId)).stream()
				.findFirst().orElse(null);
	}

	private <V extends IMessageUnitEntity> List<V> executeMessageUnitQuery(
			@SuppressWarnings("rawtypes") Function<EntityManager, TypedQuery> prepareQuery) throws StorageException {

		final EntityManager em = emf.createEntityManager();
		try {
			em.getTransaction().begin();
			return JPAObjectHelper.proxy(prepareQuery.apply(em).getResultList());
		} catch (final Exception e) {
			throw new StorageException("Could not execute query \"getMessageUnitsForPModesInState\"", e);
		} finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	@Override
	public int getNumberOfTransmissions(IUserMessageEntity userMessage) throws StorageException {
		assertManagedType(userMessage);

        int result = 0;
        final EntityManager em = emf.createEntityManager();

        final String query = "SELECT COUNT(s1.STATE) "
                           + "FROM UserMessage um "
                           + "JOIN um.states s1 "
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
            throw new StorageException("Could not execute query \"getNumberOfTransmissions\"", e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }
        return result;
	}

	@Override
	public boolean isAlreadyProcessed(IUserMessageEntity userMessage) throws StorageException {
		assertManagedType(userMessage);

        boolean result = false;
        final EntityManager em = emf.createEntityManager();

        final String query = "SELECT COUNT(um) "
                           + "FROM UserMessage um "
                           + "JOIN um.states s1 "
                           + "WHERE um.DIRECTION = org.holodeckb2b.interfaces.messagemodel.Direction.IN "
                           + "AND um.MESSAGE_ID = :msgId "
                           + "AND s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM um.states s2) "
                           + "AND s1.STATE IN ( org.holodeckb2b.interfaces.processingmodel.ProcessingState.DELIVERED, "
                           + 			"org.holodeckb2b.interfaces.processingmodel.ProcessingState.OUT_FOR_DELIVERY, "
        				   + 			"org.holodeckb2b.interfaces.processingmodel.ProcessingState.FAILURE)";
        try {
            em.getTransaction().begin();
            result = em.createQuery(query, Long.class)
                                     .setParameter("msgId", userMessage.getMessageId())
                                     .getSingleResult() > 0;
        } catch (final NoResultException nothingFound) {
            result = false;
        } catch (final Exception e) {
            throw new StorageException("Could not execute query \"isAlreadyDelivered\"", e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }
        return result;
	}

	/**
     * Gets the meta-data of the specified maximum of message units which last processing state was before the given
     * time stamp. The resulting list is ordered descending by the last processing state's start time.
     * <p>
     * NOTE: This query is not part of {@link IMetadataStorageProvider} and is added for use by the default UI to
     * monitor the message processing. If you use this method in other code note that the GPLv3 applies and your code
     * MUST also be licensed under GPLv3.
     *
     * @param upto		Most recent time stamp to include
     * @param max		Maximum number of results
     * @return			List of message units that match the given criteria, limited to the given maximum number
     * 					of entries
     * @throws StorageException When an error occurs in retrieving the message unit meta-data
     */
    public List<IMessageUnitEntity> getMessageHistory(final Date upto, final int max)
    																						throws StorageException {
		 return executeMessageUnitQuery(em -> em.createQuery(
						"SELECT mu "
		                + "FROM MessageUnit mu "
		                + "JOIN mu.states s1 "
		                + "WHERE s1.PROC_STATE_NUM = (SELECT MAX(s2.PROC_STATE_NUM) FROM mu.states s2) "
		                + "AND   s1.START <= :beforeDate "
		                + "ORDER BY s1.START DESC", MessageUnit.class)
						.setParameter("beforeDate", upto, TemporalType.TIMESTAMP)
						.setMaxResults(max));
    }


	private void assertManagedType(Object entity) throws StorageException {
		if (!(entity instanceof MessageUnitEntity<?>) && !(entity instanceof PayloadEntity))
			throw new StorageException("Unsuported entity class");
	}
}

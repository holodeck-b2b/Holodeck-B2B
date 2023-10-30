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

import java.lang.reflect.Constructor;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;

import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.persistency.AlreadyChangedException;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPayloadEntity;
import org.holodeckb2b.interfaces.submit.DuplicateMessageIdException;
import org.holodeckb2b.persistency.entities.MessageUnitEntity;
import org.holodeckb2b.persistency.entities.PayloadEntity;
import org.holodeckb2b.persistency.jpa.MessageUnit;
import org.holodeckb2b.persistency.jpa.Payload;
import org.holodeckb2b.persistency.util.EntityManagerUtil;
import org.holodeckb2b.persistency.util.JPAEntityHelper;

/**
 * Is the default persistency provider's implementation of the {@link IUpdateManager} interface.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class UpdateManager implements IUpdateManager {

    @Override
    public <T extends IMessageUnit, V extends IMessageUnitEntity> V storeMessageUnit(final T messageUnit)
                                                            throws DuplicateMessageIdException, PersistenceException {
        // If this is an outgoing message check its messageId is unique
    	if (messageUnit.getDirection() == Direction.OUT 
    		&& !Utils.isNullOrEmpty(HolodeckB2BCoreInterface.getQueryManager()
    											.getMessageUnitsWithId(messageUnit.getMessageId(), Direction.OUT)))
    		throw new DuplicateMessageIdException(messageUnit.getMessageId());
    	
    	EntityManager em = null;
        MessageUnit jpaMsgUnit = null;
        EntityTransaction tx = null;                
        try {
            // Determine which JPA class should be created to store the meta-data
            Class<T> jpaEntityClass = JPAEntityHelper.determineJPAClass(messageUnit);
            Constructor<T> cons = jpaEntityClass.getConstructor(MessageUnitUtils.getMessageUnitType(messageUnit));
            jpaMsgUnit = (MessageUnit) cons.newInstance(messageUnit);
            jpaMsgUnit.setCoreId(UUID.randomUUID().toString());
            em = EntityManagerUtil.getEntityManager();
            tx = em.getTransaction();
            tx.begin();
            em.persist(jpaMsgUnit);
            tx.commit();
        } catch (Exception ex) {
    		if (tx != null && tx.isActive())
    			tx.rollback();    	
            throw new PersistenceException("An error occurred while saving the message unit's meta-data!", ex);
        } finally {
            if (em != null && em.isOpen())
                em.close();
        }

        return JPAEntityHelper.wrapInEntity(jpaMsgUnit, true);
    }

    @Override
    public void updateMessageUnit(IMessageUnitEntity messageUnit) throws AlreadyChangedException, PersistenceException {
    	if (!(messageUnit instanceof MessageUnitEntity<?>))
    		throw new PersistenceException("Unsuported entity class");
    	
    	@SuppressWarnings("unchecked")
		MessageUnitEntity<MessageUnit> entityObj = (MessageUnitEntity<MessageUnit>) messageUnit;		
    	EntityManager em = null;
		EntityTransaction tx = null;
		try {
			em = EntityManagerUtil.getEntityManager();
			tx = em.getTransaction();
			tx.begin();
			MessageUnit updated = em.merge(entityObj.getJPAObject());
			// Flushing will trigger the OptimisticLockException
			em.flush();
			entityObj.updateJPAObject(updated);
		} catch (OptimisticLockException alreadyChanged) {
			em.refresh(entityObj.getJPAObject());
			tx.setRollbackOnly();			
			throw new AlreadyChangedException();			
		} catch (Exception updateFailure) {
			tx.setRollbackOnly();
			throw new PersistenceException("Failure updating message unit meta-data", updateFailure);			
		} finally {
			// Ensure that the object stays completely loaded if it was already so previously
			if (messageUnit.isLoadedCompletely())
				QueryManager.loadCompletely(entityObj.getJPAObject());			
			if (tx != null && tx.isActive() && tx.getRollbackOnly())
				tx.rollback();
			else if (tx != null && tx.isActive())
				tx.commit();		
			if (em != null && em.isOpen())
				em.close();
		}
    }    

    @Override
    public void updatePayload(IPayloadEntity payload) throws PersistenceException {
    	if (!(payload instanceof PayloadEntity))
    		throw new PersistenceException("Unsuported entity class");
    	
    	PayloadEntity entityObj = (PayloadEntity) payload;		
    	EntityManager em = null;
		EntityTransaction tx = null;
		try {
			em = EntityManagerUtil.getEntityManager();
			tx = em.getTransaction();
			tx.begin();
			Payload updated = em.merge(entityObj.getJPAObject());
			tx.commit();		
			entityObj.updateJPAObject(updated);
		} catch (Exception updateFailure) {
			if (tx != null && tx.isActive())
				tx.rollback();
			throw new PersistenceException("Failure updating payload meta-data", updateFailure);			
		} finally {
			if (em != null && em.isOpen())
				em.close();
		}
    }    

    @Override
    public void deleteMessageUnit(final IMessageUnitEntity messageUnit) throws PersistenceException {
    		EntityManager em = null;
        EntityTransaction tx = null;
        try {
        		em = EntityManagerUtil.getEntityManager();
        		tx = em.getTransaction();
            tx.begin();
            // Reload the entity object from the database so we've actual data and a managed JPA object ready for change
            MessageUnit jpaMsgUnit = em.find(MessageUnit.class, ((MessageUnitEntity<?>) messageUnit).getOID());
            em.remove(jpaMsgUnit);
            tx.commit();
        } catch (final Exception e) {
            // Something went wrong while executing the update, rollback the transaction (if active) and throw exception
	        	if (tx!=null && tx.isActive()) {
	    			tx.rollback();
	    		}
            throw new PersistenceException("An error occurred in the update of the message unit meta-data!", e);
        }finally {
            em.close();
        }
    }  
}

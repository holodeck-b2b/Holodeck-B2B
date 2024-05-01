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
package org.holodeckb2b.persistency;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.persistency.IPersistencyProvider;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.IUpdateManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.persistency.managers.QueryManager;
import org.holodeckb2b.persistency.managers.UpdateManager;
import org.holodeckb2b.persistency.util.EntityManagerUtil;

import javax.persistence.EntityManager;

/**
 * Is the default implementation of a Holodeck B2B <i>Persistency Provider</i>. This provider uses an integrated Derby
 * database for storing all the data. It is suitable for smaller gateway deployments. For larger gateways that have
 * additional requirements on performance and high availability a different provider should be used.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class DefaultProvider implements IPersistencyProvider {

    @Override
    public String getName() {
        return  "HB2B Default Persistency/" + VersionInfo.fullVersion;
    }

    @Override
    public void init(final IConfiguration config) throws PersistenceException {
        EntityManagerUtil.check();
        removeOrphanedPayloads();
    }

    /**
     * Removes any orphaned payload records.
     * <p>
     * Due to a bug in the previous version of the provider there will exist orphaned payload records in the database
     * which will never be removed (as only payloads related to User Message will be deleted upon purge).
     *
     * @since 6.1.1
     */
    private void removeOrphanedPayloads() {
        EntityManager em = null;
        try {
            em = EntityManagerUtil.getEntityManager();
            em.getTransaction().begin();
            em.createNativeQuery("DELETE FROM PL_PROPERTIES plp " +
                    "WHERE plp.PAYLOAD_OID NOT IN (SELECT ump.PAYLOADS_OID FROM USER_MESSAGE_PAYLOAD ump)").executeUpdate();
            int r = em.createNativeQuery("DELETE FROM PAYLOAD pl " +
                    "WHERE pl.OID NOT IN (SELECT ump.PAYLOADS_OID FROM USER_MESSAGE_PAYLOAD ump)").executeUpdate();
            em.getTransaction().commit();
            if (r > 0)
                LogManager.getLogger().debug("Removed {} orphaned payload records", r);
        } catch (Throwable t) {
            em.getTransaction().rollback();
            LogManager.getLogger().warn("An error occurred trying to remove orphaned payloads : {}",
                                        Utils.getExceptionTrace(t));
        } finally {
            if (em != null && em.isOpen())
                em.close();
        }
    }

    @Override
    public void shutdown() {
    }
    
    @Override
    public IUpdateManager getUpdateManager() {
        return new UpdateManager();
    }

    @Override
    public IQueryManager getQueryManager() {
        return new QueryManager();
    }
}

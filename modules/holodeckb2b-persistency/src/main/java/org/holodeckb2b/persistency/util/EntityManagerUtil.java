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
package org.holodeckb2b.persistency.util;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.persistency.PersistenceException;

/**
 * Is a helper class to easily get hold of the JPA <code>EntityManager</code> to access the database where the message
 * unit meta-data is stored. This default persistency provider uses a fixed and programmatically built persistency unit
 * that will create an embedded Derby database.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class EntityManagerUtil {
    // We use SingletonHolder pattern for the reference to the EntityManagerFactory object
    private static final class SingletonHolder
    {
      static final EntityManagerFactory instance =  new HibernatePersistenceProvider()
                                                        .createContainerEntityManagerFactory(getPersistenceUnitInfo(),
                                                                                             Collections.emptyMap());
    }

    private static PersistenceUnitInfo getPersistenceUnitInfo() {
        return new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return "hb2b-default-persistency";
            }

            @Override
            public String getPersistenceProviderClassName() {
                return "org.hibernate.jpa.HibernatePersistenceProvider";
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }

            @Override
            public DataSource getJtaDataSource() {
                return null;
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return null;
            }

            @Override
            public List<String> getMappingFileNames() {
                return Collections.emptyList();
            }

            @Override
            public List<URL> getJarFileUrls() {
                return Collections.emptyList();
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                return Arrays.asList("org.holodeckb2b.persistency.jpa.AgreementReference",
                                     "org.holodeckb2b.persistency.jpa.CollaborationInfo",
                                     "org.holodeckb2b.persistency.jpa.Description",
                                     "org.holodeckb2b.persistency.jpa.EbmsError",
                                     "org.holodeckb2b.persistency.jpa.ErrorMessage",
                                     "org.holodeckb2b.persistency.jpa.MessageUnit",
                                     "org.holodeckb2b.persistency.jpa.MessageUnitProcessingState",
                                     "org.holodeckb2b.persistency.jpa.PartyId",
                                     "org.holodeckb2b.persistency.jpa.Payload",
                                     "org.holodeckb2b.persistency.jpa.Property",
                                     "org.holodeckb2b.persistency.jpa.PullRequest",
                                     "org.holodeckb2b.persistency.jpa.Receipt",
                                     "org.holodeckb2b.persistency.jpa.SchemaReference",
                                     "org.holodeckb2b.persistency.jpa.SelectivePullRequest",
                                     "org.holodeckb2b.persistency.jpa.Service",
                                     "org.holodeckb2b.persistency.jpa.TradingPartner",
                                     "org.holodeckb2b.persistency.jpa.UserMessage");
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return false;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return null;
            }

            @Override
            public ValidationMode getValidationMode() {
                return null;
            }

            @Override
            public Properties getProperties() {
                Properties props = new Properties();
                props.put(org.hibernate.cfg.AvailableSettings.DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
                String dbPath = System.getenv("HB2B_DB_DIR");
                if (Utils.isNullOrEmpty(dbPath) || !Files.isDirectory(Paths.get(dbPath)) || !Files.isWritable(Paths.get(dbPath)))
                	dbPath = "db";
                props.put(org.hibernate.cfg.AvailableSettings.URL,
                                                                "jdbc:derby:" + dbPath + "/coreDB;databaseName=coreDB;create=true");
                props.put(org.hibernate.cfg.AvailableSettings.DIALECT, DerbyTenSevenDialect.class);
                props.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "update");
                props.put(org.hibernate.cfg.AvailableSettings.SHOW_SQL, false);
                props.put(org.hibernate.cfg.AvailableSettings.QUERY_STARTUP_CHECKING, false);
                props.put(org.hibernate.cfg.AvailableSettings.GENERATE_STATISTICS, false);
                props.put(org.hibernate.cfg.AvailableSettings.USE_REFLECTION_OPTIMIZER, false);
                props.put(org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE, false);
                props.put(org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE, false);
                props.put(org.hibernate.cfg.AvailableSettings.USE_STRUCTURED_CACHE, false);
                props.put(org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, 20);

                return props;
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return null;
            }

            @Override
            public ClassLoader getClassLoader() {
                return null;
            }

            @Override
            public void addTransformer(ClassTransformer transformer) {}

            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }
        };
    }

    /**
     * Gets a JPA {@link EntityManager} to execute database operations.
     *
     * @return  An <code>EntityManager</code> to access the database
     * @throws  PersistenceException   When exception occurs getting hold of an EntityManager object
     */
    public static EntityManager getEntityManager() throws PersistenceException {
       try {
           // The class is loaded upon first call
           return  SingletonHolder.instance.createEntityManager();
       } catch (final Exception e) {
           // Oh oh, something went wrong creating the entity manager
           throw new PersistenceException("Error while creating the EntityManager", e);
       }
    }

    /**
     * Checks whether the EntityManager can be successfully intialized.
     *
     * @throws PersistenceException If there is an issue intializing the EntityManager
     */
    public static void check() throws PersistenceException {
        try {
            SingletonHolder.instance.isOpen();
        } catch (final Throwable t) {
            // Seems something went wrong in opening the database connnection
            throw new PersistenceException("The database cannot be initialized successfully!", t);
        }
    }
}

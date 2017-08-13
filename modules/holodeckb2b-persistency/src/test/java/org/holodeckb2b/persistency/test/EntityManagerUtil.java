package org.holodeckb2b.persistency.test;

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


import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.holodeckb2b.interfaces.persistency.PersistenceException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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
    private static final class SingletonHolder {
      static final EntityManagerFactory instance =  new HibernatePersistenceProvider()
              .createContainerEntityManagerFactory(getPersistenceUnitInfo(),
                      Collections.emptyMap());
    }

    private static PersistenceUnitInfo getPersistenceUnitInfo() {
        System.out.println("[getPersistenceUnitInfo()]");
        return new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return "holodeckb2b-test";
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
                        "org.holodeckb2b.persistency.jpa.Service",
                        "org.holodeckb2b.persistency.jpa.TradingPartner",
                        "org.holodeckb2b.persistency.jpa.UserMessage",
                        "org.holodeckb2b.persistency.test.wrappers.WAgreementReference",
                        "org.holodeckb2b.persistency.test.wrappers.WService");
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
                props.put(AvailableSettings.DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
                props.put(AvailableSettings.URL,
                        "jdbc:derby:db/coreDB;databaseName=coreDB;create=true");
                props.put(AvailableSettings.DIALECT, DerbyTenSevenDialect.class);
                props.put(AvailableSettings.HBM2DDL_AUTO, "update");
                props.put(AvailableSettings.SHOW_SQL, false);
                props.put(AvailableSettings.QUERY_STARTUP_CHECKING, false);
                props.put(AvailableSettings.GENERATE_STATISTICS, false);
                props.put(AvailableSettings.USE_REFLECTION_OPTIMIZER, false);
                props.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, false);
                props.put(AvailableSettings.USE_QUERY_CACHE, false);
                props.put(AvailableSettings.USE_STRUCTURED_CACHE, false);
                props.put(AvailableSettings.STATEMENT_BATCH_SIZE, 20);

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
}

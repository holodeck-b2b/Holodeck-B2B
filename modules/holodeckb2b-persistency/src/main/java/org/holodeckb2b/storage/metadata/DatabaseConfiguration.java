/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.holodeckb2b.commons.util.Utils;

/**
 * Contains the database configuration used by the default Meta-data Storage Provider of Holodeck B2B. It creates an
 * embedded Derby database in the <code>db</code> subdirectory of the Holodeck B2B home directory. The directory were
 * the database is stored can be changed by setting the environment variable <code>HB2B_DB_DIR</code>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
final class DatabaseConfiguration implements PersistenceUnitInfo {

	public static final DatabaseConfiguration INSTANCE = new DatabaseConfiguration();

    @Override
    public String getPersistenceUnitName() {
        return "hb2b-default-persistency";
    }

    @Override
    public String getPersistenceProviderClassName() {
        return HibernatePersistenceProvider.class.getName();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }

    @Override
    public List<String> getManagedClassNames() {
        return Arrays.asList("org.holodeckb2b.storage.metadata.entities.AgreementReference",
                             "org.holodeckb2b.storage.metadata.entities.CollaborationInfo",
                             "org.holodeckb2b.storage.metadata.entities.Description",
                             "org.holodeckb2b.storage.metadata.entities.EbmsError",
                             "org.holodeckb2b.storage.metadata.entities.ErrorMessage",
                             "org.holodeckb2b.storage.metadata.entities.MessageUnit",
                             "org.holodeckb2b.storage.metadata.entities.MessageUnitProcessingState",
                             "org.holodeckb2b.storage.metadata.entities.PartyId",
                             "org.holodeckb2b.storage.metadata.entities.Payload",
                             "org.holodeckb2b.storage.metadata.entities.Property",
                             "org.holodeckb2b.storage.metadata.entities.PullRequest",
                             "org.holodeckb2b.storage.metadata.entities.Receipt",
                             "org.holodeckb2b.storage.metadata.entities.SchemaReference",
                             "org.holodeckb2b.storage.metadata.entities.SelectivePullRequest",
                             "org.holodeckb2b.storage.metadata.entities.Service",
                             "org.holodeckb2b.storage.metadata.entities.TradingPartner",
                             "org.holodeckb2b.storage.metadata.entities.UserMessage");
    }

    @Override
    public Properties getProperties() {
        Properties props = new Properties();
        props.put(org.hibernate.cfg.AvailableSettings.DRIVER, org.apache.derby.jdbc.EmbeddedDriver.class.getName());
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
    public boolean excludeUnlistedClasses() {
        return true;
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

    private DatabaseConfiguration() {}
}

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
package org.holodeckb2b.ebms3.pulling;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.workerpool.WorkerPool;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.core.Validate;

/**
 * Is a implementation of {@link IWorkerPoolConfiguration} specifically for creating a pool of <i>pull workers</i> that
 * send out the pull requests.
 * <p>The pull worker pool exists of one default pull worker and zero or more specific pull workers. The specific
 * pull workers handle the pulling for a given set of P-Modes. The default pull worker will handle all other pulling.
 * For each worker an interval is specified to wait between sending of the pull request. If the interval equals 0 the
 * pull worker will not be activated and pulling will be disabled.
 * <p>The configuration of the pool is read from an XML document defined by the schema <code>http://holodeck-b2b.org/schemas/2014/05/pullconfiguration</code>
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see WorkerPool
 * @see PullWorker
 * @see PullerConfig
 */
@Root(name = "pulling", strict = false)
@Namespace(reference="http://holodeck-b2b.org/schemas/2014/05/pullconfiguration")
public class PullConfiguration implements IWorkerPoolConfiguration {

    /**
     * The name of the worker pool that contains the workers that execute the PullRequests
     */
    public static final String PULL_WORKER_POOL_NAME = "holodeckb2b:pullers";

    @Element(name = "default")
    private PullerConfig  defaultPuller;

    @ElementList(entry = "pull", inline = true, required = false)
    private List<PullerConfig>  pullers;

    /**
     * Performs additional checks to ensure that the read XML document is valid according to the XSD.
     * <p>Because a general definition is used for reading both the default and specific pullers there is no check
     * on the number of P-Modes referenced by the pullers when the XML is deserialized. Therefor this method performs
     * checks:<ol>
     * <li>There are no referenced P-Modes for the default puller</li>
     * <li>There is at least one reference P-Mode for each specific puller</li>
     * </ol>
     *
     * @throws PersistenceException When the read XML fails one of the checks
     */
    @Validate
    private void validate() throws PersistenceException {
        // Default puller should have no PModes associated with it
        if(defaultPuller.pmodes != null && !defaultPuller.pmodes.isEmpty())
            throw new PersistenceException("The default puller should not specify specific PModes!");

        // A specific puller must specifiy at least one PMode
        if(pullers != null) {
            for(final PullerConfig p : pullers)
                if(p.pmodes == null || p.pmodes.isEmpty())
                    throw new PersistenceException("Specific puller must reference at least one PMode!");
        }
    }


    /**
     * @return The name of this worker pool is fixed and defined by {@link HolodeckB2BCore#PULL_WORKER_POOL_NAME}.
     */
    @Override
    public String getName() {
        return PULL_WORKER_POOL_NAME;
    }

    /**
     * Converts the different pull options defined in the XML configuration to a list {@link IWorkerConfiguration}
     * objects needed for the configuration of the worker pool.
     *
     * @return The configurations for the pull workers part of the worker pool based on the XML configuration.
     * @see    WorkerPool
     */
    @Override
    public List<IWorkerConfiguration> getWorkers() {
        final ArrayList<IWorkerConfiguration> pullWorkers = new ArrayList<>();
        // The P-Modes that have a specific pull worker should not be included in the default one, so collect them
        final ArrayList<PullerConfig.PMode>   notDefault = new ArrayList<>();

        for (int i = 0; pullers != null && i < pullers.size(); i++) {
            final PullerConfig p = pullers.get(i);
            // Set a unique name for each puller worker
            p.name = "p" + (new Date()).getTime() + i;
            pullWorkers.add(p);
            // The handled P-Mode should be excluded from the default pull worker
            notDefault.addAll(p.pmodes);
        }

        // Configure the default pull worker to exclude all P-Modes already taken care of
        defaultPuller.name = "default";
        defaultPuller.pmodes = notDefault;
        defaultPuller.inclusive = false;
        pullWorkers.add(defaultPuller);

        return pullWorkers;
    }

    /**
     * Loads the pulling configuration from file.
     *
     * @param path      Path to the XML document containing the pulling configuration
     * @return          The pulling configuration if successfully loaded, null otherwise
     */
    public static PullConfiguration  loadFromFile(final String  path) {
        final Log log = LogFactory.getLog(PullConfiguration.class);
        PullConfiguration    pullCfg = null;

        log.debug("Loading pulling configuration from XML document in " + path);

        final File f = new File(path);

        if (f.exists() && f.canRead()) {
            final Serializer serializer = new Persister();
            try {
                pullCfg = serializer.read(PullConfiguration.class, f);
                log.debug("Loaded configuration");
            } catch (final Exception ex) {
                log.error("Error while reading configuration from " + path + "! Details: " + ex.getMessage());
            }
         } else
            log.error("Unable to access configuration file" + path + "!");


        return pullCfg;
    }
}

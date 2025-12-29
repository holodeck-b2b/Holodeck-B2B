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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.core.workerpool.WorkerPool;
import org.holodeckb2b.interfaces.storage.StorageException;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.core.Validate;

/**
 * Represent the root element of the XML document that configures the pulling mechanism as defined in XML schema 
 * <i>http://holodeck-b2b.org/schemas/2014/05/pullconfiguration</i>.
 * <p>Pulling is based on a Holodeck B2B worker pull that contains a set {@link PullWorker}s that send out the <i>Pull 
 * Request</i>. The workers can be configured to send the Pull Request for a specific P-Mode or for all P-Modes (that 
 * don't have a specific config). In the XML document this is represented by the <code>default</code> and <code>pull
 * </code> elements which represent the default puller and the ones for specific P-Modes. For each worker an interval is 
 * specified to wait between sending of the pull request. If the interval equals 0 the pull worker will not be activated 
 * and pulling will be disabled.<br/>
 * Besides the configuration of the pull workers the configuration also includes a parameter that indicates the interval
 * at which it should be refreshed and the worker pool should be reconfigured. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see PullWorker
 * @see PullerConfigElement
 */
@Root(name = "pulling", strict = false)
@Namespace(reference="http://holodeck-b2b.org/schemas/2014/05/pullconfiguration")
class PullConfigDocument {
	static final Logger log = LogManager.getLogger();    

    @Attribute(name = "refresh", required = false)
    private Integer	refreshInterval;
    
    @Element(name = "default")
    private PullerConfigElement  defaultPuller;

    @ElementList(entry = "pull", inline = true, required = false)
    private List<PullerConfigElement>  pullers;

    /**
     * Performs additional checks to ensure that the read XML document is valid according to the XSD.
     * <p>Because a general definition is used for reading both the default and specific pullers there is no check
     * on the number of P-Modes referenced by the pullers when the XML is deserialized. Therefor this method performs
     * checks:<ol>
     * <li>There are no referenced P-Modes for the default puller</li>
     * <li>There is at least one reference P-Mode for each specific puller</li>
     * </ol>
     *
     * @throws StorageException When the read XML fails one of the checks
     */
    @Validate
    private void validate() throws StorageException {
        // Default puller should have no PModes associated with it
        if(defaultPuller.pmodes != null && !defaultPuller.pmodes.isEmpty())
            throw new StorageException("The default puller should not specify specific PModes!");

        // A specific puller must specifiy at least one PMode
        if(pullers != null) {
            for(final PullerConfigElement p : pullers)
                if(p.pmodes == null || p.pmodes.isEmpty())
                    throw new StorageException("Specific puller must reference at least one PMode!");
        }
        // If supplied, the refresh interval must be a non negative integer
        if (refreshInterval != null && refreshInterval.intValue() < 0)
        	throw new StorageException("Refresh interval must be a non negative integer");
    }
    
    /**
     * Converts the different pull options defined in the XML configuration to a list {@link IWorkerConfiguration}
     * objects needed for the configuration of the worker pool.
     *
     * @return The configurations for the pull workers part of the worker pool based on the XML configuration.
     * @see    WorkerPool
     */    
    public List<IWorkerConfiguration> getWorkers() {
        final ArrayList<IWorkerConfiguration> pullWorkers = new ArrayList<>();
        // The P-Modes that have a specific pull worker should not be included in the default one, so collect them
        final ArrayList<PullerConfigElement.PMode>   notDefault = new ArrayList<>();

        for (int i = 0; pullers != null && i < pullers.size(); i++) {
            final PullerConfigElement p = pullers.get(i);
            // Set a unique name for each puller worker
            p.name = "puller" + (new Date()).getTime() + i;
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
     * @return  the configured interval at which the configuration should be refreshed 
     */
    public int getRefreshInterval() {
    	return refreshInterval != null ? refreshInterval.intValue() : 60;
    }

    /**
     * Loads the pulling configuration from file.
     *
     * @param path      Path to the XML document containing the pulling configuration
     * @return          The pulling configuration if successfully loaded, null otherwise
     */
    static PullConfigDocument  loadFromFile(final Path  path) {
        PullConfigDocument    pullCfg = null;

        if (Files.isReadable(path)) {
            final Serializer serializer = new Persister();
            try {
                pullCfg = serializer.read(PullConfigDocument.class, path.toFile());
                log.debug("Loaded configuration");
            } catch (final Exception ex) {
                log.error("Error reading configuration from {}! Details: {} - {}", path.toString(), 
                			ex.getClass().getSimpleName(), ex.getMessage());
            }
        } else
            log.warn("Configuration file not available/readible at {}", path.toString());

        return pullCfg;
    }
}

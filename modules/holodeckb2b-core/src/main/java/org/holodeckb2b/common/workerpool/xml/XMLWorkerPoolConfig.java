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
package org.holodeckb2b.common.workerpool.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Implements the configuration of a WorkerPool as defined by {@link IWorkerPoolConfiguration} using XML.
 * <p>The pool configuration is expressed by a simple XML document with a <code>workers</code> root element
 * and one or more <code>worker</code> child that specify the workers to be in the pool.
 *
 * @see XMLWorkerConfig
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Root(name = "workers", strict=false)
public class XMLWorkerPoolConfig implements IWorkerPoolConfiguration {

    @Attribute(name="poolName", required=false)
    private String  name;

    @ElementList(entry="worker", type=XMLWorkerConfig.class, inline=true, required=false)
    private List<IWorkerConfiguration> workers = new ArrayList<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<IWorkerConfiguration> getWorkers() {
        return workers;
    }

    /**
     * Loads the worker pool configuration from file.
     *
     * @param path      Path to the XML document containing the pool's configuration
     * @return          The worker pool configuration if succesfully loaded, null otherwise
     */
    public static IWorkerPoolConfiguration  loadFromFile(final String  path) {
        final Log log = LogFactory.getLog(XMLWorkerPoolConfig.class);
        XMLWorkerPoolConfig    poolCfg = null;

        log.debug("Loading worker pool configuration from XML document in " + path);

        final File f = new File(path);

        if (f.exists() && f.canRead()) {
            final Serializer serializer = new Persister();
            try {
                poolCfg = serializer.read(XMLWorkerPoolConfig.class, f);
                // If config file does not set pool name, set it here to file name
                if (poolCfg.getName() == null || poolCfg.getName().isEmpty())
                    poolCfg.name = f.getName().substring(0, f.getName().indexOf("."));
                log.debug("Loaded configuration");
            } catch (final Exception ex) {
                log.error("Error while reading configuration from " + path + "! Details: " + ex.getMessage());
            }
         } else
            log.error("Unable to access configuration file" + path + "!");


        return poolCfg;
    }
}

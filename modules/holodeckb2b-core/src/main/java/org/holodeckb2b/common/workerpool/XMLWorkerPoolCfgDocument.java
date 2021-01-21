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
package org.holodeckb2b.common.workerpool;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;

/**
 * Represents the <code>workers</code> XML document element of the XML based worker pool configuration as specified by 
 * XML schema <i>http://holodeck-b2b.org/schemas/2012/12/workers</i>. It is defined as: 
 * <pre>
 *  &lt;workers
 *      refresh=<i>positiveInteger : the interval, in seconds, at which the configuration should be refreshed</i>
 *  &gt;
 *      &lt;-- <i>List of worker configuration that specify the tasks to be executed within this pool </i>--&gt;
 *      &lt;worker&gt; 
 *      ...
 *      &lt;/worker&gt;
 *  &lt;/worker&gt;<br></pre>
 * <b>NOTE:</b> The XML schema also defines a <i>poolName</i> attribute. Due to a change in the way how worker pools are
 * managed this attribute is deprecated and not used anymore since version 5.1.0.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.1.0
 */
@Root (name="worker", strict = false)
class XMLWorkerPoolCfgDocument {
	/**
	 * XML parser 
	 */
	private static final Persister	xmlReader = new Persister();

	/*
	 * XML definition
	 */
	@Attribute(name="refresh", required=false)
	private Integer  refreshInterval;
	
    @ElementList(entry="worker", type=XMLWorkerConfig.class, inline=true, required=false)
    private List<IWorkerConfiguration> workers = new ArrayList<>();

    /**
     * Creates a new instance by reading the worker pool configuration from the XML document at the specified path.
     * 
     * @param xmlFile path to the file containing the configuration document
     * @return	a new instance based on the provided XML document
     * @throws WorkerPoolException	if the document could not be processed, for example because it does not exist or
     * 								does not contain a valid configuration
     */
    static XMLWorkerPoolCfgDocument readFromFile(Path xmlFile) throws WorkerPoolException { 
    	try {
    		return xmlReader.read(XMLWorkerPoolCfgDocument.class, xmlFile.toFile()); 
    	} catch (Exception readFailure) {
    		throw new WorkerPoolException(readFailure);
    	}		
	}

    /**
     * @return the value of the <code>refresh</code> attribute. 
     */
    Integer getRefreshInterval() {
    	return refreshInterval;
    }
    
    /**
     * @return the list of worker configurations as contained in the <code>worker</code> elements. 
     */
    List<IWorkerConfiguration> getWorkers() {
    	return workers;
    }
}

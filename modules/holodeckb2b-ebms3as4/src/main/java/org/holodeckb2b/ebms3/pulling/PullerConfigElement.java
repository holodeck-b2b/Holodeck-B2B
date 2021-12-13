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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.holodeckb2b.interfaces.general.Interval;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Represents the <code>genericPullerType</code> complex type defined in the XML schema 
 * <i>http://holodeck-b2b.org/schemas/2014/05/pullconfiguration≤/i>. It implements {@link IWorkerConfiguration} so it 
 * can directly be used for the configuration of the {@link PullWorker}s in the pull worker pool. 
 * <p>There are two type of pull workers: one specific for a set of P-Modes and a default one responsible for all other
 * P-Modes. Both type share this configuration which includes a list of P-Modes which for the specific worker indicates
 * the P-Modes for which to send the pull requests and for the default worker which P-Modes to exclude.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see PullConfigDocument
 */
@Root
public class PullerConfigElement implements IWorkerConfiguration {

    /**
     * Represents the <code>pmode</code> element in the XML document for the pulling configuration
     */
    @Root
    static class PMode {
        @Attribute(required = true)
        private String id;
    }

    @Attribute(required = true)
    int interval;

    @ElementList(required = false)
    List<PMode> pmodes;

    /**
     * The name to assign to this <i>pull worker</i>. Is not read from the XML configuration but assigned at runtime
     * by the pool configuration
     *
     * @see PullConfigDocument
     */
    String  name;

    /**
     * Indicator whether the pull operation should (value = <code>true</code>) or should not (value = <code>false</code>)
     * be executed for the given P-Modes.
     */
    boolean inclusive = true;

    @Override
    public String getName() {
        return name;
    }

    /**
     * @return Fixed value "<i>org.holodeckb2b.ebms3.pulling.PullWorker</i>"
     */
    @Override
    public String getWorkerTask() {
        return org.holodeckb2b.ebms3.pulling.PullWorker.class.getName();
    }

    /**
     * Gets the parameters for the <i>pull worker</i> which are a list of P-Mode ids (parameter defined by
     * {@link PullWorker#PARAM_PMODES}} and an indication (parameter defined by {@link PullWorker#PARAM_INCLUDE}}
     * whether pulling should or should not be executed for these P-Modes.
     *
     * @return  The parameters for the pull worker
     */
    @Override
    public Map<String, Object> getTaskParameters() {
        final Map<String, Object> params = new HashMap<>();
        final Collection<String> pmodeIds = new ArrayList<>();
        for(final PMode p : pmodes)
            pmodeIds.add(p.id);

        params.put(PullWorker.PARAM_PMODES, pmodeIds);
        params.put(PullWorker.PARAM_INCLUDE, inclusive);

        return params;
    }

    /**
     * @return true if a interval is specified
     */
    @Override
    public boolean activate() {
        return interval > 0;
    }

    /**
     * @return The
     */
    public int getDelay() {
        // Return value is in millieseconds, interval is spec'd in seconds
        return interval * 1000;
    }

    /**
     * @return 0 as the <i>pull workers</i> should not run concurrently
     */
    @Override
    public int getConcurrentExecutions() {
        return 0;
    }

    @Override
    public Interval getInterval() {
        return new Interval(interval, TimeUnit.SECONDS);
    }

}

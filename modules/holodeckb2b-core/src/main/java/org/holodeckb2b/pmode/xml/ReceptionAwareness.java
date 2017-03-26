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
package org.holodeckb2b.pmode.xml;

import java.util.concurrent.TimeUnit;
import org.holodeckb2b.interfaces.as4.pmode.IReceptionAwareness;
import org.holodeckb2b.interfaces.general.Interval;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Validate;
import org.simpleframework.xml.core.ValueRequiredException;

/**
 * Represents the <code>ReceptionAwareness<code> element from the P-Mode document. Contains the configuration settings
 * for the reception awareness feature.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @author Bram Bakx <bram at holodeck-b2b.org>
 *
 * @see IReceptionAwareness
 */
public class ReceptionAwareness implements IReceptionAwareness {

    @Element(name = "MaxRetries", required = false)
    private int maxRetries = -1;

    @Element(name = "RetryInterval", required = false)
    private long retryIntervalDuration = -1;

    @Element(name = "UseDuplicateElimination", required = false)
    private Boolean useDupElimination = Boolean.TRUE;

    @Transient
    private Interval retryInterval;

    /**
     * Validates the data read from the XML document by checking that when <code>MaxRetries</code> is supplied
     * <code>RetryInterval</code> contains positive non zero value;</li></ol>
     *
     * @throws Exception When the read XML is not valid
     */
    @Validate
    public void validate() throws Exception {
        if (maxRetries > -1)
            if (retryIntervalDuration <= 0)
                throw new ValueRequiredException("ReceptionAwareness/RetryInterval must have positive non zero value");
    }

    /**
     * Is a helper to construct the {@link Interval} object. Uses the commit function of the Simple framework.
     */
    @Commit
    public void calculateInterval() {
        retryInterval = new Interval(retryIntervalDuration, TimeUnit.SECONDS);
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public Interval getRetryInterval() {
        return retryInterval;
    }

    @Override
    public boolean useDuplicateDetection() {
        return useDupElimination;
    }

}

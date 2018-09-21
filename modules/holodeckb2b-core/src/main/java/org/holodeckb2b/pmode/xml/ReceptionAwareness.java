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
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.as4.pmode.IReceptionAwareness;
import org.holodeckb2b.interfaces.general.Interval;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.ElementException;
import org.simpleframework.xml.core.TextException;
import org.simpleframework.xml.core.Validate;
import org.simpleframework.xml.core.ValueRequiredException;

/**
 * Represents the <code>ReceptionAwareness<code> element from the P-Mode document. Contains the configuration settings
 * for the reception awareness feature.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @author Bram Bakx (bram at holodeck-b2b.org)
 *
 * @see IReceptionAwareness
 */
public class ReceptionAwareness implements IReceptionAwareness {

    @Element(name = "MaxRetries", required = false)
    private int maxRetries = -1;

    @Element(name = "RetryInterval", required = false)
    private long fixedInterval = -1;

    @Element(name = "WaitIntervals", required = false)
    private String flexibleIntervalsText = null;

    @Element(name = "UseDuplicateElimination", required = false)
    private boolean useDupElimination = true;

    @Transient
    private Interval[] waitIntervals = null;

    /**
     * Validates the data read from the XML document by checking that when <code>MaxRetries</code> is supplied
     * <code>RetryInterval</code> contains positive non zero value.
     *
     * @throws Exception When the read XML is not valid
     */
    @Validate
    public void validate() throws Exception {
        if (maxRetries > -1)
            if (fixedInterval <= 0)
                throw new ValueRequiredException(
                            "ReceptionAwareness/RetryInterval must have positive non zero value if MaxRetries is set");
            else if (!Utils.isNullOrEmpty(flexibleIntervalsText))
                throw new ElementException("Invalid combination of MaxRetries and WaitIntervals settings");

        // Create the array of intervals and check also that the specified intervals are valid
        calculateIntervals();
    }

    /**
     * Is a helper to construct the array of {@link Interval} objects.
     */
    private void calculateIntervals() throws TextException {
        if (!Utils.isNullOrEmpty(flexibleIntervalsText)) {
            if (flexibleIntervalsText.endsWith(","))
                throw new TextException("WaitIntervals does not contain valid list of intervals!");
            try {
                final String[] sIntervals = flexibleIntervalsText.split(",");
                this.waitIntervals = new Interval[sIntervals.length];
                for(int i = 0; i < sIntervals.length; i++) {
                    int intervalLength = Integer.parseInt(sIntervals[i].trim());
                    if (intervalLength < 0)
                        throw new TextException("WaitIntervals must contain non-negative integers!");
                    this.waitIntervals[i] = new Interval(intervalLength, TimeUnit.SECONDS);
                }
            } catch (NumberFormatException nan) {
                throw new TextException("WaitIntervals does not contain valid list of intervals!");
            }
        } else if (maxRetries >= 0) {
            // Configuration with fixed intervals, convert to new structure so add an extra wait interval
            waitIntervals = new Interval[maxRetries + 1];
            for(int i = 0; i < maxRetries + 1; i++)
                waitIntervals[i] = new Interval(fixedInterval, TimeUnit.SECONDS);
        } 
    }


    @Override
    public boolean useDuplicateDetection() {
        return useDupElimination;
    }

    /**
     * Gets an array of nIntervals to wait for a <i>Receipt</i> Signal before a <i>User Message</i> should be
     * retransmitted.
     * <p>If the fixed intervals are used in the XML it is converted to the new method.
     *
     * @return The periods to wait for a <i>Receipt</i> Signal expressed as an array of {@link Interval}
     * @since 4.0.0
     */
    @Override
    public Interval[] getWaitIntervals() {
        return waitIntervals;
    }

    /**
     * Parses a string containing a comma-separated list of integers to an array of integers.
     *
     * @param intervalText  The text containing the comma separated list
     * @return  An array containing the integers from the list, or<br>
     *          <code>null</code> if the string could not be parsed
     */
    private static int[] parseRetryIntervals(final String intervalText) {
        if (Utils.isNullOrEmpty(intervalText))
            return null;

        try {
            final String[] sIntervals = intervalText.split(",");
            final int[] nIntervals = new int[sIntervals.length];
            for(int i = 0; i < sIntervals.length ; i++)
                nIntervals[i] = Integer.parseInt(sIntervals[i]);
            return nIntervals;
        } catch (NumberFormatException nan) {
            // The given text does not contain a comma-separated list of integers
            return null;
        }
    }
}

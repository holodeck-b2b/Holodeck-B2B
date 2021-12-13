/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.Interval;
import org.holodeckb2b.interfaces.pmode.IReceptionAwareness;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.ElementException;
import org.simpleframework.xml.core.Persist;
import org.simpleframework.xml.core.TextException;
import org.simpleframework.xml.core.Validate;
import org.simpleframework.xml.core.ValueRequiredException;

/**
 * Contains the parameters related to the configuration of the AS4 Reception
 * Awareness feature.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class ReceptionAwarenessConfig implements IReceptionAwareness, Serializable {
	private static final long serialVersionUID = -3921059933083375333L;

	@Element(name = "MaxRetries", required = false)
	private Integer maxRetries;

	@Element(name = "RetryInterval", required = false)
	private Long fixedInterval;

	@Element(name = "WaitIntervals", required = false)
	private String flexibleIntervalsText = null;

	@Element(name = "UseDuplicateElimination", required = false)
	private Boolean useDupElimination;

	@Transient
	private Interval[] intervals = null;

	/**
	 * Default constructor creates a new and empty
	 * <code>ReceptionAwarenessConfig</code> instance.
	 */
	public ReceptionAwarenessConfig() {
	}

	/**
	 * Creates a new <code>ReceptionAwarenessConfig</code> instance using the parameters from the provided 
	 * {@link IReceptionAwareness} object.
	 *
	 * @param source The source object to copy the parameters from
	 */
	public ReceptionAwarenessConfig(final IReceptionAwareness source) {
		this.intervals = source.getWaitIntervals();
		if (intervals == null || intervals.length == 0)
			this.useDupElimination = Boolean.valueOf(source.useDuplicateDetection());		
	}

	/**
	 * Validates the data read from the XML document by checking that either a number of fixed intervals is specified 
	 * using the <code>MaxRetries</code> and <code>RetryInterval</code> elements or that a custom series of intervals 
	 * is specified in the <code>WaitIntervals</code> element.
	 * <p>Also converts both to the array of actual intervals to use.
	 *
	 * @throws Exception When the read XML is not valid
	 */
	@Validate
	public void validate() throws Exception {
		if (maxRetries != null)
			if (fixedInterval == null)
				throw new ValueRequiredException(
						"ReceptionAwareness/RetryInterval must have positive non zero value if MaxRetries is set");
			else if (!Utils.isNullOrEmpty(flexibleIntervalsText))
				throw new ElementException("Invalid combination of MaxRetries and WaitIntervals settings");

		// Create the array of intervals and check also that the specified intervals are
		// valid
		calculateIntervals();		
	}

	@Persist
	public void createIntervalString() {
		if (intervals != null && intervals.length > 0) {
			StringBuilder csList = new StringBuilder(); 
			for(int i = 0; i < intervals.length; i++) {				
				csList.append(TimeUnit.SECONDS.convert(intervals[i].getLength(), intervals[i].getUnit()));
				if (i < intervals.length - 1)
					csList.append(',');
			}
			flexibleIntervalsText = csList.toString();
			maxRetries = null;
			fixedInterval = null;
		} else
			flexibleIntervalsText = null;
	}
	
	@Override
	public Interval[] getWaitIntervals() {
		return intervals;
	}

	public void setWaitIntervals(Interval[] newIntervals) {
		this.intervals = newIntervals;
	}

	@Override
	public boolean useDuplicateDetection() {
		return useDupElimination == null ? true : useDupElimination;
	}

	public void setDuplicateDetection(final boolean useDupDetection) {
		this.useDupElimination = useDupDetection;
	}

	/**
	 * Is a helper to construct the array of {@link Interval} objects.
	 * 
	 * @throws TextException When the String containing the list of intervals is not a comma-separated list of integers
	 */
	private void calculateIntervals() throws TextException {
		if (!Utils.isNullOrEmpty(flexibleIntervalsText)) {
			if (flexibleIntervalsText.endsWith(","))
				throw new TextException("WaitIntervals does not contain valid list of intervals!");
			try {
				final String[] sIntervals = flexibleIntervalsText.split(",");
				this.intervals = new Interval[sIntervals.length];
				for (int i = 0; i < sIntervals.length; i++) {
					int intervalLength = Integer.parseInt(sIntervals[i].trim());
					if (intervalLength < 0)
						throw new TextException("WaitIntervals must contain non-negative integers!");
					this.intervals[i] = new Interval(intervalLength, TimeUnit.SECONDS);
				}
			} catch (NumberFormatException nan) {
				throw new TextException("WaitIntervals does not contain valid list of intervals!");
			}
		} else if (maxRetries != null && maxRetries >= 0) {
			// Configuration with fixed intervals, convert to new structure so add an extra
			// wait interval
			intervals = new Interval[maxRetries + 1];
			for (int i = 0; i < maxRetries + 1; i++)
				intervals[i] = new Interval(fixedInterval.longValue(), TimeUnit.SECONDS);
		}
	}
}

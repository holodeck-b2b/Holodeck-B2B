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
 * Contains the parameters related to the configuration of the AS4 Reception
 * Awareness feature.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class ReceptionAwarenessConfig implements IReceptionAwareness, Serializable {
	private static final long serialVersionUID = -3921059933083375333L;

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
		this.useDupElimination = source.useDuplicateDetection();
		setWaitIntervals(source.getWaitIntervals());
	}

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

		// Create the array of intervals and check also that the specified intervals are
		// valid
		calculateIntervals();
	}

	@Override
	public Interval[] getWaitIntervals() {
		if (waitIntervals == null)
			try {
				calculateIntervals();
			} catch (TextException e) {
			}

		return waitIntervals;
	}

	public void setWaitIntervals(Interval[] newIntervals) {
		this.waitIntervals = newIntervals;
		if (waitIntervals.length > 0) {
			StringBuilder csList = new StringBuilder(); 
			for(int i = 0; i < waitIntervals.length; i++) {
				csList.append(waitIntervals[i]);
				if (i < waitIntervals.length - 1)
					csList.append(',');
			}
			flexibleIntervalsText = csList.toString();
		} else
			flexibleIntervalsText = null;
	}

	@Override
	public boolean useDuplicateDetection() {
		return useDupElimination;
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
				this.waitIntervals = new Interval[sIntervals.length];
				for (int i = 0; i < sIntervals.length; i++) {
					int intervalLength = Integer.parseInt(sIntervals[i].trim());
					if (intervalLength < 0)
						throw new TextException("WaitIntervals must contain non-negative integers!");
					this.waitIntervals[i] = new Interval(intervalLength, TimeUnit.SECONDS);
				}
			} catch (NumberFormatException nan) {
				throw new TextException("WaitIntervals does not contain valid list of intervals!");
			}
		} else if (maxRetries >= 0) {
			// Configuration with fixed intervals, convert to new structure so add an extra
			// wait interval
			waitIntervals = new Interval[maxRetries + 1];
			for (int i = 0; i < maxRetries + 1; i++)
				waitIntervals[i] = new Interval(fixedInterval, TimeUnit.SECONDS);
		}
	}
}

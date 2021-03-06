/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.general;


import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Represents a time interval defined by the length of the interval and the unit of time
 * the length is specified in.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class Interval implements Serializable {
	private static final long serialVersionUID = -8248625111840397966L;

	/**
     * The unit of time the interval is specified in
     */
    private TimeUnit  unit;

    /**
     * The length of the interval
     */
    private long  length;

    /**
     * Create a new <code>Interval</code> interval with specified length
     *
     * @param length the length to set
     * @param unit the unit to set
     */
    public Interval(final long length, final TimeUnit unit) {
        this.length = length;
        this.unit = unit;
    }

    /**
     * Creates a new <code>Interval</code> instance that is a copy of the given instance.
     *
     * @param source    The <code>Interval</code> instance to copy
     * @since 4.0.0
     */
    public Interval(final Interval source) {
        this.length = source.length;
        this.unit = source.unit;
    }

    /**
     * Gets the unit of time the interval is specified in
     *
     * @return the unit
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Sets the unit of time the interval is specified in
     *
     * @param unit the unit to set
     */
    public void setUnit(final TimeUnit unit) {
        this.unit = unit;
    }

    /**
     * Gets the length of the interval
     *
     * @return the length
     */
    public long getLength() {
        return length;
    }

    /**
     * Sets the length of the interval
     *
     * @param length the length to set
     */
    public void setLength(final long length) {
        this.length = length;
    }

    /**
     * Determines if two <code>Interval</code> objects are equal, i.e. last the same amount of time.
     *
     * @param   o   The object to compare with, should be an instance of {@link Interval}
     * @return      <code>true</code> if the intervals last the same amount of time
     *              <code>false</code> otherwise
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this)
          return true;
        if (o == null || !getClass ().equals (o.getClass ()))
          return false;

        final Interval rhs = (Interval) o;
        if (this.unit ==  rhs.unit)
            // Unit of time equal, so directly compare lengths
            return this.length == rhs.length;
        else
            // Units not equal, convert to smallest unit of time
            if (this.unit.ordinal() < rhs.unit.ordinal())
                return this.length == this.unit.convert(rhs.length, rhs.unit);
            else
                return rhs.unit.convert(this.length, this.unit) == rhs.length;
   }
}

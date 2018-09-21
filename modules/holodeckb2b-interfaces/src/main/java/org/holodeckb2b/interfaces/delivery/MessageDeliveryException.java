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
package org.holodeckb2b.interfaces.delivery;

/**
 * Indicates a problem that occurred in the delivery of a message unit to the business application. This exception can
 * be thrown because a delivery method can not be successfully instantiated, i.e. the factory class can not be
 * initialized or during the actual delivery of a message.
 * <p>Since version 4.0.0 the exception also has an indicator whether the encountered problem is
 * permanent or not, i.e. any new attempt of delivery is guaranteed to fail as well making retries meaningless. This
 * indicator is used when the delivery of a <i>User Message</i> message unit fails. When the delivery error is permanent
 * the delivery handler can generate an ebMS Error Signal to inform the <i>Sending</i> MSH.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IMessageDeliverer
 * @see IMessageDelivererFactory
 */
public class MessageDeliveryException extends Exception {
    /**
     * Indicator whether this delivery exception represents a permanent or recoverable failure. For backward
     * compatibility the default is <code>false</code>
     */
    private boolean permanent = false;

    public MessageDeliveryException() {
        super();
    }

    public MessageDeliveryException(final String message) {
        super(message);
    }

    public MessageDeliveryException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new <code>MessageDeliveryException</code> that includes a message explaining why the delivery fails
     * and the indicator whether the failure is permanent.
     *
     * @param message       Textual explanation of the failure
     * @param isPermanent   Indication whether the failure is permanent
     * @since 4.0.0
     */
    public MessageDeliveryException(final String message, final boolean isPermanent) {
        this(message, isPermanent, null);
    }

    /**
     * Creates a new <code>MessageDeliveryException</code> that includes a message explaining why the delivery fails,
     * the <code>Throwable</code> that caused the failure and the indicator whether the failure is permanent.
     *
     * @param message       Textual explanation of the failure
     * @param isPermanent   Indication whether the failure is permanent
     * @param cause         The exception that caused the delivery failure
     * @since 4.0.0
     */
    public MessageDeliveryException(final String message, final boolean isPermanent, final Throwable cause) {
        super(message, cause);
        this.permanent = isPermanent;
    }

    /**
     * Gets the indication whether this delivery failure is permanent.
     *
     * @return <code>true</code> when this exception represent a permanent error, i.e. a new delivery attempt will again
     *          result in an error, or<br>
     *         <code>false</code> if the error that causes this exception is recoverable and a new deliver attempt may
     *          succeed
     * @since 4.0.0
     */
    public boolean isPermanent() {
        return this.permanent;
    }

    /**
     * Sets the indication whether this delivery failure is permanent.
     *
     * @param permanent Indicator whether this failure is permanent or not
     * @since 4.0.0
     */
    public void setPermanent(final boolean permanent) {
        this.permanent = permanent;
    }
}

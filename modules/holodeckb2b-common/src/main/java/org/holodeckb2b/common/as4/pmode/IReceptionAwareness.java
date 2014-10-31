/*
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

package org.holodeckb2b.common.as4.pmode;

import org.holodeckb2b.common.util.Interval;

/**
 * Represents P-Mode parameters related to the <i>AS4 Reception Awareness feature</i>. This feature is a (simple)
 * reliability protocol that uses ebMS Receipt signals for acknowledgments. See section 3.2 of the AS4 profile for more
 * information.
 * <p>The AS4 profile defines five additional P-Mode parameters for the reception awareness feature. Holodeck B2B uses
 * only three parameters as some of the P-Mode parameters from the spec are combined:<ol>
 * <li><b>Maximum number of retries</b> : Indicates how many times a message should be resende if no Receipt is 
 * received. Setting this value to zero disables the retry functionality;</li>
 * <li><b>Interval before resend</b> : The time to wait for a Receipt and before a message is resend;</li>
 * <li><b>Use duplicate elimination</b> : Indication whether a message that is received twice should be delivered to the
 * business application or not. If enabled Holodeck B2B will search all received messages in database to check if the
 * message was received (and delivered) before. There is no further parameterization.</li>
 * </ol>
 * Enabling the Reception Awareness feature itself is done by including an object of this type on the leg, i.e. when
 * {@link ILegAS4#getReceptionAwareness()} returns a non-null value. Note that we use a special type of leg ({@link 
 * ILegAS4}) to indicate that the leg includes AS4 specific features.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IReceptionAwareness {

    /**
     * Gets the maximum number of times a message should be retransmitted when no receipt is received. If a message
     * should not be retransmitted this method must return 0. In this case a <i>MissingReceipt</i> error will be 
     * generated directly after the first "retry" interval has elapsed.
     * 
     * @return  The maximum number of retries to executed. 0 if a message should not be retransmitted.
     */
    public int getMaxRetries();
    
    /**
     * Gets the period to wait for a receipt signal before a message should be retransmitted (if there are retries left)
     * 
     * @return The period to wait for a receipt signal expressed as an {@link Interval}
     */
    public Interval getRetryInterval();
    
    /**
     * Indicates whether duplicate detection and elimination should be used. 
     * 
     * @return  <code>true</code> if duplicates should be detected and eliminated,<br>
     *          <code>false</code> if messages should always be delivered even when duplicate
     */
    public boolean useDuplicateDetection();
}

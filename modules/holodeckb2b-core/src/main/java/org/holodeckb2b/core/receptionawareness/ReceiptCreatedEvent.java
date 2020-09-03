/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.receptionawareness;

import org.holodeckb2b.common.events.impl.AbstractMessageProcessingEvent;
import org.holodeckb2b.interfaces.events.IReceiptCreated;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is the implementation class of {@link IReceiptCreated} to indicate that a Receipt is created for a received
 * User Message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.1.0
 */
public class ReceiptCreatedEvent extends AbstractMessageProcessingEvent implements IReceiptCreated {

    /**
     * The Receipt that was created for the received User Message
     */
    private final IReceipt    receipt;

    /**
     * Indicator whether the User Message is a duplicate and therefore not delivered to the business application. Note
     * that this only applies to P-Modes that use the duplicate detection and elimination function of the AS4 Reception
     * Awareness feature.
     */
    private final boolean     forDuplicate;

    /**
     * Creates a new <code>ReceiptCreatedEvent</code> for the given User Message and Receipt without duplicate indicator
     * <p>NOTE: As Receipt can only be created for User Message message units there is no constructor for other message
     * unit types.
     *
     * @param subject The received User Message for which the Receipt was created
     * @param receipt The created Receipt
     */
    public ReceiptCreatedEvent(final IUserMessage subject, final IReceipt receipt) {
        this(subject, receipt, false);
    }

    /**
     * Creates a new <code>ReceiptCreatedEvent</code> for the given User Message, Receipt and duplicate indicator.
     * <p>NOTE: As Receipt can only be created for User Message message units there is no constructor for other message
     * unit types.
     *
     * @param subject           The received User Message for which the Receipt was created
     * @param receipt           The created Receipt
     * @param isForDuplicate    Indicates whether the received User Message is a duplicate and not delivered to the
     *                          business application.
     */
    public ReceiptCreatedEvent(final IUserMessage subject, final IReceipt receipt, final boolean isForDuplicate) {
        super(subject);
        this.receipt = receipt;
        this.forDuplicate = isForDuplicate;
    }

    @Override
    public IReceipt getReceipt() {
        return receipt;
    }

    @Override
    public boolean isForEliminatedDuplicate() {
        return forDuplicate;
    }
}

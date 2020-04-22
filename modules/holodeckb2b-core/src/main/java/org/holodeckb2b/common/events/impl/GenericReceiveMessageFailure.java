/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.events.impl;

import org.holodeckb2b.interfaces.events.IReceivedMessageProcessingFailure;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is an implementation class of {@link IReceivedMessageProcessingFailure} used to indicate that an error occurred 
 * during the processing of an incoming message unit for which no other specific event is defined. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @@since 5.0.0
 */
public class GenericReceiveMessageFailure extends AbstractMessageProcessingEvent 
																		implements IReceivedMessageProcessingFailure {

    /**
     * Creates a new <code>GenericReceiveMessageFailure</code> for the unsuccessful processing of the given message unit 
     *
     * @param subject   			The failed message unit 
     * @param failureDescription   	Descriptive text of the reason why the processing failed
     */
    public GenericReceiveMessageFailure(final IMessageUnit subject, final String failureDescription) {
        super(subject, failureDescription);
    }
}

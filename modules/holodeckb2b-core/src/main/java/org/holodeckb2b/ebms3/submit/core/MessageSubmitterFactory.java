/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.submit.core;

import org.holodeckb2b.common.submit.IMessageSubmitter;
import org.holodeckb2b.common.submit.IMessageSubmitterFactory;

/**
 * Is an implementation of {@see IMessageSubmitterFactory} to create instances of
 * {@see MessageSubmitter} which can be used to submit user messages to the Holodeck B2B
 * core engine.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class MessageSubmitterFactory implements IMessageSubmitterFactory {

    /**
     * Returns an instance of {@see MessageSubmitter} that can be used to submit
     * an user message to Holodeck B2B.
     * 
     * @return      The MessageSubmitter instance to use for submission
     */
    @Override
    public IMessageSubmitter createMessageSubmitter() {
        return new MessageSubmitter();
    }
    
}

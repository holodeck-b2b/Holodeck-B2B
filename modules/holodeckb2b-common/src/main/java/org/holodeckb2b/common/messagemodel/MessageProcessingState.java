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
package org.holodeckb2b.common.messagemodel;

import java.util.Date;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;

/**
 * Is an implementation of {@link IMessageUnitProcessingState} used in the parent class to hold the processing
 * states of the message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class MessageProcessingState implements IMessageUnitProcessingState {

        private ProcessingState state;
        private Date            startTime;

        /**
         * Creates a new <code>MessageProcessingState</code> object with the given state and the current time as start
         */
        public MessageProcessingState(final ProcessingState state) {
            this.state = state;
            this.startTime = new Date();
        }

        /**
         * Creates a new <code>MessageProcessingState</code> object by copying the information from the given state
         *
         * @param sourceState  The source processing state to copy the information from
         */
        public MessageProcessingState(final IMessageUnitProcessingState sourceState) {
            if (sourceState != null) {
                this.state = sourceState.getState();
                this.startTime = sourceState.getStartTime();
            }
        }

        @Override
        public ProcessingState getState() {
            return state;
        }

        @Override
        public Date getStartTime() {
            return startTime;
        }
    }

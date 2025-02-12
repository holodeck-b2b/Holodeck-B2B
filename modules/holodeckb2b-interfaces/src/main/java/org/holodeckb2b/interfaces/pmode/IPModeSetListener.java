/**
 * Copyright (C) 2025 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.interfaces.pmode;

import java.util.EventListener;

import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;

/**
 * Defines the interface for components that want to be notified when changes occur in the set of registered P-Modes.
 * Such components for example could be caches that need to be updated based on the latest P-Mode configuration.
 * <p>
 * Event listeners must be registered using the {@link IPModeSet#registerEventListener(IPModeSetListener,
 * org.holodeckb2b.interfaces.pmode.PModeSetEvent.PModeSetAction...) registerEventListener()} method of the {@link
 * IPModeSet} returned by calling {@link HolodeckB2BCoreInterface#getPModeSet()}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 * @see PModeSetEvent
 * @see IPModeSet
 */
public interface IPModeSetListener extends EventListener {

	/**
	 * Is called by the {@link IPModeSet} when an event for which this listener is registered occurs.
	 * <p>
	 * NOTE: The call to this method is done synchronously on the calling thread, so the listener should not block
	 * execution.
	 *
	 * @param event the event that occurred
	 */
	void handleEvent(PModeSetEvent event);
}

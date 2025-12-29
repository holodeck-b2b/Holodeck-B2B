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

import java.util.EventObject;

/**
 * Represents an event that occurred on the P-Mode set. The {@link #getSource()} is the P-Mode subject of the change in
 * the P-Mode set. The {@link #getEventType()} indicates whether the P-Mode was added, updated or removed from the
 * P-Mode set. If the action that was attempted failed, the {@link #getFailure()} will be non-<code>null</code> and
 * contain the {@link PModeSetException} that caused the failure.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 8.0.0
 */
public class PModeSetEvent extends EventObject {
	private static final long serialVersionUID = 3126817043068658848L;

	/**
	 * Enumerates the actions that can cause the event to occur.
	 */
	public enum PModeSetAction {
		ADD,
		UPDATE,
		REMOVE
	}

	private final PModeSetAction 	action;
	private final PModeSetException failure;

	/**
	 * Creates a new event for a successfully executed action on the given P-Mode.
	 *
	 * @param source	the P-Mode subject of the event
	 * @param action	action that caused the event, i.e. add, update or remove
	 */
	public PModeSetEvent(IPMode source, PModeSetAction action) {
		super(source);
		this.action = action;
		this.failure = null;
	}

	/**
	 * Creates a new event for a failed action on the given P-Mode.
	 *
	 * @param source	the P-Mode subject of the event
	 * @param action	action that caused the event, i.e. add, update or remove
	 * @param failure	{@link PModeSetException} that caused the action to fail
	 */
	public PModeSetEvent(IPMode source, PModeSetAction action, PModeSetException failure) {
		super(source);
		this.action = action;
		this.failure = failure;
	}

	@Override
	public IPMode getSource() {
		return (IPMode) super.getSource();
	}

	/**
	 * Indicates whether the P-Mode was add, updated or removed from the P-Mode set.
	 *
	 * @return the type of the event that occurred
	 */
	public PModeSetAction getEventType() {
		return action;
	}

	/**
	 * If the action that was attempted failed, this method returns the {@link PModeSetException} that caused the
	 * failure.
	 *
	 * @return the {@link PModeSetException} that caused the attempted action to fail or<br/>
	 * 		   <code>null</code> if the action was successful
	 */
	public PModeSetException getFailure() {
		return failure;
	}
}

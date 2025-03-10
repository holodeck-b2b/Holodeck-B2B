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
package org.holodeckb2b.storage.metadata;

import java.util.List;
import java.util.stream.Collectors;

import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.storage.metadata.jpa.ErrorMessage;
import org.holodeckb2b.storage.metadata.jpa.JPAEntityObject;
import org.holodeckb2b.storage.metadata.jpa.MessageUnit;
import org.holodeckb2b.storage.metadata.jpa.PayloadInfo;
import org.holodeckb2b.storage.metadata.jpa.PullRequest;
import org.holodeckb2b.storage.metadata.jpa.Receipt;
import org.holodeckb2b.storage.metadata.jpa.SelectivePullRequest;
import org.holodeckb2b.storage.metadata.jpa.UserMessage;

/**
 * Is a helper class that provides some utility methods to handle the JPA objects.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 3.0.0
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class JPAObjectHelper {

	/**
	 * Creates a proxy to the given JPA object that holds the meta-data of an Holodeck B2B message unit or payload.
	 *
	 * @param <T>       The type of the JPA object holding the entity data
	 * @param <V>       The type of Holodeck B2B Core entity object
	 * @param jpaObject The JPA entity object to wrap in an entity object
	 * @return The given JPA entity object wrapped in the correct entity object.
	 */
	public static <T extends JPAEntityObject, V extends JPAObjectProxy<T>> V proxy(T jpaObject) {
		if (jpaObject == null)
			return null;

		V proxy = null;
		if (jpaObject instanceof UserMessage)
			proxy = (V) new UserMessageEntity((UserMessage) jpaObject);
		else if (jpaObject instanceof SelectivePullRequest)
			proxy = (V) new SelectivePullRequestEntity((SelectivePullRequest) jpaObject);
		else if (jpaObject instanceof PullRequest)
			proxy = (V) new PullRequestEntity((PullRequest) jpaObject);
		else if (jpaObject instanceof Receipt)
			proxy = (V) new ReceiptEntity((Receipt) jpaObject);
		else if (jpaObject instanceof ErrorMessage)
			proxy = (V) new ErrorMessageEntity((ErrorMessage) jpaObject);
		else if (jpaObject instanceof PayloadInfo)
			proxy = (V) new PayloadEntity((PayloadInfo) jpaObject);
		else
			throw new IllegalArgumentException("Only JPA objects can be wrapped in an entity object");

		// Make sure all meta-data is loaded from the database
		proxy.loadCompletely();
		return proxy;
	}

	/**
	 * Converts the given list of JPA objects holding the meta-data of message units to a list of Holodeck B2B entity
	 * objects, i.e. each member of the list is proxied in the corresponding entity implementation.
	 *
	 * @param <T>        The type of the JPA object holding the meta-data
	 * @param <V>        The type of Holodeck B2B Core entity object
	 * @param jpaObjects The list of JPA entity objects
	 * @return List with all JPA entity objects from the given list wrapped in the correct entity object.
	 */
	public static <T extends JPAEntityObject, V extends JPAObjectProxy<T>> List<V> proxy(List<T> jpaObjects) {
		return jpaObjects.stream().map(jpa -> (V) proxy(jpa)).collect(Collectors.toList());
	}

	/**
	 * Determines the JPA Class that corresponds to the given message unit.
	 *
	 * @param <T>     Restricts this method to only message unit classes
	 * @param msgUnit The message unit type represented by a class descending from {@link IMessageUnit}
	 * @return The JPA class that corresponds to the given message unit type
	 */
	public static <T extends IMessageUnit> Class<? extends MessageUnit> getJPAClass(T msgUnit) {
		return getJPAClass(msgUnit.getClass());
	}

	/**
	 * Determines the JPA Class that corresponds to the given message unit type.
	 *
	 * @param msgUnitType The message unit type represented by a class descending from {@link IMessageUnit}
	 * @return The JPA class that corresponds to the given message unit type
	 */
	public static Class<? extends MessageUnit> getJPAClass(Class<? extends IMessageUnit> msgUnitType) {
		Class jpaClass = null;
		if (IUserMessage.class.isAssignableFrom(msgUnitType))
			jpaClass = UserMessage.class;
		else if (ISelectivePullRequest.class.isAssignableFrom(msgUnitType))
			jpaClass = SelectivePullRequest.class;
		else if (IPullRequest.class.isAssignableFrom(msgUnitType))
			jpaClass = PullRequest.class;
		else if (IReceipt.class.isAssignableFrom(msgUnitType))
			jpaClass = Receipt.class;
		else if (IErrorMessage.class.isAssignableFrom(msgUnitType))
			jpaClass = ErrorMessage.class;
		else
			jpaClass = MessageUnit.class;

		return jpaClass;
	}

}

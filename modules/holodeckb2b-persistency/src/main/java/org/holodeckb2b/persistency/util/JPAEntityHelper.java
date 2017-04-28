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
package org.holodeckb2b.persistency.util;

import java.util.ArrayList;
import java.util.List;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.persistency.entities.ErrorMessageEntity;
import org.holodeckb2b.persistency.entities.MessageUnitEntity;
import org.holodeckb2b.persistency.entities.PullRequestEntity;
import org.holodeckb2b.persistency.entities.ReceiptEntity;
import org.holodeckb2b.persistency.entities.UserMessageEntity;
import org.holodeckb2b.persistency.jpa.ErrorMessage;
import org.holodeckb2b.persistency.jpa.MessageUnit;
import org.holodeckb2b.persistency.jpa.PullRequest;
import org.holodeckb2b.persistency.jpa.Receipt;
import org.holodeckb2b.persistency.jpa.UserMessage;

/**
 * Is a helper class that provides some utility methods to handle the JPA entity objects.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class JPAEntityHelper {


    /**
     * Converts the JPA entity objects to a {@link IMessageUnitEntity} of the correct type.
     *
     * @param <V>               The type of Holodeck B2B Core entity object
     * @param jpaObject         The JPA entity object to wrap in an entity object
     * @param completelyLoaded  Indicator whether all data of the message unit has been loaded
     * @return                  The given JPA entity object wrapped in the correct entity object.
     */
    public static <V extends IMessageUnitEntity> V wrapInEntity(MessageUnit jpaObject, boolean completelyLoaded) {
        if (jpaObject == null)
            return null;

        V msgUnitEntity = null;
        if (jpaObject instanceof UserMessage)
            msgUnitEntity = (V) new UserMessageEntity((UserMessage) jpaObject);
        else if (jpaObject instanceof PullRequest)
            msgUnitEntity = (V) new PullRequestEntity((PullRequest) jpaObject);
        else if (jpaObject instanceof Receipt)
            msgUnitEntity = (V) new ReceiptEntity((Receipt) jpaObject);
        else if (jpaObject instanceof ErrorMessage)
            msgUnitEntity = (V) new ErrorMessageEntity((ErrorMessage) jpaObject);
        else
            throw new IllegalArgumentException("Only JPA objects can be wrapped in an entity object");

        ((MessageUnitEntity) msgUnitEntity).setMetadataLoaded(completelyLoaded);
        return msgUnitEntity;
    }

    /**
     * Converts the given list of JPA entity objects to a list of {@link IMessageUnitEntity} of the correct type, i.e.
     * each member of the list is wrapped in the corresponding entity implementation. This method also assumes that the
     * entities are by default not loaded completely. 
     *
     * @param <V>           The type of Holodeck B2B Core entity object
     * @param jpaObjects    The list of JPA entity objects
     * @return              List with all JPA entity objects from the given list wrapped in the correct entity object.
     */
    public static <T extends IMessageUnit, V extends IMessageUnitEntity> List<V> wrapInEntity(List<T> jpaObjects) {
        if (Utils.isNullOrEmpty(jpaObjects))
            return null;

        List<V> result = new ArrayList(jpaObjects.size());
        for(T jpaObject : jpaObjects)
            result.add((V) wrapInEntity((MessageUnit) jpaObject, false));

        return result;
    }

    /**
     * Determines the JPA Class that corresponds to the given message unit.
     *
     * @param <T>       Restricts this method to only message unit classes
     * @param msgUnit   The message unit type represented by a class descending from {@link IMessageUnit}
     * @return          The JPA class that corresponds to the given message unit type
     */
    public static <T extends IMessageUnit> Class determineJPAClass(T msgUnit) {
        return determineJPAClass(msgUnit.getClass());
    }

    /**
     * Determines the JPA Class that corresponds to the given message unit type.
     *
     * @param <T>           Restricts this method to only message unit classes
     * @param msgUnitType   The message unit type represented by a class descending from {@link IMessageUnit}
     * @return              The JPA class that corresponds to the given message unit type
     */
    public static <T extends IMessageUnit> Class determineJPAClass(Class<T> msgUnitType) {
        Class  jpaEntityClass = null;
        if (IUserMessage.class.isAssignableFrom(msgUnitType))
            jpaEntityClass = UserMessage.class;
        else if (IPullRequest.class.isAssignableFrom(msgUnitType))
            jpaEntityClass = PullRequest.class;
        else if (IReceipt.class.isAssignableFrom(msgUnitType))
            jpaEntityClass = Receipt.class;
        else if (IErrorMessage.class.isAssignableFrom(msgUnitType))
            jpaEntityClass = ErrorMessage.class;
        else
            jpaEntityClass = MessageUnit.class;

        return jpaEntityClass;
    }

}

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
package org.holodeckb2b.persistency.entities;

import java.util.Date;
import java.util.List;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.holodeckb2b.persistency.jpa.MessageUnit;
import org.holodeckb2b.persistency.managers.QueryManager;

/**
 * Is the {@link IMessageUnitEntity} implementation of the default persistency provider of Holodeck B2B. It acts as a
 * proxy to the JPA object that represents the data as stored in the database. Because the JPA object may change when
 * executing database operations a proxy object is used.<br>
 * This is the generic base class that contains the functionality that applies to all message units types. For each
 * specific message unit type there is a non-generic sub class.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @param <T>   The class of the proxied JPA entity object
 * @since  3.0.0
 */
public abstract class MessageUnitEntity<T extends MessageUnit> implements IMessageUnitEntity {

    /**
     * The JPA entity object that is being proxied
     */
    protected T   jpaEntityObject;

    /**
     * Indicates whether all the meta-data on the Message Unit has been loaded
     */
    protected boolean allMetadataLoaded = false;

    /**
     * Creates a new <code>MessageUnitEntity</code> object for the given JPA entity object.
     *
     * @param jpaObject     The JPA entity object to create a proxy for
     */
    public MessageUnitEntity(final T jpaObject) {
        this.jpaEntityObject = jpaObject;
    }

    /**
     * Gets the OID of the JPA object that is being proxied. This OID is needed to retrieve the actual meta-data from
     * the database.
     *
     * @return  The OID of the proxied message unit JPA object
     */
    public long getOID() {
        return jpaEntityObject.getOID();
    }

    /**
     * Updates the JPA object that is being proxied.
     *
     * @param jpaObject
     */
    public void updateJPAObject(final T jpaObject) {
        this.jpaEntityObject = jpaObject;
    }

    @Override
    public ILeg.Label getLeg() {
        return jpaEntityObject.getLeg();
    }

    @Override
    public boolean usesMultiHop() {
        return jpaEntityObject.usesMultiHop();
    }

    @Override
    public Direction getDirection() {
        return jpaEntityObject.getDirection();
    }

    @Override
    public Date getTimestamp() {
        return jpaEntityObject.getTimestamp();
    }

    @Override
    public String getMessageId() {
        return jpaEntityObject.getMessageId();
    }

    @Override
    public String getRefToMessageId() {
        return jpaEntityObject.getRefToMessageId();
    }

    @Override
    public String getPModeId() {
        return jpaEntityObject.getPModeId();
    }

    @Override
    public List<IMessageUnitProcessingState> getProcessingStates() {
        return jpaEntityObject.getProcessingStates();
    }

    @Override
    public IMessageUnitProcessingState getCurrentProcessingState() {
        return jpaEntityObject.getCurrentProcessingState();
    }

    /**
     * Indicates whether all meta-data of the Message Unit has been loaded from the database.
     *
     * @return <code>true</code> if all meta-data on the User Message has been loaded, <code>false</code> otherwise
     */
    @Override
    public boolean isLoadedCompletely() {
        return allMetadataLoaded;
    }

    /**
     * Sets the indication whether all meta-data of the Message Unit has been loaded.
     * <p>NOTE: This message should only be called by {@link QueryManager#ensureCompletelyLoaded(IMessageUnitEntity)}.
     *
     * @param allLoaded Indicator if User Message has been completely loaded
     */
    public void setMetadataLoaded(final boolean allLoaded) {
        this.allMetadataLoaded = allLoaded;
    }
}

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

import java.util.List;
import org.apache.axiom.om.OMElement;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.persistency.jpa.Receipt;

/**
 * Is the {@link IReceiptEntity} implementation of the default persistency provider of Holodeck B2B.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.2
 */
public class ReceiptEntity extends MessageUnitEntity<Receipt> implements IReceiptEntity {

    public ReceiptEntity(Receipt jpaObject) {
        super(jpaObject);
        // All the data for the Receipt is directly loaded
        this.allMetadataLoaded = true;
    }

    @Override
    public List<OMElement> getContent() {
        return jpaEntityObject.getContent();
    }

    /**
     * This method should not be used for <code>ReceiptEntity</code> as it is always completely loaded and there
     * is no need to change this      *
     */
    @Override
    public void setMetadataLoaded(final boolean allLoaded) {
    }

}

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
package org.holodeckb2b.common.messagemodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.axiom.om.OMElement;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;

/**
 * Is an in memory only implementation of {@link IReceipt} to temporarily store the meta-data information on a Receipt
 * Signal message unit.
 * <p><b>NOTE:</b> The content of the Receipt is marked as <i>transient</i> and therefore is not serialized,
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class Receipt extends MessageUnit implements IReceipt, Serializable {
	private static final long serialVersionUID = 2638231745969207571L;
	
	private transient ArrayList<OMElement>    content;

    /**
     * Default constructor creates a new empty <code>Receipt</code> object
     */
    public Receipt() {}

    /**
     * Creates a new <code>Receipt</code> object using the data provided in the given source object
     *
     * @param source    The source object to get the data from
     */
    public Receipt(final IReceipt source) {
        super(source);

        if (source == null)
            return;
        else
            setContent(source.getContent());
    }

    @Override
    public List<OMElement> getContent() {
        return content;
    }

    public void setContent(final List<OMElement> content) {
        setContent(content != null ? content.iterator() : null);
    }

    public void setContent(final Iterator<OMElement> content) {
        if (!Utils.isNullOrEmpty(content)) {
            this.content = new ArrayList<>();
            while (content.hasNext())
                this.content.add(content.next().cloneOMElement());
        } else
            this.content = null;
    }

    public void addElementToContent(final OMElement element) {
        if (element != null) {
            if (content == null)
                this.content = new ArrayList<>();
            this.content.add(element.cloneOMElement());
        }
    }

}

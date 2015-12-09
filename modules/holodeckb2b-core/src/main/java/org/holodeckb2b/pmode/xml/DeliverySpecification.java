/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.pmode.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.holodeckb2b.common.delivery.IDeliverySpecification;
import org.holodeckb2b.common.delivery.IMessageDeliverer;
import org.holodeckb2b.common.delivery.IMessageDelivererFactory;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Transient;

/**
 * Represents XML elements in the P-Mode document with type <code>DeliverySpecification</code>. Elements from this type
 * are used to specify how message units must be delivered to the connected business application. The delivery 
 * specification consist of a class name, in element <code>DeliveryMethod</code> that identifies the factory class that 
 * can create the actual deliverers. As the message deliverer may need configuration the delivery specification element
 * may contain one or more <code>Parameter</code> elements. These consist of name value pairs and will be passed to the 
 * factory.
 * <p>Delivery specifications are cached by the Holodeck B2B Core and therefore need to be uniquely identified. For this
 * identification the P-Mode id is used in combination with the delivery type (default, receipt or error). Because the
 * class does not know for which type of delivery it is used the class representing the parent element is responsible 
 * for setting the id.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see IMessageDelivererFactory
 * @see IMessageDeliverer
 */
public class DeliverySpecification implements IDeliverySpecification {

    @Element(name = "DeliveryMethod", required = true)
    private String  delivererFactoryClass;
    
    @ElementList(entry = "Parameter", inline = true, required = false)
    private Collection<Property>    parameters;
    
    // The id of the delivery specification is not from the XML, but set based on the P-Mode id and type of delivery
    @Transient
    private String id;
    
    @Override
    public String getId() {
        return id;
    }
    
    public void setId(String newId) {
        this.id = newId;
    }

    @Override
    public String getFactory() {
        return delivererFactoryClass;
    }

    @Override
    public Map<String, ?> getSettings() {
        HashMap<String, String>  settings = new HashMap<String, String>();
        for (Property p : parameters)
            settings.put(p.getName(), p.getValue());
        
        return settings;
    }
}

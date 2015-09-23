/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.pmode.xml;

import org.holodeckb2b.common.general.IProperty;
import org.simpleframework.xml.Element;

/**
 * Implements a Property class with name value pairs.
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
public class Property implements IProperty {
    
    @Element (name = "name")
    private String name;
    
    @Element (name = "value", required = false)
    private String value;
    
    // Type cannot appear in the message since it then would be 
    // no longer schema compliant. See also issue database at the Oasis organisation.
    // Type is added here so that is is compliant with the specification, since the spec
    // does describe the type.
    private String type;
    
    /**
     * Default constructor
     */
    public Property() {}
    
    
    /**
     * Gets the type of the property
     * @return The type of property as string
     */
    @Override
    public String getType() {
        return this.type;
    }
 
    /**
     * Gets the name of the property
     * @return The name of property as string
     */
    @Override
    public String getName() {
        return this.name;
    }
    
    /**
     * Gets the value of the property
     * @return The value of property as string
     */
    @Override
    public String getValue() {
        return this.value;
    }
    
}

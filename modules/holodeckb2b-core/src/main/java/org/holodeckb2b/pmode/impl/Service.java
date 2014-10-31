/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.pmode.impl;

import org.holodeckb2b.common.general.IService;
import org.simpleframework.xml.Element;

/**
 *
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
class Service implements IService {
    
    
    @Element (name = "type", required = true)
    private String type;
    
    @Element (name = "name", required = true)
    private String name;
    
    
    /**
     * Get the Service type.
     * @return String Service type.
     */
    @Override
    public String getType() {
        return this.type;
    }
    
    /**
     * Get the Service name.
     * @return String Service name
     */
    @Override
    public String getName() {
        return this.name;
    }
}

/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.pmode.impl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import org.holodeckb2b.common.pmode.IPMode;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persister;

/**
 * PMode implementation class.
 * @author Bram Bakx <bram at holodeck-b2b.org>
 */
@Root (name="PMode", strict=false)
public class PMode implements IPMode {
    
    @Element (name = "id", required = true)
    private PModeId pmodeId;
    
    @Element (name = "mep", required = true)
    private String mep;
    
    @Element (name = "mepBinding", required = true)
    private String mepBinding;
    
    @Element (name = "Initiator", required = false)
    private TradingPartnerConfiguration initiator;
    
    @Element (name = "Responder", required = false)
    private TradingPartnerConfiguration responder;
    
    @Element (name = "Agreement", required = false)
    private Agreement agreement;
    
    @ElementList (entry = "Leg", type = Leg.class , required = true, inline = true)
    private ArrayList<Leg> Leg;
    
    
    /**
     * Is responsible for solving dependencies child elements/objects may have on the P-Mode id. Currently this applies
     * to the identification of the delivery specifications included in the P-Mode. Because the Holodeck B2B Core 
     * requires each delivery specification to have a unique id to enable reuse each delivery specification included in
     * the P-Mode is given an id combined of the P-Mode id, current time and type of delivery, for example the default 
     * delivery specification defined on the Leg will have «P-Mode id»+"-"+«hhmmss» +"-defaultDelivery" as id.
     * <p>The objects containing the {@link DeliverySpecification}s are responsible for including these in the given
     * <code>Map</code> using the type of delivery of key and the object as value.
     * 
     * @param dependencies  A <code>Map</code> containing all {@link DeliverySpecification} objects that have to be 
     *                      assigned an id. The key of the entry MUST be a <code>String</code> containing the type
     *                      of delivery, e.g. "defaultDelivery".
     */
    @Commit
    public void solveDepencies(Map dependencies) {
        if (dependencies == null)
            return;
        
        for(Object k : dependencies.keySet()) {
            Object dep = dependencies.get(k);
            if (k instanceof String && dep != null && dep instanceof DeliverySpecification)
                ((DeliverySpecification) dep).setId(this.pmodeId.id 
                                                    + "-" + new SimpleDateFormat("HHmmss").format(new Date()) 
                                                    + "-" + k);
        }
    }
    
    /**
     * Gets the P-Mode <code>id</code>.
     * 
     * @return The PMode <code>id</code>
     */
    @Override
    public String getId() {
        return pmodeId.id;
    }
    
    /**
     * Gets the P-Mode <code>id</code> include parameter.
     * 
     * @return The PMode <code>id</code> include parameter.
     */
    @Override
    public Boolean includeId() {
        return pmodeId.include;
    }
    
    /**
     * Gets the PMode <code>mep</code>.
     * 
     * @return The PMode <code>mep</code>.
     */
    @Override
    public String getMep() {
        return mep;
    }
    
    /**
     * Gets the PMode <code>mepBinding</code>.
     * 
     * @return The PMode <code>mepBinding</code>.
     */
    @Override
    public String getMepBinding() {
        return mepBinding;
    }
    
    /**
     * Gets the PMode <code>legs</code>.
     * @return The PMode <code>legs</code>.
     */
    @Override
    public ArrayList getLegs() {
        return this.Leg;
    }
    
    
    /**
     * 
     * @return The PMode <code>initiator</code> 
     */
    @Override
    public TradingPartnerConfiguration getInitiator() {
        return this.initiator;
    }
    
    
    /**
     * 
     * @return The PMode <code>responder</code> 
     */
    @Override
    public TradingPartnerConfiguration getResponder() {
        return this.responder;
    }    
    
    
    /**
     * 
     * @return The PMode <code>agreement</code> 
     */
    @Override
    public Agreement getAgreement() {
        return this.agreement;
    }
    
    
    /**
     * Creates a new <code>PMode</code> object based 
     * the PMode definition in the XML file {@see File}.
     * 
     * @param  xsdFile      A handle to file that contains the meta data
     * @return              A <code>PMode</code> for the message meta
     *                      data contained in the given file
     * @throws Exception    When the specified file is not found, readable or
     *                      does not contain a XML document.
     */
    public static PMode createFromFile(File xmlFile) throws Exception {
        if( !xmlFile.exists() || !xmlFile.canRead())
            // Given file must exist and be readable to be able to read PMode
            throw new Exception("Specified XML file '" + xmlFile.getAbsolutePath() + "' not found or no permission to read!");
        
        PMode pmode = null;
        
        try {
            Serializer  serializer = new Persister();
            pmode = serializer.read(PMode.class, xmlFile);
        } catch (Exception ex) {
            // The specified file could not be read as an XML document
            throw new Exception("Problem reading XML from '" + xmlFile.getAbsolutePath() + "'", ex);
        }
                
        return pmode;
    }
            
    /**
     * Constructor
     */
    public void Pmode() {};
}

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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.ebms3.persistent.message;

import java.io.Serializable;
import javax.persistence.Embeddable;
import org.holodeckb2b.common.messagemodel.IAgreementReference;

/**
 * Is an <i>embeddable</i> persistency class containing a reference to the agreement
 * for a message exchange between trading partners.
 * 
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@Embeddable
public class AgreementReference implements Serializable, IAgreementReference {

    /*
     * Getters and setters
     */
    @Override
    public String getName() {
        return A_NAME;
    }

    public void setName(String agreementName) {
        A_NAME = agreementName;
    }
    
    @Override
    public String getType() {
        return A_TYPE;
    }
    
    public void setType(String agreementType) {
        A_TYPE = agreementType;
    }

    @Override
    public String getPModeId() {
        return P_MODE_ID;
    }
    
    public void setPModeId(String pmodeId) {
        P_MODE_ID = pmodeId;
    }
    
    /*
     * Constructore
     */
    
    public AgreementReference() {}
    
    /**
     * Creates a new agreement reference that only refers to an agreement but not a Pmode
     */
    public AgreementReference(String name, String type) {
        A_NAME = name;
        A_TYPE = type;
    }
    
    /**
     * Creates a new agreement reference that only has a P-Mode reference
     */
    public AgreementReference(String pmodeId) {
        P_MODE_ID = pmodeId;
    }

    
    /*
     * Fields
     * 
     * NOTE: The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    private String  A_NAME;
    private String  A_TYPE;
    private String  P_MODE_ID;
    
}

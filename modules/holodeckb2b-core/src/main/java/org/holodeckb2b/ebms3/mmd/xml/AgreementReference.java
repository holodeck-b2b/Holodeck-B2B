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
package org.holodeckb2b.ebms3.mmd.xml;

import org.holodeckb2b.common.messagemodel.IAgreementReference;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

/**
 * Represents the <code>AgreementRef</code> element from the MMD document.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class AgreementReference implements IAgreementReference {
   
    @Text(required = false)
    private String      name;
    
    @Attribute(name = "type", required = false)
    private String      type;
    
    @Attribute(name = "pmode", required = false)
    private String      pmode;

    /**
     * Default constructor
     */
    public AgreementReference() {}
    
    /**
     * Creates an AgreementReference object with the given data
     * 
     * @param agreeRef  The data to use
     */
    public AgreementReference(IAgreementReference agreeRef) {
        this.name = agreeRef.getName();
        this.type = agreeRef.getType();
        this.pmode = agreeRef.getPModeId();
    }
    
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public String getPModeId() {
        return pmode;
    }
    
    public void setPModeId(String pmode) {
        this.pmode = pmode;
    }
}

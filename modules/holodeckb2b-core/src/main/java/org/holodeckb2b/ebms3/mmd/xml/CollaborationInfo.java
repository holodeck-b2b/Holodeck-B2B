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

import org.holodeckb2b.common.general.IService;
import org.holodeckb2b.common.messagemodel.IAgreementReference;
import org.holodeckb2b.common.messagemodel.ICollaborationInfo;
import org.simpleframework.xml.Element;

/**
 * Represents the <code>CollaborationInfo</code> element from the MMD document.
 * <p>This element contains meta data about the message exchange context, 
 * i.e. the business collaboration. There is one <b>REQUIRED</b> child element, 
 * <code>ConversationId</code>, which must be supplied when submitting a message 
 * to Holodeck B2B. This element is used to identify related message exchanges 
 * within one business conversation.
 * <p>When submitting a message it is RECOMMENDED to supply the P-Mode, in the
 * <code>pmode</code> attribute of the <code>AgreementRef</code> child element, 
 * so Holodeck B2B can unambiguously determine the P-Mode to use for processing 
 * the message. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CollaborationInfo implements ICollaborationInfo {
    
    @Element(name = "AgreementRef", required = false)
    private AgreementReference  agreementRef;
    
    @Element(name = "Service", required = false)
    private Service             service;
    
    @Element(name = "Action", required = false)
    private String              action;
    
    @Element(name = "ConversationId")
    private String              convId;
    
    /**
     * Default constructor
     */
    public CollaborationInfo() {}
    
    /**
     * Creates an <code>CollaborationInfo</code> object based on the given data
     * 
     * @param ci    The data to use
     */
    public CollaborationInfo(ICollaborationInfo ci) {
        this.action = ci.getAction();
        this.convId = ci.getConversationId();
        
        setService(ci.getService());
        setAgreement(ci.getAgreement());
    }

    @Override
    public IService getService() {
        return service;
    }

    public void setService(IService svc) {
        if (svc != null)
            this.service = new Service(svc);
        else
            this.service = null;
    }
    
    @Override
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
            
    @Override
    public String getConversationId() {
        return convId;
    }

    public void setConversationId(String convId) {
        this.convId = convId;
    }
            
    @Override
    public IAgreementReference getAgreement() {
        return agreementRef;
    }
    
    public void setAgreement(IAgreementReference agreeRef) {
        if(agreeRef != null)
            this.agreementRef = new AgreementReference(agreeRef);
        else
            this.agreementRef = null;
    }    
}

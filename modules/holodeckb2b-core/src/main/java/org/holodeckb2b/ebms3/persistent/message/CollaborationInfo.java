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
package org.holodeckb2b.ebms3.persistent.message;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import org.holodeckb2b.common.general.IService;
import org.holodeckb2b.common.messagemodel.IAgreementReference;
import org.holodeckb2b.common.messagemodel.ICollaborationInfo;
import org.holodeckb2b.ebms3.persistent.general.Service;

/**
 * Is a persistency class representing the business collaboration meta data about
 * a message exchange.
 * <p>This class is defined as <i>Embeddable</i> because the collaboration information
 * is always related to a specific message and not a real entity of its own.
 * 
 * @author Sander Fieten <sander at holodeckb2b.org>
 */
@Embeddable
public class CollaborationInfo implements Serializable, ICollaborationInfo {

    /*
     * Getters and setters
     */
    
    @Override
    public IService getService() {
        return service;
    }
    
    public void setService(Service service) {
        this.service = service;
    }

    @Override
    public String getAction() {
        return CI_ACTION;
    }

    public void setAction(String action) {
        CI_ACTION = action;
    }
    
    @Override
    public String getConversationId() {
        return CONVERSATION_ID;
    }
    
    public void setConversationId(String convid) {
        CONVERSATION_ID = convid;
    }
    
    
    @Override
    public IAgreementReference getAgreement() {
        return agreementRef;
    }
    
    public void setAgreement(AgreementReference ref) {
        this.agreementRef = ref;
    }
    
    /**
     * Sets the agreement reference to the given P-Mode id. 
     */
    public void setPModeId(String pmodeId) {
        agreementRef.setPModeId(pmodeId);
    }
    
    /*
     * Constructors
     */
    public CollaborationInfo() {}
    
    /**
     * Creates a new instance with the given service, action and agreement reference.
     */
    public CollaborationInfo(Service service, String action, AgreementReference agreement) {
        this.service = service;
        this.CI_ACTION = action;
        this.agreementRef = agreement;
    }

    /**
     * Creates a new instance with a service with the given name, action and P-Mode Id
     * 
     * <p><b>Note:</b>In this constructor new persistent objects are created (for Service and
     * AgreementReference). This is possible because these are embedded and are automaticly
     * persisted with the CollaborationInfo object.
     */
    public CollaborationInfo(String service, String action, String pmodeId) {
        this.service = new Service(service);
        this.CI_ACTION = action;
        this.agreementRef = new AgreementReference(pmodeId);
    }

    
    /*
     * Fields
     * 
     * NOTE: The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    
    @Embedded
    private Service             service;
    
    /*
     * Because ACTION is a SQL-99 reserved word it is prefixed here
     */
    private String              CI_ACTION;
    
    private String              CONVERSATION_ID;
    
    @Embedded
    private AgreementReference  agreementRef;
}

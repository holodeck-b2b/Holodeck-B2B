/**
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
package org.holodeckb2b.storage.metadata.jpa;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Lob;

import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;

/**
 * Is an <i>embeddable</i> JPA persistency class used to store the information described by {@link ICollaborationInfo}
 * interface in the Holodeck B2B messaging model.
 * <p>This class is <i>embeddable</i> as the collaboration information meta-data is always specific to one instance of a
 * User Message.
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 * @since  3.0.0
 */
@Embeddable
public class CollaborationInfo implements ICollaborationInfo, Serializable {
	private static final long serialVersionUID = -3387867953653779116L;

    /*
     * Getters and setters
     */

	@Override
    public IService getService() {
        return service;
    }

    public void setService(final IService service) {
        this.service = service != null ? new Service(service) : null;
    }

    @Override
    public String getAction() {
        return CI_ACTION;
    }

    public void setAction(final String action) {
        CI_ACTION = action;
    }

    @Override
    public String getConversationId() {
        return CONVERSATION_ID;
    }

    public void setConversationId(final String convid) {
        CONVERSATION_ID = convid;
    }


    @Override
    public IAgreementReference getAgreement() {
        return agreementRef;
    }

    public void setAgreement(final IAgreementReference ref) {
        this.agreementRef = ref != null ? new AgreementReference(ref) : null;
    }

    /**
     * Sets the agreement reference to the given P-Mode id.
     */
    public void setPModeId(final String pmodeId) {
        agreementRef.setPModeId(pmodeId);
    }

    /*
     * Constructors
     */
    /**
     * Default constructor
     */
    public CollaborationInfo() {}

    /**
     * Creates an <code>CollaborationInfo</code> object based on the given data
     *
     * @param source    The data to use
     */
    public CollaborationInfo(final ICollaborationInfo source) {
        this.CI_ACTION = source.getAction();
        this.CONVERSATION_ID = source.getConversationId();

        setService(source.getService());
        setAgreement(source.getAgreement());
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
    @Lob
    @Column(length = 1024)
    private String              CI_ACTION;

    @Lob
    @Column(length = 1024)
    private String              CONVERSATION_ID;

    @Embedded
    private AgreementReference  agreementRef;
}

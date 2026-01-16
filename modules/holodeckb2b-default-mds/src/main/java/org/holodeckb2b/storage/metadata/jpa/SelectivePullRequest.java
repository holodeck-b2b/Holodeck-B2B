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

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;

/**
 * Is the JPA entity class to store the meta-data of a <b>selective PullRequest Signal</b> message unit as described by
 * the {@link ISelectivePullRequest} interface in the Holodeck B2B messaging model. Extends The maximum length of the MPC URL is 1024 characters.
 * <p><b>NOTE:</b> The current version only supports the <b>simple</b> selection items as described in the interface.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  4.1.0
 */
@Entity
@Table(name="SELECTPULLREQUEST")
@DiscriminatorValue("SELECTPULL")
public class SelectivePullRequest extends PullRequest {
	private static final long serialVersionUID = -7323036617505416772L;

	/*
     * Getters and setters
     */
    public String getReferencedMessageId() { return REFD_MESSAGE_ID; }

    public void setReferencedMessageId(final String messageId) { REFD_MESSAGE_ID = messageId; }

    public String getConversationId() {
        return CONVERSATION_ID;
    }

    public void setConversationId(final String convid) {
        CONVERSATION_ID = convid;
    }

    public IAgreementReference getAgreementRef() {
        return agreementRef;
    }

    public void setAgreementRef(final IAgreementReference ref) {
        this.agreementRef = ref != null ? new AgreementReference(ref) : null;
    }

    public IService getService() {
        return service;
    }

    public void setService(final IService service) {
        this.service = service != null ? new Service(service) : null;
    }

    public String getAction() {
        return S_ACTION;
    }

    public void setAction(final String action) {
        S_ACTION = action;
    }

    /*
     * Constructors
     */
    /**
     * Default constructor creates a new empty <code>PullRequest</code> object
     */
    public SelectivePullRequest() {}

    /**
     * Creates a new <code>SelectivePullRequest</code> object using the mpc from the given source pull request.
     *
     * @param source    The Pull Request data to use
     */
    public SelectivePullRequest(final IPullRequest source) {
        super(source);

        if (source instanceof ISelectivePullRequest) {
            ISelectivePullRequest selectPR = (ISelectivePullRequest) source;

            this.REFD_MESSAGE_ID = selectPR.getReferencedMessageId();
            this.CONVERSATION_ID = selectPR.getConversationId();
            this.S_ACTION = selectPR.getAction();
            setAgreementRef(selectPR.getAgreementRef());
            setService(selectPR.getService());
        }

    }

    /*
     * Fields
     *
     * NOTES:
     * 1) The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     * 2) The primary key field is inherited from super class
     */
    private String              REFD_MESSAGE_ID;

    @Lob
    @Column(length = 1024)
    private String              CONVERSATION_ID;

    @Embedded
    private AgreementReference  agreementRef;

    @Embedded
    private Service             service;

    /*
     * Because ACTION is a SQL-99 reserved word it is prefixed here
     */
    @Lob
    @Column(length = 1024)
    private String              S_ACTION;
}


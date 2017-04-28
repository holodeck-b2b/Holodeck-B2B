/**
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.messagemodel;

import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;

/**
 * Is an in memory only implementation of {@link ICollaborationInfo} to temporarily store the business information meta-
 * data of a User Message message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class CollaborationInfo implements ICollaborationInfo {

    private AgreementReference  agreementRef;
    private Service             service;
    private String              action;
    private String              convId;

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
        this.action = source.getAction();
        this.convId = source.getConversationId();

        setService(source.getService());
        setAgreement(source.getAgreement());
    }

    @Override
    public Service getService() {
        return service;
    }

    public void setService(final IService svc) {
        this.service = (svc != null) ?  new Service(svc) : null;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    @Override
    public String getConversationId() {
        return convId;
    }

    public void setConversationId(final String convId) {
        this.convId = convId;
    }

    @Override
    public AgreementReference getAgreement() {
        return agreementRef;
    }

    public void setAgreement(final IAgreementReference agreeRef) {
        this.agreementRef = agreeRef != null ? new AgreementReference(agreeRef) : null;
    }
}

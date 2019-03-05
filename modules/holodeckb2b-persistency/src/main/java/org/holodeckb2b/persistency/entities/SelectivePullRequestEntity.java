/*
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
package org.holodeckb2b.persistency.entities;

import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;
import org.holodeckb2b.interfaces.messagemodel.ISelectivePullRequest;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.ISelectivePullRequestEntity;
import org.holodeckb2b.persistency.jpa.PullRequest;
import org.holodeckb2b.persistency.jpa.SelectivePullRequest;

/**
 * Is the {@link ISelectivePullRequestEntity} implementation of the default persistency provider of Holodeck B2B.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  4.1.0
 */
public class SelectivePullRequestEntity extends PullRequestEntity implements ISelectivePullRequestEntity {

    public SelectivePullRequestEntity(PullRequest jpaObject) {
        super(jpaObject);
    }

    @Override
    public String getReferencedMessageId() {
        return ((SelectivePullRequest) jpaEntityObject).getReferencedMessageId();
    }

    @Override
    public String getConversationId() {
        return ((SelectivePullRequest) jpaEntityObject).getConversationId();
    }

    @Override
    public IAgreementReference getAgreementRef() {
        return ((SelectivePullRequest) jpaEntityObject).getAgreementRef();
    }

    @Override
    public IService getService() {
        return ((SelectivePullRequest) jpaEntityObject).getService();
    }

    @Override
    public String getAction() {
        return ((SelectivePullRequest) jpaEntityObject).getAction();
    }
}

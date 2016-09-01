/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.pmode.helpers;

import java.util.ArrayList;
import java.util.List;
import org.holodeckb2b.interfaces.general.IAgreement;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import static org.holodeckb2b.interfaces.pmode.ILeg.Label.REPLY;
import static org.holodeckb2b.interfaces.pmode.ILeg.Label.REQUEST;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PMode implements IPMode {

    private String          id;
    private Boolean         include;
    private String          mep;
    private String          mepBinding;
    private Agreement       agreement;
    private PartnerConfig   initiator;
    private PartnerConfig   responder;
    private ArrayList<Leg>  legs;

    @Override
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public Boolean includeId() {
        return include;
    }

    public void setIncludeId(final Boolean includeId) {
        this.include = includeId;
    }

    @Override
    public IAgreement getAgreement() {
        return agreement;
    }

    public void setAgreement(final Agreement agreement) {
        this.agreement = agreement;
    }

    @Override
    public String getMep() {
        return mep;
    }

    public void setMep(final String mep) {
        this.mep = mep;
    }

    @Override
    public String getMepBinding() {
        return mepBinding;
    }

    public void setMepBinding(final String mepBinding) {
        this.mepBinding = mepBinding;
    }

    @Override
    public PartnerConfig getInitiator() {
        return initiator;
    }

    public void setInitiator(final PartnerConfig initiator) {
        this.initiator = initiator;
    }

    @Override
    public PartnerConfig getResponder() {
        return responder;
    }

    public void setResponder(final PartnerConfig responder) {
        this.responder = responder;
    }

    @Override
    public List<? extends ILeg> getLegs() {
        return legs;
    }

    public Leg getLeg(Label label) {
        // First try to find the leg based on the label
        for (Leg l : legs)
            if (l.getLabel() == label)
                return l;

        // If the Leg were not labelled, get the first leg when requested label is REQUEST, second when REPLY
        switch (label) {
            case REQUEST :
                return legs.get(0);
            case REPLY :
                return legs.get(1);
        }

        return null;
    }

    public void addLeg(final Leg leg) {
        if (this.legs == null)
            this.legs = new ArrayList<Leg>();

        if (leg != null)
            this.legs.add(leg);
    }

}

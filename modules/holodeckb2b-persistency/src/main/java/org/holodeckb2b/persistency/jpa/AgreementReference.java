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
package org.holodeckb2b.persistency.jpa;

import java.io.Serializable;
import javax.persistence.Embeddable;
import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;

/**
 * Is an <i>embeddable</i> JPA persistency class used to store the information described by {@link IAgreementReference}
 * interface in the Holodeck B2B messaging model.
 * <p>This class is <i>embeddable</i> as the agreement reference meta-data is always specific to one instance of a
 * User Message.
 *
 * @author Sander Fieten <sander at holodeckb2b.org>
 * @since HB2B_NEXT_VERSION
 */
@Embeddable
public class AgreementReference implements IAgreementReference, Serializable {

    /*
     * Getters and setters
     */
    @Override
    public String getName() {
        return A_NAME;
    }

    public void setName(final String agreementName) {
        A_NAME = agreementName;
    }

    @Override
    public String getType() {
        return A_TYPE;
    }

    public void setType(final String agreementType) {
        A_TYPE = agreementType;
    }

    @Override
    public String getPModeId() {
        return P_MODE_ID;
    }

    public void setPModeId(final String pmodeId) {
        P_MODE_ID = pmodeId;
    }

    /*
     * Constructore
     */
    /**
     * Default constructor
     */
    public AgreementReference() {}

    /**
     * Creates an AgreementReference object with the given data
     *
     * @param agreeRef  The data to use
     */
    public AgreementReference(final IAgreementReference agreeRef) {
        this(agreeRef.getName(), agreeRef.getType(), agreeRef.getPModeId());
    }

    /**
     * Creates a new <code>AgreementReference</code> object with the specified agreement name and type and P-Mode id.
     *
     * @param name      The agreement's name
     * @param type      The agreement type
     * @param pmodeId   The P-Mode Id
     */
    public AgreementReference(final String name, final String type, final String pmodeId) {
        this.A_NAME = name;
        this.A_TYPE = type;
        this.P_MODE_ID = pmodeId;
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

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

import org.holodeckb2b.interfaces.messagemodel.IAgreementReference;

/**
 * Is an in memory only implementation of {@link IAgreementReference} to temporarily store the business agreement meta-
 * data of a User Message message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class AgreementReference implements IAgreementReference {

    private String      name;
    private String      type;
    private String      pmodeId;

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
        this.name = agreeRef.getName();
        this.type = agreeRef.getType();
        this.pmodeId = agreeRef.getPModeId();
    }

    /**
     * Creates a new <code>AgreementReference</code> object with the specified agreement name and type and P-Mode id.
     *
     * @param name      The agreement's name
     * @param type      The agreement type
     * @param pmodeId   The P-Mode Id
     */
    public AgreementReference(final String name, final String type, final String pmodeId) {
        this.name = name;
        this.type = type;
        this.pmodeId = pmodeId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String getPModeId() {
        return pmodeId;
    }

    public void setPModeId(final String pmode) {
        this.pmodeId = pmode;
    }
}

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

import org.holodeckb2b.interfaces.as4.pmode.IAS4PayloadProfile;

/**
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PayloadProfile implements IAS4PayloadProfile {

    private String  compressionType;

    @Override
    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(final String compressionType) {
        this.compressionType = compressionType;
    }
}
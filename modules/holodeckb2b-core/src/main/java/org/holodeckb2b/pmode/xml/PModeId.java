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
package org.holodeckb2b.pmode.xml;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

/**
 * Represents the configuration for the <b>PMode.id</b> parameter.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @author Bram Bakx (bram at holodeck-b2b.org)
 */
class PModeId {

    /**
     * Identifies the PMode uniquely
     */
    @Text
    String  id;

    /**
     * Specifies if the PMode ID should be included in the actual message
     */
    @Attribute(required = false)
    Boolean include = false;

}

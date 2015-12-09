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
package org.holodeckb2b.common.general;

/**
 * Represents a description for an item and is used for processing <code>eb:Description</code> elements in the ebMS
 * messaging header. Descriptions are always user specified and not used by Holodeck B2B when processing messages.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IDescription {
   
    /**
     * Gets the language the description is written in.
     * 
     * @return  Identification of the language the description is written in
     */
    public String  getLanguage();
    
    /**
     * Gets the text of description
     * 
     * @return  The text of the description
     */
    public String getText();
}

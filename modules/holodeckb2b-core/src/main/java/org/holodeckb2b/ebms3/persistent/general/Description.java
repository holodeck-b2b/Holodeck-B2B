/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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

package org.holodeckb2b.ebms3.persistent.general;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.holodeckb2b.common.general.IDescription;

/**
 * Is a persistency class for object descriptions. As a description is always tight to 
 * another entity (a business partner, payload, error, etc.) it is defined as an 
 * <code>Embedable</code> class.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Embeddable
public class Description implements Serializable, IDescription {

    /*
     * Getters and setters
     */
    
    @Override
    public String getText() {
        return DESCRIPTION;
    }

    public void setText(String text) {
        DESCRIPTION = text;
    }
    
    @Override
    public String getLanguage() {
        return LANG;
    }
    
    public void setLanguage(String language) {
        LANG = language;
    }
    
    /*
     * Constructors
     */
    public Description() {};

    /**
     * Create a new <code>Description</code> with the given text.
     * 
     * @param String   The text itself
     */
    public Description(String text) {
        DESCRIPTION = text;
    }

    /**
     * Create a new <code>Description</code> with the given text and language indication.
     * 
     * @param String   The text itself
     * @param String   The language the text is written in
     */
    public Description(String text, String language) {
        DESCRIPTION = text;
        LANG = language;
    }
    
    /*
     * Fields
     * 
     * NOTE: The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    @Column(name = "DESCRIPTION", length = 10000)
    private String  DESCRIPTION;
    
    private String  LANG;
}

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
package org.holodeckb2b.persistency.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

import org.holodeckb2b.interfaces.general.IDescription;

/**
 * Is the JPA embeddable persistency class for storing descriptions contained in an ebMS message unit. It is defined as
 * <i>embeddable</i> because the description is always tight to another entity (Payload or Error).
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
        return DESCRIPTION_TEXT;
    }

    public void setText(final String text) {
        DESCRIPTION_TEXT = text;
    }

    @Override
    public String getLanguage() {
        return LANG;
    }

    public void setLanguage(final String language) {
        LANG = language;
    }

    /*
     * Constructors
     */
    public Description() {}

    /**
     * Create a new <code>Description</code> with the given text.
     *
     * @param text   The text itself
     */
    public Description(final String text) {
        DESCRIPTION_TEXT = text;
    }

    /**
     * Create a new <code>Description</code> with the given text and language indication.
     *
     * @param text     The text itself
     * @param language The language the text is written in
     */
    public Description(final String text, final String language) {
        DESCRIPTION_TEXT = text;
        LANG = language;
    }

    /*
     * Fields
     *
     * NOTE: The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    @Lob
    @Column(name = "DESCRIPTION", length = 32768)
    private String  DESCRIPTION_TEXT;

    private String  LANG;
}

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
package org.holodeckb2b.storage.metadata.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

import org.holodeckb2b.interfaces.general.IDescription;

/**
 * Is an <i>embeddable</i> JPA persistency class used to store a description as described by {@link IDescription}
 * interface. The maximum length of the description's text is 32K characters.
 * <p>This class is <i>embeddable</i> as the description data is always bound specifically to one other object and
 * therefor can be easily stored together with that object.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
@Embeddable
public class Description implements IDescription, Serializable {
	private static final long serialVersionUID = 810330427029281720L;

    /*
     * Getters and setters
     */


	@Override
    public String getText() {
        return DESCRIPTION_TEXT;
    }

    public void setText(final String text) {
        DESCRIPTION_TEXT = text.substring(0, Math.min(10000, text.length()));
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
    /**
     * Default constructor
     */
    public Description() {}

    /**
     * Creates a new <code>Description</code> object based on the given data
     *
     * @param data  The description data to use
     */
    public Description(final IDescription data) {
        this.LANG = data.getLanguage();
        setText(data.getText());
    }

    /**
     * Creates a new <code>Description</code> object with the given text and no language indication.
     *
     * @param text  The description text to use
     */
    public Description(final String text) {
        setText(text);
    }


    /*
     * Fields
     *
     * NOTE: The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    @Lob
    @Column(name = "DESCRIPTION", length = 10000)
    private String  DESCRIPTION_TEXT;

    private String  LANG;
}

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

import org.holodeckb2b.interfaces.general.IDescription;

/**
 * Is an in memory only implementation of {@link IDescription} to temporarily store a generic description contained in
 * the ebMS header of a message unit.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
public class Description implements IDescription {

    private String  text;
    private String  lang;

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
        this.lang = data.getLanguage();
        this.text = data.getText();
    }

    /**
     * Creates a new <code>Description</code> object with the given text and no language indication.
     *
     * @param text  The description text to use
     */
    public Description(final String text) {
        this.text = text;
    }

    /**
     * Creates a new <code>Description</code> object with the given text and language indication.
     *
     * @param text  The description text to use
     * @param lang  The indication of the language the description is in
     */
    public Description(final String text, final String lang) {
        this.text = text;
        this.lang = lang;
    }

    /**
     * @return the text
     */
    @Override
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(final String text) {
        this.text = text;
    }

    /**
     * @return the language indication
     */
    @Override
    public String getLanguage() {
        return lang;
    }

    /**
     * @param lang the language to set
     */
    public void setLanguage(final String lang) {
        this.lang = lang;
    }
}

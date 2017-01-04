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
package org.holodeckb2b.common.mmd.xml;

import org.holodeckb2b.interfaces.general.IDescription;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Text;

/**
 * Represents the <code>Description</code> element from a MMD document. The information
 * contained in this element may be used by the business applications that
 * handle the payload. It is not used by Holodeck B2B in processing the message.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class Description implements IDescription {

    @Text
    private String  text;

    @Attribute(name="lang",required = false)
    @Namespace(prefix = "xml", reference = "http://www.w3.org/XML/1998/namespace")
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
     * @return the lang
     */
    @Override
    public String getLanguage() {
        return lang;
    }

    /**
     * @param lang the lang to set
     */
    public void setLanguage(final String lang) {
        this.lang = lang;
    }


}

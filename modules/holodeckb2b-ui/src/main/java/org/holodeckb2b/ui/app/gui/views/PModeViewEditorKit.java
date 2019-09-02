/*
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ui.app.gui.views;

import javax.swing.JTextPane;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;

/**
 * Is a specialised {@link StyledEditorKit} that installs the {@link PModeXMLView} on the given {@link JTextPane} so
 * the displayed XML has syntax highlighting.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class PModeViewEditorKit extends StyledEditorKit {

	private ViewFactory xmlViewFactory;

    public PModeViewEditorKit() {
        xmlViewFactory = PModeXMLView.getFactory();
    }
    
    @Override
    public ViewFactory getViewFactory() {
        return xmlViewFactory;
    }

    @Override
    public String getContentType() {
        return "text/xml";
    }	
}

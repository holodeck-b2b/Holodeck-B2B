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

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Extends {@link PlainView} to add simple colour highlighting of the P-Mode XML documents. It uses regexps to find the
 * start and end of the XML components and then sets the colours for these text segments. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class PModeXMLView extends PlainView {

	public static ViewFactory getFactory() {
		return new ViewFactory() {			
			@Override
			public View create(Element elem) {
				return new PModeXMLView(elem);
			}
		};
		
	}
	
	public PModeXMLView(Element e) {
		super(e);
        // Set tabsize to 4 (instead of the default 8)
        getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);				
	}
	
	private static HashMap<Pattern, Color> xmlColours;
    private static String GENERIC_XML_NAME = "[A-Za-z]+[A-Za-z0-9\\-_]*(:[A-Za-z]+[A-Za-z0-9\\-_]+)?";
    private static String TAG_PATTERN = "(</?" + GENERIC_XML_NAME + ")";
    private static String TAG_END_PATTERN = "(>|/>)";
    private static String TAG_ATTRIBUTE_PATTERN = "(" + GENERIC_XML_NAME + ")\\w*\\=";
    private static String TAG_ATTRIBUTE_VALUE = "\\w*\\=\\w*(\"[^\"]*\")";
    private static String TAG_COMMENT = "(<\\!--[\\w ]*-->)";
   
    static {
        xmlColours = new LinkedHashMap<Pattern, Color>();
        xmlColours.put(Pattern.compile(TAG_PATTERN), new Color(0, 0, 150));
        xmlColours.put(Pattern.compile(TAG_ATTRIBUTE_PATTERN), new Color(102, 0, 0));
        xmlColours.put(Pattern.compile(TAG_END_PATTERN), new Color(0, 0, 150));
        xmlColours.put(Pattern.compile(TAG_ATTRIBUTE_VALUE), new Color(204, 96, 0));
        xmlColours.put(Pattern.compile(TAG_COMMENT), new Color(0, 153, 204));
    }

    @Override
    protected int drawUnselectedText(Graphics graphics, int x, int y, int p0, int p1) throws BadLocationException {
        Document doc = getDocument();
        String text = doc.getText(p0, p1 - p0);
        Segment segment = getLineBuffer();

        List<Part>	parts = new ArrayList<>();
        
        // Match the regexps on this snippet to find the parts to colour
        for (Map.Entry<Pattern, Color> part : xmlColours.entrySet()) {
            Matcher matcher = part.getKey().matcher(text);
            while (matcher.find())
                parts.add(new Part(matcher.start(1), matcher.end(1), part.getValue()));
        }
        // Ensure parts are in correct order as matching of regexps may have change order of parts in the list, i.e
        // attributes are after tag close
        Collections.sort(parts);
        // Now set the colours on the parts
        int cp = 0; // pointer to current position within text to colour        
        for (Part p : parts) {
        	// use black for non-XML components parts
            if (cp < p.start) {
                graphics.setColor(Color.black);
                doc.getText(p0 + cp, p.start - cp, segment);
                x = Utilities.drawTabbedText(segment, x, y, graphics, this, cp);
            }
            graphics.setColor(p.colour);            
            doc.getText(p0 + p.start, p.end - p.start, segment);
            x = Utilities.drawTabbedText(segment, x, y, graphics, this, p.start);
            cp = p.end;
        }
        // Paint possible remaining text black
        if (cp < text.length()) {
            graphics.setColor(Color.black);
            doc.getText(p0 + cp, text.length() - cp, segment);
            x = Utilities.drawTabbedText(segment, x, y, graphics, this, cp);
        }

        return x;
    }
	
    /**
     * Helper structure to administer the parts to be coloured. Consists of the start and end positions of the part to
     * colour and the actual colour to use.  
     */
    class Part implements Comparable<Part> {
    	int start, end;
    	Color colour;   
    	
    	Part(int s, int e, Color c) {
    		this.start = s;
    		this.end = e;
    		this.colour = c;
    	}

		@Override
		public int compareTo(Part o) {			
			return this.start - o.start;
		}
    
    }
}

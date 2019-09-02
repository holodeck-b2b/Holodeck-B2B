/**
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

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Contains utility functions for creating and modifying the views of the GUI.
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class ViewUtils {
	
    /**
     * Calculates the maximum column sizes needed to show all data in the given resultset. Also adjust the total width 
     * of the table to fit the newly sized columns.
     * 
     * @param table 	The table to size
     */
    public static void setColumnAndTableSize(final JTable table) {
    	TableColumn column = null;
        Component comp = null;
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        int tableWidth = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
        	int colWidth = 0;
            column = table.getColumnModel().getColumn(i); 
            comp = headerRenderer.getTableCellRendererComponent(null, column.getHeaderValue(), false, false, 0, 0);
            colWidth = comp.getPreferredSize().width;
            for (int j = 0; j < table.getRowCount(); j++) {
            	comp = table.getDefaultRenderer(String.class).getTableCellRendererComponent(table, 
            																		table.getModel().getValueAt(j, i), 
            																		false, false, 0, i);            	             	
            	colWidth = Math.max(colWidth, comp.getPreferredSize().width);
            }
            column.setPreferredWidth(colWidth + 15);
            tableWidth += colWidth + 15;
        }				
        // Last column will be extended if the total table is smaller than its parent
        if (table.getParent() != null && tableWidth < table.getParent().getSize().width) 
        	column.setPreferredWidth(table.getParent().getSize().width - (tableWidth - column.getPreferredWidth()));
        else
        	table.setPreferredScrollableViewportSize(new Dimension(tableWidth, 0));
    }
}

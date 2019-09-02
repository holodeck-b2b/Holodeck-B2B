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
package org.holodeckb2b.ui.app.gui.models;

import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ui.app.gui.views.MessageStatusPanel;

/**
 * Is the data model object for the message status panel. It is a list of <i>Message Units</i> that have the requested
 * <i>MessageId</i> since multiple instances can exists either due to resending or applications not satisfying the 
 * uniqueness constraint. The model is implemented as a {@link TableModel} so it can be directly used in {@link 
 * MessageStatusPanel}. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class MessageUnitStatesData extends AbstractTableModel {

	/**
	 * The columns of the message unit list  
	 */
	private static final String[] MU_LIST_COLUMNS = new String[] {"Message Unit Type", 
																  "Direction", 
																  "Timestamp"
																  };
	/**
	 * The columns of the processing states list  
	 */
	private static final String[] STATES_LIST_COLUMNS = new String[] {"Processing state", 
																	  "Start time"
																	 };
	
	/**
	 * The message unit meta-data to show
	 */
	private MessageUnit[] msgUnits;	

	public void setMessageUnits(final MessageUnit[] msgUnitData) {
		this.msgUnits = msgUnitData;
		fireTableDataChanged();
	}
	
	@Override
	public int getRowCount() {
		return msgUnits != null ? msgUnits.length : 0;
	}

	@Override
	public int getColumnCount() {
		return MU_LIST_COLUMNS.length;
	}

    @Override
	public String getColumnName(int col) {
        return MU_LIST_COLUMNS[col];
    }
    
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final MessageUnit m = msgUnits[rowIndex];
		switch (columnIndex) {
		case 0 : 
			return MessageUnitUtils.getMessageUnitName(m);
		case 1 :
			return m.getDirection().name();
		case 2 :  
			return Utils.toXMLDateTime(m.getTimestamp());
		}
		throw new IllegalArgumentException("Unknown column requested");
	}    
	
    /**
     * Gets that list of processing states for the selected message unit or an empty list if the given index does not
     * exists.
     * 
     * @param i		The currently selected message unit
     * @return		{@link TableModel} with the processing states for displaying
     */
    public DefaultTableModel getStatesModel(final int i) {
    	if (0 <= i && msgUnits != null && i < msgUnits.length)
    		return new DefaultTableModel(msgUnits[i].getProcessingStates().parallelStream()
    																  .map(s -> new String[] { s.getState().name(), 	
    																		  Utils.toXMLDateTime(s.getStartTime())})
    																  .collect(Collectors.toList()).toArray(new Object[][] {})    																 
    									, STATES_LIST_COLUMNS);
    	else
    		return new DefaultTableModel(new String[][] {}, STATES_LIST_COLUMNS);
    }
    

}

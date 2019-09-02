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

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;

/**
 * The {@link TableModel} for the table listing the meta-data of the message units in the current history.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class MessageHistoryData extends AbstractTableModel {

	/**
	 * The columns 
	 */
	private static final String[] COLUMNS = new String[] {"Timestamp", 
														  "Current state",
														  "Message Unit Type", 
														  "Direction", 
														  "MessageId", 
														  "RefMessageId", 
														  "PMode.id"
														  };
	
	public static final int STATE_COLUMN = 1;
	
	/**
	 * The message unit meta-data to show
	 */
	private MessageUnit[] msgUnits;

	/**
	 * Sets the meta-data of the message units that should be displayed in the message history.
	 * 
	 * @param msgUnitData	Array of message units to be shown
	 */
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
		return COLUMNS.length;
	}

    @Override
	public String getColumnName(int col) {
        return COLUMNS[col];
    }
    
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final MessageUnit m = msgUnits[rowIndex];
		switch (columnIndex) {
		case 0 : 
			return Utils.toXMLDateTime(m.getTimestamp());
		case 1 :
			 return m.getCurrentProcessingState().getState().name();
		case 2 :  
		     return MessageUnitUtils.getMessageUnitName(m);
		case 3 : 
		     return m.getDirection().name();
		case 4 : 
			return m.getMessageId();
		case 5 : 
		     return !Utils.isNullOrEmpty(m.getRefToMessageId()) ? m.getRefToMessageId() : "";
		case 6 : 
   		 	return !Utils.isNullOrEmpty(m.getPModeId()) ? m.getPModeId() : "";
		}
		throw new IllegalArgumentException("Unknown column requested");
	}
}

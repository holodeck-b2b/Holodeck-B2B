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

import java.io.ByteArrayOutputStream;
import java.util.Date;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IAgreement;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;

/**
 * The {@link TableModel} for the table listing the P-Modes currently installed on the Holodeck B2B instance
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class PModesData extends AbstractTableModel {

	/**
	 * The columns 
	 */
	private static final String[] COLUMNS = new String[] {"PMode.id", 
														  "Agreement",
														  "MEP-Binding", 
														  "Initiator PartyId", 
														  "Responder PartyId"
														  };
	
	/**
	 * Host name of the connected Holodeck B2B instance
	 */
	private String hb2bHostName;
	
	/**
	 * The P-Modes to show
	 */
	private PMode[] pmodes;
	
	/**
	 * Creates a new model instances and initialises the host name.
	 * 
	 * @param hostName	Host name of the monitored Holodeck B2B instance
	 */
	public PModesData(final String hostName) {
		this.hb2bHostName = hostName;
	}
	
	/**
	 * Sets the meta-data of the P-Modes that should be displayed in the table
	 * 
	 * @param msgUnitData	Array of P-Modes to be shown
	 */
	public void setPModes(final PMode[] pmodeData) {
		this.pmodes = pmodeData;
		fireTableDataChanged();
	}
	
	/**
	 * Gets the XML representation of the P-Mode displayed at the given row.
	 *   
	 * @param row	Row number
	 * @return		The P-Mode if a valid row number was given, <code>null</code> otherwise
	 */
	public String getPModeXML(int row) {
		if (0 <= row && row < pmodes.length) {
			StringBuffer sb = new StringBuffer();
			sb.append("<!--\n P-Mode as extracted from Holodeck B2B instance ").append(hb2bHostName);
			sb.append(" on ").append(new Date()).append("\n-->\n");					 				 						
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) { 
				pmodes[row].writeAsXMLTo(baos);
				sb.append(baos.toString());
			} catch (Exception e) {
				return "COULD NOT CREATE THE XML REPRESENTATION FOR P-MODE: " + pmodes[row].getId();
			}
			return sb.toString();
		} else
			return null;
	}
	
	@Override
	public int getRowCount() {
		return pmodes != null ? pmodes.length : 0;
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
		final PMode p = pmodes[rowIndex];
		switch (columnIndex) {
		case 0 : 
			return p.getId();
		case 1 : 
			IAgreement a = p.getAgreement();
			if (a == null || Utils.isNullOrEmpty(a.getName()))
				return "";
			else 
				return (!Utils.isNullOrEmpty(a.getType()) ? a.getType() + "::" : "" ) + a.getName(); 				 
		case 2 :
			return p.getMepBinding();
		case 3 :  
			ITradingPartnerConfiguration i = p.getInitiator();
			if (i == null || Utils.isNullOrEmpty(i.getPartyIds()))
				return "";
			else {
				IPartyId pid = i.getPartyIds().iterator().next();
				return (!Utils.isNullOrEmpty(pid.getType()) ? pid.getType() + "::" : "" ) + pid.getId();
			}
		case 4 :  
			ITradingPartnerConfiguration r = p.getResponder();
			if (r == null || Utils.isNullOrEmpty(r.getPartyIds()))
				return "";
			else {
				IPartyId pid = r.getPartyIds().iterator().next();
				return (!Utils.isNullOrEmpty(pid.getType()) ? pid.getType() + "::" : "" ) + pid.getId();
			}
		}
		throw new IllegalArgumentException("Unknown column requested");
	}
}

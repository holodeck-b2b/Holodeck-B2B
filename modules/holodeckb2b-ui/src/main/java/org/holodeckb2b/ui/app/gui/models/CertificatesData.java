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

import java.awt.Color;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.Arrays;
import org.holodeckb2b.common.util.Utils;

/**
 * The {@link TableModel} for the table listing one of the three sets of certificates currently registered with the
 * default <i>Certificate Manager</i> on the Holodeck B2B instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class CertificatesData extends AbstractTableModel {

	/**
	 * The columns 
	 */
	private static final String[] COLUMNS = new String[] {"Alias", 
														  "Subject",
														  "Issuer", 
														  "Serial No.", 
														  "Valid from",
														  "Valid until",
														  "SKI",
														  "SHA-1"
														  };
	
	/**
	 * The sorted list of aliases of the certificates to show
	 */
	private List<String> aliases;
	
	/**
	 * The alias and certificates to show
	 */
	private Map<String, X509Certificate> certs;	
	
	/**
	 * Creates a new instance of the data model for the given set of certificates that 
	 * should be displayed in the table
	 * 
	 * @param certData	Certificates mapped to their aliases
	 */
	public CertificatesData(final Map<String, X509Certificate> certData) {
		if (Utils.isNullOrEmpty(certData)) {
			aliases = null;
			certs = null;
		} else {
			aliases = new ArrayList<>(certData.keySet());
			Collections.sort(aliases);			
			certs = certData;
		}
			
		fireTableDataChanged();
	}
	
	@Override
	public int getRowCount() {
		return certs != null ? certs.size() : 0;
	}

	@Override
	public int getColumnCount() {
		return COLUMNS.length;
	}

    @Override
	public String getColumnName(int col) {
        return COLUMNS[col];
    }
    
    /**
     * Gets the color for the text of the indicated row. This will be red when the certificate on that row is not valid.
     * 
     * @param row	The row to get color for
     * @return		The color to use
     */
    public Color getRowColor(final int row) {
		final String alias = aliases.get(row);
		final X509Certificate c = certs.get(alias);
		final Date now = new Date();
		
		return c.getNotBefore().after(now) || c.getNotAfter().before(now) ? Color.RED : Color.BLACK; 
    }
    
    
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final String alias = aliases.get(rowIndex);
		final X509Certificate c = certs.get(alias);
		switch (columnIndex) {
		case 0 : 
			return alias;
		case 1 :
			return c.getSubjectDN().toString();
		case 2 :
			return c.getIssuerDN().toString();
		case 3 :
			return c.getSerialNumber().toString(16);
		case 4 :  
			return c.getNotBefore().toString();
		case 5 :  
			return c.getNotAfter().toString();
		case 6 :
			byte[] skiExtValue = c.getExtensionValue("2.5.29.14");
			byte[] ski = null;
			if (skiExtValue != null)
				ski = Arrays.copyOfRange(skiExtValue, 4, skiExtValue.length);
			return ski != null ? Hex.encodeHexString(ski) : "";
		case 7 :
	    	try {
		        byte[] encCertInfo = c.getEncoded();
		        MessageDigest md = MessageDigest.getInstance("SHA-1");
		        byte[] digest = md.digest(encCertInfo);
		        return Hex.encodeHexString(digest);	
	    	} catch (Exception e) {
	    		return "";
	    	}						
		}
		throw new IllegalArgumentException("Unknown column requested");
	}
}

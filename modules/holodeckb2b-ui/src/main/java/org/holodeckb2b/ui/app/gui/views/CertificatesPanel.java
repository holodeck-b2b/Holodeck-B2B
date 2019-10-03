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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.holodeckb2b.ui.api.CertType;
import org.holodeckb2b.ui.app.gui.HB2BMonitoringApp;
import org.holodeckb2b.ui.app.gui.models.CertificatesData;

/**
 * Is the JPanel for displaying the information about the certificates installed on the Holodeck B2B instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class CertificatesPanel extends JPanel {
	
	/**
	 * Create the panel.
	 */
	public CertificatesPanel(final HB2BMonitoringApp controller) {
		setBorder(new EmptyBorder(5, 0, 5, 5));
		setLayout(new BorderLayout(0, 0));

		JLabel contentTitle = new JLabel("Installed Certificates");
		contentTitle.setBorder(new EmptyBorder(12, 0, 0, 0));
		contentTitle.setFont(new Font("Arial", Font.PLAIN, 21));
		contentTitle.setHorizontalAlignment(SwingConstants.LEFT);
		add(contentTitle, BorderLayout.NORTH);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);		
		add(tabbedPane, BorderLayout.CENTER);
		
		tabbedPane.addTab("Private", new CertTabPanel(controller.getCertificates(CertType.Private)));
		tabbedPane.addTab("Partner", new CertTabPanel(controller.getCertificates(CertType.Partner)));
		tabbedPane.addTab("Trusted", new CertTabPanel(controller.getCertificates(CertType.Trusted)));
	}
	
	/**
	 * Is a tab showing one set of certificates. 
	 */
	public class CertTabPanel extends JPanel implements TableModelListener {
		private JTable certsTable;
		
		CertTabPanel(final CertificatesData certData) {
			certData.addTableModelListener(this);

			setLayout(new BorderLayout(0, 0));
			certsTable = new JTable(certData);
			certsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			certsTable.setFillsViewportHeight(true);		

	        // Add a custom rendering to the processing state column so that FAILURE and WARNING 
	        // states are highlighted
			certsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			    @Override
			    public Component getTableCellRendererComponent(JTable table,
			            Object value, boolean isSelected, boolean hasFocus, int row, int col) {

			        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
		            setForeground(((CertificatesData) certsTable.getModel()).getRowColor(row));
			        return this;
			    }   
			});
			
			ViewUtils.setColumnAndTableSize(certsTable);
					
			add(new JScrollPane(certsTable), BorderLayout.CENTER);
		}		
		
		@Override
		public void tableChanged(TableModelEvent e) {
			ViewUtils.setColumnAndTableSize(certsTable);
		}		
	}
}

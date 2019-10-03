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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.holodeckb2b.ui.app.gui.HB2BMonitoringApp;

/**
 * Is the JPanel for displaying the information about the P-Modes installed on the Holodeck B2B instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class PModesPanel extends JPanel implements TableModelListener {
	private HB2BMonitoringApp controller;

	private JTable		pmodesTable;
	private JEditorPane	pmodeXML;
	
	/**
	 * Create the panel.
	 */
	public PModesPanel(final HB2BMonitoringApp controller) {
		this.controller = controller;
		controller.getPModes().addTableModelListener(this);

		setBorder(new EmptyBorder(5, 0, 5, 5));
		setLayout(new BorderLayout(0, 0));

		JLabel contentTitle = new JLabel("Installed P-Modes");
		contentTitle.setBorder(new EmptyBorder(12, 0, 0, 0));
		contentTitle.setFont(new Font("Arial", Font.PLAIN, 21));
		contentTitle.setHorizontalAlignment(SwingConstants.LEFT);
		add(contentTitle, BorderLayout.NORTH);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.CENTER);
		GridBagLayout gbl_panel = new GridBagLayout();
		panel.setLayout(gbl_panel);
		
		pmodesTable = new JTable(controller.getPModes());
		pmodesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pmodesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		pmodesTable.setFillsViewportHeight(true);					
		JScrollPane scrollPane = new JScrollPane(pmodesTable);
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.weightx = 1;
		gbc_scrollPane.weighty = 1.5;
		gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		panel.add(scrollPane, gbc_scrollPane);
		ViewUtils.setColumnAndTableSize(pmodesTable);
		
		pmodesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting())
					return;
				pmodeXML.setText(controller.getPModes().getPModeXML(pmodesTable.getSelectedRow()));
				pmodeXML.setCaretPosition(0);
			}
		});		
		
		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.weightx = 1;
		gbc_separator.weighty = 0.1;
		gbc_separator.gridwidth = 1;
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 1;
		gbc_separator.anchor = GridBagConstraints.NORTH;
		gbc_separator.fill = GridBagConstraints.BOTH;
		panel.add(separator, gbc_separator);
		
		pmodeXML = new JEditorPane();		
		pmodeXML.setEditable(false);
		pmodeXML.setEditorKit(new PModeViewEditorKit());
		JScrollPane editorScrollPane = new JScrollPane(pmodeXML);
		GridBagConstraints gbc_editorScrollPane = new GridBagConstraints();
		gbc_editorScrollPane.fill = GridBagConstraints.BOTH;
		gbc_editorScrollPane.weightx = 1;
		gbc_editorScrollPane.weighty = 2;
		gbc_editorScrollPane.insets = new Insets(0, 0, 5, 0);
		gbc_editorScrollPane.gridx = 0;
		gbc_editorScrollPane.gridy = 2;
		panel.add(editorScrollPane, gbc_editorScrollPane);
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		ViewUtils.setColumnAndTableSize(pmodesTable);		
	}

}

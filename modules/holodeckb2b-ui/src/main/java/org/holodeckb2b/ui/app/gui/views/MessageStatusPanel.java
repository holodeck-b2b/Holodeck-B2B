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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.ui.app.gui.HB2BMonitoringApp;
import org.holodeckb2b.ui.app.gui.models.MessageUnitStatesData;

/**
 * Is the JPanel for querying and displaying the processing states of a message unit.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class MessageStatusPanel extends JPanel implements TableModelListener, DocumentListener {
	private HB2BMonitoringApp controller;

	private JTextField msgIdField;
	private JButton searchButton;
	private JTable msgUnitsTable;
	private JScrollPane msgUnitList;
	private JTable statesTable;
	private JLabel resultLbl;

	private JScrollPane statesList;

	private final static String NO_RESULTS = "No message units found with this MessageId.";
	private final static String MULTI_RESULTS = "Multiple message units found with this MessageId. Please select one to show states.";
	private JSeparator separator;

	/**
	 * Create the panel.
	 */
	public MessageStatusPanel(final HB2BMonitoringApp controller) {
		this.controller = controller;
		controller.getMessageUnitStatus().addTableModelListener(this);

		setBorder(new EmptyBorder(5, 0, 5, 5));
		setLayout(new BorderLayout(0, 0));

		JLabel contentTitle = new JLabel("Message Status");
		contentTitle.setBorder(new EmptyBorder(12, 0, 0, 0));
		contentTitle.setFont(new Font("Arial", Font.PLAIN, 21));
		contentTitle.setHorizontalAlignment(SwingConstants.LEFT);
		add(contentTitle, BorderLayout.NORTH);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));

		JPanel selectionPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) selectionPanel.getLayout();
		flowLayout.setHgap(0);
		flowLayout.setAlignment(FlowLayout.LEADING);
		panel.add(selectionPanel, BorderLayout.NORTH);

		JLabel lblNewLabel = new JLabel("MessageId of the message unit to get states of:");
		selectionPanel.add(lblNewLabel);

		msgIdField = new JTextField();
		msgIdField.setFocusCycleRoot(true);
		msgIdField.setHorizontalAlignment(SwingConstants.TRAILING);
		selectionPanel.add(msgIdField);
		msgIdField.setColumns(20);
		msgIdField.getDocument().addDocumentListener(this);

		searchButton = new JButton("Show");
		searchButton.setEnabled(false);
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				controller.retrieveProcessingStates(msgIdField.getText());
				setCursor(null);
			}
		});
		searchButton.setHorizontalAlignment(SwingConstants.TRAILING);
		selectionPanel.add(searchButton);

		JPanel resultPanel = new JPanel();
		panel.add(resultPanel, BorderLayout.CENTER);
		GridBagLayout gbl_resultPanel = new GridBagLayout();
		resultPanel.setLayout(gbl_resultPanel);

		JSeparator separator = new JSeparator();
		separator.setFocusTraversalKeysEnabled(false);
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.weighty = 0.1;
		gbc_separator.gridwidth = 2;
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 0;
		gbc_separator.anchor = GridBagConstraints.NORTH;
		gbc_separator.fill = GridBagConstraints.BOTH;
		resultPanel.add(separator, gbc_separator);

		resultLbl = new JLabel("Execute a search to show processing states of a message unit.");
		resultLbl.setOpaque(true);
		GridBagConstraints gbc_resultLbl = new GridBagConstraints();
		gbc_resultLbl.gridwidth = 2;
		gbc_resultLbl.ipady = 10;
		gbc_resultLbl.anchor = GridBagConstraints.WEST;
		gbc_resultLbl.gridx = 0;
		gbc_resultLbl.gridy = 1;
		resultPanel.add(resultLbl, gbc_resultLbl);

		msgUnitsTable = new JTable(controller.getMessageUnitStatus());
		msgUnitsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		msgUnitsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		msgUnitsTable.setFillsViewportHeight(true);	
		msgUnitList = new JScrollPane(msgUnitsTable);
		GridBagConstraints gbc_msgUnitList = new GridBagConstraints();
		gbc_msgUnitList.insets = new Insets(0, 0, 0, 15);
		gbc_msgUnitList.fill = GridBagConstraints.BOTH;
		gbc_msgUnitList.weightx = 1.0;
		gbc_msgUnitList.weighty = 5.0;
		gbc_msgUnitList.anchor = GridBagConstraints.NORTHWEST;
		gbc_msgUnitList.gridx = 0;
		gbc_msgUnitList.gridy = 2;
		resultPanel.add(msgUnitList, gbc_msgUnitList);
		
		statesTable = new JTable(controller.getMessageUnitStatus().getStatesModel(0));
		statesTable.setEnabled(false);
		statesTable.setFillsViewportHeight(true);
		statesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		statesList = new JScrollPane(statesTable);
		GridBagConstraints gbc_statesList = new GridBagConstraints();
		gbc_statesList.weightx = 1.0;
		gbc_statesList.weighty = 4.0;
		gbc_statesList.anchor = GridBagConstraints.NORTHEAST;
		gbc_statesList.fill = GridBagConstraints.BOTH;
		gbc_statesList.gridx = 1;
		gbc_statesList.gridy = 2;
		resultPanel.add(statesList, gbc_statesList);
		
		msgUnitsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting())
					return;
				statesTable.setModel(controller.getMessageUnitStatus().getStatesModel(msgUnitsTable.getSelectedRow()));
				ViewUtils.setColumnAndTableSize(statesTable);
			}
		});
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		MessageUnitStatesData result = controller.getMessageUnitStatus();
		
		// Clear the states table
		statesTable.setModel(result.getStatesModel(-1));
		if (result.getRowCount() == 0) {
			// No message unit found with the given id, show only message
			resultLbl.setText(NO_RESULTS);
			resultLbl.setBackground(Color.ORANGE);
		} else {			
			if (result.getRowCount() == 1) {			
				// Single result, direct display list of statuses
				resultLbl.setText("");
				statesTable.setModel(result.getStatesModel(0));
			} else {
				// Multiple result, enable selection of a message unit
				resultLbl.setText(MULTI_RESULTS);
			}
			resultLbl.setBackground(null);
			ViewUtils.setColumnAndTableSize(msgUnitsTable);
			ViewUtils.setColumnAndTableSize(statesTable);
		}
	}
	
	@Override
	public void insertUpdate(DocumentEvent e) {
		checkMessageIdAvailable();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		checkMessageIdAvailable();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		checkMessageIdAvailable();
	}

	private void checkMessageIdAvailable() {
		boolean enableSearch = !Utils.isNullOrEmpty(msgIdField.getText());
		searchButton.setEnabled(enableSearch);
		if (enableSearch)
			getRootPane().setDefaultButton(searchButton);
		else
			getRootPane().setDefaultButton(null);
	}
}

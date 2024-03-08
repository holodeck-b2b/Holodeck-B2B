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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.ui.app.gui.HB2BMonitoringApp;
import org.holodeckb2b.ui.app.gui.models.MessageHistoryData;

import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;

/**
 * Is the JPanel for displaying the message history.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class MessageHistoryPanel extends JPanel implements TableModelListener {

	private JTable msgUnitsTable;

	private int	selectedRow = -1;

	/**
	 * Create the panel.
	 */
	public MessageHistoryPanel(final HB2BMonitoringApp controller) {
		setBorder(new EmptyBorder(5, 0, 5, 5));

		setLayout(new BorderLayout(0, 0));

		JLabel contentTitle = new JLabel("Message History");
		contentTitle.setBorder(new EmptyBorder(12, 0, 0, 0));
		contentTitle.setFont(new Font("Arial", Font.PLAIN, 21));
		contentTitle.setHorizontalAlignment(SwingConstants.LEFT);
		add(contentTitle, BorderLayout.NORTH);

		msgUnitsTable = new JTable(controller.getMessageHistoryData());
		msgUnitsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		msgUnitsTable.setFillsViewportHeight(true);
		msgUnitsTable.getModel().addTableModelListener(this);
    	msgUnitsTable.getTableHeader().setFont(msgUnitsTable.getFont().deriveFont(Font.BOLD, msgUnitsTable.getFont().getSize() + 1));

    	// Add a popup menu to copy the MessageId or RefToMessageId to the clipboard
    	final JPopupMenu popupMenu = new JPopupMenu();

    	popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int rowAtPoint = msgUnitsTable.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), msgUnitsTable));
                        if (rowAtPoint > -1) {
                            msgUnitsTable.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                            selectedRow = rowAtPoint;
                        }
                    }
                });
            }
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    	popupMenu.add(createCopyItem(msgUnitsTable, "MessageId", 4));
    	popupMenu.add(createCopyItem(msgUnitsTable, "RefToMessageId", 5));
    	popupMenu.add(createCopyItem(msgUnitsTable, "PMode.Id", 6));

        msgUnitsTable.setComponentPopupMenu(popupMenu);

        // Add a custom rendering to the processing state column so that FAILURE and WARNING
        // states are highlighted
        msgUnitsTable.getColumnModel().getColumn(MessageHistoryData.STATE_COLUMN).setCellRenderer(
        		new DefaultTableCellRenderer() {
        			@Override
        			public Component getTableCellRendererComponent(JTable table, Object value,
                            							boolean isSelected, boolean hasFocus, int row, int column) {
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        				if (ProcessingState.FAILURE.name().equals(value))
        					setForeground(Color.RED);
        				else if (ProcessingState.WARNING.name().equals(value))
        					setForeground(new Color(255, 153, 0));
        				return this;
        			}
        		});

        ViewUtils.setColumnAndTableSize(msgUnitsTable);

		JScrollPane scrollPane = new JScrollPane(msgUnitsTable);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		add(panel_1, BorderLayout.SOUTH);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		panel_1.setLayout(gbl_panel_1);

		JLabel lblShow = new JLabel("Show last");
		lblShow.setHorizontalAlignment(SwingConstants.LEFT);
		lblShow.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_lblShow = new GridBagConstraints();
		gbc_lblShow.anchor = GridBagConstraints.WEST;
		gbc_lblShow.insets = new Insets(0, 0, 5, 5);
		gbc_lblShow.gridy = 0;
		panel_1.add(lblShow, gbc_lblShow);

		JComboBox comboBox = new JComboBox();
		comboBox.setModel(new DefaultComboBoxModel(new String[] {"10", "25", "50"}));
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.insets = new Insets(0, 0, 5, 5);
		gbc_comboBox.anchor = GridBagConstraints.WEST;
		gbc_comboBox.gridy = 0;
		panel_1.add(comboBox, gbc_comboBox);

		JLabel lblStartFrom = new JLabel("messages starting before:");
		lblStartFrom.setHorizontalAlignment(SwingConstants.LEFT);
		lblStartFrom.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_lblStartFrom = new GridBagConstraints();
		gbc_lblStartFrom.anchor = GridBagConstraints.WEST;
		gbc_lblStartFrom.insets = new Insets(0, 0, 5, 5);
		gbc_lblStartFrom.gridy = 0;
		panel_1.add(lblStartFrom, gbc_lblStartFrom);

		DatePickerSettings dateSettings = new DatePickerSettings();
		dateSettings.setFormatForDatesCommonEra("uuuu/MM/dd");
        dateSettings.setAllowEmptyDates(false);

        TimePickerSettings timeSettings = new TimePickerSettings();
	    timeSettings.use24HourClockFormat();
	    timeSettings.setAllowEmptyTimes(false);

        DateTimePicker dateTimePicker = new DateTimePicker(dateSettings, timeSettings);
        dateTimePicker.datePicker.setDateToToday();
        dateTimePicker.timePicker.setTime(LocalTime.now(ZoneOffset.UTC));

        JButton datePickerButton = dateTimePicker.datePicker.getComponentToggleCalendarButton();
        datePickerButton.setText("");
        datePickerButton.setIcon(new ImageIcon(MainWindow.class.getResource("/img/datepicker.png")));
        JButton timePickerButton = dateTimePicker.timePicker.getComponentToggleTimeMenuButton();
        timePickerButton.setText("");
        timePickerButton.setIcon(new ImageIcon(MainWindow.class.getResource("/img/timepicker.png")));
        timePickerButton.setPreferredSize(datePickerButton.getPreferredSize());

		GridBagConstraints gbc_dtPicker = new GridBagConstraints();
		gbc_dtPicker.insets = new Insets(0, 0, 5, 5);
		gbc_dtPicker.anchor = GridBagConstraints.WEST;
		gbc_dtPicker.gridy = 0;
		panel_1.add(dateTimePicker, gbc_dtPicker);

		JLabel lblTimezone = new JLabel("(UTC)");
		GridBagConstraints gbc_lblTimezone = new GridBagConstraints();
		gbc_lblTimezone.insets = new Insets(0, 0, 5, 5);
		gbc_lblTimezone.anchor = GridBagConstraints.WEST;
		gbc_lblTimezone.gridy = 0;
		panel_1.add(lblTimezone, gbc_lblTimezone);

		JButton btnNewButton = new JButton("Apply");
		btnNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				controller.retrieveMessageUnitHistory(Date.from(dateTimePicker.getDateTimePermissive()
																			  .toInstant(ZoneOffset.UTC)),
													  Integer.parseInt((String) comboBox.getSelectedItem()));
				setCursor(null);
			}
		});
		btnNewButton.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton.anchor = GridBagConstraints.WEST;
		gbc_btnNewButton.gridy = 0;
		panel_1.add(btnNewButton, gbc_btnNewButton);
	}

    private JMenuItem createCopyItem(JTable tbl, String attrName, int attrCol) {
    	JMenuItem item = new JMenuItem("Copy " + attrName);
        item.setFont(tbl.getFont().deriveFont(getFont().getSize()-2));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	Toolkit.getDefaultToolkit().getSystemClipboard()
							                .setContents(
							                        new StringSelection((String) tbl.getValueAt(selectedRow, attrCol)),
							                        null
							                );
            }
        });
        return item;
	}

	@Override
	public void tableChanged(TableModelEvent e) {
    	ViewUtils.setColumnAndTableSize(msgUnitsTable);
    }


}

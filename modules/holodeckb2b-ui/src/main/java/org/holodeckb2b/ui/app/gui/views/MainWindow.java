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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.holodeckb2b.common.VersionInfo;
import org.holodeckb2b.ui.app.gui.HB2BMonitoringApp;

/**
 * Is the main application window.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class MainWindow extends JFrame {

	private static final String MSG_HISTORY_PANEL = "history";
	private static final String MSG_STATUS_PANEL = "status";
	private static final String PMODES_PANEL = "pmodes";
	private static final String CERTS_PANEL = "certs";

	/**
	 * Initialize the contents of the main window.
	 */
	public MainWindow(final HB2BMonitoringApp controller, final boolean certsAvailable) {

		setTitle("Holodeck B2B Gateway Monitoring");
		setBounds(100, 100, 879, 593);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel content = new JPanel();
		getContentPane().add(content, BorderLayout.CENTER);
		content.setLayout(new CardLayout(0, 0));

		content.add(new MessageHistoryPanel(controller), MSG_HISTORY_PANEL);
		content.add(new MessageStatusPanel(controller), MSG_STATUS_PANEL);
		content.add(new PModesPanel(controller), PMODES_PANEL);
		if (certsAvailable)
			content.add(new CertificatesPanel(controller), CERTS_PANEL);

		JPanel menuPanel = new JPanel();
		menuPanel.setBorder(new EmptyBorder(5, 5, 5, 19));
		getContentPane().add(menuPanel, BorderLayout.WEST);
		menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

		JLabel logo = new JLabel("");
		logo.setAlignmentX(Component.CENTER_ALIGNMENT);
		logo.setHorizontalAlignment(SwingConstants.CENTER);
		logo.setIcon(new ImageIcon(MainWindow.class.getResource("/img/logo-compact.png")));
		menuPanel.add(logo);

		JPanel panel = new JPanel();
		menuPanel.add(panel);

		JPanel menuOptionsPanel = new JPanel();
		menuPanel.add(menuOptionsPanel);
		menuOptionsPanel.setLayout(new GridLayout(9, 0, 0, 0));

		JLabel lblMessageHistory = new JLabel("Message History");
		lblMessageHistory.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				((CardLayout) content.getLayout()).show(content, MSG_HISTORY_PANEL);
			}
		});
		lblMessageHistory.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		menuOptionsPanel.add(lblMessageHistory);

		JLabel lblMessageStatus = new JLabel("Message Status");
		lblMessageStatus.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				((CardLayout) content.getLayout()).show(content, MSG_STATUS_PANEL);
			}
		});
		lblMessageStatus.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		menuOptionsPanel.add(lblMessageStatus);

		JLabel lblPModes = new JLabel("P-Modes");
		lblPModes.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		menuOptionsPanel.add(lblPModes);
		lblPModes.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				((CardLayout) content.getLayout()).show(content, PMODES_PANEL);
			}
		});

		JLabel lblTrustedCertificates = new JLabel("Certificates");
		lblTrustedCertificates.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		lblTrustedCertificates.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				((CardLayout) content.getLayout()).show(content, CERTS_PANEL);
			}
		});
		if (certsAvailable)
			menuOptionsPanel.add(lblTrustedCertificates);

		JPanel spacer = new JPanel();
		spacer.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		menuPanel.add(spacer);

		JPanel statusbar = new JPanel();
		getContentPane().add(statusbar, BorderLayout.SOUTH);
		statusbar.setBorder(new EmptyBorder(0, 5, 5, 5));
		statusbar.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel = new JLabel("Monitoring instance: " + controller.getMonitoredInstance());
		lblNewLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusbar.add(lblNewLabel, BorderLayout.WEST);

		JLabel lblVersion = new JLabel("Holodeck B2B " + VersionInfo.fullVersion);
		lblVersion.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
		lblVersion.setForeground(SystemColor.windowBorder);
		lblVersion.setHorizontalAlignment(SwingConstants.RIGHT);
		statusbar.add(lblVersion, BorderLayout.EAST);

	}
}

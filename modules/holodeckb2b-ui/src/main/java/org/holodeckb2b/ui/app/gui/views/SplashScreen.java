/* Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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

import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

/**
 * Is the splash screen of the monitoring application shown during initialisation. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class SplashScreen extends JFrame {
	/**
	 * The label for displaying the current status
	 */
	private JLabel lblStatus;
	
	/**
	 * Creates and shows the splash screen of the app. 
	 * 
	 *  @param port Number of the port used to connect to the Holodeck B2B instance.
	 */
	public SplashScreen(final int port) {
        getContentPane().setLayout(null);
        setUndecorated(true);
        setSize(605,196);
        setLocationRelativeTo(null);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        JLabel image=new JLabel(new ImageIcon(SplashScreen.class.getResource("/img/logo-startup.png")));
        image.setSize(600,120);
        getContentPane().add(image);

        JLabel text=new JLabel("Gateway Monitoring");
        text.setHorizontalAlignment(SwingConstants.CENTER);
        text.setFont(new Font("Arial", Font.BOLD,30));
        text.setBounds(0,125,600,40);        
        getContentPane().add(text);
        
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
        panel.setBounds(0, 175, 604, 20);
        getContentPane().add(panel);
        
        lblStatus = new JLabel("Connecting to local instance on port " + port + "...");
        panel.add(lblStatus);
        
        new Thread(new Runnable() { 
			@Override
			public void run() {
        		try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				} finally {
					synchronized(this) { 
						this.notify();
					}
				}        		
			} 
        }).start();
        setVisible(true);
	}
	
	/**
	 * Updates the displayed status message.
	 * 
	 * @param newStatus	The new status
	 */
	public void updateStatus(final String newStatus) {
		lblStatus.setText(newStatus);
	}
	
	/**
	 * Closes the splash screen and releases all attached resources.
	 */
	public void close() {
		try {
			synchronized(this) { 
				this.wait(2000);
			}
		} catch (InterruptedException e) {
		}
		this.setVisible(false);
	}
}

package com.tea.orm.thread;

import javax.swing.JDialog;
import javax.swing.JLabel;

@SuppressWarnings("serial")
public class PromptDialog extends JDialog {
	
	public PromptDialog() {
		setVisible(true);
		setAlwaysOnTop(true);
		add(new JLabel("Data Loading, Please Wait......"));
		setSize(200, 100);
		setTitle("Data Loading");
		setLocationRelativeTo(null);
	}
	
}

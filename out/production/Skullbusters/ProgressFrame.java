import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;
/*
 * Created by JFormDesigner on Tue Feb 20 09:59:27 PST 2018
 */



/**
 * @author Erik Huerta
 */
public class ProgressFrame extends JFrame {
	public ProgressFrame() {
		initComponents();
	}

	private void cancelButtonActionPerformed(ActionEvent e) {
		// TODO add your code here
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
		transferLabel = new JLabel();
		progressBar = new JProgressBar();
		progressLabel = new JLabel();
		cancelButton = new JButton();

		//======== this ========
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setName("progressFrame");
		Container contentPane = getContentPane();

		//---- transferLabel ----
		transferLabel.setText("Transfer Progress");
		transferLabel.setFont(transferLabel.getFont().deriveFont(transferLabel.getFont().getStyle() | Font.BOLD, transferLabel.getFont().getSize() + 5f));

		//---- progressLabel ----
		progressLabel.setText("0 %");
		progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
		progressLabel.setForeground(new Color(153, 0, 0));
		progressLabel.setFont(progressLabel.getFont().deriveFont(Font.BOLD|Font.ITALIC));

		//---- cancelButton ----
		cancelButton.setText("Cancel");
		cancelButton.setFont(cancelButton.getFont().deriveFont(cancelButton.getFont().getStyle() | Font.BOLD, cancelButton.getFont().getSize() + 3f));
		cancelButton.addActionListener(e -> cancelButtonActionPerformed(e));

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addGroup(contentPaneLayout.createParallelGroup()
						.addComponent(progressLabel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addComponent(transferLabel)
							.addGap(0, 342, Short.MAX_VALUE))
						.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
						.addComponent(cancelButton, GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE))
					.addGap(25, 25, 25))
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addComponent(transferLabel)
					.addGap(18, 18, 18)
					.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(progressLabel)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 75, Short.MAX_VALUE)
					.addComponent(cancelButton)
					.addGap(25, 25, 25))
		);
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Erik Huerta
	private JLabel transferLabel;
	private JProgressBar progressBar;
	private JLabel progressLabel;
	private JButton cancelButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}

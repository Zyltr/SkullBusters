import java.awt.*;
import javax.swing.*;
import javax.swing.GroupLayout;
/*
 * Created by JFormDesigner on Tue Feb 20 10:54:08 PST 2018
 */



/**
 * @author Erik Huerta
 */
public class ProgressFrame extends JFrame {
	public ProgressFrame() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
		transferLabel = new JLabel();
		progressBar1 = new JProgressBar();

		//======== this ========
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		Container contentPane = getContentPane();

		//---- transferLabel ----
		transferLabel.setText("Transfer Progress");
		transferLabel.setFont(transferLabel.getFont().deriveFont(transferLabel.getFont().getStyle() | Font.BOLD, transferLabel.getFont().getSize() + 5f));

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addGroup(contentPaneLayout.createParallelGroup()
						.addComponent(transferLabel)
						.addComponent(progressBar1, GroupLayout.PREFERRED_SIZE, 402, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(31, Short.MAX_VALUE))
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addComponent(transferLabel)
					.addGap(18, 18, 18)
					.addComponent(progressBar1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(121, Short.MAX_VALUE))
		);
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Erik Huerta
	private JLabel transferLabel;
	private JProgressBar progressBar1;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}

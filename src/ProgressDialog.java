import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;
/*
 * Created by JFormDesigner on Wed Feb 14 11:54:37 PST 2018
 */


/**
 * @author Erik Huerta
 */
public class ProgressDialog extends JDialog
{
    public ProgressDialog ( Window owner )
    {
        super ( owner );
        initComponents ();
    }

	private void cancelButtonActionPerformed(ActionEvent e)
    {
		// TODO -> Cancel Transfer
        dispose ();
	}

    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
		JLabel fileTrasnferLabel = new JLabel();
		fileProgressBar = new JProgressBar();
		fileProgressLabel = new JLabel();
		cancelButton = new JButton();

		//======== this ========
		setTitle("Progress");
		Container contentPane = getContentPane();

		//---- fileTrasnferLabel ----
		fileTrasnferLabel.setText("File Transfer Progress");
		fileTrasnferLabel.setFont(fileTrasnferLabel.getFont().deriveFont(fileTrasnferLabel.getFont().getStyle() | Font.BOLD, fileTrasnferLabel.getFont().getSize() + 3f));

		//---- fileProgressLabel ----
		fileProgressLabel.setText("0 %");
		fileProgressLabel.setHorizontalAlignment(SwingConstants.CENTER);
		fileProgressLabel.setFont(fileProgressLabel.getFont().deriveFont(Font.BOLD|Font.ITALIC, fileProgressLabel.getFont().getSize() + 3f));
		fileProgressLabel.setForeground(new Color(153, 0, 0));

		//---- cancelButton ----
		cancelButton.setText("Cancel");
		cancelButton.setFont(cancelButton.getFont().deriveFont(cancelButton.getFont().getStyle() | Font.BOLD, cancelButton.getFont().getSize() + 5f));
		cancelButton.addActionListener(e -> cancelButtonActionPerformed(e));

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
						.addGroup(GroupLayout.Alignment.LEADING, contentPaneLayout.createSequentialGroup()
							.addComponent(fileTrasnferLabel)
							.addGap(0, 0, Short.MAX_VALUE))
						.addComponent(fileProgressBar, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 548, Short.MAX_VALUE)
						.addComponent(fileProgressLabel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 548, Short.MAX_VALUE)
						.addComponent(cancelButton, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 548, Short.MAX_VALUE))
					.addGap(25, 25, 25))
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addComponent(fileTrasnferLabel)
					.addGap(18, 18, 18)
					.addComponent(fileProgressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(fileProgressLabel)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 112, Short.MAX_VALUE)
					.addComponent(cancelButton)
					.addGap(25, 25, 25))
		);
		pack();
		setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Erik Huerta
	private JProgressBar fileProgressBar;
	private JLabel fileProgressLabel;
	private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;
/*
 * Created by JFormDesigner on Tue Feb 20 10:13:05 PST 2018
 */


public class ProgressDialog extends JDialog
{
    public ProgressDialog ( Window owner )
    {
        super ( owner );
        initComponents ();
    }

    private void cancelButtonActionPerformed ( ActionEvent e )
    {
        // TODO -> Cancel File Transfer
        System.out.println ( "Progress Dialog > Cancelled" );
        dispose ();
    }

    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
		transferLabel = new JLabel();
		progressBar1 = new JProgressBar();
		progressLabel = new JLabel();
		cancelButton = new JButton();

		//======== this ========
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		Container contentPane = getContentPane();

		//---- transferLabel ----
		transferLabel.setText("Transfer Progress");
		transferLabel.setFont(transferLabel.getFont().deriveFont(Font.BOLD, transferLabel.getFont().getSize() + 5f));

		//---- progressLabel ----
		progressLabel.setText("0 %");
		progressLabel.setFont(progressLabel.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		progressLabel.setForeground(new Color(153, 0, 0));
		progressLabel.setHorizontalAlignment(SwingConstants.CENTER);

		//---- cancelButton ----
		cancelButton.setText("Cancel");
		cancelButton.setFont(cancelButton.getFont().deriveFont(cancelButton.getFont().getStyle() | Font.BOLD, cancelButton.getFont().getSize() + 3f));
		cancelButton.addActionListener(e -> cancelButtonActionPerformed(e));

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
						.addComponent(cancelButton, GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
						.addComponent(progressBar1, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
						.addGroup(GroupLayout.Alignment.LEADING, contentPaneLayout.createSequentialGroup()
							.addComponent(transferLabel)
							.addGap(0, 242, Short.MAX_VALUE))
						.addComponent(progressLabel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE))
					.addGap(25, 25, 25))
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addComponent(transferLabel)
					.addGap(18, 18, 18)
					.addComponent(progressBar1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(progressLabel)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
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
	private JProgressBar progressBar1;
	private JLabel progressLabel;
	private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

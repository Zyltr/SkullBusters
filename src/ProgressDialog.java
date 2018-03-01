import javax.swing.*;
import java.awt.*;

/*
 * Created by JFormDesigner on Tue Feb 20 10:13:05 PST 2018
 */


/**
 * A JDialog which provides a progress bar for a file that is being transferred to a Server
 */
class ProgressDialog extends JDialog
{
    ProgressDialog ()
    {
        super ( (Window ) null );
        initComponents ();
    }


	/**
	 * When the "Cancel" button is pressed, dispose of the JDialog
	 */
	private void cancelButtonActionPerformed () { dispose (); }


	/**
	 * Update the progress bar with the parameterized "newValue"
	 * @param newValue : the value for which the "progressBar" and "progressLabel" variables will be updated with
	 */
    public void updateProgressBar ( int newValue )
    {
        progressBar.setValue ( newValue );
        progressLabel.setText ( newValue + " %" );
    }


	/**
	 * JFormDesigner generated method
	 */
	private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY
		// GEN-BEGIN:initComponents
		JLabel transferLabel = new JLabel();
		progressBar = new JProgressBar();
		progressLabel = new JLabel();
		JButton cancelButton = new JButton();

		//======== this ========
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setModal(true);
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
		cancelButton.addActionListener(e -> cancelButtonActionPerformed());

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addGroup(contentPaneLayout.createParallelGroup()
						.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE)
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addComponent(transferLabel)
							.addGap(0, 182, Short.MAX_VALUE))
						.addComponent(progressLabel, GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE)
						.addComponent(cancelButton, GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE))
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
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
					.addComponent(cancelButton)
					.addGap(25, 25, 25))
		);
		pack();
		setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization
		// GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY
	// GEN-BEGIN:variables
	private JProgressBar progressBar;
	private JLabel progressLabel;
    // JFormDesigner - End of variables declaration
	// GEN-END:variables
}

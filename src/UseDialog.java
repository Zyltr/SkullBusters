import java.awt.*;
import javax.swing.*;
/*
 * Created by JFormDesigner on Fri Mar 02 09:22:00 PST 2018
 */

/**
 * Presents a JDialog that explains the functions of the application
 */
class UseDialog extends JDialog
{
	/**
	 *
	 * @param owner
	 */
    UseDialog ( Window owner )
    {
        super ( owner );
        initComponents ();
    }

	/**
	 * Creates JDialog
	 */
	private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		JScrollPane imageScrollPane = new JScrollPane();
		JLabel imageLabel = new JLabel();

		//======== this ========
		setTitle("How to Use");
		setModal(true);
		setName("useDialog");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Container contentPane = getContentPane();

		//======== imageScrollPane ========
		{

			//---- imageLabel ----
			imageLabel.setIcon(new ImageIcon(getClass().getResource("/Images/CS380-Skullbuster-Instructions.png")));
			imageScrollPane.setViewportView(imageLabel);
		}

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addComponent(imageScrollPane, GroupLayout.DEFAULT_SIZE, 698, Short.MAX_VALUE)
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addComponent(imageScrollPane, GroupLayout.DEFAULT_SIZE, 476, Short.MAX_VALUE)
		);
		pack();
		setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

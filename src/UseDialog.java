import java.awt.*;
import javax.swing.*;
/*
 * Created by JFormDesigner on Fri Mar 02 09:22:00 PST 2018
 */


/**
 * @author Erik Huerta
 */
class UseDialog extends JDialog
{
    public UseDialog ( Window owner )
    {
        super ( owner );
        initComponents ();
    }

    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
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
			imageLabel.setIcon(new ImageIcon("/Users/Erik/Documents/GitHub/Skullbusters/src/Images/CS380-Skullbuster-Instructions.png"));
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
	// Generated using JFormDesigner Evaluation license - Erik Huerta
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

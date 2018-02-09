import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;

import org.jdesktop.beansbinding.*;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
/*
 * Created by JFormDesigner on Thu Jan 25 00:54:42 PST 2018
 */

public class Skullbusters extends JFrame
{
    public Skullbusters ()
    {
        initComponents ();
    }

    private void aboutMenuItemActionPerformed ( ActionEvent e )
    {
        // TODO -> Show About Frame
        new AboutDialog ( this ).setVisible ( true );
    }

    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
		JMenuBar menuBar = new JMenuBar();
		JMenu skullbustersMenu = new JMenu();
		JMenuItem aboutMenuItem = new JMenuItem();
		JTabbedPane tabbedPane = new JTabbedPane();
		JScrollPane serverScrollPane = new JScrollPane ( new ServerPanel () );
		JScrollPane clientScrollPane = new JScrollPane ( new ClientPanel () );

		//======== this ========
		setName("frame");
		setTitle("Skullbusters's FTP Tool");
		Container contentPane = getContentPane();

		//======== menuBar ========
		{

			//======== skullbustersMenu ========
			{
				skullbustersMenu.setText("Skullbusters");

				//---- aboutMenuItem ----
				aboutMenuItem.setText("About");
				aboutMenuItem.addActionListener(e -> aboutMenuItemActionPerformed(e));
				skullbustersMenu.add(aboutMenuItem);
			}
			menuBar.add(skullbustersMenu);
		}
		setJMenuBar(menuBar);

		//======== tabbedPane ========
		{

			//======== serverScrollPane ========
			{
				serverScrollPane.setBorder(null);
			}
			tabbedPane.addTab("Server", serverScrollPane);

			//======== clientScrollPane ========
			{
				clientScrollPane.setBorder(null);
			}
			tabbedPane.addTab("Client", clientScrollPane);
		}

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGap(5, 5, 5)
					.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 438, Short.MAX_VALUE)
					.addGap(5, 5, 5))
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 742, Short.MAX_VALUE)
					.addContainerGap())
		);
		setSize(450, 800);
		setLocationRelativeTo(getOwner());

		//---- bindings ----
		bindingGroup = new BindingGroup();
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			this, BeanProperty.create("preferredSize"),
			this, BeanProperty.create("minimumSize")));
		bindingGroup.bind();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    private void initComponentBindings ()
    {
        // JFormDesigner - Component bindings initialization - DO NOT MODIFY  //GEN-BEGIN:initBindings
		// Generated using JFormDesigner Evaluation license - Erik Huerta
        // JFormDesigner - End of component bindings initialization  //GEN-END:initBindings
    }

    public static void main ( String[] args )
    {
        SwingUtilities.invokeLater ( () -> {
            new Skullbusters ().setVisible ( true );
        } );
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Erik Huerta
	private BindingGroup bindingGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
/*
 * Created by JFormDesigner on Sun Jan 28 21:23:50 PST 2018
 */

class AboutDialog extends JDialog
{
    public AboutDialog ( Window owner )
    {
        super ( owner );
        initComponents ();
    }

    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
		JPanel bulldogPanel = new JPanel();
		JLabel bulldogLabel = new JLabel();
		JLabel createdLabel = new JLabel();
		JLabel memberLabel = new JLabel();
		JLabel memberLabel2 = new JLabel();
		JLabel memberLabel3 = new JLabel();
		JLabel memberLabel4 = new JLabel();
		JLabel memberLabel5 = new JLabel();
		JLabel projectLabel = new JLabel();

		//======== this ========
		setTitle("About Skullbusters");
		setName("aboutDialog");
		setResizable(false);
		setModal(true);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Container contentPane = getContentPane();

		//======== bulldogPanel ========
		{
			bulldogPanel.setBackground(Color.white);
			bulldogPanel.setBorder(new BevelBorder(BevelBorder.RAISED));

			// JFormDesigner evaluation mark
			bulldogPanel.setBorder(new javax.swing.border.CompoundBorder(
				new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
					"JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER,
					javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
					java.awt.Color.red), bulldogPanel.getBorder())); bulldogPanel.addPropertyChangeListener(new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){if("border".equals(e.getPropertyName()))throw new RuntimeException();}});


			//---- bulldogLabel ----
			bulldogLabel.setIcon(new ImageIcon("/Users/Erik/Documents/GitHub/Skullbusters/src/Images/Bulldog.jpg"));
			bulldogLabel.setHorizontalAlignment(SwingConstants.CENTER);

			GroupLayout bulldogPanelLayout = new GroupLayout(bulldogPanel);
			bulldogPanel.setLayout(bulldogPanelLayout);
			bulldogPanelLayout.setHorizontalGroup(
				bulldogPanelLayout.createParallelGroup()
					.addGroup(bulldogPanelLayout.createSequentialGroup()
						.addGap(15, 15, 15)
						.addComponent(bulldogLabel)
						.addGap(15, 15, 15))
			);
			bulldogPanelLayout.setVerticalGroup(
				bulldogPanelLayout.createParallelGroup()
					.addGroup(GroupLayout.Alignment.TRAILING, bulldogPanelLayout.createSequentialGroup()
						.addGap(15, 15, 15)
						.addComponent(bulldogLabel)
						.addGap(15, 15, 15))
			);
		}

		//---- createdLabel ----
		createdLabel.setText("Created By ");
		createdLabel.setFont(createdLabel.getFont().deriveFont(createdLabel.getFont().getStyle() | Font.BOLD, createdLabel.getFont().getSize() + 3f));
		createdLabel.setHorizontalAlignment(SwingConstants.CENTER);

		//---- memberLabel ----
		memberLabel.setText("Sean McCullough");
		memberLabel.setFont(memberLabel.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		memberLabel.setHorizontalAlignment(SwingConstants.CENTER);
		memberLabel.setForeground(new Color(153, 0, 0));

		//---- memberLabel2 ----
		memberLabel2.setText("Ryan Tang");
		memberLabel2.setFont(memberLabel2.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		memberLabel2.setHorizontalAlignment(SwingConstants.CENTER);
		memberLabel2.setForeground(new Color(153, 0, 0));

		//---- memberLabel3 ----
		memberLabel3.setText("Erik Huerta");
		memberLabel3.setFont(memberLabel3.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		memberLabel3.setHorizontalAlignment(SwingConstants.CENTER);
		memberLabel3.setForeground(new Color(153, 0, 0));

		//---- memberLabel4 ----
		memberLabel4.setText("Eric Rensel");
		memberLabel4.setFont(memberLabel4.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		memberLabel4.setHorizontalAlignment(SwingConstants.CENTER);
		memberLabel4.setForeground(new Color(153, 0, 0));

		//---- memberLabel5 ----
		memberLabel5.setText("Yuan Kun Chen (Erik)");
		memberLabel5.setFont(memberLabel5.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		memberLabel5.setHorizontalAlignment(SwingConstants.CENTER);
		memberLabel5.setForeground(new Color(153, 0, 0));

		//---- projectLabel ----
		projectLabel.setText("Cal Poly Pomona CS 380 Project");
		projectLabel.setFont(projectLabel.getFont().deriveFont(projectLabel.getFont().getStyle() | Font.BOLD, projectLabel.getFont().getSize() + 5f));
		projectLabel.setHorizontalAlignment(SwingConstants.CENTER);

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGroup(contentPaneLayout.createParallelGroup()
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addGap(135, 135, 135)
							.addComponent(bulldogPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(0, 123, Short.MAX_VALUE))
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(projectLabel, GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE))
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(memberLabel, GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE))
						.addComponent(memberLabel2, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE)
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(memberLabel3, GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE))
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(memberLabel4, GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE))
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(memberLabel5, GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE))
						.addComponent(createdLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addContainerGap())
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addGap(20, 20, 20)
					.addComponent(bulldogPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(25, 25, 25)
					.addComponent(projectLabel)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(createdLabel)
					.addGap(18, 18, 18)
					.addComponent(memberLabel)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(memberLabel2)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(memberLabel3)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(memberLabel4)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(memberLabel5)
					.addContainerGap(27, Short.MAX_VALUE))
		);
		pack();
		setLocationRelativeTo(getOwner());

		//---- bindings ----
		bindingGroup = new BindingGroup();
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			this, BeanProperty.create("preferredSize"),
			this, BeanProperty.create("minimumSize")));
		bindingGroup.bind();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }


	//GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Erik Huerta
	private BindingGroup bindingGroup;
	// GEN-END:variables
}

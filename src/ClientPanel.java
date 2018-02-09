import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.*;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
/*
 * Created by JFormDesigner on Thu Jan 25 02:18:08 PST 2018
 */

public class ClientPanel extends JPanel
{
	// TODO -> Used for JOptionPane messages
	private String message;

	// TODO -> "Send File" Variables
	private Path filePath;
	private JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getHomeDirectory () );

	// TODO -> Variable that will store XOR-Key
	private ArrayList<Integer> xorKeyList = new ArrayList<> ();
	private String xorKey;

	// TODO -> Variable that will be used to partition file into a specific size of Bytes
	private Integer chunkSize = 1024;

	// TODO -> Required Server Variables
	private Socket serverSocket;
	private PrintWriter printWriter;
	private BufferedReader bufferedReader;

	// TODO -> Useful for controlling Thread initiated by the "Connect" Button
	private volatile boolean looping = false;

	public ClientPanel ()
	{
		initComponents ();

		// TODO -> Configure JFileChooser
		fileChooser.setDialogTitle ( "Choose File to Send" );
		fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );
	}


	// TODO -> Attempts to find the file that will be used to transfer to the Server
	private void fileButtonActionPerformed ( ActionEvent e )
	{
		// TODO -> Present Open Dialogue and Update "Transfer File" Label if a File is selected
		switch ( fileChooser.showOpenDialog ( getParent () ) )
		{
			case JFileChooser.APPROVE_OPTION:
			{
				filePath = fileChooser.getSelectedFile ().getAbsoluteFile ().toPath ();
				fileTextArea.setText ( filePath.toString () );
			}
		}
	}


	// TODO -> Attempts to open file to be used as XOR-Key
	private void xorButtonActionPerformed ( ActionEvent e )
	{
		// TODO -> Load XOR Key file that will be used by the Server
		JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getDefaultDirectory () );
		fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );

		if ( fileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
		{
			try
			{
				// TODO -> Save Path
				Path xorPath = fileChooser.getSelectedFile ().toPath ();

				// TODO -> Convert Bytes of file to a binary representation
				xorKey = Utility.transformFileAtPathToBinary ( xorPath );
			}
			catch ( IOException ioe )
			{
				System.out.println ( Arrays.toString ( ioe.getStackTrace () ) );

				// TODO -> Clear all bits that failed to load
				xorKeyList.clear ();

				// TODO -> When credentials file could not be found, alert Server
				message = "XOR key file could not be read";
				JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
			}
		}
	}


	// TODO -> Tries to send file to Server
	private void sendFileButtonActionPerformed ( ActionEvent e )
	{
		// TODO -> Send Files here
		// TODO -> First, check to see that file path is a valid file
		if ( filePath != null && Files.exists ( filePath ) && Files.isReadable ( filePath ) && printWriter != null )
		{
			try ( FileInputStream fileInputStream = new FileInputStream ( filePath.toFile () ) )
			{
				// TODO -> Notify Server File will be sent
				printWriter.println ( "FILE" );

				// TODO -> Get filename and options
				String filename = filePath.getFileName ().toString ();
				String fileOptions = ( armoringCheckBox.isSelected () ? "A" : "-" ) + ( copyRadioButton.isSelected () ? "C" : "O" );

				// TODO -> Send filename and options to Server
				printWriter.println ( filename );
				printWriter.println ( fileOptions );

				// TODO -> Send Chunk Size to Server
				printWriter.println ( chunkSize );

				for ( byte [] fileBytes = new byte [chunkSize]; fileInputStream.read ( fileBytes ) > 0; )
				{
					String byteString = Utility.byteArrayToString ( fileBytes );

					// TODO -> Write to console
					System.out.println ( "Client > " + byteString );

					// TODO -> Write Bytes to Stream
					printWriter.println ( byteString );
				}

				printWriter.println ( "FILE-DONE" );

				message = "\"" + filename + "\" was successfully transferred";
				JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.INFORMATION_MESSAGE );
			}
			catch ( IOException ioe )
			{
				System.out.println ( "ERROR" );
				String stackTrace = Arrays.toString ( ioe.getStackTrace () );
				System.out.println ( "Client @ " + new Date () + " > " + stackTrace );
			}
		}
		else
		{
			message = "File could not be found";
			JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
		}
	}


	// TODO -> As the JSlider is moved, update the Label and save the new ChunkSize
	private void chunkSizeSliderStateChanged ( ChangeEvent e )
	{
		// TODO -> When the Slider changes value, update our label
		chunkSize = chunkSizeSlider.getValue ();
		String selectedChuckText = chunkSize == chunkSizeSlider.getMaximum () ? "1 MB" : chunkSize + " KB";
		chunkSizeValueLabel.setText ( selectedChuckText );
	}


	// TODO -> Ultimately, tries to connect to Server after performing many checks
	private void connectButtonActionPerformed ( ActionEvent e )
	{
		new Thread ( () -> {
			try
			{
				// TODO -> Get name of server
				String host = serverTextArea.getText ();

				// TODO -> Parse Port. If not a valid Integer, Exception will be thrown
				String portString = portTextField.getText ();
				Integer port = Integer.parseInt ( portString );

				// TODO -> Try to open Socket
				serverSocket = new Socket ( host, port );

				// TODO -> Create I/O which will be used for communication between the Client and Server
				printWriter = new PrintWriter ( serverSocket.getOutputStream (), true );
				bufferedReader = new BufferedReader ( new InputStreamReader ( serverSocket.getInputStream () ) );

				// TODO -> Prepare Username and Password that will be sent to Server
				String username = usernameTextField.getText ();
				String password = new String ( passwordField.getPassword () );

				String credentials = username + " : " + password;

				// TODO -> Send Credentials to Server
				printWriter.println ( credentials );

				// TODO -> Read Authentication response
				Boolean didPass = Boolean.valueOf ( bufferedReader.readLine () );

				if ( didPass )
				{
					// TODO -> Update GUI
					dynamicStatusLabel.setText ( "Connected" );
					connectButton.setEnabled ( false );

					// TODO -> Create loop that will listen for the "QUIT" message from Server, if it ever occurs
					for ( looping = true; looping; )
					{
						// TODO -> This is the Server telling us they are closing the connection
						if ( bufferedReader.ready () && bufferedReader.readLine ().equals ( "QUIT" ) )
						{
							looping = false;

							// TODO -> Inform Client
							message = "Server has terminated the connection";
							JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
						}
					}
				}
				else
				{
					message = "Authentication has failed";
					JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
				}
			}
			catch ( IOException ioe )
			{
				// TODO -> Usually occurs when Port cannot be bound. E.g : Port 22 (SSH)
				if ( ioe instanceof BindException )
				{
					message = String.format ( "%s\n%s", "Bind Exception", "Port \"" + portTextField.getText () + "\" is not usable" );
					JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
				}
				else if ( ioe instanceof NoRouteToHostException )
				{
					message = String.format ( "%s\n%s", "No Route to Host Exception", "Host \"" + serverTextArea.getText () + "\" is unreachable" );
					JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
				}
				else if ( ioe instanceof ConnectException )
				{
					message = String.format ( "%s\n%s", "Connect Exception", "Connection was refused" );
					JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
				}
				else if ( ioe instanceof UnknownHostException )
				{
					message = String.format ( "%s\n%s", "Unknown Host Exception", "Invalid hostname for \"Server\"" );
					JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
				}
				else if ( ioe instanceof SocketException )
				{
					message = "Network is unreachable";
					JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
				}

				System.out.println ( Arrays.toString ( ioe.getStackTrace () ) );
			}
			catch ( NumberFormatException nfe )
			{
				// TODO -> Triggered when a Port is not a valid Integer. E.g : Port "Hello, World!"
				message = String.format ( "%s\n%s", "Number Format Exception", "\"Port\" must be a number." );
				JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );

				System.out.println ( Arrays.toString ( nfe.getStackTrace () ) );
			}
			finally
			{
				disconnectButton.doClick ();
			}
		} ).start ();
	}


	// TODO -> Closes all variables needed by the Server
	private void closeConnection () throws IOException
	{
		// TODO -> Destroy any resources used by the Server. Order of destruction is very important
		// TODO -> https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
		if ( bufferedReader != null )
		{
			bufferedReader.close ();
			bufferedReader = null;
		}

		if ( printWriter != null )
		{
			printWriter.close ();
			printWriter = null;
		}

		if ( serverSocket != null )
		{
			serverSocket.close ();
			serverSocket = null;
		}
	}


	// TODO -> Attempts to close any connections and tries to restore GUI for future connections
	private void disconnectButtonActionPerformed ( ActionEvent e )
	{
		if ( looping )
		{
			// TODO -> Alert Server that Client chose to close connection by sending "QUIT"
			// TODO -> Setting "looping" to false stops Thread's Loop, and eventually ends the Thread.
			printWriter.println ( "QUIT" );
			looping = false;
		}

		try
		{
			closeConnection ();
		}
		catch ( IOException ioe )
		{
			String stackTrace = Arrays.toString ( ioe.getStackTrace () );
			System.out.println ( "Client @ " + new Date () + " > " + stackTrace );
		}

		dynamicStatusLabel.setText ( "Stopped" );
		connectButton.setEnabled ( true );
	}


    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
		JLabel statusLabel = new JLabel();
		JLabel usernameLabel = new JLabel();
		usernameTextField = new JTextField();
		JLabel passwordLabel = new JLabel();
		passwordField = new JPasswordField();
		JLabel portLabel = new JLabel();
		portTextField = new JTextField();
		JLabel serverLabel = new JLabel();
		fileButton = new JButton();
		JLabel fileOptionsLabel = new JLabel();
		armoringCheckBox = new JCheckBox();
		overwriteRadioButton = new JRadioButton();
		chunkSizeLabel = new JLabel();
		chunkSizeSlider = new JSlider();
		chunkSizeValueLabel = new JLabel();
		sendFileButton = new JButton();
		disconnectButton = new JButton();
		connectButton = new JButton();
		dynamicStatusLabel = new JLabel();
		fileScrollPane = new JScrollPane();
		fileTextArea = new JTextArea();
		serverScrollPane = new JScrollPane();
		serverTextArea = new JTextArea();
		copyRadioButton = new JRadioButton();
		JButton xorButton = new JButton();
		xorScrollPane = new JScrollPane();
		xorTextArea = new JTextArea();
		JLabel authenticationOptionsLabel = new JLabel();
		plainRadioButton = new JRadioButton();

		//======== this ========
		setPreferredSize(new Dimension(400, 1100));
		setOpaque(false);

		//---- statusLabel ----
		statusLabel.setText("Status");
		statusLabel.setMinimumSize(new Dimension(92, 16));
		statusLabel.setMaximumSize(new Dimension(92, 16));
		statusLabel.setFont(statusLabel.getFont().deriveFont(statusLabel.getFont().getStyle() | Font.BOLD, statusLabel.getFont().getSize() + 3f));
		statusLabel.setToolTipText("Status of Client");

		//---- usernameLabel ----
		usernameLabel.setText("Username");
		usernameLabel.setFont(usernameLabel.getFont().deriveFont(usernameLabel.getFont().getStyle() | Font.BOLD, usernameLabel.getFont().getSize() + 3f));
		usernameLabel.setToolTipText("Username that will be used to connect to Server");

		//---- usernameTextField ----
		usernameTextField.setBackground(Color.white);
		usernameTextField.setText("Debug");
		usernameTextField.setFont(usernameTextField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		usernameTextField.setForeground(new Color(153, 0, 0));
		usernameTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		usernameTextField.setPreferredSize(new Dimension(45, 32));

		//---- passwordLabel ----
		passwordLabel.setText("Password");
		passwordLabel.setFont(passwordLabel.getFont().deriveFont(passwordLabel.getFont().getStyle() | Font.BOLD, passwordLabel.getFont().getSize() + 3f));
		passwordLabel.setToolTipText("Password that will be used to connect to Server");

		//---- passwordField ----
		passwordField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		passwordField.setBackground(Color.white);
		passwordField.setFont(passwordField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		passwordField.setForeground(new Color(153, 0, 0));
		passwordField.setPreferredSize(new Dimension(0, 32));

		//---- portLabel ----
		portLabel.setText("Server Port");
		portLabel.setFont(portLabel.getFont().deriveFont(portLabel.getFont().getStyle() | Font.BOLD, portLabel.getFont().getSize() + 3f));
		portLabel.setToolTipText("Port that will be used to connect to Server");

		//---- portTextField ----
		portTextField.setText("1492");
		portTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		portTextField.setFont(portTextField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		portTextField.setForeground(new Color(153, 0, 0));
		portTextField.setPreferredSize(new Dimension(36, 32));

		//---- serverLabel ----
		serverLabel.setText("Server Address");
		serverLabel.setFont(serverLabel.getFont().deriveFont(serverLabel.getFont().getStyle() | Font.BOLD, serverLabel.getFont().getSize() + 3f));
		serverLabel.setToolTipText("Address that will be used to connect to Server");

		//---- fileButton ----
		fileButton.setText("File To Transfer");
		fileButton.setMinimumSize(new Dimension(92, 29));
		fileButton.setMaximumSize(new Dimension(92, 29));
		fileButton.setFont(fileButton.getFont().deriveFont(fileButton.getFont().getStyle() | Font.BOLD, fileButton.getFont().getSize() + 3f));
		fileButton.setHorizontalAlignment(SwingConstants.LEADING);
		fileButton.setToolTipText("File that will be sent to the Server");
		fileButton.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
		fileButton.addActionListener(e -> fileButtonActionPerformed(e));

		//---- fileOptionsLabel ----
		fileOptionsLabel.setText("File Option");
		fileOptionsLabel.setFont(fileOptionsLabel.getFont().deriveFont(fileOptionsLabel.getFont().getStyle() | Font.BOLD, fileOptionsLabel.getFont().getSize() + 3f));
		fileOptionsLabel.setMinimumSize(new Dimension(95, 17));
		fileOptionsLabel.setMaximumSize(new Dimension(95, 17));
		fileOptionsLabel.setToolTipText("Options that describe how to send the file to the Server");

		//---- armoringCheckBox ----
		armoringCheckBox.setText("ASCII Armoring");
		armoringCheckBox.setSelected(true);
		armoringCheckBox.setFont(armoringCheckBox.getFont().deriveFont(armoringCheckBox.getFont().getStyle() | Font.BOLD));

		//---- overwriteRadioButton ----
		overwriteRadioButton.setText("Overwrite");
		overwriteRadioButton.setFont(overwriteRadioButton.getFont().deriveFont(overwriteRadioButton.getFont().getStyle() | Font.BOLD));

		//---- chunkSizeLabel ----
		chunkSizeLabel.setText("Chunk Size");
		chunkSizeLabel.setFont(chunkSizeLabel.getFont().deriveFont(chunkSizeLabel.getFont().getStyle() | Font.BOLD, chunkSizeLabel.getFont().getSize() + 3f));
		chunkSizeLabel.setToolTipText("Chunk size that will be used to partition the file before it is sent");

		//---- chunkSizeSlider ----
		chunkSizeSlider.setMaximum(1024);
		chunkSizeSlider.setMinimum(1);
		chunkSizeSlider.setMinorTickSpacing(64);
		chunkSizeSlider.setPaintTicks(true);
		chunkSizeSlider.setMajorTickSpacing(128);
		chunkSizeSlider.setValue(1024);
		chunkSizeSlider.addChangeListener(e -> chunkSizeSliderStateChanged(e));

		//---- chunkSizeValueLabel ----
		chunkSizeValueLabel.setText("1 MB");
		chunkSizeValueLabel.setFont(chunkSizeValueLabel.getFont().deriveFont(chunkSizeValueLabel.getFont().getStyle() | Font.BOLD, chunkSizeValueLabel.getFont().getSize() + 1f));
		chunkSizeValueLabel.setHorizontalAlignment(SwingConstants.TRAILING);

		//---- sendFileButton ----
		sendFileButton.setText("Send");
		sendFileButton.setFont(sendFileButton.getFont().deriveFont(Font.BOLD, sendFileButton.getFont().getSize() + 3f));
		sendFileButton.setToolTipText("Tries to send file to the Server");
		sendFileButton.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
		sendFileButton.addActionListener(e -> sendFileButtonActionPerformed(e));

		//---- disconnectButton ----
		disconnectButton.setText("Disconnect");
		disconnectButton.setFont(disconnectButton.getFont().deriveFont(disconnectButton.getFont().getStyle() | Font.BOLD, disconnectButton.getFont().getSize() + 3f));
		disconnectButton.setToolTipText("Disconnects from the server");
		disconnectButton.addActionListener(e -> disconnectButtonActionPerformed(e));

		//---- connectButton ----
		connectButton.setText("Connect");
		connectButton.setFont(connectButton.getFont().deriveFont(connectButton.getFont().getStyle() | Font.BOLD, connectButton.getFont().getSize() + 3f));
		connectButton.setToolTipText("Tries to connect to the Server");
		connectButton.addActionListener(e -> connectButtonActionPerformed(e));

		//---- dynamicStatusLabel ----
		dynamicStatusLabel.setFont(dynamicStatusLabel.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		dynamicStatusLabel.setForeground(new Color(153, 0, 0));
		dynamicStatusLabel.setText("Stopped");

		//======== fileScrollPane ========
		{
			fileScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			fileScrollPane.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
			fileScrollPane.setPreferredSize(new Dimension(27, 32));

			//---- fileTextArea ----
			fileTextArea.setFont(fileTextArea.getFont().deriveFont(Font.BOLD|Font.ITALIC));
			fileTextArea.setForeground(new Color(153, 0, 0));
			fileTextArea.setRows(1);
			fileTextArea.setEditable(false);
			fileTextArea.setTabSize(0);
			fileTextArea.setBorder(null);
			fileScrollPane.setViewportView(fileTextArea);
		}

		//======== serverScrollPane ========
		{
			serverScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			serverScrollPane.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
			serverScrollPane.setPreferredSize(new Dimension(60, 32));

			//---- serverTextArea ----
			serverTextArea.setFont(serverTextArea.getFont().deriveFont(Font.BOLD|Font.ITALIC));
			serverTextArea.setForeground(new Color(153, 0, 0));
			serverTextArea.setText("localhost");
			serverTextArea.setRows(1);
			serverTextArea.setTabSize(0);
			serverTextArea.setBorder(null);
			serverScrollPane.setViewportView(serverTextArea);
		}

		//---- copyRadioButton ----
		copyRadioButton.setText("Copy");
		copyRadioButton.setFont(copyRadioButton.getFont().deriveFont(copyRadioButton.getFont().getStyle() | Font.BOLD));
		copyRadioButton.setSelected(true);

		//---- xorButton ----
		xorButton.setText("XOR Key");
		xorButton.setFont(xorButton.getFont().deriveFont(xorButton.getFont().getStyle() | Font.BOLD, xorButton.getFont().getSize() + 3f));
		xorButton.setActionCommand("XOR-Key File");
		xorButton.setBorder(new MatteBorder(0, 0, 1, 0, Color.black));
		xorButton.addActionListener(e -> xorButtonActionPerformed(e));

		//======== xorScrollPane ========
		{
			xorScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			xorScrollPane.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
			xorScrollPane.setPreferredSize(new Dimension(29, 32));

			//---- xorTextArea ----
			xorTextArea.setFont(xorTextArea.getFont().deriveFont(Font.BOLD|Font.ITALIC));
			xorTextArea.setForeground(new Color(153, 0, 0));
			xorTextArea.setRows(1);
			xorTextArea.setEditable(false);
			xorTextArea.setTabSize(0);
			xorTextArea.setBorder(null);
			xorScrollPane.setViewportView(xorTextArea);
		}

		//---- authenticationOptionsLabel ----
		authenticationOptionsLabel.setText("Authentication");
		authenticationOptionsLabel.setFont(authenticationOptionsLabel.getFont().deriveFont(authenticationOptionsLabel.getFont().getStyle() | Font.BOLD, authenticationOptionsLabel.getFont().getSize() + 3f));

		//---- plainRadioButton ----
		plainRadioButton.setText("Plain Text");
		plainRadioButton.setFont(plainRadioButton.getFont().deriveFont(plainRadioButton.getFont().getStyle() | Font.BOLD));
		plainRadioButton.setSelected(true);

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addGroup(layout.createParallelGroup()
						.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup()
								.addComponent(authenticationOptionsLabel)
								.addComponent(passwordLabel)
								.addComponent(usernameLabel)
								.addComponent(portLabel)
								.addComponent(plainRadioButton)
								.addComponent(xorButton)
								.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
									.addGroup(layout.createSequentialGroup()
										.addComponent(fileButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(sendFileButton))
									.addComponent(xorScrollPane, GroupLayout.PREFERRED_SIZE, 325, GroupLayout.PREFERRED_SIZE))
								.addComponent(fileScrollPane, GroupLayout.PREFERRED_SIZE, 325, GroupLayout.PREFERRED_SIZE)
								.addComponent(fileOptionsLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(armoringCheckBox)
								.addComponent(copyRadioButton)
								.addComponent(overwriteRadioButton)
								.addComponent(chunkSizeSlider, GroupLayout.PREFERRED_SIZE, 325, GroupLayout.PREFERRED_SIZE)
								.addComponent(chunkSizeLabel)
								.addComponent(chunkSizeValueLabel, GroupLayout.PREFERRED_SIZE, 325, GroupLayout.PREFERRED_SIZE))
							.addGap(0, 50, Short.MAX_VALUE))
						.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
								.addComponent(passwordField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(disconnectButton, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(serverScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(portTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(usernameTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(connectButton, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(dynamicStatusLabel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
									.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
										.addComponent(serverLabel, GroupLayout.Alignment.LEADING)
										.addComponent(statusLabel, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
									.addGap(0, 202, Short.MAX_VALUE)))
							.addGap(50, 50, 50))))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addComponent(statusLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(dynamicStatusLabel)
					.addGap(18, 18, 18)
					.addComponent(serverLabel)
					.addGap(18, 18, 18)
					.addComponent(serverScrollPane, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(portLabel)
					.addGap(18, 18, 18)
					.addComponent(portTextField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(usernameLabel)
					.addGap(18, 18, 18)
					.addComponent(usernameTextField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(passwordLabel)
					.addGap(18, 18, 18)
					.addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(authenticationOptionsLabel)
					.addGap(18, 18, 18)
					.addComponent(plainRadioButton)
					.addGap(18, 18, 18)
					.addComponent(xorButton)
					.addGap(18, 18, 18)
					.addComponent(xorScrollPane, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(fileButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(sendFileButton))
					.addGap(18, 18, 18)
					.addComponent(fileScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(fileOptionsLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(armoringCheckBox)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(copyRadioButton)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(overwriteRadioButton)
					.addGap(18, 18, 18)
					.addComponent(chunkSizeLabel)
					.addGap(18, 18, 18)
					.addComponent(chunkSizeSlider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(chunkSizeValueLabel)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 169, Short.MAX_VALUE)
					.addComponent(connectButton)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(disconnectButton)
					.addGap(25, 25, 25))
		);

		//---- fileOptionButtonGroup ----
		ButtonGroup fileOptionButtonGroup = new ButtonGroup();
		fileOptionButtonGroup.add(overwriteRadioButton);
		fileOptionButtonGroup.add(copyRadioButton);

		//---- authenticationGroup ----
		ButtonGroup authenticationGroup = new ButtonGroup();
		authenticationGroup.add(plainRadioButton);

		//---- bindings ----
		bindingGroup = new BindingGroup();
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			connectButton, BeanProperty.create("enabled"),
			serverTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			connectButton, BeanProperty.create("enabled"),
			portTextField, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			connectButton, BeanProperty.create("enabled"),
			usernameTextField, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			connectButton, BeanProperty.create("enabled"),
			passwordField, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			connectButton, BeanProperty.create("enabled"),
			plainRadioButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			connectButton, BeanProperty.create("enabled"),
			xorTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			disconnectButton, BeanProperty.create("enabled"),
			fileButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			disconnectButton, BeanProperty.create("enabled"),
			sendFileButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			disconnectButton, BeanProperty.create("enabled"),
			fileTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			disconnectButton, BeanProperty.create("enabled"),
			armoringCheckBox, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			disconnectButton, BeanProperty.create("enabled"),
			copyRadioButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			disconnectButton, BeanProperty.create("enabled"),
			overwriteRadioButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			disconnectButton, BeanProperty.create("enabled"),
			chunkSizeSlider, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			connectButton, ELProperty.create("${!enabled}"),
			disconnectButton, BeanProperty.create("enabled")));
		bindingGroup.bind();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Erik Huerta
	private JTextField usernameTextField;
	private JPasswordField passwordField;
	private JTextField portTextField;
	private JButton fileButton;
	private JCheckBox armoringCheckBox;
	private JRadioButton overwriteRadioButton;
	private JLabel chunkSizeLabel;
	private JSlider chunkSizeSlider;
	private JLabel chunkSizeValueLabel;
	private JButton sendFileButton;
	private JButton disconnectButton;
	private JButton connectButton;
	private JLabel dynamicStatusLabel;
	private JScrollPane fileScrollPane;
	private JTextArea fileTextArea;
	private JScrollPane serverScrollPane;
	private JTextArea serverTextArea;
	private JRadioButton copyRadioButton;
	private JScrollPane xorScrollPane;
	private JTextArea xorTextArea;
	private JRadioButton plainRadioButton;
	private BindingGroup bindingGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

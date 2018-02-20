import javax.swing.border.*;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.ELProperty;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/*
 * Created by JFormDesigner on Thu Jan 25 01:00:01 PST 2018
*/


public class ServerPanel extends JPanel
{
	// TODO -> Debug Variables : Used for Debugging purposes only
	private ArrayList<String> debugCredentials = new ArrayList<> ( Collections.singletonList ( "Debug : " ) );

	// TODO -> Used for JOptionPane messages
	private String message;

	// TODO -> "Save To" Variables
	private Path saveToPath = Paths.get ( FileSystemView.getFileSystemView ().getDefaultDirectory ().getPath () );
	private JFileChooser saveToFileChooser = new JFileChooser ( saveToPath.toFile () );

	// TODO -> Variables that will store Credentials and XOR-Key
	private ArrayList<String> credentialsList = new ArrayList<> ();
	private byte [] xorKey = null;

	// TODO -> Required Server Variables
	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private PrintWriter printWriter = null;
	private BufferedReader bufferedReader = null;

	// TODO -> Useful for controlling Thread initiated by the "Start Server" Button
	private volatile boolean looping = false;
	private volatile boolean shouldRestartServer = false;


	public ServerPanel ()
	{
		initComponents ();

		// TODO -> Set "Save To" directory of Server as System Default
		saveTextArea.setText ( saveToPath.toString () );

		// TODO -> Customize settings for "Save To" JFileChooser
		saveToFileChooser.setFileSelectionMode ( JFileChooser.DIRECTORIES_ONLY );
		saveToFileChooser.setAcceptAllFileFilterUsed ( false );
		saveToFileChooser.setFileFilter ( new FileFilter ()
		{
			@Override
			public boolean accept ( File file )
			{
				return file.isDirectory ();
			}

			@Override
			public String getDescription ()
			{
				return "Directory";
			}
		} );
	}


	// TODO -> Attempts to open Credentials file and populate Credentials ArrayList
	private void credentialsButtonActionPerformed ( ActionEvent e )
	{
		// TODO -> Find credentials file that will be used by the Server ( Should be Strings )
		JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getDefaultDirectory () );
		fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );

		if ( fileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
		{
			// TODO -> Get path to credentials
			Path credentialsPath = fileChooser.getSelectedFile ().toPath ();

			// TODO -> Try to open the file for reading and store every credential in said file
			try (BufferedReader bufferedReader = Files.newBufferedReader ( credentialsPath ) )
			{
				for ( String line; ( line = bufferedReader.readLine () ) != null; )
					if ( ! line.trim ().isEmpty () )
						credentialsList.add ( line );

				// TODO -> Update GUI
				credentialTextArea.setText ( credentialsPath.toString () );
			}
			// TODO -> Usually an encoding error. File is expected to contain Strings, and nothing else
			catch ( IOException ioe )
			{
				System.out.println ( Arrays.toString ( ioe.getStackTrace () ) );

				// TODO -> Clear all credentials that failed to load
				credentialsList.clear ();

				// TODO -> Update GUI
				credentialTextArea.setText ( null );

				message = "Credentials file could not be read";
				JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
			}
		}
	}


	private void credentialClearButtonActionPerformed(ActionEvent e)
	{
		// TODO -> Clear Credential Text Area
		credentialTextArea.setText ( null );
	}


	// TODO -> Attempts to open file to be used as XOR-Key
	private void xorButtonActionPerformed ( ActionEvent e )
	{
		// TODO -> Load XOR Key file that will be used by the Server
		JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getDefaultDirectory () );
		fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );

		if ( fileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
		{
			// TODO -> Save Path
			Path xorPath = fileChooser.getSelectedFile ().toPath ();

			try ( FileInputStream fileInputStream = new FileInputStream ( xorPath.toFile () ) )
			{
				long fileSize = fileInputStream.getChannel ().size ();

				if ( fileSize > Integer.MAX_VALUE )
					throw new IOException ();

				xorKey = new byte [ ( int ) fileSize ];

				// TODO -> Convert Bytes of file to a binary representation
				for ( Integer inputByte, count = 0; ( inputByte = fileInputStream.read () ) != -1; ++count )
					xorKey [count] = inputByte.byteValue ();

				xorTextArea.setText ( xorPath.toString () );

				System.out.println ( "Server > XOR Key Bytes : " + Arrays.toString ( xorKey ) );
			}

			catch ( IOException ioe )
			{
				System.out.println ( Arrays.toString ( ioe.getStackTrace () ) );

				xorKey = null;

				// TODO -> When XOR file could not be found, alert Server
				message = "XOR file could not be read";
				JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
			}
		}
	}


	private void xorClearButtonActionPerformed(ActionEvent e)
	{
		// TODO -> Clear XOR Text Area
		xorTextArea.setText ( null );
	}


	// TODO -> Attempts to find new "Save To" path
	private void saveToButtonActionPerformed ( ActionEvent e )
	{
		if ( saveToFileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
		{
			// TODO -> Update the "Save To" path and GUI
			saveToPath = Paths.get ( saveToFileChooser.getSelectedFile ().getPath () );
			saveTextArea.setText ( saveToPath.toString () );
		}
	}


	private void processFileRequest ()
	{
		try
		{
			StringBuilder stringBuilder = new StringBuilder ( );

			// TODO -> Receive filename
			String filename = bufferedReader.readLine ();

			// TODO -> Receive File-Options and Chunk-Size from Server
			String [] fileInfo = bufferedReader.readLine ().split ( " && " );

			// TODO -> Receive options
			String options = fileInfo[0];

			boolean asciiArmoring = options.startsWith ( "A" );
			String fileOption = options.substring ( options.length () - 1 );

			// TODO -> Receive Chunk Size
			int chunkSize = Integer.parseInt ( fileInfo[1] );

			stringBuilder.append ( "Server > Received \"" + filename + "\" with options \"" + options + "\" and chunk-size of \"" + chunkSize + "\"" );

			ArrayList <Byte> transferredBytes = new ArrayList<> ();
			String clientInput;
			boolean shouldRetry = false;

			while ( ( clientInput = bufferedReader.readLine () ) != null )
			{
				if ( clientInput.equals ( "BYTES-DONE" ) )
				{
					stringBuilder.append ( "\n" + "Server > BYTES-DONE" );

					break;
				}

				if ( shouldRetry )
					continue;

				// TODO -> Split Hash and Data into Separate Strings ( [ 0 ] is Hash and [ 1 ] is Data )
				String [] hashAndData = clientInput.split ( " && " );

				// TODO -> Get Client's Hash Value and Convert it to Bytes
				String clientHashString = hashAndData[0];
				byte [] clientHashBytes = Utility.stringToBytes ( clientHashString );

				// TODO -> Get Client's String of Data Bytes
				String clientDataString = hashAndData[1];

				byte [] tempDataBytes;

				// TODO -> Decode Data Bytes using ASCII-Armoring, if ASCII-Armoring was requested
				if ( asciiArmoring )
				{
					stringBuilder.append ( "\n" + "Server > BASE64 > " + clientDataString );

					tempDataBytes = MIME.base64Decoding ( clientDataString );
				}
				// TODO -> If ASCII-Armoring is not request, process Data Bytes normally
				else
				{
					tempDataBytes = Utility.stringToBytes ( clientDataString );
				}

				// TODO -> Decrypt using Hash Bytes and Data Bytes using XOR Cipher, is available
				if ( xorKey != null )
				{
					stringBuilder.append ( "\n" + "Server > XOR Data Bytes > " + clientDataString );
					stringBuilder.append ( "\n" + "Server > XOR Hash Bytes > " + clientHashString );

					tempDataBytes = XORCipher.decrypt ( tempDataBytes, xorKey );
					clientHashBytes = XORCipher.decrypt ( clientHashBytes, xorKey );

					clientDataString = Arrays.toString ( tempDataBytes );
					clientHashString = Arrays.toString ( clientHashBytes );
				}

				// TODO -> Output the Plain ( Decrypted ) Hash Bytes and Data Bytes
				stringBuilder.append ( "\n" + "Server > Plain Data Bytes > " + clientDataString );
				stringBuilder.append ( "\n" + "Server > Plain Hash Bytes > " + clientHashString );

				// TODO -> Compute Server-Side Hash Value ( Using Data Bytes ) and Compare With The Client-Hash-Value
				// TODO -> If matching, proceed, but if not, then inform Client to retry
				Long clientHashValue = Utility.bytesToLong ( clientHashBytes );
				Long serverHashValue = Utility.hash ( tempDataBytes );

				stringBuilder.append ( "\n" + "Server > Client's Plain Hash Value > " + clientHashValue );
				stringBuilder.append ( "\n" + "Server > Plain Hash Value > " + serverHashValue );

				if ( serverHashValue.longValue () != clientHashValue.longValue () )
				{
					stringBuilder.append ( "\n" + "Server > Hash Comparison Result > RETRYING" + "\n" );

					logTextArea.append ( "\"" + filename + "\" failed to transfer" + "\n" );
					stringBuilder.append ( "Server > \"" + filename + "\" failed to transfer" + "\n" );

					System.out.println ( stringBuilder.toString () );

					shouldRetry = true;
					continue;
				}

				// TODO -> Store Bytes with all other transferred Bytes
				for ( byte byteValue : tempDataBytes )
					transferredBytes.add ( byteValue );
			}

			if ( shouldRetry )
			{
				printWriter.println ( "FILE-RETRY" );
				return;
			}

			System.out.println ( stringBuilder.toString () );
			System.out.println ( "Server > Finished Receiving Bytes" + "\n" );

			Path filePath = Paths.get ( saveToPath.toString (), filename );

			byte [] fileBytes = new byte [transferredBytes.size ()];
			for ( int count = 0; count < transferredBytes.size (); ++count )
				fileBytes[count] = transferredBytes.get ( count );

			switch ( fileOption )
			{
				case "C":
				{
					if ( Files.exists ( filePath ) )
					{
						int lastPeriod = filename.lastIndexOf ( "." );
						filename = filename.substring ( 0, lastPeriod ) + "@" + ( new Date () ) + "." + filename.substring ( lastPeriod + 1 );
						filePath = Paths.get ( saveToPath.toString (), filename );
					}

					Files.write ( filePath, fileBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE );

					logTextArea.append ( "\"" + filename + "\" was copied" + "\n" );

					break;
				}
				case "O":
				{
					Files.write ( filePath, fileBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING );

					logTextArea.append ( "\"" + filename + "\" was overwritten" + "\n" );

					break;
				}
			}

			printWriter.println ( "FILE-SUCCESS" );
		}
		catch ( IOException ioe )
		{
			String stackTrace = Arrays.toString ( ioe.getStackTrace () );
			System.out.println ( "Server @ " + new Date () + " > " + stackTrace );
		}
	}


	// TODO -> As the name of the function implies, it performs many checks and if successful, starts the Server
	private void startButtonActionPerformed ( ActionEvent e )
	{
		// TODO -> The Thread that will manage the conversation between the Server and Client
		// TODO -> Necessary because if not done as a separate thread, then it will block the main GUI Thread (Very Bad) (Will cause GUI to freeze)
		new Thread ( () -> {
			try
			{
				// TODO -> Parse Port. If not a valid Integer, Exception will be thrown
				String portString = portTextField.getText ();
				Integer port = Integer.parseInt ( portString );

				// TODO -> Update GUI
				dynamicStatusLabel.setText ( "Listening" );
				startButton.setEnabled ( false );

				// TODO -> Update Log to show Server is now Listening
				logTextArea.append ( "Server is now listening" + "\n" );

				// TODO -> Initialize Socket/Stream Variables
				serverSocket = new ServerSocket ( port );
				clientSocket = serverSocket.accept ();

				printWriter = new PrintWriter ( clientSocket.getOutputStream (), true );
				bufferedReader = new BufferedReader ( new InputStreamReader ( clientSocket.getInputStream () ) );

				// TODO -> Receive Credentials from Client
				String credentials = bufferedReader.readLine ();

				// TODO -> Check to see if they match any of the stores Credentials
				Boolean didPass = credentialsList.contains ( credentials ) || debugCredentials.contains ( credentials );

				// TODO -> Return result fo Authentication
				printWriter.println ( didPass );

				if ( didPass )
				{
					// TODO -> Update GUI with new Status and log the Client's connection
					dynamicStatusLabel.setText ( "Running" );

					String clientHostAddress = clientSocket.getInetAddress ().getHostAddress ();
					logTextArea.append ( clientHostAddress + " has connected" + "\n" );

					// TODO -> Start accepting data
					for ( looping = true; looping; )
					{
						if ( bufferedReader.ready () )
						{
							String clientInput = bufferedReader.readLine ();

							System.out.println ( "Server > Buffer is ready and contains \"" + clientInput + "\"" );

							// TODO -> If the Client sends a QUIT message, close the connection and restart the Server for the next connection
							if ( clientInput.equals ( "QUIT" ) )
							{
								looping = false;
								shouldRestartServer = true;
								logTextArea.append ( clientHostAddress + " has decided to close the connection" + "\n" );
							}

							// TODO -> Here, the Client is now beginning to send file data
							else if ( clientInput.equals ( "FILE" ) )
							{
								processFileRequest ();
							}
						}
					}

					logTextArea.append ( clientHostAddress + " has disconnected." + "\n" );
				}

				// TODO -> Authentication has failed, so restart the Server and let another person try to connect
				else
				{
					shouldRestartServer = true;
					logTextArea.append ( "Client failed authentication" + "\n" );
				}
			}
			catch ( IOException ioe )
			{
				// TODO -> Usually occurs when Port cannot be bound. E.g : Port 22 (SSH)
				if ( ioe instanceof BindException )
				{
					message = "Port \"" + portTextField.getText () + "\" is not usable";
					JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
				}

				String stackTrace = Arrays.toString ( ioe.getStackTrace () );
				System.out.println ( "Server @ " + new Date () + " > " + stackTrace );
			}
			catch ( NumberFormatException nfe )
			{
				// TODO -> Triggered when a Port is not a valid Integer. E.g : Port "Hello, World!"
				message = "\"Port\" must be a number";
				JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );

				String stackTrace = Arrays.toString ( nfe.getStackTrace () );
				System.out.println ( "Server @ " + new Date () + " > " + stackTrace );
			}
			finally
			{
				// TODO -> This check occurs when a Client connects and disconnects
				// TODO -> Rather than having to restart the Server manually, this will do it automatically
				if ( shouldRestartServer )
					stopButtonActionPerformed ( null );
			}
		} ).start ();
	}


	// TODO -> Closes all variables needed by the Server
	private void closeConnection () throws IOException
	{
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

		if ( clientSocket != null )
		{
			clientSocket.close ();
			clientSocket = null;
		}

		if ( serverSocket != null )
		{
			serverSocket.close ();
			serverSocket = null;
		}
	}

	// TODO -> Attempts to close any connections and tries to restore GUI for future connections
	private void stopButtonActionPerformed ( ActionEvent actionEvent )
	{
		if ( looping )
		{
			// TODO -> Alert Client that Server chose to close connection by sending "QUIT"
			// TODO -> Setting "looping" to false stops Thread's Loop, and eventually ends the Thread
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
			System.out.println ( "Server @ " + new Date () + " > " + stackTrace );
		}

		// TODO -> Restore GUI
		dynamicStatusLabel.setText ( "Stopped" );
		startButton.setEnabled ( true );

		if ( actionEvent != null )
		{
			xorKey = null;

			credentialTextArea.setText ( null );
			xorTextArea.setText ( null );
			saveTextArea.setText ( saveToPath.toString () );
			portTextField.setText ( "1492" );
		}

		logTextArea.append ( "Server was terminated " + "\n\n" );

		// TODO -> Restart Server, if requested
		if ( shouldRestartServer )
		{
			shouldRestartServer = false;
			startButton.doClick ();
		}
	}


    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
		JLabel staticStatusLabel = new JLabel();
		JButton credentialButton = new JButton();
		JButton saveButton = new JButton();
		startButton = new JButton();
		stopButton = new JButton();
		JLabel portLabel = new JLabel();
		portTextField = new JTextField();
		dynamicStatusLabel = new JLabel();
		JLabel logLabel = new JLabel();
		JScrollPane logScrollPane = new JScrollPane();
		logTextArea = new JTextArea();
		JScrollPane saveScrollPane = new JScrollPane();
		saveTextArea = new JTextArea();
		JScrollPane credentialScrollPane = new JScrollPane();
		credentialTextArea = new JTextArea();
		JButton xorButton = new JButton();
		JScrollPane xorScrollPane = new JScrollPane();
		xorTextArea = new JTextArea();
		JButton credentialClearButton = new JButton();
		JButton xorClearButton = new JButton();

		//======== this ========
		setPreferredSize(new Dimension(400, 900));
		setOpaque(false);

		//---- staticStatusLabel ----
		staticStatusLabel.setText("Status");
		staticStatusLabel.setFont(staticStatusLabel.getFont().deriveFont(staticStatusLabel.getFont().getStyle() | Font.BOLD, staticStatusLabel.getFont().getSize() + 5f));

		//---- credentialButton ----
		credentialButton.setText("Credentials Path");
		credentialButton.setFont(credentialButton.getFont().deriveFont(credentialButton.getFont().getStyle() | Font.BOLD, credentialButton.getFont().getSize() + 5f));
		credentialButton.setHorizontalAlignment(SwingConstants.LEADING);
		credentialButton.setToolTipText("Click");
		credentialButton.addActionListener(e -> credentialsButtonActionPerformed(e));

		//---- saveButton ----
		saveButton.setText("Save-Files-To Path");
		saveButton.setFont(saveButton.getFont().deriveFont(saveButton.getFont().getStyle() | Font.BOLD, saveButton.getFont().getSize() + 5f));
		saveButton.setToolTipText("Click");
		saveButton.setHorizontalAlignment(SwingConstants.LEADING);
		saveButton.addActionListener(e -> saveToButtonActionPerformed(e));

		//---- startButton ----
		startButton.setText("Start");
		startButton.setFont(startButton.getFont().deriveFont(startButton.getFont().getStyle() | Font.BOLD, startButton.getFont().getSize() + 5f));
		startButton.addActionListener(e -> startButtonActionPerformed(e));

		//---- stopButton ----
		stopButton.setText("Stop");
		stopButton.setFont(stopButton.getFont().deriveFont(stopButton.getFont().getStyle() | Font.BOLD, stopButton.getFont().getSize() + 5f));
		stopButton.addActionListener(e -> stopButtonActionPerformed(e));

		//---- portLabel ----
		portLabel.setText("Port");
		portLabel.setFont(portLabel.getFont().deriveFont(portLabel.getFont().getStyle() | Font.BOLD, portLabel.getFont().getSize() + 5f));

		//---- portTextField ----
		portTextField.setText("1492");
		portTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		portTextField.setForeground(new Color(153, 0, 0));
		portTextField.setFont(portTextField.getFont().deriveFont(Font.BOLD|Font.ITALIC));

		//---- dynamicStatusLabel ----
		dynamicStatusLabel.setText("Stopped");
		dynamicStatusLabel.setFont(dynamicStatusLabel.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		dynamicStatusLabel.setForeground(new Color(153, 0, 0));

		//---- logLabel ----
		logLabel.setText("Log");
		logLabel.setFont(logLabel.getFont().deriveFont(logLabel.getFont().getStyle() | Font.BOLD, logLabel.getFont().getSize() + 5f));

		//======== logScrollPane ========
		{
			logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			logScrollPane.setBorder(null);

			//---- logTextArea ----
			logTextArea.setForeground(new Color(153, 0, 0));
			logTextArea.setRows(1);
			logTextArea.setEditable(false);
			logTextArea.setFont(logTextArea.getFont().deriveFont(Font.BOLD|Font.ITALIC));
			logTextArea.setBorder(null);
			logScrollPane.setViewportView(logTextArea);
		}

		//======== saveScrollPane ========
		{
			saveScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			saveScrollPane.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));

			//---- saveTextArea ----
			saveTextArea.setFont(saveTextArea.getFont().deriveFont(Font.BOLD|Font.ITALIC));
			saveTextArea.setForeground(new Color(153, 0, 0));
			saveTextArea.setEditable(false);
			saveTextArea.setBorder(null);
			saveTextArea.setTabSize(0);
			saveTextArea.setRows(1);
			saveScrollPane.setViewportView(saveTextArea);
		}

		//======== credentialScrollPane ========
		{
			credentialScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			credentialScrollPane.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));

			//---- credentialTextArea ----
			credentialTextArea.setFont(credentialTextArea.getFont().deriveFont(Font.BOLD|Font.ITALIC));
			credentialTextArea.setForeground(new Color(153, 0, 0));
			credentialTextArea.setRows(1);
			credentialTextArea.setEditable(false);
			credentialTextArea.setBorder(null);
			credentialTextArea.setTabSize(0);
			credentialScrollPane.setViewportView(credentialTextArea);
		}

		//---- xorButton ----
		xorButton.setText("XOR-Key Path");
		xorButton.setFont(xorButton.getFont().deriveFont(xorButton.getFont().getStyle() | Font.BOLD, xorButton.getFont().getSize() + 5f));
		xorButton.setToolTipText("Click");
		xorButton.setHorizontalAlignment(SwingConstants.LEADING);
		xorButton.addActionListener(e -> xorButtonActionPerformed(e));

		//======== xorScrollPane ========
		{
			xorScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			xorScrollPane.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));

			//---- xorTextArea ----
			xorTextArea.setRows(1);
			xorTextArea.setTabSize(0);
			xorTextArea.setBorder(null);
			xorTextArea.setForeground(new Color(153, 0, 0));
			xorTextArea.setFont(xorTextArea.getFont().deriveFont(Font.BOLD|Font.ITALIC));
			xorTextArea.setEditable(false);
			xorScrollPane.setViewportView(xorTextArea);
		}

		//---- credentialClearButton ----
		credentialClearButton.setText("Clear");
		credentialClearButton.setFont(credentialClearButton.getFont().deriveFont(credentialClearButton.getFont().getStyle() | Font.BOLD));
		credentialClearButton.setToolTipText("Click");
		credentialClearButton.addActionListener(e -> credentialClearButtonActionPerformed(e));

		//---- xorClearButton ----
		xorClearButton.setText("Clear");
		xorClearButton.setFont(xorClearButton.getFont().deriveFont(xorClearButton.getFont().getStyle() | Font.BOLD));
		xorClearButton.setToolTipText("Click");
		xorClearButton.addActionListener(e -> xorClearButtonActionPerformed(e));

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addGroup(layout.createParallelGroup()
						.addGroup(layout.createSequentialGroup()
							.addComponent(staticStatusLabel)
							.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
								.addGroup(layout.createSequentialGroup()
									.addComponent(xorButton, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(xorClearButton))
								.addComponent(portTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addComponent(logScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addComponent(saveScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addComponent(xorScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addComponent(credentialScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addComponent(dynamicStatusLabel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
									.addComponent(credentialButton, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 66, Short.MAX_VALUE)
									.addComponent(credentialClearButton))
								.addComponent(stopButton, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addComponent(startButton, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE))
							.addGap(50, 50, 50))
						.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup()
								.addComponent(saveButton)
								.addComponent(logLabel)
								.addComponent(portLabel))
							.addGap(0, 0, Short.MAX_VALUE))))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addComponent(staticStatusLabel)
					.addGap(18, 18, 18)
					.addComponent(dynamicStatusLabel)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(credentialClearButton)
						.addComponent(credentialButton))
					.addGap(18, 18, 18)
					.addComponent(credentialScrollPane, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(xorButton)
						.addComponent(xorClearButton))
					.addGap(18, 18, 18)
					.addComponent(xorScrollPane, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(saveButton)
					.addGap(18, 18, 18)
					.addComponent(saveScrollPane, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(portLabel)
					.addGap(18, 18, 18)
					.addComponent(portTextField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(logLabel)
					.addGap(18, 18, 18)
					.addComponent(logScrollPane, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)
					.addComponent(startButton)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(stopButton)
					.addGap(25, 25, 25))
		);

		//---- bindings ----
		bindingGroup = new BindingGroup();
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, ELProperty.create("${!enabled}"),
			stopButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, BeanProperty.create("enabled"),
			saveButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, BeanProperty.create("enabled"),
			portTextField, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, BeanProperty.create("enabled"),
			saveTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, BeanProperty.create("enabled"),
			credentialTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, BeanProperty.create("enabled"),
			xorButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, BeanProperty.create("enabled"),
			credentialButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, BeanProperty.create("enabled"),
			xorTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, BeanProperty.create("enabled"),
			credentialClearButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			startButton, BeanProperty.create("enabled"),
			xorClearButton, BeanProperty.create("enabled")));
		bindingGroup.bind();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Erik Huerta
	private JButton startButton;
	private JButton stopButton;
	private JTextField portTextField;
	private JLabel dynamicStatusLabel;
	private JTextArea logTextArea;
	private JTextArea saveTextArea;
	private JTextArea credentialTextArea;
	private JTextArea xorTextArea;
	private BindingGroup bindingGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

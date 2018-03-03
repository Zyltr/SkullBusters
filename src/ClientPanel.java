import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.ELProperty;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/*
 * Created by JFormDesigner on Thu Jan 25 02:18:08 PST 2018
 */


/**
 * A JPanel which is used by the Client to connect to a Server
 */
public class ClientPanel extends JPanel implements ThreadCompletionListener
{
    // filePath : variable that will be used to load a file when it is time to send to the Server
    private Path filePath = null;

    // xorKey : variable that will store the XOR-Key Byte representation used by the Client
    private byte[] xorKey = null;

    // chunkSize : variable that will be used to read file in a buffer of a specific size of Bytes
    private Integer chunkSize = 64000;

    // serverSocket
    // serverPrintWriter
    // serverBufferedReader : variables required for communicating with the Server
    private Socket serverSocket = null;
    private PrintWriter serverPrintWriter = null;
    private BufferedReader serverBufferedReader = null;

    // connectThread :
    // quitThread :
    // fileTransferThread : variables that execute specific functions of the program so that the GUI can be updated in the foreground
    private NotificationThread connectThread = null;
    private NotificationThread quitThread = null;
    private NotificationThread fileTransferThread = null;

    // clientIsQuitting :
    // failedAuthentication :
    // serverIsQuitting : variables useful for controlling/exiting NotificationThread's currently active
    private volatile boolean clientIsQuitting = false;
    private volatile boolean serverIsQuitting = false;
    private volatile boolean failedAuthentication = true;

    /**
     * Creates the ClientPanel
     */
    public ClientPanel ()
    {
        initComponents ();
    }


    /**
     * ThreadCompletionListener interface method that notifies the Client whenever any NotificationThread has finished executing
     * @param thread : the Thread ( NotificationThread ) that has finished executing
     */
    @Override
    public void threadCompletedNotification ( Thread thread )
    {
        System.out.println ( "threadCompletedNotification" );

        if ( thread.equals ( connectThread ) )
        {
            connectThread = null;

            connectButton.setVisible ( true );

            if ( failedAuthentication )
                disconnectButtonActionPerformed ();
            else
                startQuitThread ();
        }
        else if ( thread.equals ( quitThread ) )
        {
            quitThread = null;
        }
        else if ( thread.equals ( fileTransferThread ) )
        {
            fileTransferThread = null;
        }

        // This Will Execute When the Server Requests Termination, not the Client
        if ( serverIsQuitting )
            disconnectButtonActionPerformed ();
    }


    /**
     * This Thread, which is usually started after a Client has established a connection with the server, will listen for any
     * "SERVER-QUIT" message that can be sent by the Server which signifies the Server is terminating the connection.
     */
    private void startQuitThread ()
    {
        System.out.println ( "startQuitThread" );

        quitThread = new NotificationThread ()
        {
            @Override
            public void notifyingRunnable ()
            {
                while ( !clientIsQuitting && !serverIsQuitting )
                {
                    try
                    {
                        if ( fileTransferThread == null && serverBufferedReader.ready () )
                        {
                            String clientInput = serverBufferedReader.readLine ();

                            System.out.println ( "Client > Received message \"" + clientInput + "\"" );

                            if ( clientInput.equals ( "SERVER-QUIT" ) )
                            {
                                // Set Server Quit Flag
                                serverIsQuitting = true;

                                // resent Message to Client
                                String message = "Server has closed the connection";
                                JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.INFORMATION_MESSAGE );
                            }
                        }
                    }
                    catch ( IOException ioe )
                    {
                        ioe.printStackTrace ();
                    }
                }
            }
        };

        quitThread.setName ( "Client Quit Thread" );
        quitThread.addListener ( this );
        quitThread.start ();
    }


    /**
     * This method attempts to load the file specified by the JFileChooser so that it can be used as a XOR-Key, and eventually
     * passed into the XOR-Cipher for encryption Byte data. If a file can be used as a XOR-Key, then the "xorKey" and "xorTextArea"
     * variables will be updated to reflect these changes
     */
    private void xorButtonActionPerformed ()
    {
        System.out.println ( "xorButtonActionPerformed" );

        JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getDefaultDirectory () );
        fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );

        if ( fileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
        {
            Path xorPath = fileChooser.getSelectedFile ().toPath ();

            try ( FileInputStream fileInputStream = new FileInputStream ( xorPath.toFile () ) )
            {
                long fileSize = fileInputStream.getChannel ().size ();
                if ( fileSize > Integer.MAX_VALUE ) throw new IOException ();

                xorKey = new byte[ ( int ) fileSize ];

                for ( Integer inputByte, count = 0; ( inputByte = fileInputStream.read () ) != -1; ++count )
                    xorKey[ count ] = inputByte.byteValue ();

                xorTextArea.setText ( xorPath.toString () );

                System.out.println ( "Client > XOR Key Bytes : " + Arrays.toString ( xorKey ) );
            }

            catch ( IOException ioe )
            {
                System.out.println ( Arrays.toString ( ioe.getStackTrace () ) );

                xorKey = null;

                String message = "XOR file could not be read";
                JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
            }
        }
    }


    /**
     * Nullifies the XOR-Key, if one is being used
     */
    private void xorClearButtonActionPerformed ()
    {
        System.out.println ( "xorClearButtonActionPerformed" );

        xorTextArea.setText ( null );
        xorKey = null;
    }


    /**
     * Attempts to find the file that will be used to transfer to the Server. If a file is selected, then update the
     * "filePath" and "fileTextArea" variables.
     */
    private void fileButtonActionPerformed ()
    {
        System.out.println ( "fileButtonActionPerformed" );

        JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getHomeDirectory () );
        fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );

        if ( fileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
        {
            filePath = fileChooser.getSelectedFile ().toPath ();
            fileTextArea.setText ( filePath.toString () );
        }
    }


    /**
     * Nullifies the "filePath" and "fileTextArea" variables
     */
    private void fileClearButtonActionPerformed ()
    {
        System.out.println ( "fileClearButtonActionPerformed" );

        // Clear File Text Area
        fileTextArea.setText ( null );
        filePath = null;
    }


    /**
     * Tries to send the selected file, "filePath," to a Server. If the options XOR-Key or ASCII-Armoring are selected, then
     * the data is transformed into new Bytes, or a ASCII String, before being sent to the Server. Along the way, the Server
     * will respond with routine integrity checks which describe if the data sent by the Client was correctly received. If there
     * was an error during transmission, the a JDialog asking for a retry attempt will be presented, which can be cancelled. Depending
     * on the outcome of the JDialog, the Client either tries to resend the data, or cancels the entire transmission.
     */
    private void sendFileButtonActionPerformed ()
    {
        System.out.println ( "sendFileButtonActionPerformed" );

        ProgressDialog progressDialog = new ProgressDialog ();

        fileTransferThread = new NotificationThread ()
        {
            @Override
            public void notifyingRunnable ()
            {
                // Send Files here
                // First, check to see that file path is a valid file
                if ( filePath != null && Files.exists ( filePath ) && Files.isReadable ( filePath ) )
                {
                    try ( FileInputStream fileInputStream = new FileInputStream ( filePath.toFile () ) )
                    {
                        // Inform Server a File is about to be Transferred
                        serverPrintWriter.println ( "CLIENT-FILE" );

                        // Get filename and send to Server
                        String filename = filePath.getFileName ().toString ();
                        serverPrintWriter.println ( filename );

                        // Get file options and send to Server
                        String fileOptions = ( armoringCheckBox.isSelected () ? "A" : "-" ) + ( copyRadioButton.isSelected () ? "C" : "O" );

                        // Get Size of File in Bytes
                        long sizeOfFile = fileInputStream.getChannel ().size ();

                        // Send File-Options and Chunk-Size to Server
                        serverPrintWriter.println ( fileOptions + " && " + sizeOfFile + " && " + chunkSize );

                        long fileSize = fileInputStream.getChannel ().size ();
                        long bytesRead, totalBytesCurrentlyRead = 0;

                        boolean retrying = false;
                        byte[] hashBytes, dataBytes = new byte[ chunkSize ];
                        String hashString = "", dataString = "";

                        label:
                        while ( !serverIsQuitting )
                        {
                            if ( retrying )
                            {
                                serverPrintWriter.println ( hashString + " && " + dataString );
                            }
                            else
                            {
                                if ( ( bytesRead = fileInputStream.read ( dataBytes ) ) < 0 )
                                {
                                    // Inform Server Bytes are Done
                                    serverPrintWriter.println ( "BYTES-DONE" );

                                    System.out.println ( "Client > Finished Writing Bytes" );

                                    String message = "\"" + filename + "\" was successfully transferred";
                                    JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.INFORMATION_MESSAGE );

                                    break;
                                }

                                // Calculate Hash of Bytes
                                hashBytes = Utility.longToBytes ( Utility.hash ( dataBytes ) );

                                hashString = Arrays.toString ( hashBytes );
                                System.out.println ( "Client > Plain Hash Bytes > " + hashString );

                                // Display Read Bytes
                                dataString = Arrays.toString ( dataBytes );
                                System.out.println ( "Client > Plain Data Bytes > " + dataString );

                                // Encrypt Hash and Data Bytes with XOR Key, if available
                                if ( xorKey != null )
                                {
                                    hashBytes = XORCipher.encrypt ( hashBytes, xorKey );
                                    dataBytes = XORCipher.encrypt ( dataBytes, xorKey );

                                    hashString = Arrays.toString ( hashBytes );
                                    dataString = Arrays.toString ( dataBytes );

                                    System.out.println ( "Client > XOR Hash Bytes > " + hashString );
                                    System.out.println ( "Client > XOR Data Bytes > " + dataString );
                                }

                                // Apply ASCII Armoring to Data Bytes, if available
                                if ( armoringCheckBox.isSelected () )
                                {
                                    dataString = MIME.base64Encoding ( dataBytes );

                                    System.out.println ( "Client > BASE64 > " + dataString );
                                }

                                if ( !progressDialog.isShowing () )
                                {
                                    System.out.println ( "Client > Progress Bar was Closed Prematurely" );
                                    serverPrintWriter.println ( "FILE-CANCELLED" );
                                    return;
                                }

                                // Send Hash Bytes and Data Bytes to the Server ( Data Bytes Could be in ASCII-Armored format if requested )
                                serverPrintWriter.println ( hashString + " && " + dataString );

                                // Update Progress Bar
                                totalBytesCurrentlyRead += bytesRead * 100;
                                double percentage = totalBytesCurrentlyRead / fileSize;

                                progressDialog.updateProgressBar ( ( int ) ( percentage ) );
                            }

                            // Get Hash Result
                            String dataIntegrityResult = serverBufferedReader.readLine ();
                            System.out.println ( "Client > Data Integrity Result > " + dataIntegrityResult );

                            switch ( dataIntegrityResult )
                            {
                                case "SERVER-HASH-FAILED":
                                    String message = "File failed to transfer. Retry?";
                                    int optionType = JOptionPane.showConfirmDialog ( getParent (), message, null, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE );

                                    if ( optionType == JOptionPane.YES_OPTION )
                                    {
                                        System.out.println ( "Client > Retrying" );

                                        retrying = true;
                                    }
                                    else
                                    {
                                        System.out.println ( "Client > Cancelling File Transfer" );

                                        serverPrintWriter.println ( "FILE-CANCELLED" );
                                        return;
                                    }
                                    break;
                                case "SERVER-HASH-SUCCESS":
                                    if ( retrying )
                                        retrying = false;
                                    break;
                                case "SERVER-QUIT":
                                    serverIsQuitting = true;
                                    break label;
                            }
                        }
                    }
                    catch ( IOException ioe )
                    {
                        ioe.printStackTrace ();
                    }
                    finally
                    {
                        progressDialog.dispose ();

                        if ( serverIsQuitting )
                        {
                            // Present Message to Client
                            String message = "Server has closed the connection";
                            JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.INFORMATION_MESSAGE );
                        }
                    }
                }
                else
                {
                    String message = "File could not be found";
                    JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
                }
            }
        };

        fileTransferThread.setName ( "Client File Transfer Thread" );
        fileTransferThread.addListener ( this );
        fileTransferThread.start ();

        progressDialog.setVisible ( true );
    }


    /**
     * As the JSlider is moved by the Client, update our corresponding "chunkSize" variable to reflect this change
     */
    private void chunkSizeSliderStateChanged ()
    {
        System.out.println ( "chunkSizeSliderStateChanged" );

        chunkSize = chunkSizeSlider.getValue () * 1000;
    }


    /**
     * Will try to connect the Client to the Server. It does so by sending the Client's credentials, which will then be
     * validated by the Server, and who will respond with either "AUTH-SUCCESS" or "AUTH-FAILED."
     */
    private void connectButtonActionPerformed ()
    {
        System.out.println ( "connectButtonActionPerformed" );

        connectThread = new NotificationThread () {
            @Override
            protected void notifyingRunnable ()
            {
                try
                {
                    connectButton.setVisible ( false );

                    // Get name of server
                    String host = serverTextField.getText ();

                    // Parse Port. If not a valid Integer, Exception will be thrown
                    String portString = portTextField.getText ();
                    Integer port = Integer.parseInt ( portString );

                    // Try to open Socket
                    serverSocket = new Socket ( host, port );

                    // Create I/O which will be used for communication between the Client and Server
                    serverPrintWriter = new PrintWriter ( serverSocket.getOutputStream (), true );
                    serverBufferedReader = new BufferedReader ( new InputStreamReader ( serverSocket.getInputStream () ) );

                    // Prepare Username and Password that will be sent to Server
                    String username = usernameTextField.getText ().trim ();
                    String password = new String ( passwordField.getPassword () ).trim ();

                    String credentials = AES.encrypt ( username + ":" + password, username );

                    // Send credentials to Server
                    serverPrintWriter.println ( credentials );

                    // Read authentication response
                    failedAuthentication = serverBufferedReader.readLine ().equals ( "AUTH-FAILED" );

                    if ( failedAuthentication )
                    {
                        // Authentication Failed so Inform Client
                        String message = "Authentication has failed";
                        JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
                    }
                    else
                    {
                        // Update GUI to Reflect Connection Status
                        dynamicStatusLabel.setText ( "Connected" );
                        connectButton.setEnabled ( false );

                        // Inform Client Connection Has Been Established
                        String serverHostName = serverSocket.getInetAddress ().getHostAddress ();

                        String message = "Connection with " + serverHostName + " was established";
                        JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.INFORMATION_MESSAGE );
                    }
                }
                catch ( IOException ioe )
                {
                    String message;

                    if ( ioe instanceof BindException )
                    {
                        message = String.format ( "%s\n%s", "Bind Exception", "Port \"" + portTextField.getText () + "\" is not usable" );
                        JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
                    }
                    else if ( ioe instanceof NoRouteToHostException )
                    {
                        message = String.format ( "%s\n%s", "No Route to Host Exception", "Host \"" + serverTextField.getText () + "\" is unreachable" );
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

                    ioe.printStackTrace ( System.err );
                }
                catch ( NumberFormatException nfe )
                {
                    String message = String.format ( "%s\n%s", "Number Format Exception", "\"Port\" must be a number." );
                    JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );

                    nfe.printStackTrace ( System.err );
                }
            }
        };

        connectThread.setName ( "Client Connect Thread" );
        connectThread.addListener ( this );
        connectThread.start ();
    }


    /**
     * This method aims to disconnect the Client from the Server and performs and cleanup necessary, such as reverting the
     * GUI to it's original state, if necessary, or restoring internal variables to their original values.
     */
    private void disconnectButtonActionPerformed ()
    {
        System.out.println ( "disconnectButtonActionPerformed" );

        /*
         * During a Quit Thread, if the Client presses disconnect, then set the quit Flag ( clientIsQuitting )
         * to true in order to terminate the Quit Thread. Afterwards, wait for Thread to completely
         * finish before moving on ( join )
         */
        if ( quitThread != null )
        {
            clientIsQuitting = true;

            try { quitThread.join (); }
            catch ( InterruptedException ie ) { ie.printStackTrace ( System.err ); }

            if ( serverPrintWriter != null )
                serverPrintWriter.println ( "CLIENT-QUIT" );
        }

        /*
         * Close the "serverSocket" variable to close connection to the Server and restore internal variables to null
         * Closing Socket also closes relevant I/O Variables
         */
        try
        {
            if ( serverSocket != null )
            {
                serverSocket.close ();
                serverSocket = null;
                serverPrintWriter = null;
                serverBufferedReader = null;
            }
        }
        catch ( IOException ioe ) { ioe.printStackTrace ( System.err ); }

        // Restore GUI to original state
        dynamicStatusLabel.setText ( "Stopped" );
        connectButton.setEnabled ( true );

        if ( !failedAuthentication )
            failedAuthentication = true;

        if ( clientIsQuitting )
            clientIsQuitting = false;

        if ( serverIsQuitting )
            serverIsQuitting = false;
    }


    /**
     * Creates the ClientPanel and is generated with the help of JFormDesigner generated code
     */
    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY
        // GEN-BEGIN:initComponents
		JLabel statusLabel = new JLabel();
		JLabel usernameLabel = new JLabel();
		usernameTextField = new JTextField();
		JLabel passwordLabel = new JLabel();
		passwordField = new JPasswordField();
		JLabel portLabel = new JLabel();
		portTextField = new JTextField();
		JLabel serverLabel = new JLabel();
		JButton fileButton = new JButton();
		JLabel fileOptionsLabel = new JLabel();
		armoringCheckBox = new JCheckBox();
		JRadioButton overwriteRadioButton = new JRadioButton();
		JLabel chunkSizeLabel = new JLabel();
		chunkSizeSlider = new JSlider();
		JLabel chunkSizeValueLabel = new JLabel();
		JButton sendFileButton = new JButton();
		JButton disconnectButton = new JButton();
		connectButton = new JButton();
		dynamicStatusLabel = new JLabel();
		JScrollPane fileScrollPane = new JScrollPane();
		fileTextArea = new JTextArea();
		serverTextField = new JTextField();
		copyRadioButton = new JRadioButton();
		JButton xorButton = new JButton();
		JScrollPane xorScrollPane = new JScrollPane();
		xorTextArea = new JTextArea();
		JButton xorClearButton = new JButton();
		JButton fileClearButton = new JButton();
		JSpinner chunkSizeSpinner = new JSpinner();

		//======== this ========
		setBackground(Color.white);

		//---- statusLabel ----
		statusLabel.setText("Status");
		statusLabel.setFont(statusLabel.getFont().deriveFont(statusLabel.getFont().getStyle() | Font.BOLD));

		//---- usernameLabel ----
		usernameLabel.setText("Username");
		usernameLabel.setFont(usernameLabel.getFont().deriveFont(usernameLabel.getFont().getStyle() | Font.BOLD));

		//---- usernameTextField ----
		usernameTextField.setBackground(Color.white);
		usernameTextField.setFont(usernameTextField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		usernameTextField.setForeground(new Color(153, 0, 0));
		usernameTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		usernameTextField.setPreferredSize(new Dimension(45, 32));

		//---- passwordLabel ----
		passwordLabel.setText("Password");
		passwordLabel.setFont(passwordLabel.getFont().deriveFont(passwordLabel.getFont().getStyle() | Font.BOLD));

		//---- passwordField ----
		passwordField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		passwordField.setBackground(Color.white);
		passwordField.setFont(passwordField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		passwordField.setForeground(new Color(153, 0, 0));
		passwordField.setPreferredSize(new Dimension(0, 32));

		//---- portLabel ----
		portLabel.setText("Server Port");
		portLabel.setFont(portLabel.getFont().deriveFont(portLabel.getFont().getStyle() | Font.BOLD));

		//---- portTextField ----
		portTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		portTextField.setFont(portTextField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		portTextField.setForeground(new Color(153, 0, 0));
		portTextField.setPreferredSize(new Dimension(36, 32));

		//---- serverLabel ----
		serverLabel.setText("Server Address");
		serverLabel.setFont(serverLabel.getFont().deriveFont(serverLabel.getFont().getStyle() | Font.BOLD));

		//---- fileButton ----
		fileButton.setText("File To Transfer");
		fileButton.setMinimumSize(new Dimension(92, 29));
		fileButton.setMaximumSize(new Dimension(92, 29));
		fileButton.setFont(fileButton.getFont().deriveFont(fileButton.getFont().getStyle() | Font.BOLD));
		fileButton.setHorizontalAlignment(SwingConstants.LEADING);
		fileButton.addActionListener(e -> fileButtonActionPerformed());

		//---- fileOptionsLabel ----
		fileOptionsLabel.setText("File Option");
		fileOptionsLabel.setFont(fileOptionsLabel.getFont().deriveFont(fileOptionsLabel.getFont().getStyle() | Font.BOLD));
		fileOptionsLabel.setMinimumSize(new Dimension(95, 17));
		fileOptionsLabel.setMaximumSize(new Dimension(95, 17));

		//---- armoringCheckBox ----
		armoringCheckBox.setText("ASCII Armoring");
		armoringCheckBox.setFont(armoringCheckBox.getFont().deriveFont(armoringCheckBox.getFont().getStyle() | Font.BOLD));
		armoringCheckBox.setBackground(Color.white);

		//---- overwriteRadioButton ----
		overwriteRadioButton.setText("Overwrite");
		overwriteRadioButton.setFont(overwriteRadioButton.getFont().deriveFont(overwriteRadioButton.getFont().getStyle() | Font.BOLD));
		overwriteRadioButton.setBackground(Color.white);

		//---- chunkSizeLabel ----
		chunkSizeLabel.setText("Chunk Size");
		chunkSizeLabel.setFont(chunkSizeLabel.getFont().deriveFont(chunkSizeLabel.getFont().getStyle() | Font.BOLD));

		//---- chunkSizeSlider ----
		chunkSizeSlider.setMaximum(1000);
		chunkSizeSlider.setMinimum(1);
		chunkSizeSlider.setPaintTicks(true);
		chunkSizeSlider.setMajorTickSpacing(100);
		chunkSizeSlider.setValue(64);
		chunkSizeSlider.setMinorTickSpacing(50);
		chunkSizeSlider.addChangeListener(e -> chunkSizeSliderStateChanged());

		//---- chunkSizeValueLabel ----
		chunkSizeValueLabel.setText("KB");
		chunkSizeValueLabel.setFont(chunkSizeValueLabel.getFont().deriveFont(chunkSizeValueLabel.getFont().getStyle() | Font.BOLD, chunkSizeValueLabel.getFont().getSize() + 1f));

		//---- sendFileButton ----
		sendFileButton.setText("Send");
		sendFileButton.setFont(sendFileButton.getFont().deriveFont(sendFileButton.getFont().getStyle() | Font.BOLD));
		sendFileButton.addActionListener(e -> sendFileButtonActionPerformed());

		//---- disconnectButton ----
		disconnectButton.setText("Disconnect");
		disconnectButton.setFont(disconnectButton.getFont().deriveFont(disconnectButton.getFont().getStyle() | Font.BOLD));
		disconnectButton.addActionListener(e -> disconnectButtonActionPerformed());

		//---- connectButton ----
		connectButton.setText("Connect");
		connectButton.setFont(connectButton.getFont().deriveFont(connectButton.getFont().getStyle() | Font.BOLD));
		connectButton.addActionListener(e -> connectButtonActionPerformed());

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
			fileTextArea.setColumns(1);
			fileScrollPane.setViewportView(fileTextArea);
		}

		//---- serverTextField ----
		serverTextField.setFont(serverTextField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		serverTextField.setForeground(new Color(153, 0, 0));
		serverTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));

		//---- copyRadioButton ----
		copyRadioButton.setText("Copy");
		copyRadioButton.setFont(copyRadioButton.getFont().deriveFont(copyRadioButton.getFont().getStyle() | Font.BOLD));
		copyRadioButton.setSelected(true);
		copyRadioButton.setBackground(Color.white);

		//---- xorButton ----
		xorButton.setText("XOR Key");
		xorButton.setFont(xorButton.getFont().deriveFont(xorButton.getFont().getStyle() | Font.BOLD));
		xorButton.setActionCommand("XOR-Key File");
		xorButton.addActionListener(e -> xorButtonActionPerformed());

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
			xorTextArea.setColumns(1);
			xorScrollPane.setViewportView(xorTextArea);
		}

		//---- xorClearButton ----
		xorClearButton.setText("Clear");
		xorClearButton.setFont(xorClearButton.getFont().deriveFont(xorClearButton.getFont().getStyle() | Font.BOLD));
		xorClearButton.addActionListener(e -> xorClearButtonActionPerformed());

		//---- fileClearButton ----
		fileClearButton.setText("Clear");
		fileClearButton.setFont(fileClearButton.getFont().deriveFont(fileClearButton.getFont().getStyle() | Font.BOLD));
		fileClearButton.addActionListener(e -> fileClearButtonActionPerformed());

		//---- chunkSizeSpinner ----
		chunkSizeSpinner.setModel(new SpinnerNumberModel(1, 1, 1000, 1));
		chunkSizeSpinner.setFont(chunkSizeSpinner.getFont().deriveFont(Font.BOLD|Font.ITALIC));

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup()
				.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
						.addGroup(layout.createSequentialGroup()
							.addContainerGap(235, Short.MAX_VALUE)
							.addComponent(chunkSizeSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
							.addComponent(chunkSizeValueLabel))
						.addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
							.addGap(25, 25, 25)
							.addGroup(layout.createParallelGroup()
								.addComponent(disconnectButton, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(fileScrollPane, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(dynamicStatusLabel, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(passwordField, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(usernameTextField, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(portTextField, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(serverTextField, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(xorScrollPane, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addGroup(layout.createSequentialGroup()
									.addGroup(layout.createParallelGroup()
										.addComponent(passwordLabel)
										.addComponent(usernameLabel)
										.addComponent(portLabel)
										.addComponent(serverLabel)
										.addComponent(statusLabel)
										.addComponent(fileOptionsLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addComponent(armoringCheckBox)
										.addComponent(copyRadioButton)
										.addComponent(overwriteRadioButton)
										.addComponent(chunkSizeLabel))
									.addGap(0, 190, Short.MAX_VALUE))
								.addComponent(chunkSizeSlider, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(connectButton, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
									.addComponent(xorButton)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 144, Short.MAX_VALUE)
									.addComponent(xorClearButton))
								.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
									.addGap(0, 0, Short.MAX_VALUE)
									.addComponent(sendFileButton))
								.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
									.addComponent(fileButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 96, Short.MAX_VALUE)
									.addComponent(fileClearButton)))))
					.addGap(50, 50, 50))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addComponent(statusLabel)
					.addGap(18, 18, 18)
					.addComponent(dynamicStatusLabel)
					.addGap(18, 18, 18)
					.addComponent(serverLabel)
					.addGap(18, 18, 18)
					.addComponent(serverTextField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
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
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(xorClearButton)
						.addComponent(xorButton))
					.addGap(18, 18, 18)
					.addComponent(xorScrollPane, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(fileClearButton)
						.addComponent(fileButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addComponent(fileScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
					.addComponent(sendFileButton)
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
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(chunkSizeValueLabel)
						.addComponent(chunkSizeSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(58, 58, 58)
					.addComponent(connectButton)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(disconnectButton)
					.addGap(25, 25, 25))
		);

		//---- fileOptionButtonGroup ----
		ButtonGroup fileOptionButtonGroup = new ButtonGroup();
		fileOptionButtonGroup.add(overwriteRadioButton);
		fileOptionButtonGroup.add(copyRadioButton);

		//---- bindings ----
		BindingGroup bindingGroup = new BindingGroup();
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			connectButton, BeanProperty.create("enabled"),
			serverTextField, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			connectButton, BeanProperty.create("enabled"),
			portTextField, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			connectButton, BeanProperty.create("enabled"),
			usernameTextField, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			connectButton, BeanProperty.create("enabled"),
			passwordField, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			fileButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			sendFileButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			fileTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			armoringCheckBox, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			copyRadioButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			overwriteRadioButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			chunkSizeSlider, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			chunkSizeValueLabel, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			connectButton, ELProperty.create("${!enabled}"),
			disconnectButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			connectButton, BeanProperty.create("enabled"),
			xorButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			connectButton, BeanProperty.create("enabled"),
			xorTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			connectButton, BeanProperty.create("enabled"),
			xorClearButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			fileClearButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			disconnectButton, BeanProperty.create("enabled"),
			chunkSizeSpinner, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			chunkSizeSlider, BeanProperty.create("value"),
			chunkSizeSpinner, BeanProperty.create("value")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			chunkSizeSpinner, BeanProperty.create("value"),
			chunkSizeSlider, BeanProperty.create("value")));
		bindingGroup.bind();
        // GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY
    // GEN-BEGIN:variables
	private JTextField usernameTextField;
	private JPasswordField passwordField;
	private JTextField portTextField;
	private JCheckBox armoringCheckBox;
	private JSlider chunkSizeSlider;
	private JButton connectButton;
	private JLabel dynamicStatusLabel;
	private JTextArea fileTextArea;
	private JTextField serverTextField;
	private JRadioButton copyRadioButton;
	private JTextArea xorTextArea;
    // GEN-END:variables
}

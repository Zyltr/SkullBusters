import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.ELProperty;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/*
 * Created by JFormDesigner on Thu Jan 25 02:18:08 PST 2018
 */

public class ClientPanel extends JPanel implements ThreadCompleteListener
{
    // TODO -> Used for JOptionPane messages
    private String message;

    // TODO -> "Send File" Variables
    private Path filePath = null;
    private JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getHomeDirectory () );

    // TODO -> Variable that will store XOR-Key
    private byte[] xorKey = null;

    // TODO -> Variable that will be used to partition file into a specific size of Bytes
    private Integer chunkSize = 1000;

    // TODO -> Required Server Variables
    private Socket serverSocket = null;
    private PrintWriter printWriter = null;
    private BufferedReader bufferedReader = null;

    private NotifyingThread connectThread = null;
    private NotifyingThread quitThread = null;
    private NotifyingThread fileTransferThread = null;

    // TODO -> Useful for controlling Thread initiated by the "Connect" Button
    private volatile boolean clientIsQuitting = false;
    private volatile boolean serverIsQuitting = false;
    private volatile boolean failedAuthentication = false;

    public ClientPanel ()
    {
        initComponents ();

        // TODO -> Configure JFileChooser
        fileChooser.setDialogTitle ( "Choose File to Send" );
        fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );
    }


    @Override
    public void notifyOfThreadComplete ( final Thread thread )
    {
        if ( thread == connectThread )
        {
            connectThread.removeListener( this );
            connectThread = null;

            if ( failedAuthentication )
            {
                disconnectButtonActionPerformed ();
            }
            else
            {
                startQuitThread();
            }
        }
        else if ( thread == quitThread )
        {
            quitThread.removeListener ( this );
            quitThread = null;
        }
        else if ( thread == fileTransferThread )
        {
            fileTransferThread.removeListener ( this );
            fileTransferThread = null;
        }

        // TODO -> This Will Execute When the Server Requests Termination, not the Client
        if ( serverIsQuitting )
            disconnectButtonActionPerformed ();
    }

    private void startQuitThread ()
    {
        // TODO -> Begin the Thread that Will Only Listens For Server's Quit Message
        quitThread = new NotifyingThread ()
        {
            @Override
            public void doRun ()
            {
                while ( !clientIsQuitting && !serverIsQuitting )
                {
                    try
                    {
                        if ( fileTransferThread == null && bufferedReader.ready () )
                        {
                            String clientInput = bufferedReader.readLine ();

                            System.out.println ( "Client > Received message \"" + clientInput + "\"" );

                            if ( clientInput.equals ( "SERVER-QUIT" ) )
                            {
                                // TODO -> Set Server Quit Flag
                                serverIsQuitting = true;

                                // TODO -> Present Message to Client
                                message = "Server has closed the connection";
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

        // TODO -> This Thread Will only Listen for the Server's Quit Response, Which May Never Happen
        quitThread.addListener ( this );
        quitThread.start ();
    }

    // TODO -> Attempts to open file to be used as XOR-Key
    private void xorButtonActionPerformed ()
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

                if ( fileSize > Integer.MAX_VALUE ) throw new IOException ();

                xorKey = new byte[( int ) fileSize];

                // TODO -> Convert Bytes of file to a binary representation
                for ( Integer inputByte, count = 0; ( inputByte = fileInputStream.read () ) != -1; ++count )
                    xorKey[count] = inputByte.byteValue ();

                xorTextArea.setText ( xorPath.toString () );

                System.out.println ( "Client > XOR Key Bytes : " + Arrays.toString ( xorKey ) );
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


    private void xorClearButtonActionPerformed ()
    {
        // TODO -> Clear XOR Text Area
        xorTextArea.setText ( null );
        xorKey = null;
    }


    // TODO -> Attempts to find the file that will be used to transfer to the Server
    private void fileButtonActionPerformed ()
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


    private void fileClearButtonActionPerformed ()
    {
        // TODO -> Clear File Text Area
        fileTextArea.setText ( null );
        filePath = null;
    }


    // TODO -> Tries to send file to Server
    private void sendFileButtonActionPerformed ()
    {
        ProgressDialog progressDialog = new ProgressDialog ( null );

        fileTransferThread = new NotifyingThread ()
        {
            @Override
            public void doRun ()
            {
                // TODO -> Send Files here
                // TODO -> First, check to see that file path is a valid file
                if ( filePath != null && Files.exists ( filePath ) && Files.isReadable ( filePath ) )
                {
                    try ( FileInputStream fileInputStream = new FileInputStream ( filePath.toFile () ) )
                    {
                        // TODO -> Inform Server a File is about to be Transferred
                        printWriter.println ( "CLIENT-FILE" );

                        // TODO -> Get filename and send to Server
                        String filename = filePath.getFileName ().toString ();
                        printWriter.println ( filename );

                        // TODO -> Get file options and send to Server
                        String fileOptions = ( armoringCheckBox.isSelected () ? "A" : "-" ) + ( copyRadioButton.isSelected () ? "C" : "O" );

                        // TODO -> Get Size of File in Bytes
                        long sizeOfFile = fileInputStream.getChannel().size();

                        // TODO -> Send File-Options and Chunk-Size to Server
                        printWriter.println ( fileOptions + " && " + sizeOfFile + " && " + chunkSize );

                        long fileSize = fileInputStream.getChannel ().size ();
                        long bytesRead, totalBytesCurrentlyRead = 0;

                        boolean retrying = false;
                        byte[] hashBytes, dataBytes = new byte[chunkSize];
                        String hashString = "", dataString = "";

                        while ( !serverIsQuitting )
                        {
                            if ( retrying )
                            {
                                printWriter.println ( hashString + " && " + dataString );
                            }
                            else
                            {
                                if ( ( bytesRead = fileInputStream.read ( dataBytes ) ) < 0 )
                                {
                                    // TODO -> Inform Server Bytes are Done
                                    printWriter.println ( "BYTES-DONE" );
                                    break;
                                }

                                // TODO -> Calculate Hash of Bytes
                                hashBytes = Utility.longToBytes ( Utility.hash ( dataBytes ) );

                                hashString = Arrays.toString ( hashBytes );
                                System.out.println ( "Client > Plain Hash Bytes > " + hashString );

                                // TODO -> Display Read Bytes
                                dataString = Arrays.toString ( dataBytes );
                                System.out.println ( "Client > Plain Data Bytes > " + dataString );

                                // TODO -> Encrypt Hash and Data Bytes with XOR Key, if available
                                if ( xorKey != null )
                                {
                                    hashBytes = XORCipher.encrypt ( hashBytes, xorKey );
                                    dataBytes = XORCipher.encrypt ( dataBytes, xorKey );

                                    hashString = Arrays.toString ( hashBytes );
                                    dataString = Arrays.toString ( dataBytes );

                                    System.out.println ( "Client > XOR Hash Bytes > " + hashString );
                                    System.out.println ( "Client > XOR Data Bytes > " + dataString );
                                }

                                // TODO -> Apply ASCII Armoring to Data Bytes, if available
                                if ( armoringCheckBox.isSelected () )
                                {
                                    dataString = MIME.base64Encoding ( dataBytes );

                                    System.out.println ( "Client > BASE64 > " + dataString );
                                }

                                if ( ! progressDialog.isShowing () )
                                {
                                    System.out.println ( "Client > Progress Bar was Closed Prematurely" );
                                    printWriter.println ( "FILE-CANCELLED" );
                                    return;
                                }

                                // TODO -> Send Hash Bytes and Data Bytes to the Server ( Data Bytes Could be in ASCII-Armored format if requested )
                                printWriter.println ( hashString + " && " + dataString );

                                // TODO -> Update Progress Bar
                                totalBytesCurrentlyRead += bytesRead * 100;
                                double percentage = totalBytesCurrentlyRead / fileSize;

                                progressDialog.updateProgressBar (  ( int ) ( percentage )  );
                            }

                            // TODO -> Get Hash Result
                            String dataIntegrityResult =  bufferedReader.readLine ();
                            System.out.println ( "Client > Data Integrity Result > " + dataIntegrityResult );

                            if ( dataIntegrityResult.equals ( "SERVER-HASH-FAILED" ) )
                            {
                                message = "File failed to transfer. Retry?";
                                int optionType = JOptionPane.showConfirmDialog ( getParent (), message,null, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE );

                                if ( optionType == JOptionPane.YES_OPTION )
                                {
                                    System.out.println ( "Client > Retrying" );

                                    retrying = true;
                                }
                                else
                                {
                                    System.out.println ( "Client > Cancelling File Transfer" );

                                    printWriter.println ( "FILE-CANCELLED" );
                                    return;
                                }
                            }
                            else if ( dataIntegrityResult.equals ( "SERVER-HASH-SUCCESS" ) )
                            {
                                if ( retrying )
                                    retrying = false;
                            }
                        }

                        System.out.println ( "Client > Finished Writing Bytes" );

                        message = "\"" + filename + "\" was successfully transferred";
                        JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.INFORMATION_MESSAGE );
                    }
                    catch ( IOException ioe )
                    {
                        ioe.printStackTrace ();
                    }
                    finally
                    {
                        progressDialog.dispose ();
                    }
                }
                else
                {
                    message = "File could not be found";
                    JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
                }
            }
        };

        fileTransferThread.addListener ( this );
        fileTransferThread.start ();

        progressDialog.setVisible ( true );
    }


    // TODO -> As the JSlider is moved, update the Label and save the new ChunkSize
    private void chunkSizeSliderStateChanged ()
    {
        // TODO -> When the Slider changes value, update our label
        chunkSize = chunkSizeSlider.getValue () * 1000;
    }


    // TODO -> Ultimately, tries to connect to Server after performing many checks
    private void connectButtonActionPerformed ()
    {
        connectThread = new NotifyingThread() {
            @Override
            public void doRun() {
                try
                {
                    // TODO -> Get name of server
                    String host = serverTextField.getText ();

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
                    failedAuthentication =  bufferedReader.readLine ().equals ( "AUTH-FAILED" );

                    if ( failedAuthentication )
                    {
                        // TODO -> Authentication Failed so Inform Client
                        message = "Authentication has failed";
                        JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
                    }
                    else
                    {
                        // TODO -> Update GUI to Reflect Connection Status
                        dynamicStatusLabel.setText ( "Connected" );
                        connectButton.setEnabled ( false );

                        // TODO -> Inform Client Connection Has Been Established
                        String serverHostName = serverSocket.getInetAddress ().getHostName ();

                        message = "Connection with " + serverHostName + " was established";
                        JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.INFORMATION_MESSAGE );
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

                    ioe.printStackTrace ();
                }
                catch ( NumberFormatException nfe )
                {
                    // TODO -> Triggered when a Port is not a valid Integer. E.g : Port "Hello, World!"
                    message = String.format ( "%s\n%s", "Number Format Exception", "\"Port\" must be a number." );
                    JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );

                    nfe.printStackTrace ();
                }
            }
        };

        connectThread.addListener( this );
        connectThread.start();
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
    private void disconnectButtonActionPerformed ()
    {
        if ( quitThread != null && !serverIsQuitting )
        {
            clientIsQuitting = true;

            if ( printWriter != null )
                printWriter.println ( "CLIENT-QUIT" );
        }

        try
        {
            closeConnection ();
        }
        catch ( IOException ioe )
        {
            ioe.printStackTrace ();
        }

        // TODO -> Restore GUI
        dynamicStatusLabel.setText ( "Stopped" );
        connectButton.setEnabled ( true );

        if ( failedAuthentication )
        {
            failedAuthentication = false;
        }

        if ( clientIsQuitting )
        {
            clientIsQuitting = false;
        }

        if ( serverIsQuitting )
        {
            serverIsQuitting = false;
        }
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
		JButton fileButton = new JButton();
		JLabel fileOptionsLabel = new JLabel();
		armoringCheckBox = new JCheckBox();
		overwriteRadioButton = new JRadioButton();
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
		JLabel authenticationOptionsLabel = new JLabel();
		plainRadioButton = new JRadioButton();
		JButton xorClearButton = new JButton();
		JButton fileClearButton = new JButton();
		JSpinner chunkSizeSpinner = new JSpinner();

		//======== this ========
		setPreferredSize(new Dimension(400, 1200));
		setOpaque(false);
		setMinimumSize(new Dimension(450, 1200));

		//---- statusLabel ----
		statusLabel.setText("Status");
		statusLabel.setFont(statusLabel.getFont().deriveFont(statusLabel.getFont().getStyle() | Font.BOLD, statusLabel.getFont().getSize() + 5f));

		//---- usernameLabel ----
		usernameLabel.setText("Username");
		usernameLabel.setFont(usernameLabel.getFont().deriveFont(usernameLabel.getFont().getStyle() | Font.BOLD, usernameLabel.getFont().getSize() + 5f));

		//---- usernameTextField ----
		usernameTextField.setBackground(Color.white);
		usernameTextField.setText("Debug");
		usernameTextField.setFont(usernameTextField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		usernameTextField.setForeground(new Color(153, 0, 0));
		usernameTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		usernameTextField.setPreferredSize(new Dimension(45, 32));

		//---- passwordLabel ----
		passwordLabel.setText("Password");
		passwordLabel.setFont(passwordLabel.getFont().deriveFont(passwordLabel.getFont().getStyle() | Font.BOLD, passwordLabel.getFont().getSize() + 5f));

		//---- passwordField ----
		passwordField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		passwordField.setBackground(Color.white);
		passwordField.setFont(passwordField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		passwordField.setForeground(new Color(153, 0, 0));
		passwordField.setPreferredSize(new Dimension(0, 32));

		//---- portLabel ----
		portLabel.setText("Server Port");
		portLabel.setFont(portLabel.getFont().deriveFont(portLabel.getFont().getStyle() | Font.BOLD, portLabel.getFont().getSize() + 5f));

		//---- portTextField ----
		portTextField.setText("1492");
		portTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		portTextField.setFont(portTextField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		portTextField.setForeground(new Color(153, 0, 0));
		portTextField.setPreferredSize(new Dimension(36, 32));

		//---- serverLabel ----
		serverLabel.setText("Server Address");
		serverLabel.setFont(serverLabel.getFont().deriveFont(serverLabel.getFont().getStyle() | Font.BOLD, serverLabel.getFont().getSize() + 5f));

		//---- fileButton ----
		fileButton.setText("File To Transfer");
		fileButton.setMinimumSize(new Dimension(92, 29));
		fileButton.setMaximumSize(new Dimension(92, 29));
		fileButton.setFont(fileButton.getFont().deriveFont(fileButton.getFont().getStyle() | Font.BOLD, fileButton.getFont().getSize() + 5f));
		fileButton.setHorizontalAlignment(SwingConstants.LEADING);
		fileButton.addActionListener(e -> fileButtonActionPerformed());

		//---- fileOptionsLabel ----
		fileOptionsLabel.setText("File Option");
		fileOptionsLabel.setFont(fileOptionsLabel.getFont().deriveFont(fileOptionsLabel.getFont().getStyle() | Font.BOLD, fileOptionsLabel.getFont().getSize() + 5f));
		fileOptionsLabel.setMinimumSize(new Dimension(95, 17));
		fileOptionsLabel.setMaximumSize(new Dimension(95, 17));

		//---- armoringCheckBox ----
		armoringCheckBox.setText("ASCII Armoring");
		armoringCheckBox.setFont(armoringCheckBox.getFont().deriveFont(armoringCheckBox.getFont().getStyle() | Font.BOLD));

		//---- overwriteRadioButton ----
		overwriteRadioButton.setText("Overwrite");
		overwriteRadioButton.setFont(overwriteRadioButton.getFont().deriveFont(overwriteRadioButton.getFont().getStyle() | Font.BOLD));

		//---- chunkSizeLabel ----
		chunkSizeLabel.setText("Chunk Size");
		chunkSizeLabel.setFont(chunkSizeLabel.getFont().deriveFont(chunkSizeLabel.getFont().getStyle() | Font.BOLD, chunkSizeLabel.getFont().getSize() + 5f));

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
		sendFileButton.setFont(sendFileButton.getFont().deriveFont(sendFileButton.getFont().getStyle() | Font.BOLD, sendFileButton.getFont().getSize() + 5f));
		sendFileButton.addActionListener(e -> sendFileButtonActionPerformed());

		//---- disconnectButton ----
		disconnectButton.setText("Disconnect");
		disconnectButton.setFont(disconnectButton.getFont().deriveFont(disconnectButton.getFont().getStyle() | Font.BOLD, disconnectButton.getFont().getSize() + 5f));
		disconnectButton.addActionListener(e -> disconnectButtonActionPerformed());

		//---- connectButton ----
		connectButton.setText("Connect");
		connectButton.setFont(connectButton.getFont().deriveFont(connectButton.getFont().getStyle() | Font.BOLD, connectButton.getFont().getSize() + 5f));
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
		serverTextField.setText("localhost");
		serverTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));

		//---- copyRadioButton ----
		copyRadioButton.setText("Copy");
		copyRadioButton.setFont(copyRadioButton.getFont().deriveFont(copyRadioButton.getFont().getStyle() | Font.BOLD));
		copyRadioButton.setSelected(true);

		//---- xorButton ----
		xorButton.setText("XOR Key");
		xorButton.setFont(xorButton.getFont().deriveFont(xorButton.getFont().getStyle() | Font.BOLD, xorButton.getFont().getSize() + 5f));
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

		//---- authenticationOptionsLabel ----
		authenticationOptionsLabel.setText("Authentication");
		authenticationOptionsLabel.setFont(authenticationOptionsLabel.getFont().deriveFont(authenticationOptionsLabel.getFont().getStyle() | Font.BOLD, authenticationOptionsLabel.getFont().getSize() + 5f));

		//---- plainRadioButton ----
		plainRadioButton.setText("Plain Text");
		plainRadioButton.setFont(plainRadioButton.getFont().deriveFont(plainRadioButton.getFont().getStyle() | Font.BOLD));
		plainRadioButton.setSelected(true);

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
				.addGroup(layout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addGroup(layout.createParallelGroup()
						.addGroup(layout.createSequentialGroup()
							.addComponent(chunkSizeLabel)
							.addGap(190, 271, Short.MAX_VALUE))
						.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup()
								.addComponent(fileOptionsLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addGroup(layout.createSequentialGroup()
									.addGroup(layout.createParallelGroup()
										.addComponent(armoringCheckBox)
										.addComponent(copyRadioButton)
										.addComponent(overwriteRadioButton))
									.addGap(0, 0, Short.MAX_VALUE)))
							.addGap(94, 94, 94))
						.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
								.addComponent(authenticationOptionsLabel, GroupLayout.Alignment.LEADING)
								.addComponent(passwordLabel, GroupLayout.Alignment.LEADING)
								.addComponent(usernameLabel, GroupLayout.Alignment.LEADING)
								.addComponent(portLabel, GroupLayout.Alignment.LEADING)
								.addComponent(plainRadioButton, GroupLayout.Alignment.LEADING)
								.addComponent(serverLabel, GroupLayout.Alignment.LEADING)
								.addComponent(statusLabel, GroupLayout.Alignment.LEADING))
							.addGap(0, 0, Short.MAX_VALUE))
						.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
								.addComponent(dynamicStatusLabel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(disconnectButton, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(connectButton, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(chunkSizeSlider, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addGroup(layout.createSequentialGroup()
									.addComponent(fileButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
									.addComponent(fileClearButton))
								.addComponent(fileScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addGroup(layout.createSequentialGroup()
									.addComponent(xorButton)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 123, Short.MAX_VALUE)
									.addComponent(xorClearButton))
								.addComponent(xorScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(passwordField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(usernameTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(portTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(serverTextField, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addGroup(layout.createSequentialGroup()
									.addGap(0, 210, Short.MAX_VALUE)
									.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
										.addGroup(layout.createSequentialGroup()
											.addComponent(chunkSizeSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
											.addComponent(chunkSizeValueLabel))
										.addComponent(sendFileButton))))
							.addGap(50, 50, 50))))
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
					.addComponent(authenticationOptionsLabel)
					.addGap(18, 18, 18)
					.addComponent(plainRadioButton)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(xorButton)
						.addComponent(xorClearButton))
					.addGap(18, 18, 18)
					.addComponent(xorScrollPane, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(fileButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(fileClearButton))
					.addGap(18, 18, 18)
					.addComponent(fileScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
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
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 72, Short.MAX_VALUE)
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
			connectButton, BeanProperty.create("enabled"),
			plainRadioButton, BeanProperty.create("enabled")));
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
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Erik Huerta
	private JTextField usernameTextField;
	private JPasswordField passwordField;
	private JTextField portTextField;
	private JCheckBox armoringCheckBox;
	private JRadioButton overwriteRadioButton;
	private JSlider chunkSizeSlider;
	private JButton connectButton;
	private JLabel dynamicStatusLabel;
	private JTextArea fileTextArea;
	private JTextField serverTextField;
	private JRadioButton copyRadioButton;
	private JTextArea xorTextArea;
	private JRadioButton plainRadioButton;
	private BindingGroup bindingGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

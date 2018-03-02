import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.ELProperty;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/*
 * Created by JFormDesigner on Thu Jan 25 01:00:01 PST 2018
 */

/**
 * A JPanel which is used by the Server
 */
public class ServerPanel extends JPanel implements ThreadCompletionListener
{
    // saveToPath : a variable that will hold the path where transferred files will be saved to
    private Path saveToPath = Paths.get ( FileSystemView.getFileSystemView ().getDefaultDirectory ().getPath () );

    // credentialsMap : a variable that will store credentials as a <Key, Value> pair
    private final HashMap< String, String > credentialsMap = new HashMap<> ();
    private final HashMap< String, String > saltsMap = new HashMap<> ();

    // xorKey : variable that will store the XOR-Key Byte representation used by the Server
    private byte[] xorKey = null;

    // serverSocket : a variable where a listener will be created so that a Client may connect
    private ServerSocket serverSocket = null;

    // clientSocket :
    // clientPrintWriter :
    // clientBufferedReader : variables that will be used by the Server to receive/send data to and from the Client
    private BufferedReader clientBufferedReader = null;
    private PrintWriter clientPrintWriter = null;
    private Socket clientSocket = null;

    // createServerThread :
    // responseThread :
    // fileThread : NotificationThread variables that will perform a specific function of the Server
    private NotificationThread createServerThread = null;
    private NotificationThread fileThread = null;
    private NotificationThread responseThread = null;

    // clientIsQuitting :
    // serverIsQuitting :
    // shouldRestartServer : variables useful for controlling/exiting NotificationThread's currently active
    private volatile boolean clientIsQuitting = false;
    private volatile boolean serverIsQuitting = false;
    private volatile boolean shouldRestartServer = false;


    /**
     * Creates the ServerPanel, adds a "Debug" credential for debugging, and updates "saveTextArea" to display the default
     * directory to save transferred files to
     */
    public ServerPanel ()
    {
        initComponents ();

        // Add Debug to Credentials Map
        credentialsMap.put ( "Debug", "" );

        // Set "Save To" directory of Server as System Default
        saveTextArea.setText ( saveToPath.toString () );
    }


    /**
     * ThreadCompletionListener interface method that notifies the Client whenever any NotificationThread has finished executing
     * @param thread : the Thread ( NotificationThread ) that has finished executing
     */
    @Override
    public void threadCompletedNotification ( Thread thread )
    {
        System.out.println ( "Server > threadCompletedNotification" );

        if ( thread.equals ( createServerThread ) )
        {
            createServerThread = null;

            if ( !serverIsQuitting )
            {
                // TODO -> Failed Authentication
                if ( shouldRestartServer )
                    stopButtonActionPerformed ();
                else
                    startResponseThread ();
            }
        }
        else if ( thread.equals ( responseThread ) )
        {
            responseThread = null;

            if ( clientIsQuitting )
                stopButtonActionPerformed ();
        }
        else if ( thread.equals ( fileThread ) )
        {
            fileThread = null;
        }
    }


    /**
     * This method will start the "Response Thread" which will listen for any incoming messages from the Client. If a
     * "CLIENT-QUIT" message is received, then it means that the Client is terminating the connection. Else, if a
     * "CLIENT-FILE" message is received, being the "File Thread" to process the file request. When a "File Thread" is
     * started, there is a tendency for data to be captured by this method, which is why when the "File Thread" is active,
     * ignore any message as it is file data.
     */
    private void startResponseThread ()
    {
        System.out.println ( "Server > startResponseThread" );

        responseThread = new NotificationThread ()
        {
            @Override
            public void notifyingRunnable ()
            {
                while ( !clientIsQuitting && !serverIsQuitting )
                {
                    try
                    {
                        /*
                         * Only accept messages when "File Thread" is Null
                         * If "File Thread" is not Null, then a File is being processed
                         */
                        if ( fileThread == null && clientBufferedReader.ready () )
                        {
                            String serverInput = clientBufferedReader.readLine ();

                            System.out.println ( "Server > Received Message \"" + serverInput + "\"" );

                            // Client has sent "Quit" message.
                            switch ( serverInput )
                            {
                                case "CLIENT-QUIT":
                                    /* Update Log and Set Flags */
                                    String clientHostAddress = clientSocket.getInetAddress ().getHostAddress ();
                                    logTextArea.append ( clientHostAddress + " has decided to close the connection" + "\n" );

                                    shouldRestartServer = true;
                                    clientIsQuitting = true;
                                    break;
                                // Client has sent "File" message
                                case "CLIENT-FILE":
                                    System.out.println ( "Server > Processing File Request" );
                                    startFileThread ();
                                    break;
                                default:
                                    System.out.println ( "Server > Message is not valid" );
                                    break;
                            }
                        }
                    }
                    catch ( IOException ioe )
                    {
                        ioe.printStackTrace ( System.err );
                    }
                }
            }
        };

        responseThread.setName ( "Server Response Thread" );
        responseThread.addListener ( this );
        responseThread.start ();
    }


    /**
     * This method will take the selected file, if one was selected, and will attempt to read the "Credentials" that fit
     * the format of username:password. If the file cannot be parsed, such as an Image, then an error will be thrown
     * and no "Credentials" will be read and the GUI will display an empty path.
     */
    private void credentialsButtonActionPerformed ()
    {
        // Find credentials file that will be used by the Server ( Should be Strings )
        JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getDefaultDirectory () );
        fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );

        if ( fileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
        {
            // Get path to credentials
            Path credentialsPath = fileChooser.getSelectedFile ().toPath ();

            // Try to open the file for reading and store every credential in said file
            try ( BufferedReader bufferedReader = Files.newBufferedReader ( credentialsPath ) )
            {
                for ( String line; ( line = bufferedReader.readLine () ) != null; )
                {
                    String[] credentialData = line.split ( "\\p{Space}*:\\p{Space}*" );

                    if ( credentialData.length == 2 )
                    {
                        credentialsMap.put ( credentialData[0], credentialData[1] );
                    }
                    else if ( credentialData.length == 3 )
                    {
                        saltsMap.put ( credentialData[0], credentialData[1] );
                        credentialsMap.put ( credentialData[0], credentialData[2] );
                    }
                }

                credentialsMap.put ( "Debug", "" );

                // Update GUI
                credentialTextArea.setText ( credentialsPath.toString () );
            }
            // Usually an encoding error. File is expected to contain Strings, and nothing else
            catch ( IOException ioe )
            {
                ioe.printStackTrace ( System.err );

                // Clear all credentials that failed to load
                credentialsMap.clear ();

                // Update GUI
                credentialTextArea.setText ( null );

                String message = "Credentials file could not be read";
                JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
            }
        }
    }


    /**
     * Clear the Credentials Map and nullifies the String presented in "credentialTextArea"
     */
    private void credentialClearButtonActionPerformed ()
    {
        credentialTextArea.setText ( null );
        credentialsMap.clear ();
        saltsMap.clear ();
        credentialsMap.put ( "Debug", "" );
    }


    /**
     * This method attempts to load the file specified by the JFileChooser so that it can be used as a XOR-Key, and eventually
     * passed into the XOR-Cipher for encryption Byte data. If a file can be used as a XOR-Key, then the "xorKey" and "xorTextArea"
     * variables will be updated to reflect these changes
     */
    private void xorButtonActionPerformed ()
    {
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

                System.out.println ( "Server > XOR Key Bytes : " + Arrays.toString ( xorKey ) );
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
        // TODO -> Clear XOR Text Area
        xorTextArea.setText ( null );
        xorKey = null;
    }


    /**
     * This method will ask the Server to specify a "Save To" directory, which is the directory where all the files received
     * from the Client will be placed. If a valid directory is selected, then the "saveTo" "saveTextArea" variables will
     * be updated to reflect changes
     */
    private void saveToButtonActionPerformed ()
    {
        JFileChooser saveToFileChooser = new JFileChooser ( saveToPath.toFile () );

        // Customize "Save To" JFileChooser
        saveToFileChooser.setFileSelectionMode ( JFileChooser.DIRECTORIES_ONLY );
        saveToFileChooser.setAcceptAllFileFilterUsed ( false );
        saveToFileChooser.setFileFilter ( new FileFilter ()
        {
            @Override
            public boolean accept ( File file ) { return file.isDirectory (); }

            @Override
            public String getDescription () { return "Directory"; }
        } );

        if ( saveToFileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
        {
            saveToPath = Paths.get ( saveToFileChooser.getSelectedFile ().getPath () );
            saveTextArea.setText ( saveToPath.toString () );
        }
    }


    /**
     * This method will process the file data sent by the Client. First, the "filename," "options," such as ASCII-Armoring
     * and Copy/Overwrite, and "size of file in Bytes" "and chunk size." Afterwards, the actual data Bytes will be processed,
     * which will be decrypted depending on the options sent over by the Client. For any mismatched hash, the Server will issue
     * a "HASH-FAILED" message to the Client, which will either retry sending the data or cancel the entire file transfer. If
     * the file Bytes are received in full, then the final file will be created signifying success
     */
    private void startFileThread ()
    {
        fileThread = new NotificationThread ()
        {
            @Override
            public void notifyingRunnable ()
            {
                try
                {
                    // Receive filename
                    String filename = clientBufferedReader.readLine ();

                    // Receive File-Options and Chunk-Size from Server
                    String[] fileInfo = clientBufferedReader.readLine ().split ( " && " );

                    // Receive options
                    String options = fileInfo[ 0 ];

                    // Receive size of file in Bytes
                    Long sizeOfFile = new Long ( fileInfo[ 1 ] );

                    boolean asciiArmoring = options.startsWith ( "A" );
                    String fileOption = options.substring ( options.length () - 1 );

                    // Receive Chunk Size
                    int chunkSize = Integer.parseInt ( fileInfo[ 2 ] );

                    System.out.println ( "Server > Received \"" + filename + "\" with options \"" + options + "\" of size \"" + sizeOfFile + "\" in bytes and chunk-size of \"" + chunkSize + "\"" );

                    byte[] dataBytes = new byte[ sizeOfFile.intValue () ];
                    int counter = 0;

                    String clientInput;

                    while ( !serverIsQuitting && ( clientInput = clientBufferedReader.readLine () ) != null )
                    {
                        if ( clientInput.equals ( "BYTES-DONE" ) )
                        {
                            System.out.println ( "Server > File > BYTES-DONE" );

                            break;
                        }
                        else if ( clientInput.equals ( "FILE-CANCELLED" ) )
                        {
                            System.out.println ( "Transfer of file \"" + filename + "\" was cancelled" );
                            logTextArea.append ( "Transfer of file \"" + filename + "\" was cancelled" + "\n" );

                            return;
                        }

                        // Split Hash and Data into Separate Strings ( [ 0 ] is Hash and [ 1 ] is Data )
                        String[] hashAndData = clientInput.split ( " && " );

                        // Get Client's Hash Value and Convert it to Bytes
                        String clientHashString = hashAndData[ 0 ];
                        byte[] clientHashBytes = Utility.stringToBytes ( clientHashString );

                        // Get Client's String of Data Bytes
                        String clientDataString = hashAndData[ 1 ];

                        byte[] tempDataBytes;

                        // Decode Data Bytes using ASCII-Armoring, if ASCII-Armoring was requested
                        if ( asciiArmoring )
                        {
                            System.out.println ( "Server > BASE64 > " + clientDataString );

                            tempDataBytes = MIME.base64Decoding ( clientDataString );
                        }
                        // If ASCII-Armoring is not request, process Data Bytes normally
                        else
                        {
                            tempDataBytes = Utility.stringToBytes ( clientDataString );
                        }

                        // Decrypt using Hash Bytes and Data Bytes using XOR Cipher, is available
                        if ( xorKey != null )
                        {
                            System.out.println ( "Server > XOR Data Bytes > " + clientDataString );
                            System.out.println ( "Server > XOR Hash Bytes > " + clientHashString );

                            tempDataBytes = XORCipher.decrypt ( tempDataBytes, xorKey );
                            clientHashBytes = XORCipher.decrypt ( clientHashBytes, xorKey );

                            clientDataString = Arrays.toString ( tempDataBytes );
                            clientHashString = Arrays.toString ( clientHashBytes );
                        }

                        // Output the Plain ( Decrypted ) Hash Bytes and Data Bytes
                        System.out.println ( "Server > Plain Data Bytes > " + clientDataString );
                        System.out.println ( "Server > Plain Hash Bytes > " + clientHashString );

                        // Compute Server-Side Hash Value ( Using Data Bytes ) and Compare With The Client-Hash-Value
                        // If matching, proceed, but if not, then inform Client to retry
                        Long clientHashValue = Utility.bytesToLong ( clientHashBytes );
                        Long serverHashValue = Utility.hash ( tempDataBytes );

                        System.out.println ( "Server > Client's Plain Hash Value > " + clientHashValue );
                        System.out.println ( "Server > Plain Hash Value > " + serverHashValue );

                        Boolean identicalHash = serverHashValue.equals ( clientHashValue );

                        if ( !identicalHash )
                        {
                            System.out.println ( "Server > Hash Comparison Result > FAILED" );

                            clientPrintWriter.println ( "SERVER-HASH-FAILED" );
                        }
                        else
                        {
                            System.out.println ( "Server > Hash Comparison Result > SUCCESS" );

                            clientPrintWriter.println ( "SERVER-HASH-SUCCESS" );

                            // Store Bytes with all other transferred Bytes
                            for ( byte byteValue : tempDataBytes )
                                if ( counter < dataBytes.length )
                                    dataBytes[ counter++ ] = byteValue;
                        }
                    }

                    if ( !serverIsQuitting )
                    {
                        Path filePath = Paths.get ( saveToPath.toString (), filename );

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

                                Files.write ( filePath, dataBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE );

                                logTextArea.append ( "\"" + filename + "\" was copied" + "\n" );

                                break;
                            }
                            case "O":
                            {
                                Files.write ( filePath, dataBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING );

                                logTextArea.append ( "\"" + filename + "\" was overwritten" + "\n" );

                                break;
                            }
                        }
                    }
                }
                catch ( IOException ioe )
                {
                    ioe.printStackTrace ( System.err );
                }
            }
        };

        fileThread.setName ( "Server File Thread" );
        fileThread.addListener ( this );
        fileThread.start ();
    }


    /**
     * This method opens a listener on a port and waits for a Client to connect. If a Client manages to connect, then
     * the Server attempts to authenticate the Client's credentials, which will send the results to the Client and close
     * the connection if authentication failed.
     */
    private void startButtonActionPerformed ()
    {
        System.out.println ( "Server > startButtonActionPerformed" );

        createServerThread = new NotificationThread ()
        {
            @Override
            public void notifyingRunnable ()
            {
                try
                {
                    // Parse Port. If not a valid Integer, Exception will be thrown
                    String portString = portTextField.getText ();
                    Integer port = Integer.parseInt ( portString );

                    // Update GUI
                    dynamicStatusLabel.setText ( "Listening" );
                    startButton.setEnabled ( false );

                    // Update Log to show Server is now Listening
                    logTextArea.append ( "Server is now listening" + "\n" );

                    // Initialize Socket/Stream Variables
                    serverSocket = new ServerSocket ( port );
                    clientSocket = serverSocket.accept ();

                    clientPrintWriter = new PrintWriter ( clientSocket.getOutputStream (), true );
                    clientBufferedReader = new BufferedReader ( new InputStreamReader ( clientSocket.getInputStream () ) );

                    // Try to Authenticate Client
                    String encryptedCredentials = clientBufferedReader.readLine ();
                    boolean validClient = false;
                    boolean plainTextAuthentication = plainTextRadioButton.isSelected ();

                    for ( Map.Entry< String, String > entry : credentialsMap.entrySet () )
                    {
                        String decryptedCredential = AES.decrypt ( encryptedCredentials, entry.getKey () );
                        if ( decryptedCredential == null )
                            continue;

                        String [] usernameAndPassword = decryptedCredential.split ( "\\p{Space}*:\\p{Space}*" );
                        if ( usernameAndPassword.length != 2 )
                            continue;

                        String decryptedUsername = usernameAndPassword[0];
                        String decryptedPassword = usernameAndPassword[1];

                        if ( plainTextAuthentication )
                        {
                            if (decryptedUsername.equals ( entry.getKey () ) && decryptedPassword.equals ( entry.getValue () )  )
                            {
                                validClient = true;
                                break;
                            }
                        }
                        else
                        {
                            try
                            {
                                MessageDigest sha256 = MessageDigest.getInstance ( "SHA-256" );

                                String salt = saltsMap.get ( entry.getKey () );
                                if ( salt == null )
                                    continue;

                                String hashedPassword = credentialsMap.get ( entry.getKey () );
                                if ( hashedPassword == null )
                                    continue;

                                byte[] saltedHashedPasswordBytes= sha256.digest ( ( salt + decryptedPassword ).getBytes ( StandardCharsets.UTF_8 ) );
                                String saltedHashedPasswordString = DatatypeConverter.printHexBinary ( saltedHashedPasswordBytes ).replaceFirst ( "0x", "" );

                                System.out.println ( "Server > Salt > " + salt );
                                System.out.println ( "Server > Hashed-Password > " + hashedPassword );
                                System.out.println ( "Server > Computed Hashed-Password > " + saltedHashedPasswordString );

                                if ( decryptedUsername.equals ( entry.getKey () ) && saltedHashedPasswordString.equals ( hashedPassword ) )
                                {
                                    validClient = true;
                                    break;
                                }
                            }
                            catch ( NoSuchAlgorithmException nae )
                            {
                                nae.printStackTrace ( System.err );
                            }
                        }
                    }

                    if ( validClient )
                    {
                        // TODO -> Send Success Response to Client
                        clientPrintWriter.println ( "AUTH-SUCCESS" );

                        // TODO -> Log Success Message to Log Text Area
                        String clientHostAddress = clientSocket.getInetAddress ().getHostAddress ();
                        logTextArea.append ( clientHostAddress + " has connected" + "\n" );

                        // TODO -> Update GUI with new Status and log the Client's connection
                        dynamicStatusLabel.setText ( "Running" );
                    }
                    else
                    {
                        // TODO -> Send Failed Response to Client
                        clientPrintWriter.println ( "AUTH-FAILED" );

                        // TODO -> Set Flag to restart Server
                        shouldRestartServer = true;

                        // TODO -> Log Failed Message to Log Text Area
                        String clientHostAddress = clientSocket.getInetAddress ().getHostAddress ();
                        logTextArea.append ( clientHostAddress + " tried to connect but failed authentication" + "\n" );
                    }
                }
                catch ( IOException ioe )
                {
                    // TODO -> Usually occurs when Port cannot be bound. E.g : Port 22 (SSH)
                    if ( ioe instanceof BindException )
                    {
                        String message = "Port \"" + portTextField.getText () + "\" is not usable";
                        JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
                    }

                    ioe.printStackTrace ( System.err );
                }
                catch ( NumberFormatException nfe )
                {
                    // TODO -> Triggered when a Port is not a valid Integer. E.g : Port "Hello, World!"
                    String message = "\"Port\" must be a number";
                    JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );

                    nfe.printStackTrace ( System.err );
                }
            }
        };

        createServerThread.setName ( "Server Creation Thread" );
        createServerThread.addListener ( this );
        createServerThread.start ();
    }


    /**
     * This method aims to disconnect the Server from the Client and performs and cleanup necessary, such as reverting the
     * GUI to it's original state, if necessary, or restoring internal variables to their original values.
     */
    private void stopButtonActionPerformed ()
    {
        System.out.println ( "Server > stopButtonActionPerformed" );

        /*
         * When Creating a Server Thread, if the Server presses cancel before the Client has the opportunity to
         * connect, then set the quit Flag ( serverIsQuitting ) and then terminate the Thread by closing
         * serverSocket. Afterwards, wait for Thread to completely finish before moving on ( join )
         */
        if ( createServerThread != null )
        {
            logTextArea.append ( "Server has terminated connection" + "\n" );

            serverIsQuitting = true;
            try
            {
                serverSocket.close ();
                createServerThread.join ();
            }
            catch ( IOException | InterruptedException exception )
            {
                exception.printStackTrace ( System.err );
            }
        }

        /*
         * When Response Thread is active and Server presses disconnect, then terminate Thread by setting
         * Flag ( serverIsQuitting ) which will break the Response Loop. Afterwards,  wait for Thread to
         * completely finish before moving on ( join )
         * When Response Thread is active and Server presses disconnect, then terminate Thread by setting
         * Flag ( serverIsQuitting ) which will break the Response Loop. Afterwards,  wait for Thread to
         * completely finish before moving on ( join )
         */
        if ( responseThread != null || fileThread != null )
        {
            serverIsQuitting = true;
            try
            {
                if ( responseThread != null )
                    responseThread.join ();
                else
                    fileThread.join ();
            }
            catch ( InterruptedException ie )
            {
                ie.printStackTrace ( System.err );
            }

            // Message Client that Server is Quitting
            if ( clientPrintWriter != null )
                clientPrintWriter.println ( "SERVER-QUIT" );

            logTextArea.append ( "Server decided to terminate connection" + "\n" );
        }

        // Close all Sockets needed by the Server
        try
        {
            if ( clientSocket != null )
            {
                System.out.println ( "Server > Closing Client Socket" );

                if ( !clientSocket.isClosed () )
                    clientSocket.close ();

                clientSocket = null;
                clientPrintWriter = null;
                clientBufferedReader = null;
            }

            if ( serverSocket != null )
            {
                System.out.println ( "Server > Closing Server Socket" );

                if ( !serverSocket.isClosed () )
                    serverSocket.close ();

                serverSocket = null;
            }
        }
        catch ( IOException ioe )
        {
            ioe.printStackTrace ();
        }

        // Restore GUI
        dynamicStatusLabel.setText ( "Stopped" );
        startButton.setEnabled ( true );

        if ( !shouldRestartServer )
        {
            xorKey = null;

            credentialTextArea.setText ( null );
            xorTextArea.setText ( null );
            saveTextArea.setText ( saveToPath.toString () );
            portTextField.setText ( "1492" );
        }

        logTextArea.append ( "Server was terminated " + "\n\n" );

        if ( serverIsQuitting )
            serverIsQuitting = false;

        if ( clientIsQuitting )
            clientIsQuitting = false;

        // TRestart Server, if requested
        if ( shouldRestartServer )
        {
            shouldRestartServer = false;
            startButton.doClick ();
        }
    }


    /**
     * Creates the ClientPanel and is generated with the help of JFormDesigner generate code
     */
    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY
        // GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Erik Huerta
		JLabel staticStatusLabel = new JLabel();
		JButton credentialButton = new JButton();
		JButton saveButton = new JButton();
		startButton = new JButton();
		JButton stopButton = new JButton();
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
		JLabel authenticationLabel = new JLabel();
		plainTextRadioButton = new JRadioButton();
		JRadioButton saltedHashedRadioButton = new JRadioButton();

		//======== this ========
		setBackground(Color.white);

		//---- staticStatusLabel ----
		staticStatusLabel.setText("Status");
		staticStatusLabel.setFont(staticStatusLabel.getFont().deriveFont(staticStatusLabel.getFont().getStyle() | Font.BOLD));

		//---- credentialButton ----
		credentialButton.setText("Credentials Path");
		credentialButton.setFont(credentialButton.getFont().deriveFont(credentialButton.getFont().getStyle() | Font.BOLD));
		credentialButton.setHorizontalAlignment(SwingConstants.LEADING);
		credentialButton.setToolTipText("Click");
		credentialButton.addActionListener(e -> credentialsButtonActionPerformed());

		//---- saveButton ----
		saveButton.setText("Save-Files-To Path");
		saveButton.setFont(saveButton.getFont().deriveFont(saveButton.getFont().getStyle() | Font.BOLD));
		saveButton.setToolTipText("Click");
		saveButton.setHorizontalAlignment(SwingConstants.LEADING);
		saveButton.addActionListener(e -> saveToButtonActionPerformed());

		//---- startButton ----
		startButton.setText("Start");
		startButton.setFont(startButton.getFont().deriveFont(startButton.getFont().getStyle() | Font.BOLD));
		startButton.addActionListener(e -> startButtonActionPerformed());

		//---- stopButton ----
		stopButton.setText("Stop");
		stopButton.setFont(stopButton.getFont().deriveFont(stopButton.getFont().getStyle() | Font.BOLD));
		stopButton.addActionListener(e -> stopButtonActionPerformed());

		//---- portLabel ----
		portLabel.setText("Port");
		portLabel.setFont(portLabel.getFont().deriveFont(portLabel.getFont().getStyle() | Font.BOLD));

		//---- portTextField ----
		portTextField.setText("1492");
		portTextField.setBorder(new MatteBorder(0, 0, 1, 0, new Color(204, 204, 204)));
		portTextField.setForeground(new Color(153, 0, 0));
		portTextField.setFont(portTextField.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		portTextField.setColumns(1);

		//---- dynamicStatusLabel ----
		dynamicStatusLabel.setText("Stopped");
		dynamicStatusLabel.setFont(dynamicStatusLabel.getFont().deriveFont(Font.BOLD|Font.ITALIC));
		dynamicStatusLabel.setForeground(new Color(153, 0, 0));

		//---- logLabel ----
		logLabel.setText("Log");
		logLabel.setFont(logLabel.getFont().deriveFont(logLabel.getFont().getStyle() | Font.BOLD));

		//======== logScrollPane ========
		{
			logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			logScrollPane.setBorder(null);
			logScrollPane.setAutoscrolls(true);

			//---- logTextArea ----
			logTextArea.setForeground(new Color(153, 0, 0));
			logTextArea.setRows(1);
			logTextArea.setEditable(false);
			logTextArea.setFont(logTextArea.getFont().deriveFont(Font.BOLD|Font.ITALIC));
			logTextArea.setBorder(null);
			logTextArea.setTabSize(0);
			logTextArea.setColumns(1);
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
			saveTextArea.setColumns(1);
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
		xorButton.setFont(xorButton.getFont().deriveFont(xorButton.getFont().getStyle() | Font.BOLD));
		xorButton.setToolTipText("Click");
		xorButton.setHorizontalAlignment(SwingConstants.LEADING);
		xorButton.addActionListener(e -> xorButtonActionPerformed());

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
			xorTextArea.setColumns(1);
			xorScrollPane.setViewportView(xorTextArea);
		}

		//---- credentialClearButton ----
		credentialClearButton.setText("Clear");
		credentialClearButton.setFont(credentialClearButton.getFont().deriveFont(credentialClearButton.getFont().getStyle() | Font.BOLD));
		credentialClearButton.setToolTipText("Click");
		credentialClearButton.addActionListener(e -> credentialClearButtonActionPerformed());

		//---- xorClearButton ----
		xorClearButton.setText("Clear");
		xorClearButton.setFont(xorClearButton.getFont().deriveFont(xorClearButton.getFont().getStyle() | Font.BOLD));
		xorClearButton.setToolTipText("Click");
		xorClearButton.addActionListener(e -> xorClearButtonActionPerformed());

		//---- authenticationLabel ----
		authenticationLabel.setText("Authentication");
		authenticationLabel.setFont(authenticationLabel.getFont().deriveFont(authenticationLabel.getFont().getStyle() | Font.BOLD));

		//---- plainTextRadioButton ----
		plainTextRadioButton.setText("Plain-Text");
		plainTextRadioButton.setFont(plainTextRadioButton.getFont().deriveFont(plainTextRadioButton.getFont().getStyle() | Font.BOLD));
		plainTextRadioButton.setSelected(true);
		plainTextRadioButton.setBackground(Color.white);

		//---- saltedHashedRadioButton ----
		saltedHashedRadioButton.setText("Salted-Hashed");
		saltedHashedRadioButton.setFont(saltedHashedRadioButton.getFont().deriveFont(saltedHashedRadioButton.getFont().getStyle() | Font.BOLD));
		saltedHashedRadioButton.setBackground(Color.white);

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addGroup(layout.createParallelGroup()
						.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup()
								.addComponent(portLabel)
								.addComponent(staticStatusLabel))
							.addContainerGap(334, Short.MAX_VALUE))
						.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup()
								.addComponent(logLabel)
								.addComponent(saltedHashedRadioButton)
								.addComponent(plainTextRadioButton)
								.addComponent(authenticationLabel)
								.addComponent(saveButton))
							.addGap(0, 203, Short.MAX_VALUE))
						.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
								.addComponent(logScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(portTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(saveScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addGroup(layout.createSequentialGroup()
									.addComponent(credentialButton)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 91, Short.MAX_VALUE)
									.addComponent(credentialClearButton))
								.addComponent(credentialScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
								.addComponent(dynamicStatusLabel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addComponent(stopButton, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addComponent(startButton, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
								.addGroup(layout.createSequentialGroup()
									.addComponent(xorButton)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 106, Short.MAX_VALUE)
									.addComponent(xorClearButton))
								.addComponent(xorScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE))
							.addGap(50, 50, 50))))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGap(25, 25, 25)
					.addComponent(staticStatusLabel)
					.addGap(18, 18, 18)
					.addComponent(dynamicStatusLabel)
					.addGap(18, 18, 18)
					.addComponent(authenticationLabel)
					.addGap(18, 18, 18)
					.addComponent(plainTextRadioButton)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(saltedHashedRadioButton)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(credentialClearButton)
						.addComponent(credentialButton))
					.addGap(18, 18, 18)
					.addComponent(credentialScrollPane, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(xorClearButton)
						.addComponent(xorButton))
					.addGap(18, 18, 18)
					.addComponent(xorScrollPane, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE)
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
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 108, Short.MAX_VALUE)
					.addComponent(startButton)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(stopButton)
					.addGap(25, 25, 25))
		);

		//---- authenticationGroup ----
		ButtonGroup authenticationGroup = new ButtonGroup();
		authenticationGroup.add(plainTextRadioButton);
		authenticationGroup.add(saltedHashedRadioButton);

		//---- bindings ----
		BindingGroup bindingGroup = new BindingGroup();
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, ELProperty.create("${!enabled}"),
			stopButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			saveButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			portTextField, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			saveTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			credentialTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			xorButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			credentialButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			xorTextArea, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			credentialClearButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			xorClearButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			plainTextRadioButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			startButton, BeanProperty.create("enabled"),
			saltedHashedRadioButton, BeanProperty.create("enabled")));
		bindingGroup.bind();
        // GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY
    // GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Erik Huerta
	private JButton startButton;
	private JTextField portTextField;
	private JLabel dynamicStatusLabel;
	private JTextArea logTextArea;
	private JTextArea saveTextArea;
	private JTextArea credentialTextArea;
	private JTextArea xorTextArea;
	private JRadioButton plainTextRadioButton;
    // GEN-END:variables
}
